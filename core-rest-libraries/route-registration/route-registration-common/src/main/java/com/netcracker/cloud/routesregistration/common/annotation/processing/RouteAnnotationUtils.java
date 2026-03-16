package com.netcracker.cloud.routesregistration.common.annotation.processing;


import com.netcracker.cloud.routesregistration.common.annotation.FacadeRoute;
import com.netcracker.cloud.routesregistration.common.annotation.Route;
import com.netcracker.cloud.routesregistration.common.gateway.route.RouteType;

import static com.netcracker.cloud.routesregistration.common.gateway.route.Constants.UNSPECIFIED_TIMEOUT_FOR_ROUTE;

public class RouteAnnotationUtils {
    private RouteAnnotationUtils() {}

    private static final RouteType DEFAULT_ROUTE_TYPE = RouteType.INTERNAL;

    public static RouteType getAnnotationRouteType(Route route) {
        return (route.value() == DEFAULT_ROUTE_TYPE) ? route.type() : route.value();
    }

    public static Long getAnnotationRouteTimeout(Route route) {
        return (route.timeout() == UNSPECIFIED_TIMEOUT_FOR_ROUTE) ? null : route.timeout();
    }

    public static Long getAnnotationRouteTimeout(FacadeRoute route) {
        return (route.timeout() == UNSPECIFIED_TIMEOUT_FOR_ROUTE) ? null : route.timeout();
    }

    public static String[] getAnnotationGateways(Route route) {
        String[] gateways = route.gateways();
        return isSingleEmptyStringArray(gateways) ? null : gateways;
    }

    public static String[] getAnnotationGateways(FacadeRoute route) {
        String[] gateways = route.gateways();
        return isSingleEmptyStringArray(gateways) ? null : gateways;
    }

    public static String[] getAnnotationHosts(Route route) {
        String[] hosts = route.hosts();
        return isSingleEmptyStringArray(hosts) ? null : hosts;
    }

    public static boolean isSingleEmptyStringArray(String[] array) {
        return array == null || array.length == 0
                || (array.length == 1 && (array[0] == null || array[0].isEmpty()));
    }

}
