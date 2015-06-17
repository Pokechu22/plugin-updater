package pokechu22.plugins.pluginupdater;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public class PluginUpdater extends JavaPlugin {
	private HashMap<String, File> plugins;
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		
		this.plugins = new HashMap<>();
		
		ConfigurationSection section = this.getConfig()
				.getConfigurationSection("updatables");
		
		if (section == null) {
			getLogger().info("No plugin settings found!");
		} else {
			Set<String> keys = section.getKeys(false);
			
			for (String key : keys) {
				if (section.isConfigurationSection(key)) {
					ConfigurationSection pluginSection = section
							.getConfigurationSection(key);
					
					if (pluginSection.isString("path")) {
						plugins.put(key, new File(pluginSection
								.getString("path")));
					} else {
						getPluginLoader().disablePlugin(this);
						throw new RuntimeException("Config entry for plugin " + 
								key + " is invalid!");
					}
				} else {
					getPluginLoader().disablePlugin(this);
					throw new RuntimeException("Config entry for plugin " + 
							key + " is invalid!");
				}
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (command.getName().equals("updatePlugin")) {
			try {
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
				
				Plugin p = pluginManager.getPlugin(pluginName);
				if (p == null) {
					sender.sendMessage("§cThere is no plugin named " + 
							pluginName + ".");
					return true;
				}
				if (!plugins.containsKey(pluginName)) {
					sender.sendMessage("§c" + pluginName + " is not in the " +
							"list of updatable plugins!");
					return true;
				}
				
				File newPlugin = plugins.get(pluginName);
				if (!newPlugin.exists()) {
					sender.sendMessage("§cNew plugin to copy could not be found!");
					sender.sendMessage("§c(" + newPlugin.getAbsolutePath() + ")");
					return true;
				}
				
				pluginManager.disablePlugin(p);
				
				File updateFolder = getServer().getUpdateFolderFile();
				File pluginsFolder = updateFolder.getParentFile();
				
				File pluginFile = null;
				//Find the write plugin.
				for (File file : pluginsFolder.listFiles()) {
					if (!file.isFile()) {
						continue;
					}
					
					try {
						if (getPluginLoader().getPluginDescription(
								file.getAbsoluteFile()).getName()
								.equals(pluginName)) {
							pluginFile = file;
							break;
						}
					} catch (InvalidDescriptionException e) {
						//Go on to the next one.
						continue;
					}
				}
				
				if (pluginFile==null) {
					sender.sendMessage("§cFailed to find the requestied plugin " +
							" in the plugins folder.");
					return true;
				}
				
				File newPluginTemp = new File(updateFolder, newPlugin.getName());
				
				Files.copy(newPlugin, newPluginTemp);
				Files.move(pluginFile, new File(updateFolder, pluginFile.getName()+ ".old"));
				Files.move(newPluginTemp, pluginFile);
				
				pluginManager.loadPlugin(pluginFile);
				
				sender.sendMessage("§aDone!  Updated plugin " + pluginName + ".");
				
				return true;
			} catch (Exception e) {
				sender.sendMessage("§cAn error occured: " + e);
				e.printStackTrace();
			}
			return true;
		}
		
		return false;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command,
			String alias, String[] args) {
		if (command.getName().equals("updatePlugin")) {
			return new ArrayList<String>(plugins.keySet());
		}
		
		return null;
	}
}
