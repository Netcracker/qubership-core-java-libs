package com.netcracker.cloud.restlegacy.restclient.error.v2;

class CustomException extends RuntimeException {
    private final String customData;

    CustomException(String customData) {
        this.customData = customData;
    }

    String getCustomData() {
        return customData;
    }
}
