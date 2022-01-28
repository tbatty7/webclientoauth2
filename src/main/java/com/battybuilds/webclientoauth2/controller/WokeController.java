package com.battybuilds.webclientoauth2.controller;

import com.battybuilds.webclientoauth2.AlarmRequest;
import com.battybuilds.webclientoauth2.WokeResponse;
import com.battybuilds.webclientoauth2.service.WokeService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class WokeController {

    private final WokeService service;

    public WokeController(WokeService service) {

        this.service = service;
    }

    @GetMapping(value = "/v1/alarms", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WokeResponse> wakeUp(@RequestHeader(value = "Identification-No")
                                                       String identificationNo) {
        WokeResponse response = service.getAlarms();
        response.setIdentificationNumber(identificationNo);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/v1/alarm", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WokeResponse> addAlarm(@RequestHeader(value = "Identification-No")
                                                         String identificationNo,
                                                 @RequestBody
                                                         AlarmRequest request) {
        WokeResponse response = service.addAlarm(request);
        response.setIdentificationNumber(identificationNo);
        return ResponseEntity.ok(response);
    }

}