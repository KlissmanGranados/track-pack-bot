package com.kg.mrw.tracking.telegram;

import com.kg.mrw.tracking.telegram.daos.ConfigDao;
import com.kg.mrw.tracking.telegram.documents.Config;
import com.kg.mrw.tracking.telegram.logic.TelegramManager;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.HashMap;

@SpringBootApplication
@EnableMongoRepositories
public class TrackingTelegramApplication {

    private static final String APP_NAME = "TrackingTelegramApplication";

    public static void main(String[] args) {
        SpringApplication.run(TrackingTelegramApplication.class, args);
    }

    @Bean
    public BotSession bots(TelegramManager telegramManager ) throws TelegramApiException {
        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        return botsApi.registerBot(telegramManager);
    }

    @Bean
    public Config config(ConfigDao configDao){

        return configDao.findByAppName(APP_NAME)
                .orElseGet( () -> {

                    Config config = new Config();
                    config.setAppName(APP_NAME);

                    config.setMessageByCommands(

                            new HashMap<>(){
                                {
                                    put("/records", "Showing the records of the last 15 days.");
                                    put("/start","If you send me an image containing a QR code from MRW, I can scan the code and provide you with information about your package. Alternatively, if you don’t have a QR code, you can simply send me a text message with the tracking number of your package, and I’ll be able to provide you with the information you need.");
                                }
                            }

                    );

                    return configDao.save(config);

                });

    }

}
