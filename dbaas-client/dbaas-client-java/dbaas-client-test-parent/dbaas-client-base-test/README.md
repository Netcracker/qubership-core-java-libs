# DBaaS client base test utils 

This is a plain java DBaaS client library that provides utility methods for your tests.

- [How to use](#how-to-use)
    * [Steps to set up library](#setting-up-the-library)
    * [DbaaS pool test utils](#dbaas-pool-test-utils)


## How to use

### Setting Up the Library
To set up the library, add the following dependencies to your `pom.xml`:
```xml
<dependency>
    <groupId>com.netcracker.cloud</groupId>
    <artifactId>dbaas-client-base-test</artifactId>
    <version>${dbaas-client-base-test.version}</version>
</dependency>

        <!-- Alternatively, you can add a dependency on dbaas-client-bom -->

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.netcracker.cloud</groupId>
            <artifactId>dbaas-client-bom</artifactId>
            <version>${dbaas-client-bom.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>com.netcracker.cloud</groupId>
    <artifactId>dbaas-client-base-test</artifactId>
</dependency>
```

### DbaaS pool test utils
The `DatabasePoolTestUtils` class offers methods for tests that allow you to work with the DBaaS cache.

#### Initialization
To use `DatabasePoolTestUtils`, create an instance by passing a DatabasePool. In your integration tests, you can inject a 
`DatabasePool` as a Spring bean and then construct a DatabasePoolTestUtils instance in the `@BeforeEach` method, like this:

```java
    @Autowire
    private DatabasePool databasePool;

    private DatabasePoolTestUtils databasePoolTestUtils;

    @BeforeEach
    public void init(){
        databasePoolTestUtils = new DatabasePoolTestUtils(databasePool);
    }
```
#### Clear database pool cache
`DatabasePoolTestUtils` provides a `clearCache()` method that clears the cache and removes all previously registered classifiers 
and database clients from the cache. To use this method, you should [initialize](#initialization) the `DatabasePoolTestUtils` and call `clearCache().` 
It's a good practice to call this method in the `@AfterEach` method of your JUnit tests.

```java
    @AfterEach
    public void tearDown(){
        databasePoolTestUtils.clearCache();
    }
```
