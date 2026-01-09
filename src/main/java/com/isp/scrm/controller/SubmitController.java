package com.isp.scrm.controller;
import com.isp.scrm.dto.SubmitRequestDto;
import com.isp.scrm.service.CdiService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class SubmitController {

    private final CdiService cdiService;

    public SubmitController(CdiService cdiService) {
        this.cdiService = cdiService;
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitRequestDto request) {
        log.info("Received submit request [codeIsp={}, codeCia={}, imagesCount={}]",
                request.getCodeIsp(),
                request.getCodeCia(),
                request.getImages() != null ? request.getImages().size() : 0
        );
        Object response = cdiService.forward(request);
        return ResponseEntity.ok(response);
    }
}
