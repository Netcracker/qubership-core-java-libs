framework-extension-bom
--------

This BOM contains all framework-extensions libraries.    

#### Usage

Add the following artifact to your POM:

```xml
 <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.qubership.cloud</groupId>
                <artifactId>framework-extension-bom</artifactId>
                <version>{VERSION}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

After that you can add any library from `dependencyManagement` from [POM](./pom.xml) without specifying the version.  
For example:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>framework-extension-health-indicators</artifactId>
    </dependency>
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>framework-extension-springdoc-swagger</artifactId>
    </dependency>
```