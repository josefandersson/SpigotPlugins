# TownyChunkLoader
Don't miss out on plant growth just because you go offline. This plugin allows players to select 1 or multiple chunks as "chunk loaders" which let's crops, cacti, sugar cane and sapling grow even when the chunks are unloaded. (Contradictory to the name, this plugin doesn't actually keep chunks loaded, but rather simulates Minecraft's random ticks whenever a "chunk loader" chunk is loaded. Thus, in effect the end-result is very similar except for auto-farmers not working as efficiently, or at all.) 

## How to use
Use `/chunkloader set` to set current chunk as a chunk loader.

## Commands
The main command, `/chunkloader`, may be shortened to just `/cl`.
- `/chunkloader` or `/chunkloader help` - list all commands. (Alias: `/cl`)

- `/chunkloader list` - list your own chunk loaders. (Alias: `/cl l`)
- `/chunkloader list all` - list all chunk loaders on the server. (Alias: `/cl l a`)
- `/chunkloader list player <username>` - list all chunk loaders owned by specified player. (Alias: `/cl l p <username>`) 

- `/chunkloader set` - set current chunk as chunk loader. (Alias: `/cl s`)

- `/chunkloader delete` - remove chunk loader from current chunk. (Alias: `/cl d`)
- `/chunkloader delete <x> <z> [worldname]` - remove chunk loader chunk at chunk x, z in current world or another world. (Alias: `/cl d <x> <z> [worldname]`)

- `/chunkloader bump` - resets timers for all chunk loaders. (Alias: `/cl b`)

- `/chunkloader here` - check if there's a chunk loader in the current chunk and gives info about the chunk loader. (Alias: `/cl h`)

- `/chunkloader forcedelete` - remove all chunk loaders on the server. (Alias: `/cl fd`)
- `/chunkloader forcedelete world <worldname>` - remove all chunk loaders in the specified world. (Alias: `/cl fd w <worldname>`)
- `/chunkloader forcedelete player <username>` - remove all chunk loaders owned by specified player. (Alias: `/cl fd p <username>`)

- `/chunkloader forceprogress <seconds>` - force updates to current chunk as if it had been unloaded for X seconds. (Alias: `/cl fp <seconds>`)

## Permissions
- `townychunkloader.command` - allowed to use chunk loaders.

- `townychunkloader.command.list.all` - allowed to list all chunks on the server.
- `townychunkloader.command.list.player` - allowed to list all chunks owned by a player.

- `townychunkloader.command.here` - allowed to see more information (such as the owner) about other's chunk loaders.

- `townychunkloader.command.forcedelete` - allowed to use the forcedelete command.

- `townychunkloader.command.forceprogress` - allowed to use the forceprogress command.

- `townychunkloader.chunks.<numChunks>` - if present, this is the max amount of chunk loaders a player may create. If not present, the player may create unlimited chunk loaders. *(Note: if this is changed after a chunk loader is created, the chunk loader will still use the values from when the chunk loader was created.)*
- `townychunkloader.chunks.exempt` - if present, the above permission will be ignored and the player may create unlimited chunk loaders. *(Note: if this is changed after a chunk loader is created, the chunk loader will still use the values from when the chunk loader was created.)*

- `townychunkloader.time.<hours>` - if present, this is the max amount of hours a chunk loader will work before it has to be bumped. *(Note: if this is changed after a chunk loader is created, the chunk loader will still use the values from when the chunk loader was created.)*
- `townychunkloader.time.exempt` - if present, the above permission will be ignored and the player will never have to bump their chunk loader. *(Note: if this is changed after a chunk loader is created, the chunk loader will still use the values from when the chunk loader was created.)*

## Config.yml
- `growthMultiplier` - Increase or decrease growth time for crops in chunk loaders when they are unloaded. A value of `1` means all will grow as normal, `0.5` is half as fast, `2` is twice as fast. 

- `maxUpdatesPerTick` - Number of crops allowed to be processed every tick (20 ticks = 1 second). Increasing this will increase server lag. Decreasing this may cause client lag for a brief period of time when the chunk is being processed. Defaults to `10`.

- `maxTimeHours` - Max number of hours to check for the permission `townychunkloader.time.<hours>` when setting a new chunk loader.

- `postgresql_url` - PostgreSQL server url in JDBC format (eg: `jdbc:postgresql://example.com:5432/database`)
- `postgresql_schema` - PostgreSQL schema to create TownyChunkLoader tables in.
- `postgresql_username` - PostgreSQL server username.
- `postgresql_password` - PostgreSQL server password.

- `worlds` - List of worlds with special properties. Using `worlds.<worldname>.<property>` to set one of the following properties `growthMultiplier`. For example `worlds.Towny.growthMultiplier: 5` will make crops grow 5x faster in the world `Towny`.

## Issues
- A large number of chunk loaders in close proximity may take a while to process.