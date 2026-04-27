package com.hulkhiretech.payments.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.pojo.CreateOrderReq;
import com.hulkhiretech.payments.pojo.OrderResponse;
import com.hulkhiretech.payments.service.PaymentValidator;
import com.hulkhiretech.payments.service.TokenService;
import com.hulkhiretech.payments.service.helper.CaptureOrderHelper;
import com.hulkhiretech.payments.service.helper.CreateOrderHelper;
import com.hulkhiretech.payments.service.interfaces.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

	private final TokenService tokenService;
	
	private final HttpServiceEngine httpServiceEngine;
	
	@Value("${paypal.create.order.url}")
	private String createOrderUrl;
	
	private final CreateOrderHelper createOrderHelper;
	
	private final CaptureOrderHelper captureOrderHelper;
	
	private final PaymentValidator paymentValidator;
	

	@Override
	public OrderResponse createOrder(CreateOrderReq createOrderReq) {
		log.info("Creating order in PaymentServiceImpl||createOrderReq: {}",createOrderReq);
		
		paymentValidator.validateCrateOrder(createOrderReq);
		log.info("Create order request validated successfully");
		
		String accessToken =tokenService.getAccessToken();
		log.info("Access token retrieved: {}",accessToken);
		
		HttpRequest httpRequest = createOrderHelper.prepareCreateOrderHttpRequest(createOrderReq, accessToken);
		log.info("Prepared HttpRequest for OAuth call: {}",httpRequest);
		
		ResponseEntity<String> httpResponse=httpServiceEngine.makeHttpCall(httpRequest);
		log.info("Order created successfully, response: {}",httpResponse);
		
		OrderResponse orderResponse=createOrderHelper.handlePaypalResponse(httpResponse);
		log.info("Final OrderResponse to be returned: {}",orderResponse);
		return orderResponse;
	}

	@Override
	public OrderResponse captureOrder(String orderId) {
		log.info("Capturing order in PaymentServiceImpl||orderId: {}",orderId);
		
		String accessToken =tokenService.getAccessToken();
		log.info("Access token retrieved: {}",accessToken);
		
		HttpRequest httpRequest = captureOrderHelper.prepareCaptureOrderHttpRequest(orderId, accessToken);
		log.info("Prepared HttpRequest for Capture Order call: {}",httpRequest);
		
		ResponseEntity<String> httpResponse=httpServiceEngine.makeHttpCall(httpRequest);
		log.info("Order captured successfully, response: {}",httpResponse);
		
		OrderResponse orderResponse=captureOrderHelper.handlePaypalResponse(httpResponse);
		log.info("Final OrderResponse to be returned: {}",orderResponse);
	
		return orderResponse;
	}
}
