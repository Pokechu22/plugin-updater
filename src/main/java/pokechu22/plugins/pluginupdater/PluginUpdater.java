package pokechu22.plugins.pluginupdater;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public class PluginUpdater extends JavaPlugin {
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equals("updatePlugin")) {
			if (!sender.hasPermission("pluginupdater.update")) {
				sender.sendMessage("§cYou don't have permission.");
				return true;
			}
			
			if (args.length == 0) {
				return false;
			}
			
			// I'm not sure if plugin names can contain spaces, but let's
			// handle said case.
			StringBuilder pluginNameTemp = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				pluginNameTemp.append(args[i]);
				
				if (i != args.length - 1) {
					pluginNameTemp.append(' ');
				}
			}
			
			String pluginName = pluginNameTemp.toString();
			
			if (this.getName().equals(pluginName)) {
				sender.sendMessage("§cಠ_ಠ");
				return true;
			}
			
			PluginManager pluginManager = getServer().getPluginManager();
			
			if (!pluginManager.isPluginEnabled(pluginName)) {
				sender.sendMessage("§cThere is no plugin named " + 
						pluginName + " currently enabled.");
				return true;
			}
			Plugin p = pluginManager.getPlugin(pluginName);
			if (p == null) {
				sender.sendMessage("§cThere is no plugin named " + 
						pluginName + ".");
			}
			
			//TODO
			pluginManager.disablePlugin(p);
			
			return true;
		}
		
		return false;
	}
}
