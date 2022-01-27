package com.battybuilds.webclientoauth2.controller;

import com.battybuilds.webclientoauth2.service.SecureAbcService;

public class SecureAbcController {
    private SecureAbcService service;

    public SecureAbcController(SecureAbcService service) {
        this.service = service;
    }


}
