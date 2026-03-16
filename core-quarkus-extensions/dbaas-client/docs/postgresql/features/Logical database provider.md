There may be situations when you don't need to create a `logical database` through DbaaS 
aggregator but you want to use `quarkus-dbaas-postgresql-client`. It can be useful during local development or 
when using different profiles. This can be achieved by providing information about a logical database by yourself.   
So, in order to use this features you should do the following things:  

1) Extend `PostgresqlLogicalDbProvider#PostgresqlLogicalDbProvider` and implement its abstract methods;  
2) Mark your implementation as quarkus bean.  
*For example:*
```java
@ApplicationScoped
public class ZkPostgresqlLogicalDbProvider extends PostgresqlLogicalDbProvider {

    @Override
    public int order() {
        return 0;
    }

    @Override
    // method may return NULL
    public PostgresDatabase provide(
            SortedMap<String, Object> classifier,
            DbCreateParameters params, String namespace) {

        PostgresDatabase postgresDatabase = new PostgresDatabase();
        PostgresDBConnection connectionProperties =
                new PostgresDBConnection("root", "root", "root");
        postgresDatabase.setConnectionProperties(
                connectionProperties
        );
        postgresDatabase.setClassifier(classifier);
        return postgresDatabase;  
    }
}
```  
That's all but there are some nuances:
* Provider with the lowest order will be processed first. 
Default DbaaS provider has `Integer.MAX_VALUE` value.
*  Method `PostgresqlLogicalDbProvider#provide` may return NULL value. In this case we iterate to next provider
 in a order. If you need to interrupt processing you have to throw a exception.
* `PostgresDBConnection` has method `setDataSource(Datasource)`. Do not use it 
because we init datasource on our own and set datasource in `PostgresqlLogicalDbProvider#provide` does not make sense. 