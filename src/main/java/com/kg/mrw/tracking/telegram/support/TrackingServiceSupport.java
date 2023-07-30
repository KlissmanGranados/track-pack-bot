package com.kg.mrw.tracking.telegram.support;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.service.TrackingService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public abstract class TrackingServiceSupport implements TrackingService {
    @Override
    abstract public TrackingToResponseDto parse(TrackingToResponseDto content);
    @Override
    abstract public TrackingToResponseDto getTrackingResponse(String trackingId);
    @Override
    abstract public String getTrackingIdFromQr(InputStream inputStream);
    protected String readQRCode(InputStream inputStream) throws IOException, NotFoundException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result = new MultiFormatReader().decode(bitmap);
        return result.getText();
    }
}
