package org.josefadventures.townychunkloader.Command;

import org.bukkit.command.CommandSender;
import org.josefadventures.townychunkloader.TownyChunkLoader;

public class CommandList extends SubCommand {

    public CommandList(TownyChunkLoader plugin) {
        super(plugin);
    }


    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        return false;
    }

}
