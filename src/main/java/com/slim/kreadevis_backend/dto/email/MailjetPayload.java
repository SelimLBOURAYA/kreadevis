package com.slim.kreadevis_backend.dto.email;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Payload for the Mailjet Send API v3.1 ({@code POST /v3.1/send}).
 * Field names follow Mailjet's PascalCase contract.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MailjetPayload(@JsonProperty("Messages") List<Message> messages) {

    public record Message(
            @JsonProperty("From") Contact from,
            @JsonProperty("To") List<Contact> to,
            @JsonProperty("Subject") String subject,
            @JsonProperty("HTMLPart") String htmlPart,
            @JsonProperty("Attachments") List<Attachment> attachments
    ) {}

    public record Contact(
            @JsonProperty("Email") String email,
            @JsonProperty("Name") String name
    ) {}

    public record Attachment(
            @JsonProperty("ContentType") String contentType,
            @JsonProperty("Filename") String filename,
            @JsonProperty("Base64Content") String base64Content
    ) {}
}
