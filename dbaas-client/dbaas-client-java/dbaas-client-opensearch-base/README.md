# DBaaS Client Opensearch Base

Plain java opensearch client implementation. This library does not contain spring dependencies and 
if you do not use spring then you can take this library for using opensearch client created by dbaas client.

* [Usage](#usage)
* [Restrictions](#restrictions)
    - [How to use examples](#how-to-use-examples)
        - [GetSettingsResponse](#getsettingsresponse)
        - [GetIndexResponse](#getindexresponse)
        - [CreateIndex](#createindex)
        - [DeleteIndex](#deleteindex)
    - [Native OpenSearchClient](#native-opensearchclient)
    - [Spring module](#spring-module)

## Usage
Here are the steps describing how to use dbaas-client-opensearch-base library: 
1. Add maven dependency: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-opensearch-base</artifactId>
        <version>${dbaas-client-opensearch-base.version}</version>
    </dependency>
    ```
    Where `${dbaas-client-opensearch-base.version}` is the latest release version that can be found on page [Release Version 3.X](https://github.com/Netcracker/qubership-dbaas-client/releases)
    Omit tag `version` if you use [qubership-springboot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). if you use [qubership-springboot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md).
3.  Initialize `DbaasClient` and `DatabasePool` to work with DBaaS REST API. The easiest way is to use [dbaas-client-light](../dbaas-client-light/README.md)
    module which provides `DbaasClient` implementation based on OkHttp3 library. 
    
4. To obtain `OpenSearchClient` objects use whether `DbaasOpensearchClientImpl` class or `DatabasePool` instance directly.  

## Attention!
Since 5.0.0 version `dbaas-client-opensearch-renovated-base` has been renamed to `dbaas-client-opensearch-base`

## Restrictions

All requests to indices, templates or aliases via client must be modified with `normalize(target_name)` method.
This method adds prefix with delimiter to `target_name` and returns full name with prefix. If you pass full name to normalize then function returns full name.

Example:
```java
ExistsRequest existsRequest = new ExistsRequest.Builder().index(serviceClient.normalize("uniq_name")).id("1").build(); //serviceClient.normalize() returns full_name like '{prefix}{delimiter}uniq_name'
boolean exists = serviceClient.getClient().indices().exists(existsRequest).value();
```

Prefix and delimiter can be set in `application.properties` file. More information about configuration for spring can be found at [dbaas-client-opensearch-starter](../dbaas-client-opensearch-starter/README.md)

### How to use examples

#### GetSettingsResponse
In order to get settings for specific index from response you have to use full index name with prefix.
```java
DbaasOpensearchClient client;

GetIndicesSettingsRequest getSettingsRequest = new GetIndicesSettingsRequest.Builder().index(client.normalize(targetIdx)).build();
GetIndicesSettingsResponse getSettingsResponse = client.getClient().indices().getSettings(getSettingsRequest);
String numberOfShardsString = getSettingsResponse.get(client.normalize(targetIdx)).settings().index().numberOfShards(); //client.normalize() returns full_name like '{prefix}{delimiter}{targetIdx}'
```


#### GetIndexResponse
During work with GetIndexResponse in order to get information about specific index use index name with prefix.
```java
DbaasOpensearchClient client;

GetIndexRequest request = new GetIndexRequest.Builder().index(client.normalize(anotherIdx)).build(); //client.normalize() returns full_name like '{prefix}{delimiter}{anotherIdx}'
GetIndexResponse getIndexResponse = client.getClient().indices().get(request);
Map<String, Alias> indexAliases = getIndexResponse.get(client.normalize(targetIdx)).aliases(); //client.normalize() returns full_name like '{prefix}{delimiter}{anotherIdx}'
assertTrue(indexAliases.containsKey(client.normalize(targetIdx)));
```


#### CreateIndex
```java
DbaasOpensearchClient client;

CreateIndexRequest request = new CreateIndexRequest.Builder().index(client.normalize("uniq_index_name")).build();
CreateIndexResponse createIndexResponse = client.getClient().indices().create(request);
assertEquals(fullIndexName, createIndexResponse.index());
```



#### DeleteIndex
```java
DbaasOpensearchClient client;

DeleteIndexRequest request = new DeleteIndexRequest.Builder().index(client.normalize("uniq_index_name")).build();
AcknowledgedResponseBase deleteIndexResponse = client.getClient().indices().delete(request);
assertTrue(deleteIndexResponse.acknowledged());
```


### Native OpenSearchClient

If you want to use native `OpenSearchClient`, it can be obtained by `serviceClient.getClient()`. But **pay attention**
`you have to call serviceClient.getClient() method every time before each operation with OpenSearchClient` in order
to provide some dbaas functionality such as password validation, reconnection, multi-tenancy and so on.

example:
```java
DbaasOpensearchClient serviceClient;

// index creation
CreateIndexRequest request = new CreateIndexRequest.Builder().index(serviceClient.normalize("uniq_index_name")).build();
CreateIndexResponse createIndexResponse = serviceClient.getClient().indices().create(request);
assertEquals(fullIndexName, createIndexResponse.index());

// get index
GetIndexRequest request = new GetIndexRequest.Builder().index(serviceClient.normalize("uniq_index_name")).build();
GetIndexResponse getIndexResponse = serviceClient.getClient().indices().get(request);

// put data
PutMappingRequest putMappingRequest = new PutMappingRequest.Builder().index(serviceClient.normalize(TEST_INDEX)).properties(...).build();
AcknowledgedResponseBase putMappingResponse = serviceClient.getClient().indices().putMapping(putMappingRequest);
assertTrue(putMappingResponse.acknowledged());
```

## Spring module
If you want to work with spring. You need to follow to [dbaas-client-opensearch-starter](../dbaas-client-opensearch-starter/README.md)
