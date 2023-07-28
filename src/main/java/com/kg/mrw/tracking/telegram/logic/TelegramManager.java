package com.kg.mrw.tracking.telegram.logic;

import com.kg.mrw.tracking.telegram.daos.PackageDao;
import com.kg.mrw.tracking.telegram.daos.UserDao;
import com.kg.mrw.tracking.telegram.documents.Package;
import com.kg.mrw.tracking.telegram.documents.User;
import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.exception.BotException;
import com.kg.mrw.tracking.telegram.service.TelegramService;
import com.kg.mrw.tracking.telegram.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

@Service
public class TelegramManager extends TelegramLongPollingBot implements TelegramService {

    private static final Logger logger = LoggerFactory.getLogger(TelegramManager.class);
    @Value("${mrw.tracking.bot.username}")
    private String userName;
    @Value("${mrw.tracking.bot.token}")
    private String token;
    private final TrackingService trackingManager;
    private final UserDao userDao;
    private final PackageDao packageDao;

    public TelegramManager(TrackingManager trackingManager, UserDao userDao, PackageDao packageDao) {
        this.trackingManager = trackingManager;
        this.userDao = userDao;
        this.packageDao = packageDao;
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
        Long chatId = chat.getId();

        String command = message.getText();
        Integer messageId = message.getMessageId();
        User.MessageTracking messageTracking = new User.MessageTracking();

        if("/start".equals(command)){
            messageTracking.setMessage(command);
            userDao.findByUserName(userName)
                    .orElseGet( () -> userDao.save( new User(userName, chatId, List.of(messageTracking)) ));
            replyToMessage(chatId, messageId, "If you send me an image containing a QR code from MRW, I can scan the code and provide you with information about your package. Alternatively, if you don’t have a QR code, you can simply send me a text message with the tracking number of your package, and I’ll be able to provide you with the information you need.");
            return;
        }

        Function<Package, String> getDetailsFromPackage = pack -> {
            TrackingToResponseDto trackingToResponseDto = new TrackingToResponseDto();
            trackingToResponseDto.setTrackingCode(pack.getTrackingCode());
            trackingToResponseDto.setResponse(pack.getResponse());
            trackingToResponseDto.setProvider(pack.getProvider());
            return trackingManager.parse(trackingToResponseDto).details();
        };

        if("/records".equalsIgnoreCase(command)) {

            messageTracking.setMessage("Showing the records of the last 15 days");

            userDao.findByUserName(userName)
                    .ifPresentOrElse(
                            user -> {
                                user.getMessageTracking().add(messageTracking);
                                userDao.save(user);
                            },
                            () -> userDao.save( new User(userName, chatId, List.of(messageTracking)) ) );

            Instant date = Instant.now().minus(15, ChronoUnit.DAYS);
            List<Package> recentPackages = packageDao.findPackagesUpdatedAfter(date, chatId);

            List<String> detailsByArrived = new ArrayList<>();
            List<String> detailsByPending = new ArrayList<>();

            recentPackages.forEach( pack -> {
                if (pack.isHasArrived()) {
                    detailsByArrived.add( getDetailsFromPackage.apply(pack) );
                } else {

                    TrackingToResponseDto reply = trackingManager.getTrackingResponse(pack.getTrackingCode());
                    if(reply.isHasArrived()) {
                        pack.setUpdatedAt(Instant.now());
                        packageDao.save(pack);
                        detailsByArrived.add( getDetailsFromPackage.apply(pack) );
                    } else {
                        detailsByPending.add(getDetailsFromPackage.apply(pack));
                    }
                }

            });

            StringBuilder details = new StringBuilder();

            details.append("Showing the records of the last 15 days ... \n\n");

            detailsByArrived.forEach(detail -> {
                details.append(detail);
                details.append("\n");
            });

            detailsByPending.forEach(detail -> {
                details.append(detail);
                details.append("\n");
            });

            replyToMessage(chatId, messageId, details.toString());

            return;
        }

        try {

            boolean isByQr = message.hasDocument()  || message.hasPhoto();
            String trackingId = isByQr? getTrackingIdFromQr(message) : message.getText();
            messageTracking.setMessage("find package");
            messageTracking.setTrackingId(trackingId);
            messageTracking.setByQr(isByQr);

            Function<TrackingToResponseDto, Package> packageMapFromTrackingResponse = reply -> {
                Package packageDocument = new Package();
                packageDocument.setProvider(reply.getProvider());
                packageDocument.setAddress(reply.getAddress());
                packageDocument.setClient(reply.getClient());
                packageDocument.setDestination(reply.getDestination());
                packageDocument.setResponse(reply.getResponse());
                packageDocument.setOrigin(reply.getOrigin());
                packageDocument.setTrackingCode(trackingId);
                packageDocument.setHasArrived(reply.isHasArrived());
                packageDocument.setTypeOfShipment(reply.getTypeOfShipment());
                packageDocument.setChatId(chatId);
                return packageDocument;
            };

            packageDao.findByTrackingCode(trackingId)
                    .ifPresentOrElse(
                            pack -> {

                                messageTracking.setProvider(pack.getProvider());
                                pack.setUpdatedAt(Instant.now());

                                if(pack.isHasArrived()) {
                                    replyToMessage(chatId, messageId, getDetailsFromPackage.apply(pack));
                                    return;
                                }

                                TrackingToResponseDto reply = trackingManager.getTrackingResponse(trackingId);
                                pack.setHasArrived( reply.isHasArrived() );
                                messageTracking.setProvider(reply.getProvider());
                                replyToMessage(chatId, messageId, reply.details());
                            },

                            () -> {
                                TrackingToResponseDto reply = trackingManager.getTrackingResponse(trackingId);
                                Package packageDocument = packageMapFromTrackingResponse.apply(reply);
                                packageDocument.setUpdatedAt(Instant.now());
                                packageDao.save(packageDocument);
                                messageTracking.setProvider(reply.getProvider());
                                replyToMessage(chatId, messageId, reply.details());
                            } );

        } catch (Exception e) {

            logger.error("I cant reply message: {}", e.getMessage());
            messageTracking.setError(e.getMessage());
            replyToMessage(chatId, messageId, "No se ha podido procesar la consulta \uD83D\uDE13");

        } finally {

            userDao.findByUserName(userName)
                    .ifPresentOrElse(
                            user -> {
                                user.getMessageTracking().add(messageTracking);
                                userDao.save(user);
                            },
                            () -> userDao.save( new User(userName, chatId, List.of(messageTracking)) ) );
        }

    }

    private String getTrackingIdFromQr(Message message) {

        String fileId = Optional.ofNullable(message.getDocument())
                .map(Document::getFileId)
                .orElseGet(() -> message.getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElseThrow(() -> new NoSuchElementException("No photos found"))
                        .getFileId());

        GetFile getFile = new GetFile(fileId);

        try (InputStream inputStream = downloadFileAsStream(execute(getFile).getFilePath())) {
            return trackingManager.getTrackingIdFromQr(inputStream);
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
