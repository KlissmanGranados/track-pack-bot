package com.kg.mrw.tracking.telegram.documents;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDate;
import java.util.List;

@Document
public class User {
    @MongoId
    private ObjectId id;
    @Indexed(unique = true)
    private String userName;
    @Indexed(unique = true)
    private Long chatId;
    private List<MessageTracking> messageTracking;

    public User() {
    }

    public User(String userName, Long chatId, List<MessageTracking> messageTracking) {
        this.userName = userName;
        this.chatId = chatId;
        this.messageTracking = messageTracking;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public List<MessageTracking> getMessageTracking() {
        return messageTracking;
    }

    public void setMessageTracking(List<MessageTracking> messageTracking) {
        this.messageTracking = messageTracking;
    }

    public static class MessageTracking {

        private LocalDate date;
        private Boolean byQr;
        private String message;
        private String trackingId;
        private String error;
        private String provider;

        public MessageTracking(){
            date = LocalDate.now();
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public Boolean getByQr() {
            return byQr;
        }

        public void setByQr(Boolean byQr) {
            this.byQr = byQr;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getTrackingId() {
            return trackingId;
        }

        public void setTrackingId(String trackingId) {
            this.trackingId = trackingId;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

}
