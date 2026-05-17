package com.slim.kreadevis_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KreadevisBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(KreadevisBackendApplication.class, args);
	}

}
