package com.kg.mrw.tracking.telegram.service;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface TelegramService {
    String getBotUsername();

    String getBotToken();

    void onUpdateReceived(Update update);
}
