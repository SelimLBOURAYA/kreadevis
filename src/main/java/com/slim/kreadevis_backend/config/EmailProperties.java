package com.slim.kreadevis_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.email")
public record EmailProperties(boolean enabled, Mailjet mailjet, Sender sender) {

    public record Mailjet(String apiUrl, String apiKey, String apiSecret) {}

    public record Sender(String email, String name) {}
}
