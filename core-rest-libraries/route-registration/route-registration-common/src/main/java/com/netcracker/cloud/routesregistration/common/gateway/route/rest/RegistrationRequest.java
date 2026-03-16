package com.netcracker.cloud.routesregistration.common.gateway.route.rest;

/**
 * <p>{@code RegistrationRequest} is a universal contract for routes registration requests of any version.
 *
 * <p>Each control-plane API version should have it's own {@code RegistrationRequest} implementation.
 *
 * <p>Single {@code RegistrationRequest} instance must be suitable to obtain request payload that can be sent as single HTTP request body.
 *
 */
public interface RegistrationRequest extends CommonRequest {
}
