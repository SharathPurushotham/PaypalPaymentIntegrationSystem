package com.hulkhiretech.payments.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hulkhiretech.payments.pojo.CreatePaymentRequest;
import com.hulkhiretech.payments.pojo.InitiatePaymentRequest;
import com.hulkhiretech.payments.pojo.PaymentResponse;
import com.hulkhiretech.payments.service.interfaces.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/v1/payments")
@Slf4j
@RequiredArgsConstructor
public class PaymentController {
	
	private final PaymentService paymentService;
	
	@PostMapping()
	public PaymentResponse createPayment(@RequestBody CreatePaymentRequest createPaymentRequest) {
		log.info("Payment creation started ...|| createPaymentRequest: {}", createPaymentRequest);
		
		PaymentResponse response = paymentService.createPayment(createPaymentRequest);
		log.info("Payment creation response from service: {}", response);
		
		return response;
	}
	
	@PostMapping("/{tnxReference}/initiate")
	public PaymentResponse initiatePayment(@PathVariable String tnxReference,
			@RequestBody InitiatePaymentRequest initiatePaymentRequest) {
		log.info("Payment initiation started ...tnxReference: {}| initiatePaymentRequest: {}", tnxReference, initiatePaymentRequest);
		
		PaymentResponse response = paymentService.initiatePayment(tnxReference, initiatePaymentRequest);
		log.info("Payment initiation response from service: {}", response);
		return response;
	}
	
	@PostMapping("/{tnxReference}/capture")
	public String capturePayment(@PathVariable String tnxReference) {
		log.info("Payment capture started ...tnxReference: {}", tnxReference);
		
		PaymentResponse response = paymentService.capturePayment(tnxReference);
		log.info("Payment capture response from service: {}", response);
		
		return "Payment captured successfully -" + tnxReference;
	}
	

}
