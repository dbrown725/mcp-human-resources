package com.megacorp.humanresources.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an email message with its key attributes.
 */
public class EmailMessage {
    private String messageId;
    private String from;
    private List<String> to;
    private List<String> cc;
    private String subject;
    private String body;
    private LocalDateTime sentDate;
    private LocalDateTime receivedDate;
    private boolean isRead;
    private int size;
    private List<String> attachmentNames;

    public EmailMessage() {
    }

    public EmailMessage(String messageId, String from, List<String> to, List<String> cc, 
                        String subject, String body, LocalDateTime sentDate, 
                        LocalDateTime receivedDate, boolean isRead, int size, 
                        List<String> attachmentNames) {
        this.messageId = messageId;
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.subject = subject;
        this.body = body;
        this.sentDate = sentDate;
        this.receivedDate = receivedDate;
        this.isRead = isRead;
        this.size = size;
        this.attachmentNames = attachmentNames;
    }

    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public List<String> getTo() {
        return to;
    }

    public void setTo(List<String> to) {
        this.to = to;
    }

    public List<String> getCc() {
        return cc;
    }

    public void setCc(List<String> cc) {
        this.cc = cc;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public LocalDateTime getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(LocalDateTime receivedDate) {
        this.receivedDate = receivedDate;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<String> getAttachmentNames() {
        return attachmentNames;
    }

    public void setAttachmentNames(List<String> attachmentNames) {
        this.attachmentNames = attachmentNames;
    }

    @Override
    public String toString() {
        return "EmailMessage{" +
                "messageId='" + messageId + '\'' +
                ", from='" + from + '\'' +
                ", to=" + to +
                ", subject='" + subject + '\'' +
                ", sentDate=" + sentDate +
                ", isRead=" + isRead +
                ", hasAttachments=" + (attachmentNames != null && !attachmentNames.isEmpty()) +
                '}';
    }
}
