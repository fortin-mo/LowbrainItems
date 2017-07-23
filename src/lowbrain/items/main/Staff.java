package lowbrain.items.main;

import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;

public class Staff {
    private boolean gravity;
    private double speed;
    private int durability;
    private int cooldown;
    private String effect;
    private int effectDuration;

    public Staff(boolean gravity, double speed, int durability, int cooldown, String effect, int effectDuration) {
        this.gravity = gravity;
        this.speed = speed;
        this.durability = durability;
        this.cooldown = cooldown;
        this.effect = effect;
        this.effectDuration = effectDuration;
    }

    @Contract("null -> fail")
    public Staff(ConfigurationSection config) {
        if (config == null)
            throw new Error("null argument exception");

        gravity = config.getBoolean("gravity", true);
        speed = config.getDouble("speed", 0);
        durability = config.getInt("durability", 0);
        cooldown = config.getInt("cooldown",0);
        effect = config.getString("effect", "");
        effectDuration = config.getInt("effect_duration", 0);
    }


    public boolean isGravity() {
        return gravity;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDurability() {
        return durability;
    }

    public int getCooldown() {
        return cooldown;
    }

    public String getEffect() {
        return effect;
    }

    public int getEffectDuration() {
        return effectDuration;
    }
}
