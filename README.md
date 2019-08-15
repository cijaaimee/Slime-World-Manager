# Slime World Manager [![Build Status](https://travis-ci.com/Grinderwolf/Slime-World-Manager.svg?branch=master)](https://travis-ci.com/Grinderwolf/Slime-World-Manager)
Slime World Manager is a Minecraft plugin that implements the Slime Region Format, developed by the Hypixel Dev Team. Its goal is to provide server administrators with an easy-to-use tool to load worlds faster and save space.

## Building

To build SWM, use the command:

```
mvn clean install
```

## Releases

SWM releases can be found [here](https://www.spigotmc.org/resources/slimeworldmanager.69974/history).

## Maven
```
<repository>
    <id>swm-repo</id>
    <url>https://repo.glaremasters.me/repository/concuncan/</url>
</repository>
```
```
<dependency>
    <groupId>com.grinderwolf</groupId>
    <artifactId>slimeworldmanager-api</artifactId>
    <version>1.0.2</version>
    <scope>provided</scope>
</dependency>
```

## Javadocs

Javadocs can be found [here](https://grinderwolf.github.io/Slime-World-Manager/apidocs/).

## Credits

Thanks to:
 * All the contributors who helped me by adding features to SWM.
 * [Glare](https://glaremasters.me) for providing me with a Maven repository.
 * [Minikloon](https://twitter.com/Minikloon) and all the [Hypixel](https://twitter.com/HypixelNetwork) team for developing the SRF.
