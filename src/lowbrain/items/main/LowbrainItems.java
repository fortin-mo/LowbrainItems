package lowbrain.items.main;

import lowbrain.library.config.YamlConfig;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LowbrainItems extends JavaPlugin {

    private static LowbrainItems instance;

    private HashMap<String,ItemStack> items;
    private HashMap<String,ItemStack> staffs;
    private NamespacedKey namespacedKey;

    private YamlConfig config;
    private YamlConfig staffConfig;

    @Contract(pure = true)
    public static LowbrainItems getInstance() {return instance;}

    /**
     * called when the plugin is initially enabled
     */
    @Override
    public void onEnable() {
        instance = this;

        loadConfig();
        namespacedKey = new NamespacedKey(this, "LowbrainItems");

        createCustomItems();
        createCustomStaffs();

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

    public Map<String,ItemStack> getStaffs(){
        return this.staffs;
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getStaffConfig() {
        return staffConfig;
    }

    private boolean createCustomItems(){
        try {
            items = new HashMap<>();
            for (String name : config.getKeys(false)) {

                if(getConfig().getBoolean(name +".enable")){
                    Material material = Material.valueOf(getConfig().getString(name +".material").toUpperCase());

                    if(material == null){
                        this.getLogger().info("Material for " + name + " could not be found !");
                        continue;
                    }
                    ItemStack customWeapon = new ItemStack(material, 1);
                    ItemMeta ESmeta = customWeapon.getItemMeta();

                    ChatColor color = ChatColor.getByChar(getConfig().getString(name +".display_color"));

                    if(color != null)
                        ESmeta.setDisplayName(color + name);

                    else
                        ESmeta.setDisplayName(name);

                    List<String> lores = getConfig().getStringList(name + ".lores");

                    if(lores != null && !lores.isEmpty())
                        ESmeta.setLore(lores);


                    customWeapon.setItemMeta(ESmeta);

                    ConfigurationSection attributes = getConfig().getConfigurationSection(name + ".attributes");
                    net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(customWeapon);
                    NBTTagCompound compound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                    NBTTagList modifiers = getAttributeModifiers(attributes);

                    List<String> enchts = getConfig().getStringList(name + ".enchantments");
                    NBTTagList enchModifiers = getEnchants(enchts);

                    if(!modifiers.isEmpty())
                        compound.set("AttributeModifiers", modifiers);

                    compound.set("ench", enchModifiers);

                    if(!modifiers.isEmpty() || !enchModifiers.isEmpty()) {
                        nmsStack.setTag(compound);
                        customWeapon = CraftItemStack.asBukkitCopy(nmsStack);
                    }

                    ShapedRecipe customRecipe = new ShapedRecipe(new NamespacedKey(this, "LowbrainItems." + name), customWeapon);

                    ConfigurationSection recipeSection = getConfig().getConfigurationSection(name + ".recipe");
                    if(recipeSection == null){
                        this.getLogger().info("Missing recipe section for " + name);
                        continue;
                    }

                    String[] shape = recipeSection.getString("shape").split(",");
                    if(shape.length != 3){
                        this.getLogger().info("Wrong recipe shape format for " + name);
                        continue;
                    }

                    customRecipe.shape(shape[0].trim().replace("-"," ")
                            , shape[1].trim().replace("-"," ")
                            , shape[2].trim().replace("-"," "));

                    if (setRecipeIngredients(customRecipe, recipeSection.getStringList("ingredients"))) {
                        this.getItems().put(name,customRecipe.getResult());
                        Bukkit.addRecipe(customRecipe);
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * create custom staffs from staffs.yml
     * @return
     */
    private boolean createCustomStaffs(){
        try {
            staffs = new HashMap<>();
            for (String staffName: staffConfig.getKeys(false)) {

                ConfigurationSection staffSection = staffConfig.getConfigurationSection(staffName);

                if(!staffSection.getBoolean("enable", false))
                    continue;

                Material material = Material.valueOf(staffSection.getString("material").toUpperCase());

                if(material == null){
                    this.getLogger().info("Material for " + staffName + " could not found !");
                    return false;
                }

                ItemStack customStaff = new ItemStack(material, 1);
                ItemMeta ESmeta = customStaff.getItemMeta();

                ChatColor color = ChatColor.getByChar(staffSection.getString("display_color"));
                if(color == null) {
                    this.getLogger().info("Color for " + staffName + " could not found !");
                    return false;
                }
                ESmeta.setDisplayName(color + staffName);

                List<String> lores = staffSection.getStringList("lores");

                if(lores == null)
                    lores = new ArrayList<String>();

                String cooldown = staffSection.getString("cooldown", "0");
                String baseDamage = staffSection.getString("base_damage", "0");
                String effect =  staffSection.getString("effect", "none");

                lores.add("last used : ");
                lores.add("durability : " + staffSection.getInt("durability"));
                lores.add("effect : " + effect);
                lores.add("base damage : " + baseDamage);
                lores.add("cooldown : " + cooldown);

                ESmeta.setLore(lores);

                customStaff.setItemMeta(ESmeta);

                ConfigurationSection attributes = staffSection.getConfigurationSection("attributes");

                net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(customStaff);
                NBTTagCompound compound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                NBTTagList modifiers = getAttributeModifiers(attributes);

                List<String> enchts = staffSection.getStringList("enchantments");
                NBTTagList enchModifiers = getEnchants(enchts);

                if(!modifiers.isEmpty())
                    compound.set("AttributeModifiers", modifiers);

                compound.set("ench", enchModifiers);

                if(!modifiers.isEmpty() || !enchModifiers.isEmpty()){
                    nmsStack.setTag(compound);
                    customStaff = CraftItemStack.asBukkitCopy(nmsStack);
                }

                ShapedRecipe customRecipe = new ShapedRecipe(new NamespacedKey(this, "LowbrainItems." + staffName), customStaff);

                ConfigurationSection recipeSection = staffSection.getConfigurationSection("recipe");
                if(recipeSection == null){
                    this.getLogger().info("Missing recipe section for " + staffName);
                    continue;
                }

                String[] shape = recipeSection.getString("shape").split(",");
                if(shape.length != 3){
                    this.getLogger().info("Wrong recipe shape format for " + staffName);
                    continue;
                }

                customRecipe.shape(shape[0].trim().replace("-"," ")
                        , shape[1].trim().replace("-"," ")
                        , shape[2].trim().replace("-"," "));

                if (setRecipeIngredients(customRecipe, recipeSection.getStringList("ingredients"))) {
                    this.getStaffs().put(staffName, customRecipe.getResult());
                    Bukkit.addRecipe(customRecipe);
                }
            }

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private NBTTagList getEnchants(List<String> enchts) {
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
                    this.getLogger().info("Invalid enchants parameters : level, id");
                    continue;
                }

                modifier.set("id", new NBTTagInt(id));
                modifier.set("lvl", new NBTTagInt(level));

                enchModifiers.add(modifier);
            }
        }

        return enchModifiers;
    }

    private NBTTagList getAttributeModifiers (ConfigurationSection attributes) {
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

        return modifiers;
    }

    @Contract("null, _ -> false; !null, null -> false")
    private boolean setRecipeIngredients(ShapedRecipe recipe, List<String> ingredients) {
        if (recipe == null || ingredients == null || ingredients.size() <= 0)
            return false;

        for (String ingredient: ingredients) {
            String[] i = ingredient.split(",");
            if(i.length != 2){
                this.getLogger().info("Invalid recipe ingredient format! must be like : X, MATERIAL");
                return false;
            }
            if(i[0].length() > 1){
                this.getLogger().info("Invalid ingredient format!. Must be a single character before comma");
                return false;
            }

            Material mat = Material.getMaterial(i[1].trim().toUpperCase());
            if(mat == null){
                this.getLogger().info("Ingredient material could not found !");
                return false;
            }
            recipe.setIngredient(i[0].trim().charAt(0),mat);
        }
        return true;
    }

    private void loadConfig() {
        this.config = new YamlConfig("config.yml", this);
        this.staffConfig = new YamlConfig("staffs.yml", this);
    }
}

