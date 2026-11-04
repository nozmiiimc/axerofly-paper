package me.nozmiiimc.axerofly;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class AxeroFly extends JavaPlugin implements CommandExecutor, Listener {

    private File langFile;
    private FileConfiguration langConfig;

    @Override
    public void onEnable() {
        // Save default config.yml
        saveDefaultConfig();
        
        // Save and load lang.yml
        createLangConfig();

        // Register commands & events
        if (getCommand("fly") != null) {
            getCommand("fly").setExecutor(this);
        }
        if (getCommand("axerofly") != null) {
            getCommand("axerofly").setExecutor(this);
        }
        
        getServer().getPluginManager().registerEvents(this, this);

        getLogger().info("AxeroFly v1.0.0 berhasil diaktifkan!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AxeroFly dimatikan!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // Command: /axerofly reload
        if (command.getName().equalsIgnoreCase("axerofly")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("axerofly.admin")) {
                    sender.sendMessage(getMessage("messages.no-permission"));
                    return true;
                }
                
                reloadConfig();
                reloadLangConfig();
                sender.sendMessage(getMessage("messages.reload-success"));
                return true;
            }
            sender.sendMessage(ChatColor.YELLOW + "Gunakan: /axerofly reload");
            return true;
        }

        // Command: /fly
        if (command.getName().equalsIgnoreCase("fly")) {
            
            // Self fly toggle (/fly)
            if (args.length == 0) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(getMessage("messages.only-players"));
                    return true;
                }

                Player player = (Player) sender;
                if (!player.hasPermission("axerofly.fly")) {
                    player.sendMessage(getMessage("messages.no-permission"));
                    return true;
                }

                // Check disabled worlds
                List<String> disabledWorlds = getConfig().getStringList("disabled-worlds");
                if (disabledWorlds.contains(player.getWorld().getName()) && !player.hasPermission("axerofly.bypass")) {
                    player.sendMessage(getMessage("messages.world-disabled"));
                    return true;
                }

                toggleFly(player, player);
                return true;
            }

            // Target fly toggle (/fly <player>)
            if (args.length == 1) {
                if (!sender.hasPermission("axerofly.fly.others")) {
                    sender.sendMessage(getMessage("messages.no-permission"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    sender.sendMessage(getMessage("messages.player-not-found"));
                    return true;
                }

                toggleFly(sender, target);
                return true;
            }
        }

        return false;
    }

    private void toggleFly(CommandSender sender, Player target) {
        boolean canFly = !target.getAllowFlight();
        target.setAllowFlight(canFly);
        if (!canFly) {
            target.setFlying(false);
        }

        if (sender.equals(target)) {
            String key = canFly ? "messages.fly-enabled" : "messages.fly-disabled";
            target.sendMessage(getMessage(key));
        } else {
            String targetKey = canFly ? "messages.fly-enabled" : "messages.fly-disabled";
            String senderKey = canFly ? "messages.fly-enabled-others" : "messages.fly-disabled-others";

            target.sendMessage(getMessage(targetKey));
            sender.sendMessage(getMessage(senderKey).replace("%player%", target.getName()));
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        List<String> disabledWorlds = getConfig().getStringList("disabled-worlds");

        if (disabledWorlds.contains(player.getWorld().getName()) && !player.hasPermission("axerofly.bypass")) {
            if (player.getAllowFlight()) {
                player.setAllowFlight(false);
                player.setFlying(false);
                player.sendMessage(getMessage("messages.world-disabled"));
            }
        }
    }

    // Custom Language File Handlers
    private void createLangConfig() {
        langFile = new File(getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            langFile.getParentFile().mkdirs();
            saveResource("lang.yml", false);
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    public void reloadLangConfig() {
        if (langFile == null) {
            langFile = new File(getDataFolder(), "lang.yml");
        }
        langConfig = YamlConfiguration.loadConfiguration(langFile);
    }

    private String getMessage(String path) {
        String prefix = langConfig.getString("prefix", "");
        String msg = langConfig.getString(path, "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}
