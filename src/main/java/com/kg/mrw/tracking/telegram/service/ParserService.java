package com.kg.mrw.tracking.telegram.service;

import com.kg.mrw.tracking.telegram.dto.TrackingToResponseDto;

public interface ParserService {
    TrackingToResponseDto parse(TrackingToResponseDto content);
}