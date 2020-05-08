package net.wytrem.spigot.mercatus.config.pojo;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("vanilla")
public class VanillaItemStackRef implements ItemStackRef, ConfigurationSerializable {
    private ItemStack itemStack;

    public VanillaItemStackRef(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public VanillaItemStackRef(Material material) {
        this(new ItemStack(material));
    }

    @Override
    public ItemStack get() {
        return itemStack;
    }

    /**
     * Creates a Map representation of this class.
     * <p>
     * This class must provide a method to restore this class, as defined in
     * the {@link ConfigurationSerializable} interface javadocs.
     *
     * @return Map containing the current state of this class
     */
    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("stack", this.itemStack);

        return map;
    }

    public static VanillaItemStackRef valueOf(Map<String, Object> map) {
        return new VanillaItemStackRef((ItemStack) map.get("stack"));
    }
}
