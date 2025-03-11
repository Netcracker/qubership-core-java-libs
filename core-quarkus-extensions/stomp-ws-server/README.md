# quarkus-stomp-websocket-server

`quarkus-stomp-websocket-server` is extension for Quarkus framework which allows to run a STOMP server over sockJs or over standard websocket protocol. 

First of all in order to use this extension, you should add the maven dependency:

```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>stomp-ws-server</artifactId>
            <version>${version}</version>
        </dependency>
``` 
where a version can be found in a artifactory by the link: https://artifactorycn.org.qubership/pd.saas.mvn/org/qubership/cloud/quarkus/stomp-ws-server/

Then you have to configure STOMP server. You can set the following application properties:
```text
    quarkus.stomp-server.websocket-path=/stomp // websocket path by which clients connect to server
    quarkus.stomp-server.isSockJS=true // configure stomp work over sockjs or standart websocket protocol  
``` 
Additionally, you can define pre and post subscribe interceptor by implementing `SubscribeInterceptor` interface:

```java
@ApplicationScoped
@Startup
public class SubscribeInterceptorImpl implements SubscribeInterceptor {
    @Override
    public void preSubscribe(ServerFrame serverFrame) {
        System.out.println("preSubscribe method is called");
    }

    @Override
    public void postSubscribe(ServerFrame serverFrame) {
        System.out.println("postSubscribe method is called");
    }
}

``` 

Finally, you should extend `DestinationProvider` class, override his methods, and put `@ApplicationScoped` annotation on class level.
For example:  
```java
@ApplicationScoped
public class ServiceDestinationProviderImpl extends DestinationProvider {
    private Destination destination;

    @Override
    public String getDestinationPath() {
        return "/services";
    }

    @Override
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    @Override
    public Destination getDestination() {
        return destination;
    }

    @Override
    public DestinationType getDestinationType() {
        return DestinationType.TOPIC;
    }

    @Override
    public boolean isDestinationSetUp() {
        return destination != null;
    }

}
```
`getDestinationPath` method must return path with which this destination corresponds.  
After it, you can inject this bean and sent to or receive messages from clients.  

```java
    @Inject
    ServiceDestinationProviderImpl serviceDestinationProvider;

        if (serviceDestinationProvider.isDestinationSetUp()) {
            sendToSubscriber(serviceDestinationProvider.getDestination(), "message to subscribers");
        } else {
            log.info("There are not subscribers");
        }
```
Please pay attention that before any communications with client, the client must subscribe.