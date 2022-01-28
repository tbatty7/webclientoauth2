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
    Integer year;// You cannot validate a field is not null if it has a primitive type of int, it defaults to 0
    Integer month;
    Integer day;
    Integer hour;
    String message;
}
