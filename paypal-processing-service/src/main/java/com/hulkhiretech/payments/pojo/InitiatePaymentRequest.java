package com.hulkhiretech.payments.pojo;

import lombok.Data;

@Data
public class InitiatePaymentRequest {

	private String successUrl;
	private String cancelUrl;
}
