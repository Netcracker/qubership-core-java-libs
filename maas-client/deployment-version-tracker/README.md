<!-- TOC -->
* [What is it](#what-is-it)
  * [TrackingVersionFilterImpl](#trackingversionfilterimpl)
  * [Migration to Blue Green 2](#migration-to-blue-green-2)
<!-- TOC -->

# What is it
This module provides java predicate implementation to test acceptable values of `X-Version` header for running      implementation that tracks state of Blue/Green in a runtime 
Implementing Blue/Green approach we need a tool to filter messages by value in `X-Version` header received from
Kafka topic.

deployment-version-tracker module provides single interface:
```
public interface TrackingVersionFilter extends Predicate<String> {
}
```
with two implementations: 
* `TrackingVersionFilterImpl` - implementation intended to be used in production code
* `InMemoryTrackingVersionFilter` - implementation for localdev and test purposes
* `LocalDevTrackingVersionFilter` - obsolete implementation. Migrate to `InMemoryTrackingVersionFilter`  


## TrackingVersionFilterImpl

Filter clause value changed during Blue/Green states according to diagram below: 

```plantuml
@startuml

skinparam state {
    FontColor Black
    BorderColor Black
        
    BackgroundColor<<Standalone>> #FFEB3B
    BackgroundColor<<Idle>> #9E9E9E
    BackgroundColor<<Blue>> #2196F3
    BackgroundColor<<Green>> #4CAF50
    BackgroundColor<<Legacy>> #FFC107
}

state "Standalone" as Standalone <<Standalone>> {
  state "Active" as StandaloneActive <<Green>> {
    StandaloneActive :<color:black> version: null
    StandaloneActive :<color:black> filter: true
  }
}

state "BGInitiated" as BGInitiated <<Green>> {
  state "Active" as BGInitiatedActive <<Green>> {
   BGInitiatedActive :<color:black> version: 5
   BGInitiatedActive :<color:black> filter: true
  }
  state "Idle" as BGInitiatedIdle <<Idle>> {
   BGInitiatedIdle : version: null
   BGInitiatedIdle : filter: N/A
  }
}

state "BGPrepare" as BGPrepare <<Green>> {
  state "Active" as BGPrepareActive <<Blue>> {
   BGPrepareActive : version: 5
   BGPrepareActive : filter: v != 6
  }
  state "Candidate" as BGPrepareCandidate <<Green>> {
   BGPrepareCandidate : version: 6
   BGPrepareCandidate : filter: v == 6
  }
}

state "BGFinalize" as BGFinalize  <<Blue>> {
  state "Legacy" as BGFinalizeLegacy <<Legacy>> {
   BGFinalizeLegacy : version: 5
   BGFinalizeLegacy : filter: v == 5
  }
  state "Active" as BGFinalizeActive <<Blue>> {
   BGFinalizeActive : version: 6
   BGFinalizeActive : filter: v != 5
  }
}

StandaloneActive --> StandaloneActive : Rolling Update
Standalone -down-> BGInitiated : Migrate

BGInitiatedActive -> BGInitiatedActive : Rolling Update
BGInitiated -right-> BGPrepare : Warmup

BGPrepare -left-> BGInitiated : Commit
BGFinalize -> BGInitiated : Commit

BGPrepareActive -> BGPrepareActive : Update Active
BGPrepareCandidate -> BGPrepareCandidate : Update Candidate

BGPrepare -down-> BGFinalize : Promote
BGFinalize -> BGPrepare : Rollback

BGFinalizeLegacy -> BGFinalizeLegacy : Update Legacy
BGFinalizeActive -> BGFinalizeActive : Update Active

@enduml
```

## Migration to Blue Green 2
TrackingVersionFilterImpl implementation for Blue Green 2 depends on com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher from blue-green-state-monitor-java.  
Refer to blue-green-state-monitor-java's documentation regarding how to set up BlueGreenStatePublisher.

Usage:

```java
    public void newFilter(BlueGreenStatePublisher publisher) {
    var filter = new TrackingVersionFilterImpl(publisher);
    var result = filter.test("v1");
}
```
