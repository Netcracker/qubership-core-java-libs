package com.netcracker.cloud.context.propagation.core;

import org.jboss.jandex.Main;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.File;
import java.net.URL;

import static com.netcracker.cloud.context.propagation.core.ContextProviderLoader.JANDEX_INDEX;

public class JandexContextLoaderExtension implements BeforeAllCallback {

    private static final String TEST_INDEX = "META-INF/test-index.idx";
    private static final String INDEX_FILE_LOCATION = "target/test-classes/META-INF";
    private static final String INDEX_FILE = "target/test-classes/META-INF/test-index.idx";

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        URL indexResource = Thread.currentThread().getContextClassLoader().getResource(JANDEX_INDEX);
        if (indexResource == null) {
            new File(INDEX_FILE_LOCATION).mkdirs();
            Main.main(new String[]{"-v", "-o", INDEX_FILE, "."});

            ContextManager.register(ContextProviderLoader.initProviders(ContextProviderLoader.loadProvidersFromJandex(TEST_INDEX)));
        }
    }
}
