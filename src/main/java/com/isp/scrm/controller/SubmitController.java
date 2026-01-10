package com.isp.scrm.controller;
import com.isp.scrm.dto.SubmitRequestDto;
import com.isp.scrm.service.CdiService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class SubmitController {

    private static final Logger log = LoggerFactory.getLogger(SubmitController.class);

    private final CdiService cdiService;

    public SubmitController(CdiService cdiService) {
        this.cdiService = cdiService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitRequestDto request) {
        log.info("Submit received: codeIsp={}, codeCia={}, images={}",
                request.getCodeIsp(),
                request.getCodeCia(),
                request.getImages().size());

        Object response = cdiService.forward(request);
        return ResponseEntity.ok(response);
    }
}
