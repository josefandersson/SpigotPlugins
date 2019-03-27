package org.josefadventures.townychunkloader;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.event.TownUnclaimEvent;
import com.palmergames.bukkit.towny.object.*;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TownyChunkLoader extends JavaPlugin implements Listener {

    private final String PERMISSION_COMMAND = "townychunkloader.command";
    private final String PERMISSION_COMMAND_LIST_ALL = "townychunkloader.command.list.all";
    private final String PERMISSION_COMMAND_LIST_PLAYER = "townychunkloader.command.list.player";
    private final String PERMISSION_COMMAND_HERE = "townychunkloader.command.here";
    private final String PERMISSION_COMMAND_FORCEDELETE = "townychunkloader.command.forcedelete";
    private final String PERMISSION_CHUNKS_ = "townychunkloader.chunks.";
    private final String PERMISSION_CHUNKS_EXEMPT = "townychunkloader.chunks.exempt";
    private final String PERMISSION_TIME_ = "townychunkloader.time.";
    private final String PERMISSION_TIME_EXEMPT = "townychunkloader.time.exempt"; // TODO:

    public static TownyChunkLoader instance;

    private HashMap<String, ChunkLoaderv1> chunkLoaders;
    private int maxTimeHours = 168; // 1 week

    @Override
    public void onEnable() {
        Towny towny = (Towny) this.getServer().getPluginManager().getPlugin("Towny");

        if (towny == null) {
            this.getPluginLoader().disablePlugin(this);
            this.getLogger().warning("TownyChunkLoader needs Towny to work.");
            return;
        } else {
            this.getServer().getPluginManager().registerEvents(this, this);
            this.getLogger().info("TownyChunkLoader running as should.");
        }

        instance = this;

        this.chunkLoaders = new HashMap<>();

        this.getConfig().addDefault("chunkLoaders", new String[] {});
        this.getConfig().addDefault("maxTimeHours", this.maxTimeHours);
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();

        this.maxTimeHours = getConfig().getInt("maxTimeHours");

        // TODO: Load chunks on startup. cl.bump() should do that.
        for (String str : this.getConfig().getStringList("chunkLoaders")) {
            ChunkLoaderv1 cl = ChunkLoaderv1.fromString(str);
            if (cl != null) {
                this.chunkLoaders.put(cl.id(), cl);
            }
        }
        this.getLogger().info("Loaded " + this.chunkLoaders.size() + " chunk loaders.");

        Chunk c = getServer().getWorld("Towny").getChunkAt(18, 68);
        c.load(); c.setForceLoaded(true);
        this.getServer().getScheduler().runTaskLater(this, () -> {
            this.chunkLoaders.get(ChunkLoaderv1.chunkId(c)).updateCropsList();
        }, 20);
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            randomTickChunk(c);
        }, 20*5, 20);
    }

    @Override
    public void onDisable() {
        ArrayList<String> strs = new ArrayList<>();
        for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
            strs.add(cl.toString());
        }
        this.getConfig().set("chunkLoaders", strs);
        this.saveConfig();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(PERMISSION_COMMAND)) {
            sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GREEN + "TownyChunkLoader commands:");
            sender.sendMessage(ChatColor.DARK_GREEN + "/cl help " + ChatColor.GREEN + "- Display this message.");
            sender.sendMessage(ChatColor.DARK_GREEN + "/cl list " + ChatColor.GREEN + "- List your own chunk loaders.");

            if (sender.hasPermission(PERMISSION_COMMAND_LIST_ALL))
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl list all " + ChatColor.GREEN + "- List all chunk loaders on the server.");
            if (sender.hasPermission(PERMISSION_COMMAND_LIST_PLAYER))
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl list player <player> " + ChatColor.GREEN + "- List all chunk loaders owned by specified player.");

            sender.sendMessage(ChatColor.DARK_GREEN + "/cl set " + ChatColor.GREEN + "- Attempt to set current chunk as chunk loader.");
            sender.sendMessage(ChatColor.DARK_GREEN + "/cl delete [chunk name] " + ChatColor.GREEN + "- Remove chunk loader from current chunk, or from chunk with name.");
            sender.sendMessage(ChatColor.DARK_GREEN + "/cl bump " + ChatColor.GREEN + "- Resets timers for all chunk loaders.");

            if (sender.hasPermission(PERMISSION_COMMAND_HERE))
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl here " + ChatColor.GREEN + "- Check if there's a chunk loader in the current chunk and gives info about the chunk loader.");
            else
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl here " + ChatColor.GREEN + "- Check if there's a chunk loader in the current chunk.");

            if (sender.hasPermission(PERMISSION_COMMAND_FORCEDELETE)) {
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl forcedelete " + ChatColor.GREEN + "- Remove all chunk loaders on the server.");
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl forcedelete world <world> " + ChatColor.GREEN + "- Remove all chunk loaders in the specified world.");
                sender.sendMessage(ChatColor.DARK_GREEN + "/cl forcedelete player <player> " + ChatColor.GREEN + "- Remove all chunk loaders owned by specified player.");
            }

            return true;
        }

        String subCmd = args[0].toLowerCase();

        if (subCmd.equals("list") || subCmd.equals("l")) {
            onCommandList(sender, cmd, args);
            return true;
        } else if (subCmd.equals("forcedelete") || subCmd.equals("forceremove")) {
            onCommandForcedelete(sender, cmd, args);
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Either your command was unrecognized or it cannot be used in console.");
            return true;
        }

        Player player = (Player) sender;

        if (subCmd.equals("set") || subCmd.equals("s"))
            onCommandSet(player);
        else if (subCmd.equals("delete") || subCmd.equals("del") || subCmd.equals("remove"))
            onCommandDelete(player, args);
        else if (subCmd.equals("bump") || subCmd.equals("b"))
            onCommandBump(player);
        else if (subCmd.equals("here") || subCmd.equals("h"))
            onCommandHere(player);
        else
            sender.sendMessage(ChatColor.RED + "Couldn't recognize your command. Use /cl help to list all commands.");

        return true;
    }

    private void onCommandList(CommandSender sender, Command cmd, String[] args) {
        switch (args.length) {
            case 1: // list own
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only '/cl list all' and '/cl list player <player>' can be used in the console.");
                    return;
                }

                sender.sendMessage(ChatColor.GREEN + "Listing your chunk loaders:");
                UUID uuid = ((Player) sender).getUniqueId();
                for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
                    if (cl.ownerUUID.equals(uuid)) {
                        Chunk c = cl.getChunk();
                        c.setForceLoaded(false);
                        sender.sendMessage(ChatColor.DARK_GREEN + "[" + cl.id() + "] " + ChatColor.GREEN + cl.toInfoString()
                                + "isLoaded:" + c.isLoaded()
                                + " isForceLoaded:" + c.isForceLoaded()
                                + " isChunkInUse:" + c.getWorld().isChunkInUse(cl.x, cl.z)
                                + " isChunkForceLoaded:" + c.getWorld().isChunkForceLoaded(cl.x, cl.z)
                                + " loadState:" + cl.loadState.name());

                        cl.growCropsSinceUnload();
                    }
                }
                return;
            case 2: // list all
                if (args[1].equalsIgnoreCase("all")) {
                    if (sender.hasPermission(PERMISSION_COMMAND_LIST_ALL)) {
                        sender.sendMessage(ChatColor.GREEN + "Listing all chunk loaders on the server:");
                        for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
                            sender.sendMessage(ChatColor.DARK_GREEN + "[" + cl.id() + "] " + ChatColor.GREEN + cl.toInfoString());
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
                    }

                    return;
                }
                break;
            case 3: // list players
                if (args[1].equalsIgnoreCase("player")) {
                    if (sender.hasPermission(PERMISSION_COMMAND_LIST_PLAYER)) {
                        Player targetPlayer = Bukkit.getPlayer(args[2]);
                        if (targetPlayer == null) {
                            sender.sendMessage(ChatColor.RED + "Player with name " + args[2] + " could not be found.");
                        } else {
                            sender.sendMessage(ChatColor.GREEN + "Listing chunk loaders for player " + targetPlayer.getName() + ":");
                            UUID targetUUID = targetPlayer.getUniqueId();
                            for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
                                if (cl.ownerUUID.equals(targetUUID)) {
                                    sender.sendMessage(ChatColor.DARK_GREEN + "[" + cl.id() + "] " + ChatColor.GREEN + cl.toInfoString());
                                }
                            }
                        }
                    } else {
                        sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
                    }

                    return;
                }
                break;
        }

        sender.sendMessage(ChatColor.RED + "Wrong usage of the command. Use /cl help to list all commands.");
    }

    private void onCommandForcedelete(CommandSender sender, Command cmd, String[] args) {
        if (!sender.hasPermission(PERMISSION_COMMAND_FORCEDELETE)) {
            sender.sendMessage(ChatColor.DARK_RED + cmd.getPermissionMessage());
            return;
        }

        if (args.length == 1) {
            int num = this.chunkLoaders.size();
            this.chunkLoaders.clear();
            sender.sendMessage(ChatColor.GREEN + "Removed all " + num + " chunk loaders on the server.");
            return;
        } else if (args.length == 3) {
            if (args[1].equalsIgnoreCase("world")) {
                World world = Bukkit.getWorld(args[2]);
                if (world == null) {
                    sender.sendMessage(ChatColor.RED + "World with name " + args[2] + " could not be found.");
                } else {
                    int num = 0;
                    for (Map.Entry<String, ChunkLoaderv1> entry : this.chunkLoaders.entrySet()) {
                        if (entry.getValue().world.equals(world.getName())) {
                            this.chunkLoaders.remove(entry.getKey());
                            num++;
                        }
                    }
                    sender.sendMessage(ChatColor.GREEN + "Removed " + num + " chunk loaders from world " + world.getName() + ".");
                }
            } else if (args[1].equalsIgnoreCase("player")) {
                Player targetPlayer = Bukkit.getPlayer(args[2]);
                if (targetPlayer == null) {
                    sender.sendMessage(ChatColor.RED + "Player with name " + args[2] + " could not be found.");
                } else {
                    UUID targetUUID = targetPlayer.getUniqueId();
                    int num = 0;
                    for (Map.Entry<String, ChunkLoaderv1> entry : this.chunkLoaders.entrySet()) {
                        if (entry.getValue().ownerUUID.equals(targetUUID)) {
                            this.chunkLoaders.remove(entry.getKey());
                            num++;
                        }
                    }
                    sender.sendMessage(ChatColor.GREEN + "Removed " + num + " chunk loaders by player " + targetPlayer.getName() + ".");
                }
            }
        }

        sender.sendMessage(ChatColor.RED + "Wrong usage of the command. Use /cl help to list all commands.");
    }

    private void onCommandSet(Player player) {
        if (!player.hasPermission(PERMISSION_CHUNKS_EXEMPT)) {
            int playerTotal = 0;
            for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
                if (cl.ownerUUID.equals(player.getUniqueId())) {
                    playerTotal++;
                }
            }

            for (int i = playerTotal; i >= 0; i--) {
                if (player.hasPermission(PERMISSION_CHUNKS_ + i)) {
                    player.sendMessage(ChatColor.RED + "You cannot set any more chunk loaders.");
                    return;
                }
            }
        }

        String alreadyThereId = ChunkLoaderv1.chunkId(player.getLocation().getChunk());
        if (this.chunkLoaders.containsKey(alreadyThereId)) {
            ChunkLoaderv1 alreadyThere = this.chunkLoaders.get(alreadyThereId);
            if (alreadyThere.ownerUUID.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You have already set this chunk as a chunk loader.");
            } else {
                if (player.hasPermission(PERMISSION_COMMAND_HERE)) {
                    OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(alreadyThere.ownerUUID);
                    if (ownerPlayer.getName() == null) {
                        player.sendMessage(ChatColor.RED + "This chunk already has a chunk loader.");
                    } else {
                        player.sendMessage(ChatColor.RED + ownerPlayer.getName() + " has already set this chunk as a chunk loader.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "This chunk already has a chunk loader.");
                }
            }

            return;
        }

        Resident resident = null;
        Town town = null;
        try {
            resident = TownyUniverse.getDataSource().getResident(player.getName());
            town = TownyUniverse.getTownBlock(player.getLocation()).getTown();

            if (!resident.getTown().equals(town)) {
                player.sendMessage(ChatColor.RED + "You can only set a chunk loader in your own town."); // in someone else's town
                return;
            }
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "You can only set a chunk loader in your own town."); // in the wild
            return;
        }

        int hours = 876582;
        if (!player.hasPermission(PERMISSION_TIME_EXEMPT)) {
            hours = this.maxTimeHours;
            for (; hours > 0; hours--) {
                if (player.hasPermission(PERMISSION_TIME_ + hours)) {
                    break;
                }
            }
            if (hours == 0) hours = this.maxTimeHours;
        }

        ChunkLoaderv1 chunkLoader = new ChunkLoaderv1(player.getLocation().getChunk(), player.getUniqueId(), hours);
        this.chunkLoaders.put(chunkLoader.id(), chunkLoader);
        // player.getLocation().getChunk().setForceLoaded(true);

        player.sendMessage(ChatColor.GREEN + "You set this chunk as a chunk loader. It will need to be bumped every " + hours + " hour(s).");
    }

    private void onCommandDelete(Player player, String[] args) {
        if (args.length == 1) {
            String hereId = ChunkLoaderv1.chunkId(player.getLocation().getChunk());
            if (this.chunkLoaders.containsKey(hereId)) {
                ChunkLoaderv1 cl = this.chunkLoaders.get(hereId);
                if (cl.ownerUUID.equals(player.getUniqueId())) {
                    this.chunkLoaders.remove(hereId);
                    player.sendMessage(ChatColor.GREEN + "You removed the chunk loader from this chunk.");
                } else {
                    if (player.hasPermission(PERMISSION_COMMAND_HERE)) {
                        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(cl.ownerUUID);
                        if (ownerPlayer.getName() == null) {
                            player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on this chunk because it was created by another player.");
                        } else {
                            player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on this chunk because it was created by " + ownerPlayer.getName() + ".");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on this chunk because it was created by another player.");
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "You have not set this chunk as a chunk loader.");
            }
            return;
        } else if (args.length == 2) {
            if (this.chunkLoaders.containsKey(args[1])) {
                ChunkLoaderv1 cl = this.chunkLoaders.get(args[1]);
                if (cl.ownerUUID.equals(player.getUniqueId())) {
                    this.chunkLoaders.remove(args[1]);
                    player.sendMessage(ChatColor.GREEN + "You removed the chunk loader " + args[1] + ".");
                } else {
                    if (player.hasPermission(PERMISSION_COMMAND_HERE)) {
                        OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(cl.ownerUUID);
                        if (ownerPlayer.getName() == null) {
                            player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on that chunk because it was created by another player.");
                        } else {
                            player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on that chunk because it was created by " + ownerPlayer.getName() + ".");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "You cannot delete the chunk loader on that chunk because it was created by another player.");
                    }
                }
            } else {
                player.sendMessage(ChatColor.RED + "Chunk loader with name " + args[1] + " could not be found.");
            }
            return;
        }

        player.sendMessage(ChatColor.RED + "Wrong usage of the command. Use /cl help to list all commands.");
    }

    private void onCommandBump(Player player) {
        int num = 0;
        for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
            if (cl.ownerUUID.equals(player.getUniqueId())) {
                cl.bump();
                num++;
            }
        }

        player.sendMessage(ChatColor.GREEN + "You bumped " + num + " chunk loaders.");
    }

    private void onCommandHere(Player player) {
        String hereId = ChunkLoaderv1.chunkId(player.getLocation().getChunk());
        if (this.chunkLoaders.containsKey(hereId)) {
            ChunkLoaderv1 cl = this.chunkLoaders.get(hereId);
            if (cl.ownerUUID.equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.GREEN + "You have a chunk loader in this chunk. " + cl.toInfoString());
            } else {
                if (player.hasPermission(PERMISSION_COMMAND_HERE)) {
                    OfflinePlayer ownerPlayer = Bukkit.getOfflinePlayer(cl.ownerUUID);
                    if (ownerPlayer.getName() == null) {
                        player.sendMessage(ChatColor.GREEN + "Someone has a chunk loader in this chunk. " + cl.toInfoString());
                    } else {
                        player.sendMessage(ChatColor.GREEN + ownerPlayer.getName() + " has a chunk loader in this chunk. " + cl.toInfoString());
                        player.sendMessage(ChatColor.GREEN
                                + "isLoaded:" + player.getLocation().getChunk().isLoaded()
                                + " isForceLoaded:" + player.getLocation().getChunk().isForceLoaded()
                                + " isChunkInUse:" + player.getWorld().isChunkInUse(cl.x, cl.z)
                                + " isChunkForceLoaded:" + player.getWorld().isChunkForceLoaded(cl.x, cl.z));
                    }
                } else {
                    player.sendMessage(ChatColor.GREEN + "This chunk has a chunk loader.");
                }
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "This chunk does not have a chunk loader.");
        }
    }

    private int cc = 0;

    private void randomTickChunk(Chunk chunk) {
        if (chunk.isLoaded()) {
            /*Random r = new Random();
            int x = r.nextInt(16);
            int y = r.nextInt(255) + 1;
            int z = r.nextInt(16);
            chunk.getBlock(x, y, z).getState().update(false, true);*/
            // chunk.getBlock(12, 58, 1).getState().update(false, true);

            /*Block block = chunk.getBlock(12, 58, 1);
            if (block.getState().getData() instanceof Crops) {
                Crops crop = (Crops) block.getState().getData();
                if (crop.getState() == CropState.RIPE) {
                    this.getLogger().info("Crop is done.");
                } else {
                    CropState next = CropState.values()[crop.getState().ordinal() + 1];
                    this.getLogger().info("Doing update on crop to state " + next.name());

                    block.setType(Material.LEGACY_CROPS);
                    crop = new Crops(next);
                    BlockState bs = block.getState();
                    bs.setData(crop);
                    bs.update();

                }
            } else {
                getLogger().info("Block isn't crop: " + block.getLocation().getBlockX() + " : " + block.getLocation().getBlockY() + " : " + block.getLocation().getBlockZ());
            }*/

            ChunkLoaderv1.grow(chunk.getBlock(12, 58, 1), 1);

            getLogger().info("Updated " + (cc+++1) + " times.");
        }
    }


    @EventHandler
    public void onPlayerLoginEvent(PlayerLoginEvent ev) {
        int num = 0;
        for (ChunkLoaderv1 cl : this.chunkLoaders.values()) {
            if (cl.ownerUUID.equals(ev.getPlayer().getUniqueId())) {
                if (cl.nextTimeout < System.currentTimeMillis()) {
                    num++;
                }
            }
        }

        if (num > 0) {
            ev.getPlayer().sendMessage(ChatColor.GOLD + "You have " + num + " chunk loader(s) that need bumping! Use " + ChatColor.RED + "/cl bump " + ChatColor.GOLD + "to bump.");
        }
    }

    @EventHandler
    public void iner(PlayerInteractEvent ev) {
        if (ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
            getLogger().info(ev.getClickedBlock().getType().getId() + "   " + ev.getClickedBlock().getState().getRawData() + "    " + ev.getClickedBlock().getBlockData().getAsString());
            getLogger().info(ev.getClickedBlock().getType().name() + "  " + (ev.getClickedBlock().getType() == Material.SUGAR_CANE));
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent ev) {
        String id = ChunkLoaderv1.chunkId(ev.getChunk());
        if (this.chunkLoaders.containsKey(id)) {
            this.getLogger().info("Unloading a chunkloader");
            ChunkLoaderv1 cl = this.chunkLoaders.get(id);
            if (cl.isActive()) {
                cl.unloadTime = System.currentTimeMillis();
                cl.updateCropsList();
                // ev.setCancelled(true);
                // this.getLogger().info("Cancelled chunk " + id + " from unloading.");
                // this.getLogger().info("isForceLoaded:" + ev.getChunk().isForceLoaded() + " isLoaded:" + ev.getChunk().isLoaded() + " isChunkInUse:" + ev.getWorld().isChunkInUse(cl.x, cl.z));
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent ev) {
        String chunkId = ChunkLoaderv1.chunkId(ev.getChunk());
        if (this.chunkLoaders.containsKey(chunkId)) {
            this.getLogger().info("Load...");
            ChunkLoaderv1 chunkLoader = this.chunkLoaders.get(chunkId);
            chunkLoader.growCropsSinceUnload();
        }
    }

    @EventHandler
    public void onTownUnclaim(TownUnclaimEvent ev) {
        String chunkId = ChunkLoaderv1.chunkIdFromVaribles(
                ev.getWorldCoord().getBukkitWorld(),
                ev.getWorldCoord().getX(),
                ev.getWorldCoord().getZ());
        if (this.chunkLoaders.containsKey(chunkId)) {
            this.chunkLoaders.remove(chunkId);
            this.getLogger().info("Removed chunk loader with id " + chunkId + " because chunk got unclaimed.");
        }
    }

    // @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent ev) {
        String chunkId = ChunkLoaderv1.chunkId(ev.getBlock().getChunk());
        if (this.chunkLoaders.containsKey(chunkId)) {
            this.getLogger().info("BlockPhysicsEvent in chunk loader chunk.");
        } else {
            this.getLogger().info("BlockPhysicsEvent in " + chunkId + " which is not a chunk loader.");
        }
    }

    // @EventHandler
    public void onStructureGrow(StructureGrowEvent ev) {
        String chunkId = ChunkLoaderv1.chunkId(ev.getLocation().getChunk());
        if (this.chunkLoaders.containsKey(chunkId)) {
            this.getLogger().info("StructureGrowEvent in chunk loader chunk.");
        } else {
            this.getLogger().info("StructureGrowEvent in " + chunkId + " which is not a chunk loader.");
        }
    }

}
