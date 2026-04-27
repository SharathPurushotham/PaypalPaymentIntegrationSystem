package com.hulkhiretech.payments.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
	
	private String tnxReference;
	private int tnxStatusId;
	
	private String redirectUrl;
	private String providerReference;
	
}
