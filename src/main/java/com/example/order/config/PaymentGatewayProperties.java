package com.example.order.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "payment.gateway")
public class PaymentGatewayProperties {
    private String url;
    private int timeoutMs;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
}