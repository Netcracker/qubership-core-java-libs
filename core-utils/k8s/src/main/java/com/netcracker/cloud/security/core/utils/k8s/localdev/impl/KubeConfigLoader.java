package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.KubeConfigFields;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevUtils.getTextField;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;

@Slf4j
class KubeConfigLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final long EXEC_TIMEOUT_SECONDS = 30;

    private final OidcAuthProviderTokenRefresher oidcAuthProviderTokenRefresher;
    private final Supplier<String> kubeConfigEnvSupplier;
    private final Supplier<String> userHomeSupplier;

    KubeConfigLoader() {
        this(new OidcAuthProviderTokenRefresher());
    }

    KubeConfigLoader(OidcAuthProviderTokenRefresher oidcAuthProviderTokenRefresher) {
        this(
                oidcAuthProviderTokenRefresher,
                () -> System.getenv("KUBECONFIG"),
                () -> System.getProperty("user.home"));
    }

    KubeConfigLoader(Supplier<String> kubeConfigEnvSupplier, Supplier<String> userHomeSupplier) {
        this(new OidcAuthProviderTokenRefresher(), kubeConfigEnvSupplier, userHomeSupplier);
    }

    KubeConfigLoader(OidcAuthProviderTokenRefresher oidcAuthProviderTokenRefresher,
                     Supplier<String> kubeConfigEnvSupplier,
                     Supplier<String> userHomeSupplier) {
        this.oidcAuthProviderTokenRefresher = Objects.requireNonNull(oidcAuthProviderTokenRefresher, "oidcAuthProviderTokenRefresher");
        this.kubeConfigEnvSupplier = Objects.requireNonNull(kubeConfigEnvSupplier, "kubeConfigEnvSupplier");
        this.userHomeSupplier = Objects.requireNonNull(userHomeSupplier, "userHomeSupplier");
    }

    KubeConfigCredentials load() {
        Path kubeConfigPath = resolveKubeConfigPath();
        if (!Files.isRegularFile(kubeConfigPath)) {
            throw new IllegalStateException("Kubeconfig not found at " + kubeConfigPath
                    + ". Set KUBECONFIG or create ~/.kube/config.");
        }
        try {
            JsonNode root = YAML_MAPPER.readTree(kubeConfigPath.toFile());
            String currentContext = getTextField(root, KubeConfigFields.CURRENT_CONTEXT);
            if (StringUtils.isBlank(currentContext)) {
                throw new IllegalStateException("Kubeconfig has no current-context: " + kubeConfigPath);
            }
            JsonNode context = findKubeConfigEntryByName(root.path(KubeConfigFields.CONTEXTS), currentContext)
                    .path(KubeConfigFields.CONTEXT);
            String clusterName = getTextField(context, KubeConfigFields.CLUSTER);
            String userName = getTextField(context, KubeConfigFields.USER);
            if (StringUtils.isBlank(clusterName) || StringUtils.isBlank(userName)) {
                throw new IllegalStateException("Context '" + currentContext
                        + "' must define cluster and user in " + kubeConfigPath);
            }

            JsonNode cluster = findKubeConfigEntryByName(root.path(KubeConfigFields.CLUSTERS), clusterName)
                    .path(KubeConfigFields.CLUSTER);
            JsonNode user = findKubeConfigEntryByName(root.path(KubeConfigFields.USERS), userName)
                    .path(KubeConfigFields.USER);

            String server = getTextField(cluster, KubeConfigFields.SERVER);
            if (StringUtils.isBlank(server)) {
                throw new IllegalStateException("Cluster '" + clusterName + "' has no server URL");
            }

            return KubeConfigCredentials.builder()
                    .serverUrl(StringUtils.stripEnd(server, "/"))
                    .userToken(resolveUserToken(user))
                    .certificateAuthorityData(decodeOptionalBase64(
                            getTextField(cluster, KubeConfigFields.CERTIFICATE_AUTHORITY_DATA)))
                    .insecureSkipTlsVerify(cluster.path(KubeConfigFields.INSECURE_SKIP_TLS_VERIFY).asBoolean(false))
                    .build();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse kubeconfig: " + kubeConfigPath, e);
        }
    }

    Path resolveKubeConfigPath() {
        String kubeConfig = kubeConfigEnvSupplier.get();
        if (StringUtils.isNotBlank(kubeConfig)) {
            // KUBECONFIG may be a list; use the first entry
            String first = kubeConfig.split(java.io.File.pathSeparator)[0].trim();
            return Path.of(first);
        }
        return Path.of(userHomeSupplier.get(), ".kube", "config");
    }

    private String resolveUserToken(JsonNode user) {
        String token = getTextField(user, KubeConfigFields.TOKEN);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        String authProviderToken = resolveAuthProviderToken(user.path(KubeConfigFields.AUTH_PROVIDER));
        if (StringUtils.isNotBlank(authProviderToken)) {
            return authProviderToken;
        }
        String directOidcToken = firstNonBlank(
                getTextField(user, KubeConfigFields.ID_TOKEN),
                getTextField(user, KubeConfigFields.ACCESS_TOKEN));
        if (StringUtils.isNotBlank(directOidcToken)) {
            return directOidcToken;
        }
        JsonNode exec = user.path(KubeConfigFields.EXEC);
        if (!exec.isMissingNode() && !exec.isNull()) {
            return runExecCredential(exec);
        }
        throw new IllegalStateException(
                "Kubeconfig user has neither 'token', OIDC auth-provider (with refresh-token/id-token), nor 'exec'. "
                        + "Local-dev TokenRequest supports static token, OIDC auth-provider refresh, and exec auth.");
    }

    private String resolveAuthProviderToken(JsonNode authProvider) {
        if (authProvider.isMissingNode() || authProvider.isNull()) {
            return null;
        }
        JsonNode config = authProvider.path(KubeConfigFields.CONFIG);
        if (config.isMissingNode() || config.isNull()) {
            return null;
        }
        String providerName = getTextField(authProvider, KubeConfigFields.NAME);
        if (LocalDevConstants.OIDC_AUTH_PROVIDER_NAME.equalsIgnoreCase(providerName)) {
            return oidcAuthProviderTokenRefresher.resolveToken(config);
        }
        return firstNonBlank(
                getTextField(config, KubeConfigFields.ID_TOKEN),
                getTextField(config, KubeConfigFields.ACCESS_TOKEN));
    }

    private String runExecCredential(JsonNode exec) {
        String command = requireExecCommand(exec);
        List<String> commandLine = buildExecCommandLine(exec, command);
        log.debug("Resolving kubeconfig credentials via exec: {}", commandLine);
        try {
            String output = runExecProcess(exec, commandLine, command);
            return parseExecToken(output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kubeconfig exec interrupted", e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to run kubeconfig exec command: " + command, e);
        }
    }

    private String requireExecCommand(JsonNode exec) {
        String command = getTextField(exec, KubeConfigFields.COMMAND);
        if (StringUtils.isBlank(command)) {
            throw new IllegalStateException("Kubeconfig exec.command is empty");
        }
        return command;
    }

    private List<String> buildExecCommandLine(JsonNode exec, String command) {
        List<String> commandLine = new ArrayList<>();
        commandLine.add(command);
        JsonNode args = exec.path(KubeConfigFields.ARGS);
        if (args.isArray()) {
            for (JsonNode arg : args) {
                commandLine.add(arg.asText());
            }
        }
        return commandLine;
    }

    private void applyExecEnvironment(ProcessBuilder processBuilder, JsonNode exec) {
        JsonNode env = exec.path(KubeConfigFields.ENV);
        if (!env.isArray()) {
            return;
        }
        for (JsonNode envVar : env) {
            String name = getTextField(envVar, KubeConfigFields.NAME);
            String value = getTextField(envVar, KubeConfigFields.VALUE);
            if (StringUtils.isNotBlank(name)) {
                processBuilder.environment().put(name, value == null ? "" : value);
            }
        }
    }

    private String runExecProcess(JsonNode exec, List<String> commandLine, String command)
            throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(commandLine);
        processBuilder.redirectErrorStream(true);
        applyExecEnvironment(processBuilder, exec);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(EXEC_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IllegalStateException("Kubeconfig exec timed out after " + EXEC_TIMEOUT_SECONDS + "s: " + command);
        }
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IllegalStateException("Kubeconfig exec failed (exit " + process.exitValue() + "): " + output);
        }
        return output;
    }

    private String parseExecToken(String output) throws IOException {
        JsonNode credential = JSON_MAPPER.readTree(output);
        String token = getTextField(credential.path(KubeConfigFields.STATUS), KubeConfigFields.TOKEN);
        if (StringUtils.isBlank(token)) {
            throw new IllegalStateException("Kubeconfig exec did not return status.token");
        }
        return token;
    }

    private JsonNode findKubeConfigEntryByName(JsonNode array, String name) {
        if (array != null && array.isArray()) {
            for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
                JsonNode item = it.next();
                if (name.equals(getTextField(item, KubeConfigFields.NAME))) {
                    return item;
                }
            }
        }
        throw new IllegalStateException("Kubeconfig entry not found: " + name);
    }

    private byte[] decodeOptionalBase64(String value) {
        if (StringUtils.isBlank(value)) {
            return new byte[0];
        }
        return Base64.getDecoder().decode(value.replaceAll("\\s", ""));
    }
}
