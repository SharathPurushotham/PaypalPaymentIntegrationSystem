package com.hulkhiretech.payments.dao.interfaces;

import com.hulkhiretech.payments.entity.TransactionEntity;

public interface TransactionDao {
	public TransactionEntity createTransaction(TransactionEntity transaction);
	public TransactionEntity getTransactionByReference(String tnxReference);
	public void updateTransaction(TransactionEntity transaction);
}
