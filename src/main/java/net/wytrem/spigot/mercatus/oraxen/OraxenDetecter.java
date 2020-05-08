package net.wytrem.spigot.mercatus.oraxen;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

public class OraxenDetecter {
    public static boolean detectOraxen() {
        return Bukkit.getServer().getPluginManager().getPlugin("Oraxen") != null;
    }

    public static void registerTypes() {
        ConfigurationSerialization.registerClass(OraxenItemStackRef.class);
    }
}
