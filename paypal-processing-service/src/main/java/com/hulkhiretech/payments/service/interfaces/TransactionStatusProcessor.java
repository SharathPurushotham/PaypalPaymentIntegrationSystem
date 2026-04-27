package com.hulkhiretech.payments.service.interfaces;

import com.hulkhiretech.payments.dto.TransactionDto;

public interface TransactionStatusProcessor {
	
	public TransactionDto processStatus(TransactionDto tnxDto);
}
