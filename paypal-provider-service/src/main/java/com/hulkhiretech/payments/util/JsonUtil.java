package com.hulkhiretech.payments.util;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class JsonUtil {
	private final ObjectMapper objectMapper;
	
	public String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (Exception e) {
			log.error("Error serializing object to JSON: {}", e.getMessage());
			throw new RuntimeException("JSON serialization error: " + e.getMessage());
		}
	}

	public <T> T fromJson(String json, Class<T> clazz) {
		try {
			return objectMapper.readValue(json, clazz);
		} catch (Exception e) {
			log.error("Error deserializing JSON: {}", e.getMessage());
			throw new RuntimeException("JSON deserialization error: " + e.getMessage());
		}
	}

}
