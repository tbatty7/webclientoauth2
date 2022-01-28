package com.battybuilds.webclientoauth2;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmRequest {
    @NotNull(message = "year is required")
    Integer year;
    Integer month;
    Integer day;
    Integer hour;
    String message;
}
