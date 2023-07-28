package com.kg.mrw.tracking.telegram;

import com.kg.mrw.tracking.telegram.logic.TelegramManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
@EnableMongoRepositories
public class TrackingTelegramApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrackingTelegramApplication.class, args);
    }

    @Bean
    public BotSession bots(TelegramManager telegramManager ) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        return botsApi.registerBot(telegramManager);
    }

}
