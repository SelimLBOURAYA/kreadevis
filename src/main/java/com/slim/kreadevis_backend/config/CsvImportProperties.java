package com.slim.kreadevis_backend.config;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@ConfigurationProperties("app.csv.import")
@Validated
public record CsvImportProperties(
        @DefaultValue("10000") @Positive int maxRows,
        @NotEmpty List<String> allowedContentTypes
) {}
