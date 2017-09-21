package lowbrain.items.main;

import lowbrain.library.command.Command;
import lowbrain.library.command.CommandHandler;
import lowbrain.library.config.YamlConfig;
import lowbrain.library.fn;
import lowbrain.library.main.LowbrainLibrary;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;

import java.lang.reflect.InvocationTargetException;
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

        regCommand();
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

    private void regCommand() {
        CommandHandler c = LowbrainLibrary.getInstance().getBaseCmdHandler();
        Command sub;
        Command onGive;
        Command onList;

        c.register("items", sub = new Command("items") {
            @Override
            public CommandStatus execute(CommandSender who, String[] args, String cmd) {
                return CommandStatus.INVALID;
            }
        });

        sub.register("give", onGive = new Command("give") {
            @Override
            public CommandStatus execute(CommandSender who, String[] args, String cmd) {
                Player to = null;
                int amount = 0;
                ItemStack item = null;
                String itemName = null;

                switch (args.length) {
                    case 2:
                        if (!(who instanceof Player)) {
                            who.sendMessage("This command is only available to players !");
                            return CommandStatus.VALID;
                        }

                        if (!who.hasPermission("lb.items.give-self")) {
                            who.sendMessage("Insufficient permissions !");
                            return CommandStatus.INSUFFICIENT_PERMISSION;
                        }

                        to = (Player)who;
                        itemName = args[0];
                        amount = fn.toInteger(args[1], 1);
                        break;
                    case 3:
                        if (!who.hasPermission("lb.items.give-others")) {
                            who.sendMessage("Insufficient permissions !");
                            return CommandStatus.INSUFFICIENT_PERMISSION;
                        }

                        to = LowbrainItems.getInstance().getServer().getPlayer(args[0]);
                        itemName = args[1];
                        amount = fn.toInteger(args[2],1);
                        break;
                    default:
                        return CommandStatus.INVALID;
                }

                item = LowbrainItems.getInstance().getItems().getOrDefault(itemName, LowbrainItems.getInstance().getStaffs().get(itemName));

                if(to == null){
                    who.sendMessage(ChatColor.RED + "[LowbrainItems] There is no such player ! ==> " + args[1]);
                    return CommandStatus.INVALID;
                }

                if(item == null){
                    who.sendMessage(ChatColor.RED + "[LowbrainItems] There is no such item ! ==> " + itemName);
                    return CommandStatus.INVALID;
                }

                try {
                    HashMap<Integer, ItemStack> leftOver = new HashMap<Integer, ItemStack>();

                    for (int i = 0; i < amount; i++)
                        leftOver.putAll((to.getInventory().addItem(item)));

                    if (!leftOver.isEmpty()) {
                        Location loc = to.getLocation();
                        loc.getWorld().dropItem(loc,item);
                    }

                    to.updateInventory();
                    who.sendMessage(ChatColor.GREEN + "[LowbrainItems] " + item.getItemMeta().getDisplayName() + " given to " + to.getName());
                } catch (Exception e){
                    who.sendMessage(ChatColor.RED + "[LowbrainItems] An error occurred. Could not give item to player !");
                }
                return CommandStatus.VALID;
            }
        });

        sub.register("list", onList = new Command("list") {
            @Override
            public CommandStatus execute(CommandSender who, String[] args, String cmd) {
                String lst = "[LowbrainItems] ***** list *****";

                int i = -1;

                for(Map.Entry<String, ItemStack> entry : LowbrainItems.getInstance().getItems().entrySet())
                    lst += entry.getKey() + (i++ % 4 == 0 ? " \n " : ", ");

                for(Map.Entry<String, ItemStack> entry : LowbrainItems.getInstance().getStaffs().entrySet())
                    lst += entry.getKey() + (i++ % 4 == 0 ? " \n " : ", ");

                lst += "[LowbrainItems] ****************";
                who.sendMessage(ChatColor.GREEN + lst);
                return CommandStatus.VALID;
            }
        });
        onList.addPermission("lb.items.list");
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

                    Class ItemStackClass = fn.getMinecraftClass("ItemStack");
                    Class NBTTagCompoundClass = fn.getMinecraftClass("NBTTagCompound");
                    Class NBTTagListClass = fn.getMinecraftClass("NBTTagList");
                    Class CraftItemStackClass = fn.getBukkitClass("inventory.CraftItemStack");
                    Class NBTBase = fn.getMinecraftClass("NBTBase");

                    // net.minecraft.server.v1_12_R1.ItemStack nmsStack = CraftItemStack.asNMSCopy(customWeapon);
                    Object nmsStack = CraftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, customWeapon);

                    // NBTTagCompound compound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                    Object compound = ((boolean)ItemStackClass.getMethod("hasTag").invoke(nmsStack))
                            ? ItemStackClass.getMethod("getTag").invoke(nmsStack)
                            : NBTTagCompoundClass.getConstructor().newInstance();

                    // NBTTagList modifiers = getAttributeModifiers(attributes);
                    Object modifiers = getAttributeModifiers(attributes);

                    List<String> enchts = getConfig().getStringList(name + ".enchantments");

                    // NBTTagList enchModifiers = getEnchants(enchts);
                    Object enchModifiers = getEnchants(enchts);

                    if(!(boolean)NBTTagListClass.getMethod("isEmpty").invoke(modifiers))
                        // compound.set("AttributeModifiers", enchModifiers);
                        NBTTagCompoundClass.getMethod("set", String.class, NBTBase).invoke(compound,"AttributeModifiers", modifiers);

                    // compound.set("ench", enchModifiers);
                    NBTTagCompoundClass.getMethod("set", String.class, NBTBase).invoke(compound,"ench", enchModifiers);

                    if(!(boolean)NBTTagListClass.getMethod("isEmpty").invoke(modifiers)
                            || !(boolean)NBTTagListClass.getMethod("isEmpty").invoke(enchModifiers)){
                        // nmsStack.setTag(compound);
                        ItemStackClass.getMethod("setTag", NBTTagCompoundClass).invoke(nmsStack, compound);
                        // customStaff = CraftItemStack.asBukkitCopy(nmsStack);
                        customWeapon = (ItemStack) CraftItemStackClass.getMethod("asBukkitCopy", ItemStackClass).invoke(null, nmsStack);
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

                Class ItemStackClass = fn.getMinecraftClass("ItemStack");
                Class NBTTagCompoundClass = fn.getMinecraftClass("NBTTagCompound");
                Class NBTTagListClass = fn.getMinecraftClass("NBTTagList");
                Class CraftItemStackClass = fn.getBukkitClass("inventory.CraftItemStack");
                Class NBTBase = fn.getMinecraftClass("NBTBase");

                // net.minecraft.server.v1_12_R1.ItemStack nmsStack
                Object nmsStack = CraftItemStackClass.getMethod("asNMSCopy", ItemStack.class).invoke(null, customStaff);
                //NBTTagCompound
                Object compound = ((boolean)ItemStackClass.getMethod("hasTag").invoke(nmsStack))
                        ? ItemStackClass.getMethod("getTag").invoke(nmsStack)
                        : NBTTagCompoundClass.getConstructor().newInstance();

                //NBTTagList
                Object modifiers = getAttributeModifiers(attributes);

                List<String> enchts = staffSection.getStringList("enchantments");

                //NBTTagList
                Object enchModifiers = getEnchants(enchts);

                if(!(boolean)NBTTagListClass.getMethod("isEmpty").invoke(modifiers))
                    // compound.set("AttributeModifiers", enchModifiers);
                    NBTTagCompoundClass.getMethod("set", String.class, NBTBase).invoke(compound,"AttributeModifiers", modifiers);

                // compound.set("ench", enchModifiers);
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase).invoke(compound,"ench", enchModifiers);


                if(!(boolean)NBTTagListClass.getMethod("isEmpty").invoke(modifiers)
                        || !(boolean)NBTTagListClass.getMethod("isEmpty").invoke(enchModifiers)){
                    // nmsStack.setTag(compound);
                    ItemStackClass.getMethod("setTag", NBTTagCompoundClass).invoke(nmsStack, compound);
                    // customStaff = CraftItemStack.asBukkitCopy(nmsStack);
                    customStaff = (ItemStack) CraftItemStackClass.getMethod("asBukkitCopy", ItemStackClass).invoke(null, nmsStack);
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

    private Object getEnchants(List<String> enchts)
            throws NoSuchMethodException, ClassNotFoundException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class NBTTagCompoundClass = fn.getMinecraftClass("NBTTagCompound");
        Class NBTTagListClass = fn.getMinecraftClass("NBTTagList");
        Class NBTTagIntClass = fn.getMinecraftClass("NBTTagInt");
        Class NBTBase = fn.getMinecraftClass("NBTBase");

        // NBTTagList enchModifiers = new NBTTagList();
        Object enchModifiers = NBTTagListClass.getConstructor().newInstance();

        //adding enchantments if needed
        if (enchts != null) {
            for (String ench :
                    enchts) {

                // NBTTagCompound modifier = new NBTTagCompound();
                Object modifier = NBTTagCompoundClass.getConstructor().newInstance();
                String[] temp = ench.split(",");

                int id = Integer.parseInt(temp[0].trim());
                int level = Integer.parseInt(temp[1].trim());

                if(level < 0 || id < 0 ){
                    this.getLogger().info("Invalid enchants parameters : level, id");
                    continue;
                }

                // modifier.set("id", new NBTTagInt(id));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "id", NBTTagIntClass.getConstructor(int.class).newInstance(id));
                // modifier.set("lvl", new NBTTagInt(level));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "lvl", NBTTagIntClass.getConstructor(int.class).newInstance(level));

                // enchModifiers.add(modifier);
                NBTTagListClass.getMethod("add", NBTBase).invoke(enchModifiers, modifier);
            }
        }

        return enchModifiers;
    }

    private Object getAttributeModifiers (ConfigurationSection attributes)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class NBTTagCompoundClass = fn.getMinecraftClass("NBTTagCompound");
        Class NBTTagIntClass = fn.getMinecraftClass("NBTTagInt");
        Class NBTTagStringClass = fn.getMinecraftClass("NBTTagString");
        Class NBTTagDoubleClass = fn.getMinecraftClass("NBTTagDouble");
        Class NBTTagListClass = fn.getMinecraftClass("NBTTagList");
        Class NBTBase = fn.getMinecraftClass("NBTBase");

        // NBTTagList modifiers = new NBTTagList();
        Object modifiers = NBTTagListClass.getConstructor().newInstance();

        //adding attributes if needed
        if(attributes != null){
            for (String attribute : attributes.getKeys(false)) {
                // NBTTagCompound modifier = new NBTTagCompound();

                Object modifier = NBTTagCompoundClass.getConstructor().newInstance();

                //net.minecraft.server.v1_12_R1.NBTTagList a = new net.minecraft.server.v1_12_R1.NBTTagList();

                // modifier.set("AttributeName", new NBTTagString("generic." + attributes.getString(attribute +".attribute_name")));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "AttributeName", NBTTagStringClass.getConstructor(String.class)
                            .newInstance(attributes.getString(attribute +".attribute_name")));

                // modifier.set("Name", new NBTTagString(attributes.getString(attribute +".name")));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "Name", NBTTagStringClass.getConstructor(String.class)
                            .newInstance(attributes.getString(attribute +".name")));

                // modifier.set("Amount", new NBTTagDouble(attributes.getDouble(attribute +".amount")));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "Amount", NBTTagDoubleClass.getConstructor(double.class)
                            .newInstance(attributes.getDouble(attribute +".amount")));

                // modifier.set("Operation", new NBTTagInt(attributes.getInt(attribute +".operation")));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "UUIDLeast", NBTTagIntClass.getConstructor(int.class)
                            .newInstance(attributes.getInt(attribute +".operation")));

                // modifier.set("UUIDLeast", new NBTTagInt(894654));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "UUIDLeast", NBTTagIntClass.getConstructor(int.class)
                            .newInstance(894654));

                // modifier.set("UUIDMost", new NBTTagInt(2872));
                NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                        .invoke(modifier, "UUIDMost", NBTTagIntClass.getConstructor(int.class)
                            .newInstance(2872));

                String slots = attributes.getString(attribute +".slots");
                if(slots.length() > 0){
                    // modifier.set("Slot", new NBTTagString(slots));
                    NBTTagCompoundClass.getMethod("set", String.class, NBTBase)
                            .invoke(modifier, "Slot", NBTTagStringClass.getConstructor(String.class)
                                .newInstance(slots));
                }

                // modifiers.add(modifier);
                NBTTagListClass.getMethod("add", NBTBase).invoke(modifiers, modifier);
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

