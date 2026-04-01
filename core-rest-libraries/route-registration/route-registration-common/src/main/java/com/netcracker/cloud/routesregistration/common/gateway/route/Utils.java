package com.netcracker.cloud.routesregistration.common.gateway.route;

import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Optional;

import static com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType.ISTIO;

@Slf4j
public class Utils {
    public static String formatMicroserviceInternalURL(String cloudServiceName,
                                                       String microserviceName,
                                                       String microservicePort,
                                                       String contextPath,
                                                       boolean postRoutesAppnameDisabled) {
        String host = microserviceName;
        if (cloudServiceName != null && !cloudServiceName.isEmpty()) {
            host = cloudServiceName;
        }

        return "http://" +
                (postRoutesAppnameDisabled ? "" : host) +
                ":" +
                microservicePort +
                (contextPath.equals("/") ? "" : contextPath);
    }

    public static String formatCloudNamespace(String cloudNamespace) {
        String resolvedNamespace = cloudNamespace;

        String localDevNamespace = System.getProperty("LOCALDEV_NAMESPACE", System.getenv("LOCALDEV_NAMESPACE"));
        if (localDevNamespace != null && !localDevNamespace.isEmpty()) {
            resolvedNamespace = localDevNamespace;
        }

        log.info("Cloud namespace: {}", resolvedNamespace);
        return resolvedNamespace;
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static boolean isEmpty(String[] array) {
        return !isNotEmpty(array);
    }

    public static boolean isNotEmpty(String[] array) {
        return array != null && array.length > 0 && !array[0].isBlank();
    }

    public static boolean isBorderGatewayName(String gatewayName) {
        return Constants.PUBLIC_GATEWAY_SERVICE.equals(gatewayName)
                || Constants.PRIVATE_GATEWAY_SERVICE.equals(gatewayName)
                || Constants.INTERNAL_GATEWAY_SERVICE.equals(gatewayName);
    }

    public static boolean isIstioEnabled(ServiceMeshType serviceMeshType) {
        return Optional.ofNullable(serviceMeshType)
                .map(ISTIO::equals)
                .orElse(false);
    }
}
