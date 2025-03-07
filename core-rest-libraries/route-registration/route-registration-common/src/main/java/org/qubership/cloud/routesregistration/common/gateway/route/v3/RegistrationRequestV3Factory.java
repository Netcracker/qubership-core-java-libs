package org.qubership.cloud.routesregistration.common.gateway.route.v3;

import lombok.NonNull;
import org.qubership.cloud.routesregistration.common.gateway.route.Constants;
import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.Utils;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.AbstractRegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.CompositeRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.v3.domain.*;

import java.util.*;
import java.util.stream.Collectors;

public class RegistrationRequestV3Factory extends AbstractRegistrationRequestFactory {

    public RegistrationRequestV3Factory(String microserviceURL, String microserviceName, String deploymentVersion, String cloudNamespace) {
        super(microserviceURL, microserviceName, deploymentVersion, cloudNamespace);
    }

    @Override
    public CompositeRequest<CommonRequest> createRequests(Collection<RouteEntry> routes) {
        List<RegistrationRequest> registrationRequests = new ArrayList<>();
        List<DeleteDomainConfigurationV3> deleteDomainsRequests = new ArrayList<>();

        groupRoutesByGateway(routes).forEach((gateway, groupedRoutes) -> {
            List<String> hosts = convertToHosts(gateway, groupedRoutes);
            String virtualServiceName = resolveVirtualServiceName(gateway);
            RouteConfigurationRequestV3 registrationRequest = RouteConfigurationRequestV3.builder()
                    .namespace(cloudNamespace)
                    .gateways(Collections.singletonList(gateway))
                    .virtualServices(Collections.singletonList(
                            VirtualService.builder()
                                    .name(virtualServiceName)
                                    .hosts(hosts)
                                    .routeConfiguration(RouteConfig.builder()
                                            .version(deploymentVersion)
                                            .routes(Collections.singletonList(
                                                    RouteV3.builder()
                                                            .destination(RouteDestination.builder()
                                                                    .cluster(microserviceName)
                                                                    .endpoint(microserviceURL)
                                                                    .build())
                                                            .rules(convertToRules(groupedRoutes))
                                                            .build()))
                                            .build())
                                    .build()
                    ))
                    .build();
            registrationRequests.add(new RegistrationRequestV3(registrationRequest));
            if (shouldAddDeleteDomainRequest(hosts)) {
                deleteDomainsRequests.add(new DeleteDomainConfigurationV3(virtualServiceName, gateway, Collections.singletonList("*")));
            }
        });

        return new CompositeRequestV3(registrationRequests, deleteDomainsRequests);
    }

    private Map<String, List<RouteEntry>> groupRoutesByGateway(Collection<RouteEntry> routes) {
        Map<String, List<RouteEntry>> result = new HashMap<>();
        routes.forEach(route -> {
            String gatewayName = resolveGatewayName(route);
            List<RouteEntry> groupedRoutes = result.computeIfAbsent(gatewayName, gateway -> new ArrayList<>());
            groupedRoutes.add(route);
        });
        return result;
    }

    private boolean shouldAddDeleteDomainRequest(List<String> hosts) {
        return Utils.isNotEmpty(hosts) && !hosts.get(0).equals("*");
    }

    private List<String> convertToHosts(@NonNull String gatewayName, @NonNull Collection<RouteEntry> routeEntries) {
        if (Utils.isBorderGatewayName(gatewayName) || microserviceName.equals(gatewayName)) {
            return null;
        }

        List<String> hosts = new ArrayList<>();
        boolean containsEmptyHosts = false;
        for (RouteEntry routeEntry : routeEntries) {
            Set<String> currentHosts = routeEntry.getHosts();
            if (Utils.isEmpty(currentHosts)) {
                containsEmptyHosts = true;
                continue;
            }
            hosts.addAll(currentHosts);
        }

        if (!hosts.isEmpty() && containsEmptyHosts) {
            throw new IllegalArgumentException("Route cannot have empty virtual host because there is at least one another route with different virtual host");
        }
        if (!hosts.isEmpty() && hosts.contains("*")) {
            throw new IllegalArgumentException("Route cannot have virtual host \"*\" because there is at least one another route with different virtual host");
        }

        if (hosts.isEmpty()) {
            return Collections.singletonList("*");
        }

        return hosts;
    }

    private List<Rule> convertToRules(@NonNull Collection<RouteEntry> routeEntries) {
        return routeEntries.stream()
                .map(routeEntry ->
                        Rule.builder()
                                .allowed(routeEntry.isAllowed())
                                .match(RouteMatch.builder().prefix(routeEntry.getFrom()).build())
                                .prefixRewrite(routeEntry.getTo())
                                .timeout(routeEntry.getTimeout())
                                .build())
                .collect(Collectors.toList());
    }

    private String resolveVirtualServiceName(String gateway) {
        if (gateway == null) {
            return microserviceName;
        }
        if (gateway.equals(Constants.PUBLIC_GATEWAY_SERVICE)
                || gateway.equals(Constants.PRIVATE_GATEWAY_SERVICE)
                || gateway.equals(Constants.INTERNAL_GATEWAY_SERVICE)) {
            return gateway;
        }
        return microserviceName;
    }
}
