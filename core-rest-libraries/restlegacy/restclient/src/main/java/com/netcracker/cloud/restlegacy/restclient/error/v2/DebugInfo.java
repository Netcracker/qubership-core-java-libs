package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Date;

@ToString
@AllArgsConstructor
public class DebugInfo {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss a z")
    @Getter
    private final Date date;

    @Getter
    private final String service;

    @Getter
    private final String stackTrace;
}
