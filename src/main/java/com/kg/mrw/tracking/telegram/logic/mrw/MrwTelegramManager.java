package com.kg.mrw.tracking.telegram.logic.mrw;

import com.kg.mrw.tracking.telegram.daos.PackageDao;
import com.kg.mrw.tracking.telegram.daos.UserDao;
import com.kg.mrw.tracking.telegram.documents.Config;
import com.kg.mrw.tracking.telegram.documents.Package;
import com.kg.mrw.tracking.telegram.documents.User;
import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.exception.BotException;
import com.kg.mrw.tracking.telegram.service.TrackingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class MrwTelegramManager extends TelegramLongPollingBot {

    public static final String START = "/start";
    public static final String RECORDS = "/records";
    private static final Logger logger = LoggerFactory.getLogger(MrwTelegramManager.class);
    private final TrackingService mrwTrackingManager;
    private final UserDao userDao;
    private final PackageDao packageDao;
    private final Config config;
    private final String userName;
    private final ThreadPoolTaskExecutor executorService;
    private final BiConsumer<StringBuilder, Package> appendToBuilder = (builder, pack) -> builder
            .append( getDetailsFromPackage(pack) ).append("\n");

    public MrwTelegramManager(MrwTrackingManager mrwTrackingManager, UserDao userDao, PackageDao packageDao, Config config, Environment env, ThreadPoolTaskExecutor executorService) {
        super(env.getProperty("mrw.tracking.bot.token"));
        this.userName = env.getProperty("mrw.tracking.bot.username");
        this.mrwTrackingManager = mrwTrackingManager;
        this.userDao = userDao;
        this.packageDao = packageDao;
        this.config = config;
        this.executorService = executorService;
    }

    @Scheduled(cron = "0 30 12 * * *", zone =  "America/Caracas")
    public void checkPackage(){

        Map<Long, List<Package>> packs =  packageDao.findByHasNotifiedFalseOrNotExists()
                .stream().collect(Collectors.groupingBy(Package::getChatId));

        for (Long chatId : packs.keySet()) {

            executorService.submit( () -> {

                List<Package> packages = packs.get(chatId);
                StringBuilder firstDetails = new StringBuilder();
                StringBuilder lastDetails = new StringBuilder();

                for (Package pack : packages) {

                    if( pack.isHasArrived()) {
                        appendToBuilder.accept(firstDetails, pack);
                    } else {
                        TrackingToResponseDto reply = mrwTrackingManager.getTrackingResponse(pack.getTrackingCode());
                        if(reply.isHasArrived()) {
                            pack.setHasArrived(true);
                            appendToBuilder.accept(firstDetails, pack);
                        } else {
                            appendToBuilder.accept(lastDetails, pack);
                        }
                    }

                }

                boolean isAllArrived = packages.stream().allMatch( Package::isHasArrived );
                if(isAllArrived) {
                    packages.forEach( pack -> pack.setHasNotified(true) );
                    packageDao.saveAll(packages);
                }

                firstDetails.append(lastDetails);
                sendMessage(chatId, firstDetails.toString());

            });

        }
    }

    @Override
    public String getBotUsername() {
        return userName;
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
        Supplier<String> getMessageByCommand = () -> config.getMessageByCommands().get(command).concat("\n\n");

        try {

            if (START.equals(command)) {
                messageTracking.setMessage(command);
                replyToMessage(chatId, messageId, getMessageByCommand.get());
                return;
            }

            if (RECORDS.equals(command)) {
                messageTracking.setMessage(command);
                lastRecords(chatId, messageId, getMessageByCommand);
                return;
            }

            searchPackageHandler(message, chatId, messageId, messageTracking);

        } catch (Exception e) {

            logger.error("I cant reply message: {}", e.getMessage());
            messageTracking.setError(e.getMessage());
            replyToMessage(chatId, messageId, "No se ha podido procesar la consulta \uD83D\uDE13");

        } finally {
            userTracking(userName, chatId, messageTracking);
        }

    }

    private String getDetailsFromPackage(Package pack) {
        TrackingToResponseDto trackingToResponseDto = new TrackingToResponseDto();
        trackingToResponseDto.setTrackingCode(pack.getTrackingCode());
        trackingToResponseDto.setResponse(pack.getResponse());
        trackingToResponseDto.setProvider(pack.getProvider());
        return mrwTrackingManager.parse(trackingToResponseDto).details();
    }

    private Package packageMapFromTrackingResponse(TrackingToResponseDto reply, Long chatId) {
        Package packageDocument = new Package();
        packageDocument.setProvider(reply.getProvider());
        packageDocument.setAddress(reply.getAddress());
        packageDocument.setClient(reply.getClient());
        packageDocument.setDestination(reply.getDestination());
        packageDocument.setResponse(reply.getResponse());
        packageDocument.setOrigin(reply.getOrigin());
        packageDocument.setTrackingCode(reply.getTrackingCode());
        packageDocument.setHasArrived(reply.isHasArrived());
        packageDocument.setTypeOfShipment(reply.getTypeOfShipment());
        packageDocument.setChatId(chatId);
        return packageDocument;
    }

    private void userTracking(String userName, Long chatId, User.MessageTracking messageTracking) {
        User user = userDao.findByUserName(userName).orElseGet(() -> new User(userName, chatId, new ArrayList<>()));
        user.addMessageTracking(messageTracking);
        userDao.save(user);
    }

    private void searchPackageHandler(Message message, Long chatId, Integer messageId, User.MessageTracking messageTracking) {

        boolean isByQr = message.hasDocument() || message.hasPhoto();
        String trackingId = isByQr ? getTrackingIdFromQr(message) : message.getText();
        messageTracking.setMessage("find package");
        messageTracking.setTrackingId(trackingId);
        messageTracking.setByQr(isByQr);

        Optional<Package> optionalPackage = packageDao.findByTrackingCodeAndChatId(trackingId, chatId);

        if(optionalPackage.isEmpty()) {
            TrackingToResponseDto reply = mrwTrackingManager.getTrackingResponse(trackingId);
            Package packageDocument = packageMapFromTrackingResponse(reply, chatId);
            packageDocument.setUpdatedAt(Instant.now());
            packageDao.save(packageDocument);
            messageTracking.setProvider(reply.getProvider());
            replyToMessage(chatId, messageId, reply.details());
            return;
        }

        Package pack = optionalPackage.get();
        messageTracking.setProvider(pack.getProvider());
        pack.setUpdatedAt(Instant.now());

        if (pack.isHasArrived()) {
            replyToMessage(chatId, messageId, getDetailsFromPackage(pack));
            return;
        }

        TrackingToResponseDto reply = mrwTrackingManager.getTrackingResponse(trackingId);
        pack.setHasArrived(reply.isHasArrived());
        replyToMessage(chatId, messageId, reply.details());
    }

    private void lastRecords(Long chatId, Integer messageId, Supplier<String> getMessageByCommand) {

        Instant date = Instant.now().minus(15, ChronoUnit.DAYS);
        List<Package> recentPackages = packageDao.findPackagesUpdatedAfter(date, chatId);

        StringBuilder firstDetail = new StringBuilder();
        StringBuilder lastDetail = new StringBuilder();

        firstDetail.append(getMessageByCommand.get());

        for (Package pack : recentPackages) {
            if (pack.isHasArrived()) {
                appendToBuilder.accept(firstDetail, pack);
            } else {
                TrackingToResponseDto reply = mrwTrackingManager.getTrackingResponse(pack.getTrackingCode());
                if (reply.isHasArrived()) {
                    pack.setUpdatedAt(Instant.now());
                    packageDao.save(pack);
                    appendToBuilder.accept(firstDetail, pack);
                } else {
                    appendToBuilder.accept(lastDetail, pack);
                }
            }
        }

        firstDetail.append(lastDetail);
        replyToMessage(chatId, messageId, firstDetail.toString());
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
            return mrwTrackingManager.getTrackingIdFromQr(inputStream);
        } catch (TelegramApiException | IOException e) {
            throw new BotException(e.getMessage());
        }
    }

    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setParseMode(ParseMode.HTML);
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
