### Releases

SWM releases can be found [here](https://www.spigotmc.org/resources/slimeworldmanager.69974/history).


### How to install SWM

Installing SWM is an easy task. First, download the latest version from the Spigot [resource page](https://www.spigotmc.org/resources/slimeworldmanager.69974/). Then, follow this step:
1. Place the downloaded `slimeworldmanager-plugin-<version>.jar` file inside your server's plugin folder.
2. Place the `slimeworldmanager-classmodifier-<version>.jar` file inside your server's main directory **(not the plugins folder)**.
3. Modify your server startup command and at this argument before '-jar':
```
-javaagent:slimeworldmanager-classmodifier-<version>.jar
```

That's it! Easy, right?
