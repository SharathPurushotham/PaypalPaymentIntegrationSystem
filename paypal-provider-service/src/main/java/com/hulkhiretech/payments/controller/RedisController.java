package com.hulkhiretech.payments.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hulkhiretech.payments.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/redis")
public class RedisController {
	private final RedisService redisService;
	
	@PostMapping("/value/{key}/expiry/{timeoutInSecs}")
	public void setValueWithExpiry(@PathVariable String key, @RequestBody String value, @PathVariable long timeoutInSecs) {
		redisService.setValueWithExpiry(key, value, timeoutInSecs);
	}
	@GetMapping("/value/{key}")
	public String getValue(@PathVariable String key) {
		return redisService.getValue(key);
	}
}
