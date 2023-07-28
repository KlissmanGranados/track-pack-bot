package com.kg.mrw.tracking.telegram.service;

import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;
import com.kg.mrw.tracking.telegram.dto.TrackingToSearchDto;

public interface TrackingService {
    TrackingToResponseDto getTrackingResponse(TrackingToSearchDto trackingToSearchDto);
}
