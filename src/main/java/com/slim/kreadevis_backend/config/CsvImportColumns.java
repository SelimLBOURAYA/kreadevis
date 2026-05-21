package com.slim.kreadevis_backend.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.csv.import.columns")
@Validated
public record CsvImportColumns(
        @DefaultValue("label")          @NotBlank String label,
        @DefaultValue("description")    @NotBlank String description,
        @DefaultValue("stockQuantity")  @NotBlank String stockQuantity,
        @DefaultValue("unitPrice")      @NotBlank String unitPrice,
        @DefaultValue("vatRate")        @NotBlank String vatRate,
        @DefaultValue("referenceCode")  @NotBlank String referenceCode
) {}
