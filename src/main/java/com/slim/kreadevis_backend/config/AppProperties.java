package com.slim.kreadevis_backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(Company company, DocumentConfig document) {

    public record Company(String name, String address, String phone, String email, String siren) {}

    public record DocumentConfig(String logoPath, String outputDir) {}
}
