package com.hulkhiretech.payments.service.impl.statusprocessor;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import com.hulkhiretech.payments.dao.interfaces.TransactionDao;
import com.hulkhiretech.payments.dto.TransactionDto;
import com.hulkhiretech.payments.entity.TransactionEntity;
import com.hulkhiretech.payments.service.interfaces.TransactionStatusProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreatedStatusProcessor implements TransactionStatusProcessor {
	
	private final ModelMapper modelMapper;
	
	private final TransactionDao transactionDao;

	@Override
	public TransactionDto processStatus(TransactionDto tnxDto) {
		log.info("Processing 'Created' status for tnxDto: {}", tnxDto);
		
		TransactionEntity tnxEntity = modelMapper.map(tnxDto, TransactionEntity.class);
		log.info("mapped TransactionEntity: {}", tnxEntity);
		
		TransactionEntity responseEntity = transactionDao.createTransaction(tnxEntity);
		log.info("Transaction created in DB: {}", responseEntity);
		
		tnxDto.setId(responseEntity.getId());
		log.info("Updated TransactionDto with DB ID: {}", tnxDto);
		
		return tnxDto;
	}

}
