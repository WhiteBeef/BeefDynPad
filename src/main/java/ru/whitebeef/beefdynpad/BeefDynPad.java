package ru.whitebeef.beefdynpad;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;

public final class BeefDynPad extends JavaPlugin implements Listener {

    public static FileConfiguration config;

    public static List<Material> plates;

    public static List<Material> pads;

    public static HashMap<String, Long> padUsers = new HashMap<>();

    public static Sound sound;

    public static float volume;

    public static float pitch;


    public void onEnable() {
        File cfg = new File(getDataFolder() + File.pathSeparator + "config.yml");
        if (!cfg.exists())
            saveDefaultConfig();
        config = getConfig();
        plates = new ArrayList<>();
        pads = new ArrayList<>();
        try {
            sound = Sound.valueOf(config.getString("sound.type"));
        } catch (IllegalArgumentException e) {
            getLogger().info("Sound type " + config.getString("sound.type") + " not found!");
        }
        volume = (float) config.getDouble("sound.volume", 0.1D);
        pitch = (float) config.getDouble("sound.pitch", 1.0D);
        List<String> plateList = new ArrayList<>(config.getStringList("plates"));
        for (int i = 0; i < plates.size(); i++) {
            if (Material.getMaterial(plateList.get(i)) != null) {
                plates.add(Material.getMaterial(plateList.get(i)));
            } else {
                getLogger().info("Material " + plateList.get(i) + " not found!");
            }
        }
        List<String> materials = new ArrayList<>(Objects.requireNonNull(config.getConfigurationSection("pads")).getKeys(false));
        for (String material : materials) {
            if (Material.getMaterial(material) != null)
                if (config.getDouble("pads." + material) > 0.0D)
                    pads.add(Material.getMaterial(material));
                else
                    getLogger().info("Material " + material + " has invalid power!");
            else
                getLogger().info("Material " + material + " not found!");

        }
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("Успешное включение!");
    }

    @EventHandler
    public void onPadUse(final PlayerInteractEvent e) {
        if (!e.getAction().equals(Action.PHYSICAL) || !e.hasBlock())
            return;
        if (!e.getBlockFace().equals(BlockFace.SELF))
            return;
        if (plates.contains(Objects.requireNonNull(e.getClickedBlock()).getType()))
            return;
        e.getClickedBlock().getRelative(BlockFace.DOWN);
        Material pad = e.getClickedBlock().getRelative(BlockFace.DOWN).getType();
        for (Material material : pads) {
            if (material.equals(pad) && (
                    e.getPlayer().hasPermission("beefdynpad.use." + material.toString().toLowerCase()) ||
                            e.getPlayer().hasPermission("beefdynpad.use"))) {
                double multiplier = config.getDouble("pads." + material.toString(), 1.0D);
                e.setCancelled(true);
                Bukkit.getScheduler().runTaskLater(this, () -> {
                    e.getPlayer().setVelocity(new Vector(0.0D, multiplier, 0.0D));
                    if (config.getBoolean("sound.enable", true) && sound != null)
                        e.getPlayer().playSound(e.getPlayer().getLocation(), sound, volume, pitch);
                    padUsers.put(e.getPlayer().getName(), System.currentTimeMillis());
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!e.getEntityType().equals(EntityType.PLAYER) || !e.getCause().equals(EntityDamageEvent.DamageCause.FALL))
            return;
        if (config.getBoolean("noDamage", true) &&
                System.currentTimeMillis() - padUsers.getOrDefault(e.getEntity().getName(), 0L) < 5000) {
            padUsers.remove(e.getEntity().getName());
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!config.getBoolean("noDamage", true))
            e.getPlayer().setVelocity(new Vector(0.0D, 0.0D, 0.0D));
    }

    public void onDisable() {
        getLogger().info("Успешное выключение!");
    }
}
