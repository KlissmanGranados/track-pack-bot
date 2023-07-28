package com.kg.mrw.tracking.telegram.dto;

import java.io.InputStream;

public class TrackingToSearchDto {

    private String trackingCode;
    private InputStream qrCode;
    private final boolean isQrCode;

    public TrackingToSearchDto(String trackingCode) {
        this.trackingCode = trackingCode;
        this.isQrCode = false;
    }

    public TrackingToSearchDto(InputStream qrCode) {
        this.qrCode = qrCode;
        this.isQrCode = true;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public InputStream getQrCode() {
        return qrCode;
    }

    public boolean isQrCode() {
        return isQrCode;
    }
}
