package com.kg.mrw.tracking.telegram.logic;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.exception.BotException;
import com.kg.mrw.tracking.telegram.service.HttpWrapperService;
import com.kg.mrw.tracking.telegram.service.ParserService;
import com.kg.mrw.tracking.telegram.service.TrackingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TrackingManager implements TrackingService {

    private static final Logger logger = LoggerFactory.getLogger(TrackingManager.class);
    private static final String PATTERN_AUTHORIZATION = "(?m)^[^/]*'Authorization':\\s*'([^']+)'";
    private static final String TRACKING_TOKEN = "https://phoenixos.mrwve.com/js/tracking-externo.js";
    private static final String TRACKING_FETCH = "https://phoenixos.mrwve.com/api/tracking-externo/v2";
    private static final String PROVIDER_NAME = "MRW";
    private final HttpWrapperService httpWrapperService;
    private final ParserService parserService;

    public TrackingManager(HttpWrapperService httpWrapperService) {
        this.httpWrapperService = httpWrapperService;
        this.parserService = this;
    }

    @Override
    public TrackingToResponseDto getTrackingResponse(String trackingId ) {

        if(trackingId == null)
            throw new BotException("Empty data!");

        if(inValidTrackingId(trackingId)) {
            throw new BotException("Invalid tracking code");
        }

        TrackingToResponseDto trackingToResponseDto = new TrackingToResponseDto();
        trackingToResponseDto.setTrackingCode(trackingId.trim());
        trackingToResponseDto.setResponse(
                getTracking(trackingToResponseDto.getTrackingCode())
        );
        trackingToResponseDto.setProvider(PROVIDER_NAME);
        return parserService.parse( trackingToResponseDto );
    }

    @Override
    public String getTrackingIdFromQr(InputStream inputStream) {
        try {
            return readQRCode(inputStream).split(";")[0];
        } catch (NotFoundException | IOException e) {
            logger.error("Problem when tried tread qr code: {}", e.toString());
            throw new BotException("Fail when a tried to read qr code");
        }
    }

    private boolean inValidTrackingId(String tracking) {
        return tracking == null || !tracking.matches("\\d+");
    }

    private String readQRCode(InputStream inputStream) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private String getTracking(String tracking) {

        String token = getToken();
        HttpResponse<String> httpResponseTracking = httpWrapperService.fetch(
                HttpRequest.newBuilder()
                        .uri(URI.create(TRACKING_FETCH))
                        .POST(HttpRequest.BodyPublishers.ofString(String.format("nro_tracking=%s", tracking)))
                        .setHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                        .setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .setHeader("Authorization", token)
                        .setHeader("tryCount", "0")
                        .setHeader("retryLimit", "3")
                        .setHeader("X-Requested-With", "XMLHttpRequest")
        ).orElseThrow(() -> new RuntimeException(""));
        return httpResponseTracking.body();

    }

    private String getToken() {

        HttpResponse<String> httpResponseToken = httpWrapperService
                .fetch(HttpRequest.newBuilder().uri(URI.create(TRACKING_TOKEN)))
                .orElseThrow(() -> new RuntimeException(""));

        Pattern pattern = Pattern.compile(PATTERN_AUTHORIZATION);
        Matcher matcher = pattern.matcher(httpResponseToken.body());

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("");
    }

    @Override
    public TrackingToResponseDto parse(TrackingToResponseDto trackingToResponseDto) {

        JSONObject json = new JSONObject(trackingToResponseDto.getResponse());

        if(!json.has("agencia_origen")) {
            throw new BotException("Origin not found");
        }

        Object tracking = json.get("tracking");

        List<JSONObject> trackingEntries = new ArrayList<>();

        if (tracking instanceof JSONArray trackingArray) {
            for (int i = 0; i < trackingArray.length(); i++) {
                trackingEntries.add(trackingArray.getJSONObject(i));
            }
        } else if (tracking instanceof JSONObject trackingObject) {
            for (String key : trackingObject.keySet()) {
                trackingEntries.add(trackingObject.getJSONObject(key));
            }
        }

        boolean hasArrived = false;

        for (JSONObject trackingEntry : trackingEntries) {
            String estatus = trackingEntry.getString("estatus");
            if (trackingEntry.has("agencia")) {
                if ("Disponible en Agencia".equals(estatus)) {
                    hasArrived = true;
                    break;
                }
            }
        }

        String origin = json.getString("agencia_origen");
        String destination = json.getString("agencia_destino");
        String client = json.getString("destinatario");
        String address = json.getString("direccion");
        String typeOfShipment = json.getString("tipo_envio");
        String status = hasArrived? "Available ✅" : "Not available \uD83D\uDEAB";

        trackingToResponseDto.setOrigin(origin);
        trackingToResponseDto.setDestination(destination);
        trackingToResponseDto.setClient(client);
        trackingToResponseDto.setAddress(address);
        trackingToResponseDto.setTypeOfShipment(typeOfShipment);
        trackingToResponseDto.setStatus(status);
        trackingToResponseDto.setHasArrived(hasArrived);

        return trackingToResponseDto;
    }
}
