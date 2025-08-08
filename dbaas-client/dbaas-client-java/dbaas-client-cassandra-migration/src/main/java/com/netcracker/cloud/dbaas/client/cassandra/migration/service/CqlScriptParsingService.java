package org.qubership.cloud.dbaas.client.cassandra.migration.service;

import org.qubership.cloud.dbaas.client.cassandra.migration.cql.SimpleCQLScriptParser;
import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.settings.SchemaMigrationSettings;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.output.StringBuilderWriter;
import org.qubership.cloud.dbaas.client.cassandra.migration.util.ResourceUtils;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.qubership.cloud.dbaas.client.cassandra.migration.SchemaMigrationCommonConstants.*;

@Slf4j
public class CqlScriptParsingService {
    public static final String IS_AMAZON_KEYSPACES_PROPERTY_NAME = "IS_AMAZON_KEYSPACES";

    private final Configuration configuration;

    public CqlScriptParsingService(SchemaMigrationSettings schemaMigrationSettings) {
        this.configuration = createTemplateConfiguration(
                schemaMigrationSettings.amazonKeyspaces().enabled(),
                schemaMigrationSettings.template().definitionsResourcePath()
        );
    }

    public List<String> parseStatements(String resourcePath, String script) {
        if (resourcePath.endsWith(ResourceUtils.FTL_EXTENSION)) {
            script = processTemplate(resourcePath, script);
        }
        SimpleCQLScriptParser lexer = new SimpleCQLScriptParser(script);
        log.info(MIGRATION_LOG_PREFIX + "Parsing statements from {}", resourcePath);
        return lexer.parseToCqlQueries().stream().map(String::trim).toList();
    }

    private String processTemplate(String resourcePath, String script) {
        log.info(MIGRATION_LOG_PREFIX + "Processing script template from {}", resourcePath);
        try {
            Template template = new Template(null, script, configuration);
            Writer writer = new StringBuilderWriter();
            template.process(null, writer);
            return writer.toString();
        } catch (IOException | TemplateException e) {
            throw new SchemaMigrationException("Unable to process script template from " + resourcePath, e);
        }
    }

    private Configuration createTemplateConfiguration(
            boolean isAmazonKeyspaces, String functionsFilePath
    ) {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        try {
            cfg.setSharedVariable(IS_AMAZON_KEYSPACES_PROPERTY_NAME, isAmazonKeyspaces);
            cfg.setTemplateLoader(new ClassTemplateLoader(CqlScriptParsingService.class, "/"));
            cfg.setAutoImports(Map.of("fn", functionsFilePath));
        } catch (TemplateModelException e) {
            String msg = "Unable to create FreeMarker configuration.";
            log.error(MIGRATION_LOG_PREFIX + msg, e);
            throw new SchemaMigrationException(msg, e);
        }
        cfg.setDefaultEncoding(StandardCharsets.UTF_8.name());
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        return cfg;
    }
}
