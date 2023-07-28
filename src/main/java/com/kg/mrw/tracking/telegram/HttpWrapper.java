package com.kg.mrw.tracking.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Scope("prototype")
public class HttpWrapper {

    private final HttpClient client;
    private static final Logger logger = LoggerFactory.getLogger(HttpWrapper.class);
    private static final Map<String, String> headersMap = new HashMap<>() {{
        put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0");
        put("Accept", "*/*");
        put("Accept-Language", "es-ES,es;q=0.8,en-US;q=0.5,en;q=0.3");
        put("Sec-Fetch-Dest", "script");
        put("Sec-Fetch-Mode", "no-cors");
        put("Sec-Fetch-Site", "same-origin");
        put("Pragma", "no-cache");
        put("Cache-Control", "no-cache");
    }};

    public HttpWrapper(){
        this.client = HttpClient.newBuilder().build();
    }

    public Optional< HttpResponse<String> > fetch(HttpRequest.Builder request) {

        Map<String, List<String>> headers = request.build().headers().map();
        headersMap.keySet().forEach(key ->  {
            if(!headers.containsKey(key)) {
                request.setHeader( key, headersMap.get(key));
            }
        });

        try {
            return Optional.of(client.send(request.build(), HttpResponse.BodyHandlers.ofString()));
        }catch (IOException | InterruptedException e) {
            logger.error( "Fetch error {} ", e.toString());
            return Optional.empty();
        }
    }
}
