package com.kg.mrw.tracking.telegram;

import com.kg.mrw.tracking.telegram.daos.ConfigDao;
import com.kg.mrw.tracking.telegram.documents.Config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.LongPollingBot;
import org.telegram.telegrambots.starter.SpringWebhookBot;
import org.telegram.telegrambots.starter.TelegramBotInitializer;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

@SpringBootApplication
@EnableMongoRepositories
public class TrackingTelegramApplication {

    private static final String APP_NAME = "TrackingTelegramApplication";

    public static void main(String[] args) {
        SpringApplication.run(TrackingTelegramApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean(TelegramBotsApi.class)
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        return new TelegramBotsApi(DefaultBotSession.class);
    }

    @Bean
    @ConditionalOnMissingBean
    public TelegramBotInitializer telegramBotInitializer(TelegramBotsApi telegramBotsApi,
                                                         ObjectProvider<List<LongPollingBot>> longPollingBots,
                                                         ObjectProvider<List<SpringWebhookBot>> webHookBots) {
        return new TelegramBotInitializer(telegramBotsApi,
                longPollingBots.getIfAvailable(Collections::emptyList),
                webHookBots.getIfAvailable(Collections::emptyList));
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
