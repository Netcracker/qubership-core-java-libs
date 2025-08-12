# DBaaS Client BOM Publish

DBaaS client BOM contains different dbaas client libraries which can be useful for clients. 

Usually you will obtain necessary dbaas-client libraries by using [qubership-springboot-starter-parent](<github link todo>/springboot-starter), 
but in case you don't use qubership-springboot-starter-parent, you can use this BOM to avoid version conflicts:
```xml
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
```
Where `${dbaas-client-bom.version}` is the latest release version of dbaas-client
