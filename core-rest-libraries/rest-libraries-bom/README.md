rest-libraries-bom
--------

This is a BOM which contains all necessary rest libraries and as well as rest-third-party libraries.    

#### Usage

Add the following artifact to your POM:

```xml
 <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.netcracker.cloud</groupId>
                <artifactId>rest-libraries-bom</artifactId>
                <version>{VERSION}</version>
                <scope>import</scope>
                <type>pom</type>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

After it you can add any library from `dependencyManagement` from [POM](./pom.xml) without specifying a version. For example:

```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>config-server-loader-resttemplate</artifactId>
    </dependency>
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>route-registration-resttemplate</artifactId>
    </dependency>
```

List of supported libraries:
```
    * webclient
    * route-registration-resttemplate
    * route-registration-webclient
    * config-server-loader-resttemplate
    * config-server-loader-webclient
    * restclient
    * resttemplate
```
