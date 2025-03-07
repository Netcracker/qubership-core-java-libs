package org.qubership.cloud.routesregistration.common.gateway.route;

public class RoutePostingPauseException extends Exception {
    public RoutePostingPauseException(Exception e) {
        super("Routes posting is PAUSED", e);
    }
}
