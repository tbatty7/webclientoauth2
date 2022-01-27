package com.battybuilds.webclientoauth2.controller;

import com.battybuilds.webclientoauth2.WokeResponse;
import com.battybuilds.webclientoauth2.service.SecureAbcService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecureAbcController {
    private final SecureAbcService service;

    public SecureAbcController(SecureAbcService service) {
        this.service = service;
    }

    @GetMapping(value = "/v2/alarms", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WokeResponse> wakeUp(@RequestHeader(value = "Identification-No")
                                                       String identificationNo) {
        WokeResponse response = service.getAlarmsSecurely();
        response.setIdentificationNumber(identificationNo);
        return ResponseEntity.ok(response);
    }
}
