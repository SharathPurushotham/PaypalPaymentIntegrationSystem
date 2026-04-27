package com.hulkhiretech.payments.constant;

import lombok.Getter;

@Getter
public enum ErrorCodeEnum {
	GENERIC_ERROR("30000", "Something went wrong. Please try again later."),
	INVALID_CURRENCY_CODE("30001", "Currency code is required field and cannot be null/blank"),
	RETURN_URL_REQUIRED("30002", "Return URL is required field and cannot be null/blank"),
	INVALID_REQUEST("30003", "Invalid request. Request body cannot be null."),
	INVALID_AMOUNT("30004", "Amount must be greater than zero and cannot be null"),
	CANCEL_URL_REQUIRED("30005", "Cancel URL is required field and cannot be null/blank"),
	PAYPAL_SERVICE_UNAVAILABLE("30006", "Paypal service is currently unavailable. Please try again later."),
	PAYPAL_ERROR("30007", "<Error at Paypal>"),
	PAYPAL_UNKNOWN_ERROR("30008", "Unknown error occurred while processing Paypal response."),
	RESOURCE_NOT_FOUND("30009", "Ivalid URL. Please check and try again.");
	
	private final String errorCode;
	private final String errorMessage;
	
	ErrorCodeEnum(String errorCode, String errorMessage) {
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}
}
