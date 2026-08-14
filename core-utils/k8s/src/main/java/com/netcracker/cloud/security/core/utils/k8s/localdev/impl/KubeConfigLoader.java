package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Iterator;

import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevConstants.KubeConfigFields;
import static com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevUtils.getTextField;
import static java.io.File.pathSeparator;
import static org.apache.commons.lang3.StringUtils.firstNonBlank;

@Slf4j
class KubeConfigLoader {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final OidcAuthProviderTokenRefresher oidcAuthProviderTokenRefresher;
    private final String kubeConfigEnv;
    private final String userHome;

    KubeConfigLoader() {
        this(System.getenv("KUBECONFIG"), System.getProperty("user.home"));
    }

    KubeConfigLoader(String kubeConfigEnv, String userHome) {
        this.oidcAuthProviderTokenRefresher = new OidcAuthProviderTokenRefresher();
        this.kubeConfigEnv = kubeConfigEnv;
        this.userHome = userHome;
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

    /**
     * Resolves the kubeconfig file path.
     * <p>
     * When {@code KUBECONFIG} lists multiple files (OS path-separated), only the <strong>first</strong>
     * entry is used (kubectl merges configs; local-dev does not).
     */
    Path resolveKubeConfigPath() {
        if (StringUtils.isNotBlank(kubeConfigEnv)) {
            String[] kubeConfigs = kubeConfigEnv.split(pathSeparator);
            String first = kubeConfigs[0].trim();
            if (kubeConfigs.length > 1) {
                log.warn("Local-dev takes only the first kubeconfig: {}", first);
            }
            return Path.of(first);
        }
        return Path.of(userHome, ".kube", "config");
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
            throw new IllegalStateException(
                    "Kubeconfig exec authentication is not supported for local-dev. "
                            + "Use a static user token or OIDC auth-provider (id-token / refresh-token).");
        }
        throw new IllegalStateException(
                "Kubeconfig user has neither 'token' nor OIDC auth-provider (with refresh-token/id-token). "
                        + "Local-dev TokenRequest supports static token and OIDC auth-provider refresh.");
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
        return Base64.getMimeDecoder().decode(value);
    }
}
