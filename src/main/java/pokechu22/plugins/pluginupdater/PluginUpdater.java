package pokechu22.plugins.pluginupdater;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.io.Files;

public class PluginUpdater extends JavaPlugin {
	/**
	 * Plugin names and their locations.
	 */
	private HashMap<String, File> updatablePlugins;
	/**
	 * All of the plugins on the server and their locations.
	 */
	private HashMap<String, File> serverPlugins;
	/**
	 * Files to move after bukkit has unloaded.
	 * 
	 * Key is the destination, value is the from-location.
	 * 
	 * TODO: is a map like this a good idea?
	 */
	private static HashMap<File, File> toMove;

	/**
	 * Number of updatablePlugins updated.  Used to help ensure no conflicts occur.
	 */
	static int updatedCount = 0;

	static {
		toMove = new HashMap<>();

		//Called when the JVM exits; override files.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("[PluginUpdater] Starting plugin moving.");

				for (Map.Entry<File, File> e : toMove.entrySet()) {
					try {
						System.out.println("[PluginUpdater] Moved " +
								e.getValue() + " to " + e.getKey() + 
								"successfully.");

						Files.move(e.getValue(), e.getKey());
					} catch (Exception ex) {
						System.err.println("[PluginUpdater] Failed to move " + 
								e.getValue() + " to " + e.getKey() + "!");
						ex.printStackTrace();
					}
				}

				System.out.println("[PluginUpdater] Done!");
			}
		});
	}

	@Override
	public void onEnable() {
		this.saveDefaultConfig();

		this.updatablePlugins = new HashMap<>();

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
						updatablePlugins.put(key, new File(pluginSection
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

		if (serverPlugins == null) {
			this.serverPlugins = new HashMap<>();

			File pluginsFolder = this.getDataFolder()
					.getParentFile();

			for (File file : pluginsFolder.listFiles()) {
				if (!file.isFile()) {
					continue;
				}

				try {
					serverPlugins.put(getPluginLoader().getPluginDescription(
							file.getAbsoluteFile()).getName(), 
							file.getAbsoluteFile());
				} catch (InvalidDescriptionException e) {
					//Go on to the next file; this can occur normally.
					continue;
				}
			}
		}
	}

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
			
			if (pluginName.equals("*") || pluginName.equalsIgnoreCase("all")) {
				for (String plugin : updatablePlugins.keySet()) {
					updatePlugin(sender, plugin);
				}
			} else {
				updatePlugin(sender, pluginName);
			}
			return true;
		}

		return false;
	}

	/**
	 * Updates an individual plugin.
	 * 
	 * @param sender The command sender to give output to.
	 * @param pluginName The name of the plugin to update.
	 */
	public void updatePlugin(CommandSender sender, String pluginName) {
		try {
			PluginManager pluginManager = getServer().getPluginManager();

			Plugin p = pluginManager.getPlugin(pluginName);
			if (p == null) {
				sender.sendMessage("§cThere is no plugin named " + 
						pluginName + ".");
				return;
			}
			if (!updatablePlugins.containsKey(pluginName)) {
				sender.sendMessage("§c" + pluginName + " is not in the " +
						"list of updatable updatablePlugins!");
				return;
			}

			File copyFrom = updatablePlugins.get(pluginName);
			if (!copyFrom.exists()) {
				sender.sendMessage("§cNew plugin to copy could not be found!");
				sender.sendMessage("§c(" + copyFrom.getAbsolutePath() + ")");
				return;
			}

			pluginManager.disablePlugin(p);
			File newPluginFile = new File(this.getDataFolder(), 
					"updatetemp" + updatedCount + copyFrom.getName());
			updatedCount++;

			Files.copy(copyFrom, newPluginFile);
			Plugin newPlugin = pluginManager.loadPlugin(newPluginFile);
			pluginManager.enablePlugin(newPlugin);

			//Schedule moving the new plugin to the updatablePlugins folder.
			toMove.put(serverPlugins.get(pluginName), newPluginFile);
			serverPlugins.put(pluginName, newPluginFile);

			sender.sendMessage("§aUpdated plugin " + pluginName + 
					" successfully!");

			return;
		} catch (Exception e) {
			sender.sendMessage("§cAn error occured while updating " + pluginName + 
					": " + e);
			getLogger().log(Level.WARNING, "An error occured while updating " +
					pluginName + " for " + sender + ".", e);
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command,
			String alias, String[] args) {
		if (command.getName().equals("updatePlugin")) {
			return new ArrayList<String>(updatablePlugins.keySet());
		}

		return null;
	}
}
