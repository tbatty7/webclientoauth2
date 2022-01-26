package com.battybuilds.webclientoauth2;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WokeResponse {
    private String alarm1;
    private String alarm2;
    private String alarm3;
    private String identificationNumber;
    private String error;
}
