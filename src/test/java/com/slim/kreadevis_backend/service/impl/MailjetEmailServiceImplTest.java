package com.slim.kreadevis_backend.service.impl;

import com.slim.kreadevis_backend.config.EmailProperties;
import com.slim.kreadevis_backend.dto.email.EmailAttachment;
import com.slim.kreadevis_backend.dto.email.EmailMessage;
import com.slim.kreadevis_backend.exception.EmailSendException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MailjetEmailServiceImplTest {

    private static final String API_URL = "https://api.mailjet.com/v3.1/send";

    private MockRestServiceServer server;
    private MailjetEmailServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(API_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        EmailProperties props = new EmailProperties(
                true,
                new EmailProperties.Mailjet(API_URL, "key", "secret"),
                new EmailProperties.Sender("noreply@test.local", "Kreadevis"));
        service = new MailjetEmailServiceImpl(builder.build(), props);
    }

    private EmailMessage sampleMessage() {
        return new EmailMessage("client@example.com", "Jane Doe", "Votre devis", "<html></html>",
                new EmailAttachment("quote-1.pdf", "application/pdf", "AQID"));
    }

    @Test
    void send_shouldPostMailjetPayload() {
        server.expect(requestTo(API_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.Messages[0].To[0].Email").value("client@example.com"))
                .andExpect(jsonPath("$.Messages[0].From.Email").value("noreply@test.local"))
                .andExpect(jsonPath("$.Messages[0].Attachments[0].ContentType").value("application/pdf"))
                .andRespond(withSuccess("{\"Messages\":[]}", org.springframework.http.MediaType.APPLICATION_JSON));

        service.send(sampleMessage());

        server.verify();
    }

    @Test
    void send_shouldThrowEmailSendException_onProviderError() {
        server.expect(requestTo(API_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> service.send(sampleMessage()))
                .isInstanceOf(EmailSendException.class);

        server.verify();
    }
}
