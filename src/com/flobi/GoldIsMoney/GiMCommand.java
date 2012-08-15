package com.flobi.GoldIsMoney;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class GiMCommand implements CommandExecutor {
	private GoldIsMoney plugin;
	
    public GiMCommand() {
    	plugin = GiMUtility.plugin;
    	plugin.getCommand("goldismoney").setExecutor(this);
    	plugin.getCommand("balance").setExecutor(this);
    	plugin.getCommand("money").setExecutor(this);
	}

	@Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (command.getName().equalsIgnoreCase("goldismoney")) {
        	if (args.length < 1 || args[0].equalsIgnoreCase("reload")) return false;
        } else if (command.getName().equalsIgnoreCase("money") || command.getName().equalsIgnoreCase("balance")) {
        	if (sender instanceof Player) {
//        		Player player = (Player) sender;
//            	if (!GiMUtility.hasPermission(player.getName())) {
//               	player.sendMessage(ChatColor.translateAlternateColorCodes('&', "".replaceAll("%g", formatLong(getBalanceLong(player.getName())))));
            		return false;
//            	}
///            	player.sendMessage(ChatColor.translateAlternateColorCodes('&', plugin.balanceMessage.replaceAll("%g", formatLong(getBalanceLong(player.getName())))));
//            	return true;
        	}
        }
        return false;
    }
}
