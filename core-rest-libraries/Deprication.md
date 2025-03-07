The following class/methods have benn deprecated:

| name                        | description                                                                              | out of support     | link                                                                     |
|-----------------------------|------------------------------------------------------------------------------------------|--------------------|--------------------------------------------------------------------------|
| **rest-template**           |                                                                                          |                    |
| EnableFrameworkRestTemplate | Deprecated because it contains SimpleClientHttpRequestFactory                            | since 5.x.x release | annotation.configuration.org.qubership.cloud.restlegacy.resttemplate.EnableFrameworkRestTemplate |
| **restclient**              |                                                                                          |                    |
| ProxyErrorException         | Use RestClientException                                                                  | since 5.x.x release | error.org.qubership.cloud.restlegacy.restclient.ProxyErrorException     |
| ControllersAdvice | use exception handling v2 version                                                        | since 5.x.x release | v2.error.org.qubership.cloud.restlegacy.restclient.ControllersAdvice    |
| ErrorException | use GenericDisplayedException  or custom exception annotated by DisplayedMessageException | since 5.x.x release | error.org.qubership.cloud.restlegacy.restclient.ErrorException    |
| ErrorType | bind with ErrorException                                                                 | since 5.x.x release | error.org.qubership.cloud.restlegacy.restclient.ErrorType    |
| ProxyErrorExceptionPropagator | Use for legacy error handling model only                                                 | since 5.x.x release | org.qubership.cloud.restlegacy.restclient.RestClient.ProxyErrorExceptionPropagator    |
| ErrorsDescription | used only for legacy and legacy client errorHandler                                                              | since 23.3 release | error.org.qubership.cloud.restlegacy.restclient.ErrorsDescription    |
| handleLegacyErrorException | used only for legacyr                                                               | since 5.x.x release | v2_1.v2.error.org.qubership.cloud.restlegacy.restclient.ExceptionHandlerErrorCodeControllersAdvice.handleLegacyErrorException    |
| handleLegacyProxyErrorException | used only for legacyr                                                               | since 5.x.x release | v2_1.v2.error.org.qubership.cloud.restlegacy.restclient.ExceptionHandlerErrorCodeControllersAdvice.handleLegacyProxyErrorException    |
| ProxyRethrowException | used only for legacyr                                                               | since 5.x.x release | error.org.qubership.cloud.restlegacy.restclient.ProxyRethrowException    |
