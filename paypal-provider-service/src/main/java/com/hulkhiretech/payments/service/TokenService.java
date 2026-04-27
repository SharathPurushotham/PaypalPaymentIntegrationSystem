package com.hulkhiretech.payments.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.hulkhiretech.payments.constant.Constant;
import com.hulkhiretech.payments.http.HttpRequest;
import com.hulkhiretech.payments.http.HttpServiceEngine;
import com.hulkhiretech.payments.paypal.res.PaypalOAuthToken;
import com.hulkhiretech.payments.util.JsonUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {

	private final HttpServiceEngine httpServiceEngine;
	
	private final RedisService redisService;
	
	@Value("${paypal.client.id}")
	private String clientId;
	
	@Value("${paypal.client.secret}")
	private String clientSecret;
	
	@Value("${paypal.oauth.url}")
	private String oauthUrl;
	
	private final JsonUtil jsonUtil;
	
	public String getAccessToken() {
		log.info("Retrieving access token from TokenService");
		
		String accessToken=redisService.getValue(Constant.PAYPAL_ACCESS_TOKEN);
		log.info("Access token from redis: ",accessToken);
		
		if(accessToken!=null) {
			log.info("Returning cached access token");
			return accessToken;
		}
		log.info("No cached access token found,calling OAuth service");

		HttpHeaders headers = new HttpHeaders();

		headers.setBasicAuth(clientId, clientSecret);
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);


		MultiValueMap<String, String> formData=new LinkedMultiValueMap<>();
		formData.add(Constant.GRANT_TYPE, Constant.CLIENT_CREDENTIALS);

		HttpRequest httpRequest=new HttpRequest();
		httpRequest.setHttpMethod(HttpMethod.POST);
		httpRequest.setUrl(oauthUrl);
		httpRequest.setHttpHeaders(headers);
		httpRequest.setBody(formData);
		
		log.info("Prepared HttpRequest for OAuth call: {}", httpRequest);

		ResponseEntity<String> response=httpServiceEngine.makeHttpCall(httpRequest);
		log.info("Http response from HttpServiceEngine: {}",response);
		
		String tokenBody= response.getBody();
		log.info("Access token retrieved: {}",tokenBody);
		
		PaypalOAuthToken token =jsonUtil.fromJson(tokenBody, PaypalOAuthToken.class);
		accessToken=token.getAccessToken();
		
		//Cache the access token in Redis with expiry time
		redisService.setValueWithExpiry(Constant.PAYPAL_ACCESS_TOKEN, accessToken, token.getExpiresIn()- Constant.REDIS_ACCESS_TOKEN_DIFF);
		log.info("Access token cached for future use");
		
		return accessToken;
		
		
	}
}
