package com.kg.mrw.tracking.telegram.documents;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Map;

@Document
public class Config {
    @MongoId
    private ObjectId id;
    private Map<String, String> messageByCommands;
    private String appName;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Map<String, String> getMessageByCommands() {
        return messageByCommands;
    }

    public void setMessageByCommands(Map<String, String> messageByCommands) {
        this.messageByCommands = messageByCommands;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
