package org.josefadventures.townychunkloader.Command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.josefadventures.townychunkloader.TownyChunkLoader;

/**
 * Handles main command. Sorts out permissions. Directs execution to sub-command listeners.
 */
public class CommandListener implements CommandExecutor {

    private TownyChunkLoader plugin;

    public CommandHelp commandHelp;

    public CommandListener(TownyChunkLoader plugin) {
        this.plugin = plugin;

        this.commandHelp = new CommandHelp(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        static <T> T[] copyOfRange(T[] original, int from, int to)
        ^apply to args

        if (args.length == 0) {
            this.commandHelp.onCommand(sender, args);
        } else {
            switch (args[1].toLowerCase()) {
                case "help":
                    this.commandHelp.onCommand()
            }
        }


        return true;
    }

}
