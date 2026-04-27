package com.hulkhiretech.payments.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
	private final RedisTemplate<String, String> redisTemplate;
	private final ValueOperations<String, String> valueOperation;
	
	public RedisService(RedisTemplate<String, String> redisTemplate) {
		this.redisTemplate=redisTemplate;
		this.valueOperation=redisTemplate.opsForValue();
	}
	
	public void setValueWithExpiry(String key, String value, long timeoutInSecs) {
		valueOperation.set(key,value,timeoutInSecs,TimeUnit.SECONDS);
	}
	public String getValue(String key) {
		return valueOperation.get(key);
	}
}
