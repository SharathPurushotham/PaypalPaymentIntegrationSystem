package com.hulkhiretech.payments.util;

import com.hulkhiretech.payments.paypal.res.error.PaypalErrorDetail;
import com.hulkhiretech.payments.paypal.res.error.PaypalErrorResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PaypalOrderUtil {
	private PaypalOrderUtil() {
	    // Private constructor to prevent instantiation
	}
	
	public static String getPaypalErrorSummary(PaypalErrorResponse res) {
		log.info("Generating PayPal error summary from response: {}", res);

	    StringBuilder summary = new StringBuilder();

	    if (res == null) {
	        return "";
	    }

	    // Top-level fields
	    appendifpresent(summary, res.getName());
	    appendifpresent(summary, res.getMessage());
	    appendifpresent(summary, res.getError());
	    appendifpresent(summary, res.getErrorDescription());

	    // Details (first item only)
	    if (res.getDetails() != null && !res.getDetails().isEmpty()) {
	        PaypalErrorDetail d = res.getDetails().get(0);

	        if (d != null) {
	            appendifpresent(summary, d.getField());
	            appendifpresent(summary, d.getIssue());
	            appendifpresent(summary, d.getDescription());
	        }
	    }
	    
	    log.info("Generated PayPal error summary: {}", summary.toString().trim());
	    return summary.toString().trim();
	}

	// Helper to append only non-null, non-empty values
	private static void appendifpresent(StringBuilder sb, String value) {
	    if (value != null && !value.isBlank()) {
	        if (sb.length() > 0) sb.append(" | ");
	        sb.append(value);
	    }
	}


}
