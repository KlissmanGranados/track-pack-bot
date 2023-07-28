package com.kg.mrw.tracking.telegram.service;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

public interface HttpWrapperService {
    Optional<HttpResponse<String>> fetch(HttpRequest.Builder request);
}
