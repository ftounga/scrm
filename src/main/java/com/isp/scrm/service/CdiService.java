package com.isp.scrm.service;

import com.isp.scrm.dto.SubmitRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CdiService {

    private final RestClient restClient;

    public CdiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Object forward(SubmitRequestDto request) {

        log.info("Forwarding submit request to external API [codeIsp={}]", request.getCodeIsp());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", "Request forwarded successfully (mock)");
        response.put("codeIsp", request.getCodeIsp());
        response.put("imagesReceived", request.getImages().size());
        /*Object response = restClient
                .post()
                .uri("/submit")
                .body(request)
                .retrieve()
                .body(Object.class);*/
        log.info("Payload ready, external call NOT YET IMPLEMENTED");
        return response;
    }
}
