![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-dbaas-client)
![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-dbaas-client)
![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-dbaas-client)
![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-dbaas-client)
![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-dbaas-client)

# Overview

This repository should contain all supported DbaaS client libraries. 

Right now it includes: 
 - for Java: 
   - dbaas-client-core : Java (Spring powered) wrapper of DbaaS HTTP API 
     - it would require a few Spring dependencies in runtime, but you can use it without Spring Context (though it's not properly tested)
     - it could be configured using system/applycation.yml/config-server properties (see https://<github link todo>/Dbaas+Client+Configuration)
   - for Spring Boot:
     - dbaas-client-mongo-starter : module you can include in dependencies to connect to Mongo database using DbaaS
         - by default classifies databases based on default classifier ( https://<github link todo>/DbaaS+Database+Classifier#DbaaSDatabaseClassifier-Mongo )
     - dbaas-client-postgres-starter : module you can include in dependencies to connect to Postgres database using DbaaS
         - default classifier described in https://<github link todo>/DbaaS+Database+Classifier#DbaaSDatabaseClassifier-Postgres

# Contribution

You may add support of client for some new language/framework the following way:

### clone it
``` 
  git clone https://<github link todo>/dbaas-client.git 
  cd dbaas-client
```

### create subfolder
```
mkdir dbaas-client-mylanguage-myframework
cd dbaas-client-mylanguage-myframework
```

### Create module
Need to create module with name that contains your programming language
```
mkdir dbaas-client-<programming-language> && cd dbaas-client-<programming-language>
```

### Create folder: 
If you are writing common client then `dbaas-client-root`, if with some framework then `dbaas-client-framework`, if starter then `dbaas-client-<db type>-<framework>-starter`. For example:
```
mkdir dbaas-client-root && cd dbaas-client-root
```

## Notes 
By default client should send http requests to `http://dbaas-agent:8080` , but let it be possible to configure address. 

You can also support config-server ( https://cloud.spring.io/spring-cloud-config/ ) 
usually available in cloud on address `http://config-server:8080` ( more info in https://<github link todo>/be%3A+Config+Server )
and read configurations from properties described in https://<github link todo>/Dbaas+Client+Configuration . 
Note that even while config-server designed to be included in Spring Boot, it has HTTP API: 
https://<github link todo>/Saas+Cloud%3A+Config+Server+API 

For example default address mentioned earlier ( `http://dbaas-agent:8080` ) is expected to be configured using property "dbaas.api.address".

Even if you not going to support config-server, still try to support at least system properties or environment variable with same naming 
and add information about configuration available in README.md in your subfolder (we would update https://<github link todo>/Dbaas+Client+Configuration after review).

If you are going to make it much simpler for users of client library to start working with DbaaS, you may also 
want to support default DbaaS Classifier described in https://<github link todo>/DbaaS+Database+Classifier.

This way users of library would not need to understand the concept of classifier (until they need to extend/override it),
but only know how to use specific database in the mylanguage/myframework. 


## build 
Currently dbaas-client only can be built using maven. Root (aggregator) pom.xml is placed at the root of the project. 
To add your pom.xml in the build you need to create and fill it in your subfolder and then add it as a module in root pom.xml
Please firstly refer to https://maven.apache.org/pom.html#Aggregation to read more about modules aggregation in maven. 
Knowing the conecept would strongly speed up your build integration.

## test/sample
It would be convenient to create in addition to your client module another module with sample service.
Create for example sample-dbaas-client-mylanguage-myframework-service-test , mock DbaaS API (as it usually being mocked in mylanguage), write sample service and unit tests. 
Then include module in root pom.xml same way you've done it with dbaas-client-mylanguage-myframework. 

You should expect CI to build all that with `mvn install` during maven release process. Knowing that you can set dependency from 
sample-dbaas-client-mylanguage-myframework-service-test to dbaas-client-mylanguage-myframework and include both modules in root pom.xml in correct order.

# Spring based libraries
For the most cases it's recommended to use these database specific starters:
* [dbaas-client-postgres-starter](./dbaas-client-java/dbaas-client-postgres-starter/README.md)
* [dbaas-client-mongo-starter](./dbaas-client-java/dbaas-client-mongo-starter/README.md)
* [dbaas-client-cassandra-starter](./dbaas-client-java/dbaas-client-cassandra-starter/README.md)
* [dbaas-client-opensearch-starter](./dbaas-client-java/dbaas-client-opensearch-starter/README.md)
* [dbaas-client-cassandra-starter](./dbaas-client-java/dbaas-client-cassandra-starter/README.md)