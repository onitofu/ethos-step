package ru.nyansus.mc.ethosstep;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public final class EthosStepPlugin extends JavaPlugin implements Listener {
    static final String PERMISSION = "ethos.step.use";
    static final double DEFAULT_HEIGHT = 1.0;

    private NamespacedKey grantedKey;
    private Attribute stepHeightAttribute;
    private double stepHeight;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        stepHeight = readHeight(getConfig().get("step-height", DEFAULT_HEIGHT), getLogger()::warning);
        grantedKey = new NamespacedKey(this, "granted");
        stepHeightAttribute = resolveAttribute(Registry.ATTRIBUTE::get);
        if (stepHeightAttribute == null) {
            getLogger().severe("Step height attribute is unavailable on this server. Disabling ethos-step.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : getServer().getOnlinePlayers()) {
            apply(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        apply(event.getPlayer());
    }

    @EventHandler
    public void onPostRespawn(PlayerPostRespawnEvent event) {
        apply(event.getPlayer());
    }

    private void apply(Player player) {
        apply(player, stepHeightAttribute, stepHeight, grantedKey);
    }

    static void apply(Player player, Attribute attribute, double height, NamespacedKey grantedKey) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        boolean granted = Byte.valueOf((byte) 1).equals(data.get(grantedKey, PersistentDataType.BYTE));
        if (!granted && !player.hasPermission(PERMISSION)) {
            return;
        }

        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return;
        }
        if (Double.compare(instance.getBaseValue(), height) != 0) {
            instance.setBaseValue(height);
        }
        if (!granted) {
            data.set(grantedKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    static Attribute resolveAttribute(Function<NamespacedKey, Attribute> lookup) {
        Attribute attribute = lookup.apply(NamespacedKey.minecraft("step_height"));
        return attribute != null ? attribute : lookup.apply(NamespacedKey.minecraft("generic.step_height"));
    }

    static double readHeight(Object value, Consumer<String> warning) {
        if (value instanceof Number number) {
            double height = number.doubleValue();
            if (Double.isFinite(height) && height >= 0.0) {
                return height;
            }
        }
        warning.accept("Invalid step-height: expected a finite non-negative number. Using 1.0.");
        return DEFAULT_HEIGHT;
    }
}
