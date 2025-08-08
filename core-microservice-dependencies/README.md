Overview
--------
`cloud-core-java-bom` is a BOM which aggregates cloud-core libraries except the `microesrvice-framework` library. 
This approach allows avoiding version conflicts and add dependencies without think of a version.

 Usage
 -----
 
 For using this library you just need to put `cloud-core-java-bom` into dependency management section of your POM file. 
 For example:
 
 ```xml
    <dependencyManagement>
           <dependencies>
               <dependency>
                   <groupId>com.netcracker.cloud</groupId>
                   <artifactId>cloud-core-java-bom</artifactId>
                   <version>{VERSION}</version>
                   <type>pom</type>
                   <scope>import</scope>
               </dependency>
           </dependencies>
       </dependencyManagement>
```
