# Slime World Manager [![Build Status](https://travis-ci.com/Grinderwolf/Slime-World-Manager.svg?branch=master)](https://travis-ci.com/Grinderwolf/Slime-World-Manager)

[<img src="https://discordapp.com/assets/e4923594e694a21542a489471ecffa50.svg" alt="" height="55" />](https://discord.gg/P9Pd58d)

Slime World Manager is a Minecraft plugin that implements the Slime Region Format, developed by the Hypixel Dev Team.
 Its goal is to provide server administrators with an easy-to-use tool to load worlds faster and save space.

#### Releases

SWM releases can be found [here](https://www.spigotmc.org/resources/slimeworldmanager.69974/history).

## Using SWM in your plugin

#### Maven
```
<repositories>
  <repository>
    <id>glaremasters</id>
    <url>https://repo.glaremasters.me/repository/concuncan/</url>
  </repository>
</repositories>
```

```
<dependencies>
  <dependency>
    <groupId>com.grinderwolf</groupId>
    <artifactId>slimeworldmanager-api</artifactId>
    <version>INSERT LATEST VERSION HERE</version>
  </dependency>
</dependencies>
```

#### Gradle
```
repositories {
    maven { url "https://repo.glaremasters.me/repository/concuncan/" }
}

dependencies {
    compileOnly group: "com.grinderwolf", name: "slimeworldmanager-api", version: "INSERT LATEST VERSION HERE";
}
```

#### Javadocs

Javadocs can be found [here](https://grinderwolf.github.io/Slime-World-Manager/apidocs/).

## Wiki Overview
 * Plugin Usage
    * [Installing Slime World Manager](.docs/usage/install.md)
    * [Using Slime World Manager](.docs/usage/using.md)
    * [Commands and permissions](.docs/usage/commands-and-permissions.md)
 * Configuration
    * [Setting up the data sources](.docs/config/setup-data-sources.md)
    * [Converting traditional worlds into the SRF](.docs/config/convert-world-to-srf.md)
    * [Configuring worlds](.docs/config/configure-world.md)
    * [Async world generation](.docs/config/async-world-generation.md)
 * SWM API
    * [Getting started](.docs/api/setup-dev.md)
    * [World Properties](.docs/api/properties.md)
    * [Loading a world](.docs/api/load-world.md)
    * [Migrating a world](.docs/api/migrate-world.md)
    * [Importing a world](.docs/api/import-world.md)
    * [Using other data sources](.docs/api/use-data-source.md)
 * [FAQ](.docs/faq.md)

## Credits

Thanks to:
 * All the contributors who helped me by adding features to SWM.
 * [Glare](https://glaremasters.me) for providing me with a Maven repository.
 * [Minikloon](https://twitter.com/Minikloon) and all the [Hypixel](https://twitter.com/HypixelNetwork) team for developing the SRF.
 
## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
