package lowbrain.items.main;

import net.minecraft.server.v1_10_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_10_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Moofy on 28/08/2016.
 */
public class Main  extends JavaPlugin {

    private HashMap<String,ItemStack> items;

    /**
     * called when the plugin is initially enabled
     */
    @Override
    public void onEnable() {
        saveDefaultConfig();
        createCustomItems();
        this.getCommand("lbitems").setExecutor(new CommandHandler(this));
        this.getLogger().info("[LowbrainItems] " + getDescription().getVersion() + " enabled!");
    }
    @Override
    public void onDisable() {
        Bukkit.getServer().getScheduler().cancelTasks(this);
    }

    public Map<String,ItemStack> getItems(){
        return this.items;
    }

    private boolean createCustomItems(){
        try {
            items = new HashMap<>();
            for (String weapon :
                    getConfig().getKeys(false)) {

                if(getConfig().getBoolean(weapon +".enable")){
                    Material material = Material.valueOf(getConfig().getString(weapon +".material").toUpperCase());

                    if(material == null){
                        this.getLogger().info("Material for " + weapon + " could not found !");
                        return false;
                    }
                    ItemStack customWeapon = new ItemStack(material, 1);
                    ItemMeta ESmeta = customWeapon.getItemMeta();

                    ChatColor color = ChatColor.getByChar(getConfig().getString(weapon +".display_color"));
                    if(color != null){
                        ESmeta.setDisplayName(color + weapon);
                    }
                    else{
                        ESmeta.setDisplayName(weapon);
                    }

                    List<String> lores = getConfig().getStringList(weapon + ".lores");

                    if(lores != null && !lores.isEmpty()){
                        ESmeta.setLore(lores);
                    }

                    customWeapon.setItemMeta(ESmeta);

                    ConfigurationSection attributes = getConfig().getConfigurationSection(weapon + ".attributes");
                    net.minecraft.server.v1_10_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(customWeapon);
                    NBTTagCompound compound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                    NBTTagList modifiers = new NBTTagList();

                    //adding attributes if needed
                    if(attributes != null){
                        for (String attribute :
                                attributes.getKeys(false)) {
                            NBTTagCompound modifier = new NBTTagCompound();
                            modifier.set("AttributeName", new NBTTagString("generic." + attributes.getString(attribute +".attribute_name")));
                            modifier.set("Name", new NBTTagString(attributes.getString(attribute +".name")));
                            modifier.set("Amount", new NBTTagDouble(attributes.getDouble(attribute +".amount")));
                            modifier.set("Operation", new NBTTagInt(attributes.getInt(attribute +".operation")));
                            modifier.set("UUIDLeast", new NBTTagInt(894654));
                            modifier.set("UUIDMost", new NBTTagInt(2872));

                            String slots = attributes.getString(attribute +".slots");
                            if(slots.length() > 0){
                                modifier.set("Slot", new NBTTagString(slots));
                            }

                            modifiers.add(modifier);
                        }
                    }

                    List<String> enchts = getConfig().getStringList(weapon + ".enchantments");
                    NBTTagList enchModifiers = new NBTTagList();

                    //adding enchantments if needed
                    if (enchts != null) {
                        for (String ench :
                                enchts) {
                            NBTTagCompound modifier = new NBTTagCompound();

                            String[] temp = ench.split(",");

                            int id = Integer.parseInt(temp[0].trim());
                            int level = Integer.parseInt(temp[1].trim());

                            if(level < 0 || id < 0 ){
                                this.getLogger().info("Enchantments for " + weapon + " arent right !");
                                return false;
                            }

                            modifier.set("id", new NBTTagInt(id));
                            modifier.set("lvl", new NBTTagInt(level));

                            enchModifiers.add(modifier);
                        }
                    }

                    if(!modifiers.isEmpty()) {
                        compound.set("AttributeModifiers", modifiers);
                    }
                    compound.set("ench", enchModifiers);

                    if(!modifiers.isEmpty() || !enchModifiers.isEmpty()){
                        nmsStack.setTag(compound);
                        customWeapon = CraftItemStack.asBukkitCopy(nmsStack);
                    }

                    ShapedRecipe customRecipe = new ShapedRecipe(customWeapon);

                    ConfigurationSection recipeSection = getConfig().getConfigurationSection(weapon + ".recipe");
                    if(recipeSection == null){
                        this.getLogger().info("Missing recipe section for " + weapon);
                        return false;
                    }

                    String[] shape = recipeSection.getString("shape").split(",");
                    if(shape.length != 3){
                        this.getLogger().info("Wrong recipe shape format for " + weapon);
                        return false;
                    }

                    customRecipe.shape(shape[0].trim().replace("-"," "),shape[1].trim().replace("-"," "),shape[2].trim().replace("-"," "));

                    for (String ingredient:
                            recipeSection.getStringList("ingredients")) {
                        String[] i = ingredient.split(",");
                        if(i.length != 2){
                            this.getLogger().info("Wrong recipe ingedient format for " + weapon);
                            return false;
                        }
                        if(i[0].length() > 1){
                            this.getLogger().info("Ingredient format for " + weapon + " !. Must be a single caracter before comma");
                            return false;
                        }

                        Material mat = Material.getMaterial(i[1].trim().toUpperCase());
                        if(mat == null){
                            this.getLogger().info("Ingredient material for " + weapon + " could not found !");
                            return false;
                        }
                        customRecipe.setIngredient(i[0].trim().charAt(0),mat);
                    }
                    this.getItems().put(weapon,customRecipe.getResult());
                    Bukkit.addRecipe(customRecipe);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            this.getLogger().info(e.getMessage());
            return false;
        }
        return true;
    }
}

