package com.kg.mrw.tracking.telegram;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TrackingManager {
    public static final String PATTERN_AUTHORIZATION = "(?m)^[^/]*'Authorization':\\s*'([^']+)'";
    public static final String TRACKING_TOKEN = "https://phoenixos.mrwve.com/js/tracking-externo.js";
    public static final String TRACKING_FETCH = "https://phoenixos.mrwve.com/api/tracking-externo/v2";
    private final HttpWrapper httpWrapper;

    public TrackingManager(HttpWrapper httpWrapper) {
        this.httpWrapper = httpWrapper;
    }

    public String getTrackingResponse(String tracking) {
        if(tracking == null) return "Empty data!";
        String trackingId = tracking.trim();
        if(inValidTrackingId(trackingId)) {
            return "Invalid tracking code";
        }
        return getTracking(trackingId);
    }

    public boolean inValidTrackingId(String tracking) {
        return !tracking.matches("\\d+");
    }

    public String getTrackingResponse(InputStream qrFile) {
        try {
            return getTracking(getTrackingCodeFromQrCode(qrFile));
        } catch (NotFoundException | IOException e) {
            e.printStackTrace();
        }
        throw new RuntimeException();
    }

    private String getTrackingCodeFromQrCode(InputStream inputStream) throws NotFoundException, IOException {
        return readQRCode(inputStream).split(";")[0];
    }

    public String readQRCode(InputStream inputStream) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }

    private String summarizeTrackingInfo(String jsonString) {

        JSONObject json = new JSONObject(jsonString);

        if(!json.has("agencia_origen")) {
            return "Not found ";
        }

        StringBuilder summary = new StringBuilder();

        String agenciaOrigen = json.getString("agencia_origen");
        String agenciaDestino = json.getString("agencia_destino");
        String destinatario = json.getString("destinatario");
        String direccion = json.getString("direccion");
        String tipoEnvio = json.getString("tipo_envio");

        summary.append("The package was sent from ").append(agenciaOrigen)
                .append(" to ").append(agenciaDestino).append(".\n")
                .append("The recipient is ").append(destinatario).append(".\n")
                .append("The delivery address is ").append(direccion).append(".\n")
                .append("The type of shipment is ").append(tipoEnvio).append(".\n\n");

        JSONArray tracking = json.getJSONArray("tracking");

        summary.append("Tracking information:\n");
        boolean hasArrived = false;

        for (int i = 0; i < tracking.length(); i++) {

            JSONObject trackingEntry = tracking.getJSONObject(i);
            String fecha = trackingEntry.getString("fecha");
            String estatus = trackingEntry.getString("estatus");
            summary.append("- On ").append(fecha).append(", the package status was updated to ").append(estatus);
            if (trackingEntry.has("agencia")) {
                String agencia = trackingEntry.getString("agencia");
                summary.append(" at ").append(agencia);
                if ("Disponible en Agencia".equals(estatus) && agencia != null && agencia.equals(agenciaDestino)) {
                    hasArrived = true;
                }
            }
            summary.append(".\n");
        }

        if (hasArrived) {
            summary.append("\nThe package has arrived at its destination.");
        } else {
            summary.append("\nThe package has not yet arrived at its destination.");
        }

        return summary.toString();
    }

    private String getTracking(String tracking) {

        String token = getToken();
        HttpResponse<String> httpResponseTracking = httpWrapper.fetch(
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
        return summarizeTrackingInfo(httpResponseTracking.body());

    }

    private String getToken() {

        HttpResponse<String> httpResponseToken = httpWrapper
                .fetch(HttpRequest.newBuilder().uri(URI.create(TRACKING_TOKEN)))
                .orElseThrow(() -> new RuntimeException(""));

        Pattern pattern = Pattern.compile(PATTERN_AUTHORIZATION);
        Matcher matcher = pattern.matcher(httpResponseToken.body());

        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("");
    }

}
