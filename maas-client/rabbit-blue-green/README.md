# Rabbit Blue Green

This module provides instruments to work with Rabbit blue/green. 

## Dynamic Queue

Dynamic queue - queue that is created not declarative, but during microservice runtime working in blue/green mode. Every pod could have its own queue for example in funout case to spread a workload.

To use dynamic queue you should create it first by yourself in microservice. It is usually supposed to be `exclusive` and `auto-delete` type of queue.  
You should have business exchange, that was previously declared in one of your microservices in `versioned-entities` section in maas configuration yaml file. Version of business exchange will be resolved automatically during binding.
To bind dynamic queue to business exchange you should call method with signature:

```int queueBindDynamic(Connection connection, String queue, String exchange, String routingKey, Map<String, Object> arguments) throws IOException, TimeoutException;```

It is just standard `queueBind` method from vanilla rabbit client library, but with `Connection` argument. Method returns int as a binding id - you need to keep it, if you need to unbind queue in future.

## Usage

The simple usage example: 
```java
        //DeploymentVersionTracker is a special class, that wraps connections to control-plane and paas-mediation. You should pass microservice token here.
        try (DeploymentVersionTracker tracker = new DeploymentVersionTrackerImpl(new HttpClient( () -> "fake-token"))){
            try (Connection conn = factory.newConnection()) {
                try (Channel channel = conn.createChannel()) {
                DynamicQueueBindingsManager dynamicQueue = new DynamicQueueBindingsManagerImpl(trackerActive);

                String exchangeName = "my-exchange";
                String queueName = "my-queue";

                //exchange should be created in declarative config beforehand
                //queue should be declared by microservice itself
                channel.queueDeclare(queueName, true, false, false, null);

                //creating DynamicQueueBindingsManager class using DeploymentVersionTracker
                DynamicQueueBindingsManager dynamicQueue = new DynamicQueueBindingsManagerImpl(trackerActive);

                //saving bindingId to unbind in future
                int bindingId = dynamicQueue.queueBindDynamic(conn, queueName, exchangeName, "", null);

                byte[] messageBodyBytes = "test-message".getBytes();
                channel.basicPublish(exchangeName+"-v1", "", null, messageBodyBytes);

                GetResponse response = channel.basicGet(queueName, false);
                //process response

                //queue unbind example
                dynamicQueue.queueUnbindDynamic(conn, queueName, exchangeName, "", null, bindingId);

                }
            }
        }
```
