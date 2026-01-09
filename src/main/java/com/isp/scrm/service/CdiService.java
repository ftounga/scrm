package com.isp.scrm.service;

import com.isp.scrm.dto.SubmitRequestDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CdiService {

    private final RestClient restClient;

    public CdiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Object forward(SubmitRequestDto request) {
        return restClient
                .post()
                .uri("/submit")
                .body(request)
                .retrieve()
                .body(Object.class);
    }
}
