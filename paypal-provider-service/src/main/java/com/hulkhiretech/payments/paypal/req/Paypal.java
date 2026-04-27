package com.hulkhiretech.payments.paypal.req;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Paypal {

    @JsonProperty("experience_context")
    private ExperienceContext experienceContext;
}