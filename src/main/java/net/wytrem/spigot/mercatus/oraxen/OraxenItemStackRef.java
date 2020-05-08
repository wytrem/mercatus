package net.wytrem.spigot.mercatus.oraxen;

import io.th0rgal.oraxen.items.OraxenItems;
import net.wytrem.spigot.mercatus.config.pojo.ItemStackRef;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

@SerializableAs("oraxen")
public class OraxenItemStackRef implements ItemStackRef, ConfigurationSerializable {
    private String key;

    public OraxenItemStackRef(String key) {
        this.key = key;
    }

    @Override
    public ItemStack get() {
        return OraxenItems.getItemById(this.key).build();
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
        map.put("key", this.key);

        return map;
    }

    public static OraxenItemStackRef valueOf(Map<String, Object> map) {
        return new OraxenItemStackRef((String) map.get("key"));
    }
}
