package com.onetattva.infron.core.controllers;

import com.onetattva.infron.api.dto.InfoResponse;
import com.onetattva.infron.core.info.InfoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InfoController {

    private final InfoService infoService;

    public InfoController(InfoService infoService) {
        this.infoService = infoService;
    }

    @GetMapping("/info")
    public ResponseEntity<InfoResponse> getInfo() {
        return ResponseEntity.ok(infoService.getInfo());
    }
}

