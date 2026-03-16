package com.netcracker.cloud.maas.bluegreen.kafka.util;

import org.junit.jupiter.api.TestInfo;

import java.lang.reflect.Method;

public class TestUtils {
    public static String uniqueTopicName(TestInfo testInfo, String baseName) {
        String testName = testInfo.getTestMethod()
                .map(Method::getName)
                .orElse("unknown");
        return baseName + "-" + testName;
    }

    public static String uniqueGroupId(TestInfo testInfo, String baseGroupId) {
        String testName = testInfo.getTestMethod()
                .map(Method::getName)
                .orElse("unknown");
        return baseGroupId + "-" + testName;
    }
}
