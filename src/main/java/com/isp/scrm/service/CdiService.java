package com.isp.scrm.service;

import com.isp.scrm.dto.SubmitRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class CdiService {

    private final RestClient restClient;

    public CdiService(RestClient restClient) {
        this.restClient = restClient;
    }

    public Object forward(SubmitRequestDto request) {

        log.info("Forwarding submit request to external API [codeIsp={}]", request.getCodeIsp());

        Object response = restClient
                .post()
                .uri("/submit")
                .body(request)
                .retrieve()
                .body(Object.class);
        log.info("Payload ready, external call NOT YET IMPLEMENTED");
        return response;
    }
}
