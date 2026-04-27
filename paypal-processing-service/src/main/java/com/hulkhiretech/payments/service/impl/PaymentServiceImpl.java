package com.hulkhiretech.payments.service.impl;

import java.util.UUID;

import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDto;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.exception.ProcessingServiceException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.paypalprovider.PPOrderResponse;
import com.hulkhiretech.payments.pojo.CreatePaymentRequest;
import com.hulkhiretech.payments.pojo.InitiatePaymentRequest;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.service.PaymentStatusService;
import com.hulkhiretech.payments.service.helper.PPCaptureOrderHelper;
import com.hulkhiretech.payments.service.helper.PPCreateOrderHelper;
import com.hulkhiretech.payments.service.interfaces.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	
	private final PPCreateOrderHelper ppCreateOrderHelper;
	
	private final PPCaptureOrderHelper ppCaptureOrderHelper;
	
	private final HttpServiceEngine httpServiceEngine;
	
	private final PaymentStatusService paymentStatusService;
	
	private final ModelMapper modelMapper;
	
	private final TransactionDao transactionDao;

	@Override
	public PaymentResponse createPayment(CreatePaymentRequest createPaymentRequest) {
		log.info("Creating payment in PaymentServiceImpl...|| createPaymentRequest: {}", createPaymentRequest);
		
		TransactionDto tnxDto = modelMapper.map(createPaymentRequest, TransactionDto.class);
		log.info("Mapped CreatePaymentRequest to TransactionDto: {}", tnxDto);
		
		int tnxStatusId= 1;//CREATED
		String tnxreference= generateUniqueTnxReference();
		
		tnxDto.setTxnStatusId(tnxStatusId);
		tnxDto.setTxnReference(tnxreference);
		
		TransactionDto response = paymentStatusService.processPayment(tnxDto);
		log.info("Response from TransactionStatusProcessor: {}", response);
		
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setTnxReference(response.getTxnReference());
		paymentResponse.setTnxStatusId(response.getTxnStatusId());
		log.info("Prepared PaymentResponse: {}", paymentResponse);
		
		return paymentResponse;
	}

	private String generateUniqueTnxReference() {
		return UUID.randomUUID().toString();
	}

	@Override
	public PaymentResponse initiatePayment(String tnxReference, InitiatePaymentRequest initiatePaymentRequest) {
		log.info("Initiating payment in PaymentServiceImpl..."
				+ "tnxReference: {}|initiatePaymentRequest: {}", tnxReference, initiatePaymentRequest);
		
		TransactionEntity tnxEntity = transactionDao.getTransactionByReference(tnxReference);
		log.info("Fetched TransactionEntity from DB {}: {}", tnxEntity);
		
		TransactionDto tnxDto = modelMapper.map(tnxEntity, TransactionDto.class);
		
		tnxDto.setTxnStatusId(2); //INITIATED
		TransactionDto response = paymentStatusService.processPayment(tnxDto);
		log.info("Response from TransactionStatusProcessor after INITIATED status: {}", response);
		
		HttpRequest httpReq = ppCreateOrderHelper.prepareHttpRequest(tnxReference, initiatePaymentRequest,tnxDto);
		log.info("HttpRequest prepared for PayPalProvider Create Order API: {}", httpReq);
		
		PPOrderResponse ppOrderResponse=null;
		try {
			ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpReq);
			log.info("HttpResponse received from PayPalProvider Create Order API: {}", httpResponse);
			
			ppOrderResponse = ppCreateOrderHelper.processResponse(tnxReference, httpResponse);
			log.info("Processed Paypal Order Response: {}", ppOrderResponse);
		} catch (ProcessingServiceException e) {
			log.error("Error occured while making HTTP call to PayPalProvider: ",e );
			
			tnxDto.setTxnStatusId(6);//Failed
			tnxDto.setErrorCode(e.getErrorCode());
			tnxDto.setErrorMessage(e.getErrorMessage());
			
			paymentStatusService.processPayment(tnxDto);
			log.info("Updated transaction status to FAILED for txnReference: {}", tnxReference);
			throw e;
		}
		
		tnxDto.setTxnStatusId(3); //PENDING
		tnxDto.setProviderReference(ppOrderResponse.getOrderId());
		response = paymentStatusService.processPayment(tnxDto);
		log.info("Response from TransactionStatusProcessor after PENDING status: {}", response);
		
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setTnxReference(tnxDto.getTxnReference());
		paymentResponse.setTnxStatusId(tnxDto.getTxnStatusId());
		paymentResponse.setProviderReference(ppOrderResponse.getOrderId());
		paymentResponse.setRedirectUrl(ppOrderResponse.getRedirectUrl());
		
		log.info("Final PaymentResponse after Initiate Payment: {}", paymentResponse);
		
		return paymentResponse;
	}

	@Override
	public PaymentResponse capturePayment(String tnxReference) {
		log.info("Capturing payment in PaymentServiceImpl... "
				+ "txnReference: {}", tnxReference);
		
		TransactionEntity tnxEntity = transactionDao.getTransactionByReference(tnxReference);
		log.info("Fetched TransactionEntity from DB: {}", tnxEntity);
		
		// use modelMapper to convert Entity to DTO
		TransactionDto txnDto = modelMapper.map(
				tnxEntity, TransactionDto.class);
		log.info("Mapped TransactionEntity to TransactionDto: {}", txnDto);
		
		// update txn status to APPROVED
		txnDto.setTxnStatusId(4);
		txnDto = paymentStatusService.processPayment(txnDto);
		log.info("Response from PaymentStatusService after updating status to APPROVED: {}", txnDto);
		
		HttpRequest httpReq = ppCaptureOrderHelper.prepareHttpRequest(
				tnxReference, txnDto);
		
		PPOrderResponse ppCaptureOrderSuccessResponse = null;
		try {
			ResponseEntity<String> httpResponse = httpServiceEngine.makeHttpCall(httpReq);
			log.info("HTTP response from HttpServiceEngine: {}", httpResponse);
			
			ppCaptureOrderSuccessResponse = ppCaptureOrderHelper.processResponse(httpResponse);
			log.info("Processed PayPal order response: {}", ppCaptureOrderSuccessResponse);
		} catch (Exception e) {
			log.error("Error occurred while making captureOrder HTTP call to PayPalProvider: ", e);
			
			// Note, dont change the status to FAILED since user already APPROVED.
			// Let reconciliation job handle such cases.
			// In case reconciliation also resolved it as failed, 
			//then manually back-office can handle this payment..
			// just throw error back
			
			throw e; // rethrow the exception after updating status
		} 
		
		// update txn status to SUCCESS
		txnDto.setTxnStatusId(5); 
		txnDto = paymentStatusService.processPayment(txnDto);
		
		PaymentResponse paymentResponse = new PaymentResponse();
		paymentResponse.setTnxReference(txnDto.getTxnReference());
		paymentResponse.setTnxStatusId(txnDto.getTxnStatusId());

		log.info("Final PaymentResponse to be returned: {}", paymentResponse);
		
		return paymentResponse;
	}


}
