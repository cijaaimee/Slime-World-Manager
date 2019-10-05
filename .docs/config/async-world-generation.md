Async World Generation is a feature added in SWM 2.1.0. When enabled, worlds and their chunks are loaded on a separate thread, so there is no performance impact at all. This setting is available for every supported Minecraft version up to 1.13.2. Sadly, due to some changes on spigot 1.14.4, it is not possible to add support for it.

## How to enable Async World Gen
To enable Async World Gen, open the main config file and set `enable_async_world_gen` to `true`. Note that changing this setting requires restarting the server.