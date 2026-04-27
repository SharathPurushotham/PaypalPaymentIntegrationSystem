package com.hulkhiretech.payments.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.dto.TransactionDto;
import com.hulkhiretech.payments.exception.ProcessingServiceException;
import com.hulkhiretech.payments.service.factory.PaymentStatusFactory;
import com.hulkhiretech.payments.service.interfaces.TransactionStatusProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentStatusService {
	
	private final PaymentStatusFactory paymentStatusFactory;
	
	public TransactionDto processPayment(TransactionDto tnxDto) {
		log.info("Processing payment status with ID: {}", tnxDto);
		
		int statusId = tnxDto.getTxnStatusId();
		TransactionStatusProcessor processor = paymentStatusFactory.getStatusProcessor(statusId);
		
		if(processor == null) {
			log.error("No TransactionStatusProcessor found for statusId: {}", statusId);
			throw new ProcessingServiceException(ErrorCodeEnum.No_STATUS_PROCESSOR_FOUND.getErrorCode(),
					                     ErrorCodeEnum.No_STATUS_PROCESSOR_FOUND.getErrorMessage(),
					                     HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		TransactionDto response = processor.processStatus(tnxDto);
		log.info("Payment status processed: {}", response);
		
		return response;
	}
}
