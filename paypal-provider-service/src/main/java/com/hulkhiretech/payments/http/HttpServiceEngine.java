package com.hulkhiretech.payments.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import com.hulkhiretech.payments.constant.ErrorCodeEnum;
import com.hulkhiretech.payments.exception.PaypalProviderException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class HttpServiceEngine {

	private final RestClient restClient;
	public ResponseEntity<String> makeHttpCall(HttpRequest httpRequest) {
		log.info("Making Http call in HttpServiceEngine");

		try {
			ResponseEntity<String> httpResponse=restClient
					.method(httpRequest.getHttpMethod())
					.uri(httpRequest.getUrl())
					.headers(restClientHeader -> restClientHeader.addAll(httpRequest.getHttpHeaders()))
					.body(httpRequest.getBody())
					.retrieve()
					.toEntity(String.class);
			log.info("HTTP call completed httpResponse:{}",httpResponse);

			return httpResponse;
		}catch(HttpClientErrorException | HttpServerErrorException e) {
			//valid error response from server
			log.error("HTTP error response received: {}", e.getMessage(),e);
			
			//if error is gateway timeout or service unavailable,throw PaypalProviderException
			if(e.getStatusCode()==HttpStatus.GATEWAY_TIMEOUT || e.getStatusCode()==HttpStatus.SERVICE_UNAVAILABLE) {
				log.error("Paypal service is unavailable or gateway timeout");
				throw new PaypalProviderException(
						ErrorCodeEnum.PAYPAL_SERVICE_UNAVAILABLE.getErrorCode(),
						ErrorCodeEnum.PAYPAL_SERVICE_UNAVAILABLE.getErrorMessage(),
						HttpStatus.SERVICE_UNAVAILABLE);
			}
			
			//return responseEntity with error details
			String errorResponse = e.getResponseBodyAsString();
			return ResponseEntity
					.status(e.getStatusCode())
					.body(errorResponse);
			
		}catch (Exception e) {//No response case
			log.error("Exception while preparing form data: {}", e.getMessage(),e);
			
			throw new PaypalProviderException(
					ErrorCodeEnum.PAYPAL_SERVICE_UNAVAILABLE.getErrorCode(),
					ErrorCodeEnum.PAYPAL_SERVICE_UNAVAILABLE.getErrorMessage(),
					HttpStatus.SERVICE_UNAVAILABLE);
		}
	}
}
