package lowbrain.items.main;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler implements CommandExecutor {
    private final LowbrainItems plugin;

    public CommandHandler(LowbrainItems plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when the plugin receice a command
     */
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("lbitems"))
            return false;

        if(args.length <= 0)
            return false;

        switch (args[0].toLowerCase()){
            case "list":
                return onList(sender, cmd, label, args);
            case "give":
                return onGive(sender, cmd, label, args);
            default:
                return false;
        }
    }

    private boolean onGive(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        Player to = null;
        int amount = 0;
        ItemStack item = null;
        String iName = null;

        if(args.length == 3
                && sender instanceof Player
                &&  (sender.hasPermission("lbitems.give-self")
                || sender.hasPermission("lbitems.give-others")
                || sender.isOp())){
            to = (Player)sender;
            iName = args[1];
            amount = intTryParse(args[2].toLowerCase(),1);
        } else if(args.length == 4
                &&  (sender.hasPermission("lbitems.give-others")
                || sender.isOp()
                || sender instanceof ConsoleCommandSender)){
            to = plugin.getServer().getPlayer(args[1]);
            iName = args[2];
            amount = intTryParse(args[3].toLowerCase(),1);
        } else return false;

        item = plugin.getItems().getOrDefault(iName, plugin.getStaffs().get(iName));

        if(to == null){
            sender.sendMessage(ChatColor.RED + "[LowbrainItems] There is no such player ! ==> " + args[1]);
            return true;
        }

        if(item == null){
            sender.sendMessage(ChatColor.RED + "[LowbrainItems] There is no such item ! ==> " + iName);
            return true;
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
            sender.sendMessage(ChatColor.GREEN + "[LowbrainItems] " + item.getItemMeta().getDisplayName() + " given to " + to.getName());
        } catch (Exception e){
            sender.sendMessage(ChatColor.RED + "[LowbrainItems] An error occurred. Could not give item to player !");
        }
        return true;
    }

    private boolean onList(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (sender instanceof Player && (!sender.hasPermission("lbitems.list") && !sender.isOp()))
            return false;

        String lst = "[LowbrainItems] ***** list *****";

        int i = -1;

        for(Map.Entry<String, ItemStack> entry : plugin.getItems().entrySet())
            lst += entry.getKey() + (i++ % 4 == 0 ? " \n " : ", ");

        for(Map.Entry<String, ItemStack> entry : plugin.getStaffs().entrySet())
            lst += entry.getKey() + (i++ % 4 == 0 ? " \n " : ", ");

        lst += "[LowbrainItems] ****************";
        sender.sendMessage(ChatColor.GREEN + lst);
        return true;
    }

    private int intTryParse(String s, Integer d){
        try {
            return Integer.parseInt(s);
        }catch (Exception e){
            return d;
        }
    }
}