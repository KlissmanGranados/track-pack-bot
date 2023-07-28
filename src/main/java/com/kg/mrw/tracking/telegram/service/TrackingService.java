package com.kg.mrw.tracking.telegram.service;

import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.dto.TrackingToSearchDto;

import java.io.InputStream;

public interface TrackingService  extends ParserService{
    TrackingToResponseDto getTrackingResponse(String trackingId );
    String getTrackingIdFromQr(InputStream inputStream);
}
