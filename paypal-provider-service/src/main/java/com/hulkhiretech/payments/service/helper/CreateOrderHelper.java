package com.hulkhiretech.payments.service.helper;

import java.util.Collections;
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
import com.hulkhiretech.payments.paypal.req.Amount;
import com.hulkhiretech.payments.paypal.req.ExperienceContext;
import com.hulkhiretech.payments.paypal.req.OrderRequest;
import com.hulkhiretech.payments.paypal.req.PaymentSource;
import com.hulkhiretech.payments.paypal.req.Paypal;
import com.hulkhiretech.payments.paypal.req.PurchaseUnit;
import com.hulkhiretech.payments.paypal.res.PaypalLink;
import com.hulkhiretech.payments.paypal.res.PaypalOrder;
import com.hulkhiretech.payments.paypal.res.error.PaypalErrorResponse;
import com.hulkhiretech.payments.pojo.CreateOrderReq;
import com.hulkhiretech.payments.pojo.OrderResponse;
import com.hulkhiretech.payments.util.JsonUtil;
import com.hulkhiretech.payments.util.PaypalOrderUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateOrderHelper {
	
    private final JsonUtil jsonUtil;
	
	@Value("${paypal.create.order.url}")
	String createOrderUrl;
	
	public HttpRequest prepareCreateOrderHttpRequest(CreateOrderReq createOrderReq, String accessToken) {
		HttpHeaders headers = prepareHeader(accessToken);
		
		String requestAsJson = prepareReqBodyAsJson(createOrderReq);
        
        //create HttpRequest
		HttpRequest httpRequest=new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		
		httpRequest.setUrl(createOrderUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(requestAsJson);
		return httpRequest;
	}

	private String prepareReqBodyAsJson(CreateOrderReq createOrderReq) {
		// Create Amount
        Amount amount = new Amount();
        amount.setCurrencyCode(createOrderReq.getCurrencyCode());
        //Do comments for testing purpose only
        
        String amtString=String.format(Constant.TWO_DECIMAL_FORMAT, createOrderReq.getAmount());
        amount.setValue(amtString);

        // Create Purchase Unit
        PurchaseUnit unit = new PurchaseUnit();
        unit.setAmount(amount);

        // Experience Context
        ExperienceContext ctx = new ExperienceContext();
        ctx.setPaymentMethodPreference(Constant.IMMEDIATE_PAYMENT_REQUIRED);
        ctx.setLandingPage(Constant.LANDINGPAGE_LOGIN);
        ctx.setShippingPreference(Constant.SHIPPING_PREF_NO_SHIPPING);
        ctx.setUserAction(Constant.USER_ACTION_PAY_NOW);
        ctx.setReturnUrl(createOrderReq.getReturnUrl());
        ctx.setCancelUrl(createOrderReq.getCancelUrl());

        // Paypal
        Paypal paypal = new Paypal();
        paypal.setExperienceContext(ctx);

        // Payment Source
        PaymentSource ps = new PaymentSource();
        ps.setPaypal(paypal);

        // Order Request
        OrderRequest order = new OrderRequest();
        order.setIntent(Constant.INTENT_CAPTURE);
        order.setPurchaseUnits(Collections.singletonList(unit));
        order.setPaymentSource(ps);
        log.info("OrderRequest object created: {}",order);

        // Convert to JSON
        String requestAsJson= jsonUtil.toJson(order);
        log.info("OrderRequest converted to JSON: {}",requestAsJson);
		return requestAsJson;
	}

	private HttpHeaders prepareHeader(String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		String uuid=UUID.randomUUID().toString();
		log.info("GEnerated UUID for PayPal-Request-Id: {}",uuid);
		headers.add(Constant.PAYPAL_REQUEST_ID, uuid);
		return headers;
	}
	
	public OrderResponse toOrderResponse(PaypalOrder paypalOrder) {
		log.info("Converting PaypalOrder to OrderResponse: {}", paypalOrder);
		
		OrderResponse response = new OrderResponse();
	    response.setOrderId(paypalOrder.getId());
	    response.setPaypalStatus(paypalOrder.getStatus());

	    String redirectLink= paypalOrder.getLinks().stream()
	            .filter(l -> "payer-action".equalsIgnoreCase(l.getRel()))
	            .findFirst()
	            .map(PaypalLink::getHref)
	            .orElse(null);

	    response.setRedirectUrl(redirectLink);

	    return response;
	}
	
	public OrderResponse handlePaypalResponse(ResponseEntity<String> httpResponse) {
		log.info("Handling Paypal response in PaymentServiceImpl");
		
		if(httpResponse.getStatusCode().is2xxSuccessful()) {
			PaypalOrder paypalOrder = jsonUtil.fromJson(httpResponse.getBody(), PaypalOrder.class);
			log.info("Mapped response body to PaypalOrder: {}",paypalOrder);
			
			OrderResponse orderResponse = toOrderResponse(paypalOrder);
			log.info("Converted OrderResponse: {}",orderResponse);
			if(orderResponse!=null
					&& orderResponse.getOrderId()!=null
					&& !orderResponse.getOrderId().isEmpty()
					&& orderResponse.getPaypalStatus()!=null
					&& orderResponse.getPaypalStatus().equalsIgnoreCase(Constant.PAYER_ACTION_REQUIRED)
					&& orderResponse.getRedirectUrl()!=null
					&& !orderResponse.getRedirectUrl().isEmpty()) {
				log.info("Order created succesfully with PAYER_ACTION_REQUIRED Status");
				return orderResponse;	
			}
			log.error("Order creation failed or incomplete response received."+"orderResponse: {}",orderResponse);
		}
		
		//if 4xx or 5xx then proper error response from Paypal
		if(httpResponse.getStatusCode().is4xxClientError() || httpResponse.getStatusCode().is5xxServerError()) {
			log.info("Received 4xx, 5xx error response from Paypal service");
			
			PaypalErrorResponse paypalErrorRes = jsonUtil.fromJson(httpResponse.getBody(), PaypalErrorResponse.class);
			log.info("Mapped response body to PaypalOrder: {}",paypalErrorRes);
			
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
