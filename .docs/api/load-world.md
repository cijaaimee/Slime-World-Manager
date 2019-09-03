First, retrieve the SlimeWorldManager plugin API:
```java
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
```
Now, you need a loader. A SlimeLoader is a class that reads and stores worlds from a data source. In this case, we'll be using the MySQL loader:
```java
SlimeLoader sqlLoader = plugin.getLoader("mysql");
```

Before actually loading the world, you need a SlimeProperties object, which contains all the settings for the world. To create one, use the SlimeProperties.builder() method. Here's an example of a SlimeProperties object:
```java
SlimeWorld.SlimeProperties props = SlimeWorld.SlimeProperties.builder().difficulty(0).allowAnimals(false).allowMonsters(false).spawnX(96).spawnY(50).spawnZ(47).pvp(false).readOnly(true).build();
```

That's it, you've got everything you need! Now, let's load the world from the data source and generate it:
```java
try {
    // Note that this method should be called asynchronously
    SlimeWorld world = plugin.loadWorld(sqlLoader, "my-world", props);

    // This method must be called synchronously
    plugin.generateWorld(world);
} catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException | WorldInUseException | UnsupportedWorldException ex) {
    /* Exception handling */
}
```