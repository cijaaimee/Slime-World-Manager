### Building

To build SWM, execute the following command int the project root:

```
mvn clean install
```

## Using the API

If your plugin wants to use Slime World Manager add the following in your pom.xml

### Maven

```xml
<repository>
    <id>swm-repo</id>
    <url>https://repo.glaremasters.me/repository/concuncan/</url>
</repository>
```
```xml
<dependency>
    <groupId>com.grinderwolf</groupId>
    <artifactId>slimeworldmanager-api</artifactId>
    <version>(insert latest version here)</version>
    <scope>provided</scope>
</dependency>
```