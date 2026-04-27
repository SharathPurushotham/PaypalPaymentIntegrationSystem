package com.hulkhiretech.payments.service.helper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.dto.TransactionDto;
import com.hulkhiretech.payments.exception.ProcessingServiceException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.paypalprovider.PPCreateOrderReq;
import com.hulkhiretech.payments.paypalprovider.PPErrorResponse;
import com.hulkhiretech.payments.paypalprovider.PPOrderResponse;
import com.hulkhiretech.payments.pojo.InitiatePaymentRequest;
import com.hulkhiretech.payments.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PPCreateOrderHelper {
	
	private final JsonUtil jsonUtil;
	
	@Value("${paypalprovider.createorder.url}")
	private String paypalProviderCreateOrderUrl;

	public HttpRequest prepareHttpRequest(String tnxReference, InitiatePaymentRequest initiatePaymentRequest, TransactionDto tnxDto) {
		log.info("Preparing HttpRequest for PayPal Create Order API..."
				+ "tnxReference: {} | initiatePaymentRequest: {} | tnxDto: {}",
				tnxReference, initiatePaymentRequest, tnxDto);
		
		//Prepare Headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		PPCreateOrderReq ppCreateOrderReq = new PPCreateOrderReq();
		ppCreateOrderReq.setAmount(tnxDto.getAmount().doubleValue());
		ppCreateOrderReq.setCurrencyCode(tnxDto.getCurrency());
		ppCreateOrderReq.setReturnUrl(initiatePaymentRequest.getSuccessUrl());
		ppCreateOrderReq.setCancelUrl(initiatePaymentRequest.getCancelUrl());
		
		String requestAsJson = jsonUtil.toJson(ppCreateOrderReq);
		
		//create HttpRequest
		HttpRequest httpRequest=new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl(paypalProviderCreateOrderUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(requestAsJson);
		
		log.info("Prepared HttpRequest for PayPal Create Order API: {}", httpRequest);
		return httpRequest;
	}
	
	/**
	 * Only success valid response is returned back.
	 * Else we throw exception with valid errorCode & errorMessage
	 * @param tnxReference
	 * @return PPOrderResponse
	 */

	public PPOrderResponse processResponse(String tnxReference, ResponseEntity<String> httpResponse) {
		log.info("Processing PayPal create order response...||httpResponse: {}", httpResponse);
		
		if(httpResponse.getStatusCode().equals(HttpStatus.OK)) {
			log.info("PayPal create order successful. Response Body: {}", httpResponse.getBody());
			
			PPOrderResponse responseObj = jsonUtil.fromJson(httpResponse.getBody(), PPOrderResponse.class);
			
			if(responseObj != null
					&& responseObj.getOrderId() != null
					&& responseObj.getPaypalStatus() != null
					&& responseObj.getRedirectUrl() != null) {
				log.info("Parsed PayPal Order Response successfully: {}", responseObj);
				
				return responseObj;
			} else {
				log.error("Failed to parse PayPal Order Response");
			}
		}
		
		if(httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
			log.error("Received error response from PayPalProvider service");
			
			PPErrorResponse errorResponse = jsonUtil.fromJson(httpResponse.getBody(), PPErrorResponse.class);
			
			throw new ProcessingServiceException(
					errorResponse.getErrorCode(),
					errorResponse.getErrorMessage(),
					HttpStatus.valueOf(httpResponse.getStatusCode().value()));
			
			
		}		
		log.error("Unexpected response from PayPalProvider service. "
				+ "httpResponse: {}", httpResponse);
		throw new ProcessingServiceException(
				ErrorCodeEnum.PAYPAL_PROVIDER_UNKNOWN_ERROR.getErrorCode(),
				ErrorCodeEnum.PAYPAL_PROVIDER_UNKNOWN_ERROR.getErrorMessage(),
				HttpStatus.BAD_GATEWAY); 
		
	}

}
