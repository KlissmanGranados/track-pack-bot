package com.kg.mrw.tracking.telegram.logic;

import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.dto.TrackingToSearchDto;
import com.kg.mrw.tracking.telegram.exception.BotException;
import com.kg.mrw.tracking.telegram.service.TelegramService;
import com.kg.mrw.tracking.telegram.service.TrackingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class TelegramManager extends TelegramLongPollingBot implements TelegramService {

    @Value("${mrw.tracking.bot.username}")
    private String userName;
    @Value("${mrw.tracking.bot.token}")
    private String token;
    private final TrackingService trackingManager;

    public TelegramManager(TrackingManager trackingManager) {
        this.trackingManager = trackingManager;
    }

    @Override
    public String getBotUsername() {
        return userName;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {

        Message message = update.getMessage();
        Chat chat = message.getChat();
        String userName = chat.getUserName();

        String command = message.getText();
        Integer messageId = message.getMessageId();
        Long chatId = chat.getId();

        if("/start".equals(command)){
            replyToMessage(chatId, messageId, "If you send me an image containing a QR code from MRW, I can scan the code and provide you with information about your package. Alternatively, if you don’t have a QR code, you can simply send me a text message with the tracking number of your package, and I’ll be able to provide you with the information you need.");
            return;
        }

        TrackingToResponseDto reply = message.hasDocument()  || message.hasPhoto() ?
                fromQr(message) : trackingManager.getTrackingResponse(new TrackingToSearchDto(message.getText()));
        replyToMessage(chatId, messageId, reply.details());
    }

    private TrackingToResponseDto fromQr(Message message) {

        String fileId = Optional.ofNullable(message.getDocument())
                .map(Document::getFileId)
                .orElseGet(() -> message.getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElseThrow(() -> new NoSuchElementException("No photos found"))
                        .getFileId());

        GetFile getFile = new GetFile(fileId);

        try (InputStream inputStream = downloadFileAsStream(execute(getFile).getFilePath())) {
            return  trackingManager.getTrackingResponse(new TrackingToSearchDto(inputStream));
        } catch (TelegramApiException | IOException e) {
            throw new BotException(e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode(ParseMode.MARKDOWNV2);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new BotException(e.getMessage());
        }
    }

    private void replyToMessage(Long chatId, Integer messageId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setReplyToMessageId(messageId);
        message.setText(text);
        message.setParseMode(ParseMode.HTML);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new BotException(e.getMessage());
        }
    }

}
