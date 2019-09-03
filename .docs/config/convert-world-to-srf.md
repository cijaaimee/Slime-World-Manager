To be able to load a world with SWM, you have to convert it to the SRF. There are two ways of doing this:
## Using the in-game command (recommended)
1. Place your world inside your server's root directory.
2. Make sure the world is unloaded. Loaded worlds cannot be converted.
3. Run the command `/swm import <your-world-folder> <data-source> [new-world-name]`. If you want the world to have the same name as the world folder, simply ignore the _[new-world-name]_ argument.
4. Done! The world is now inside the data source you've provided.

## Using the importer tool

The usage of this tool is disacouraged, as the alternative provided above is faster and easier to use. However, if you still want to use it, here are the steps to be followed:
1. Place your world alongside the importer tool.
2. Open cmd.
3. Type this command:
```
java -jar slimeworldmanager-importer-1.0.0.jar <your-world-folder>
```

It'll automatically scan your world and generate a .slime file inside the same directory you placed it. That's your world!