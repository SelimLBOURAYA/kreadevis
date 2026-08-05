package com.slim.kreadevis_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * RestClient dedicated to the Mailjet v3.1 Send API.
 * Base URL and Basic Auth (api-key:api-secret) come from {@link EmailProperties}.
 */
@Configuration
public class MailjetClientConfig {

    @Bean
    public RestClient mailjetRestClient(EmailProperties emailProperties) {
        EmailProperties.Mailjet mailjet = emailProperties.mailjet();
        return RestClient.builder()
                .baseUrl(mailjet.apiUrl())
                .defaultHeaders(headers ->
                        headers.setBasicAuth(mailjet.apiKey(), mailjet.apiSecret()))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
