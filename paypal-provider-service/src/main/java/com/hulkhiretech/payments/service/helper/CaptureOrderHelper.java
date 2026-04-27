package com.hulkhiretech.payments.service.helper;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.PaypalProviderException;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.paypal.res.PaypalOrder;
import com.hulkhiretech.payments.paypal.res.error.PaypalErrorResponse;
import com.hulkhiretech.payments.pojo.OrderResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import com.hulkhiretech.payments.util.PaypalOrderUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CaptureOrderHelper {
	
	private static final String ORDER_ID_REF = "{orderId}";
	
	private final JsonUtil jsonUtil;
	
	@Value("${paypal.capture.order.url}")
	private String captureOrderUrlTemplate;

	public HttpRequest prepareCaptureOrderHttpRequest(String orderId, String accessToken) {
		log.info("Preparing HttpRequest for Capture Order. orderId: {}",orderId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		String uuid=UUID.randomUUID().toString();
		log.info("GEnerated UUID for PayPal-Request-Id: {}",uuid);
		headers.add(Constant.PAYPAL_REQUEST_ID, uuid);
		
		String requestAsJson="";
		
		String CreateOrderUrl= captureOrderUrlTemplate.replace(ORDER_ID_REF, orderId);
		log.info("Prepared Capture Order URL: {}",CreateOrderUrl);
		
		//create HttpRequest
		HttpRequest httpRequest=new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl(CreateOrderUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(requestAsJson);
		
		log.info("Prepared HttpRequest for Capture Order: {}",httpRequest);
		
		return httpRequest;
	}
	
	public OrderResponse toOrderResponse(PaypalOrder paypalOrder) {
		log.info("Converting PaypalOrder to OrderResponse: {}", paypalOrder);
		
		OrderResponse response = new OrderResponse();
	    response.setOrderId(paypalOrder.getId());
	    response.setPaypalStatus(paypalOrder.getStatus());

	    log.info("Converted PaypalOrder to OrderResponse: {}", response);
	    return response;
	}

	public OrderResponse handlePaypalResponse(ResponseEntity<String> httpResponse) {
		log.info("Handling Paypal response in PaymentServiceImpl");
		
		if(httpResponse.getStatusCode().is2xxSuccessful()) {
			PaypalOrder paypalOrder = jsonUtil.fromJson(httpResponse.getBody(), PaypalOrder.class);
			log.info("Converted response body to PaypalOrder: {}",paypalOrder);
			
			OrderResponse orderResponse = toOrderResponse(paypalOrder);
			log.info("Converted OrderResponse: {}",orderResponse);
			if(orderResponse!=null
					&& orderResponse.getOrderId()!=null
					&& !orderResponse.getOrderId().isEmpty()
					&& orderResponse.getPaypalStatus()!=null
					&& !orderResponse.getPaypalStatus().isEmpty()) {
				log.info("Order capture successful. orderResponse: {}",orderResponse);
				return orderResponse;	
			}
			log.error("Order capture failed or incomplete details received ."+"orderResponse: {}",orderResponse);
		}
		
		//if 4xx or 5xx then proper error response from Paypal
		if(httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
			log.info("Received 4xx, 5xx error response from Paypal service");
			
			PaypalErrorResponse paypalErrorRes = jsonUtil.fromJson(httpResponse.getBody(), PaypalErrorResponse.class);
			log.info("Converted response body to PaypalOrder: {}",paypalErrorRes);
			
			String errorCode= ErrorCodeEnum.PAYPAL_ERROR.getErrorCode();
			String errorMessage= PaypalOrderUtil.getPaypalErrorSummary(paypalErrorRes);
			log.info("Generated Paypal error summary: {}",errorMessage);
			
			throw new PaypalProviderException(
					errorCode,
					errorMessage,
					HttpStatus.valueOf(httpResponse.getStatusCode().value()));
		}
		log.error("Unexpected response from Paypal service. "+"httpResponse: {}",httpResponse);
		
		throw new PaypalProviderException(
				ErrorCodeEnum.PAYPAL_UNKNOWN_ERROR.getErrorCode(),
				ErrorCodeEnum.PAYPAL_UNKNOWN_ERROR.getErrorMessage(),
				HttpStatus.BAD_GATEWAY);
	}

}
