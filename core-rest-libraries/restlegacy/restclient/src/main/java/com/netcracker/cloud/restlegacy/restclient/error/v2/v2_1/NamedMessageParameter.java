package com.netcracker.cloud.restlegacy.restclient.error.v2.v2_1;

import lombok.Getter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Getter
public class NamedMessageParameter {

    private final String parameterName;
    private final Object parameterValue;
    private final String parameterType;

    private NamedMessageParameter(final String parameterName, final Object parameterValue, final String parameterType) {
        this.parameterName = parameterName;
        this.parameterValue = parameterValue;
        this.parameterType = parameterType;
    }

    public static NamedMessageParameter date(final String parameterName, final Date parameterValue) {
        String formattedDate = null;
        if (parameterValue != null) {
            ZonedDateTime zonedDateTime = parameterValue.toInstant()
                    .atZone(ZoneId.systemDefault());
            formattedDate = zonedDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
        }

        return new NamedMessageParameter(parameterName, formattedDate, "date");
    }

    public static NamedMessageParameter number(final String parameterName, final Number parameterValue) {
        return new NamedMessageParameter(parameterName, parameterValue, "number");
    }

    public static NamedMessageParameter localizedString(final String parameterName, final String parameterValue) {
        return new NamedMessageParameter(parameterName, parameterValue, "localizedString");
    }

    public static NamedMessageParameter string(final String parameterName, final String parameterValue) {
        return new NamedMessageParameter(parameterName, parameterValue, "string");
    }

    public static NamedMessageParameter generic(final String parameterName, final Object parameterValue, final String genericType) {
        return new NamedMessageParameter(parameterName, parameterValue, genericType);
    }
}
