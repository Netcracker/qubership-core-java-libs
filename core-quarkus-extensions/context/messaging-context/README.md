# Context Propagation for Quarkus SmallRye Reactive Messaging

## Attach library  
Add dependency to your pom.xml:
```xml
<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
    <artifactId>messaging-context</artifactId>
    <version>{version}</version>
</dependency>
```

## Context Propagation 

Context propagation step means dumping execution context data into message metadata before send. Usually this metadata 
serialized to message header structures.

To simplify and unify context serialization we can use Emmiter<T> implementation provided by this extension and used during injection:
```java
class OrderProcessor {
    @Channel("orders")
    Emitter<Order> emitter;
} 
```

To send message just use standard Emitter methods. For example: 
```java
class OrderProcessor{
    @Channel("orders")
    Emitter<Order> emitter;

    CompletionStage<Void> send(User data){
        return emitter.send(data);
    }
}
```

Good article how to use messaging emitter: [Emitter - Bridging the imperative and the reactive worlds](https://quarkus.io/blog/reactive-messaging-emitter/)

## Context Restoration

To run message listener within restored context we need to add `RestoreContext` annotation on listener method. This context restoration framework required that 
first parameter of message callback should be type of `org.eclipse.microprofile.reactive.messaging.Message<T>`: 
```java
class Receiver {
	@Incoming("words-in")
	@RestoreContext
	CompletionStage<Void> onMessage(Message<User> msg) {
		System.out.print("Received user: " + msg.getPayload().value());
		return msg.ack();
	} 
}
```