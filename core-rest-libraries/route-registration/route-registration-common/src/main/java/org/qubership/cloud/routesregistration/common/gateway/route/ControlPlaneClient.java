package org.qubership.cloud.routesregistration.common.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.rest.CommonRequest;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Abstract ControlPlaneClient that should be extended with actual client implementation.</p>
 *
 * <p>Client code should implement {@link ControlPlaneClient#sendRequest(CommonRequest)} method
 * and use it to send routes to control-plane REST API. </p>
 */
@Slf4j
public abstract class ControlPlaneClient {
    protected final String controlPlaneUrl;

    public ControlPlaneClient(String controlPlaneUrl) {
        if (controlPlaneUrl.endsWith("/")) {
            controlPlaneUrl = controlPlaneUrl.substring(0, controlPlaneUrl.length() - 1);
        }
        this.controlPlaneUrl = controlPlaneUrl;
    }

    @Deprecated
    public void postRoutes(RegistrationRequest registrationRequest) {
        throw new NotImplementedException();
    }
    /**
     * <p>All the {@code ControlPlaneClient} extensions should override this method
     * and perform actual REST calls to control-plane in it.
     *
     * @param request request representation containing payload.
     */
    public abstract void sendRequest(CommonRequest request);
}