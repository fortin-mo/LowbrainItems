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
    private final Main plugin;

    public CommandHandler(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when the plugin receice a command
     */
    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("lbitems")) {
            return false;
        }

        if(args.length <= 0){
            return false;
        }

        Player to = null;
        int amount = 0;
        ItemStack item = null;

        switch (args[0].toLowerCase()){
            case "give":
                if(args.length == 3
                        && sender instanceof Player
                        &&  (sender.hasPermission("lbitems.give-self")
                        || sender.hasPermission("lbitems.give-others")
                        || sender.isOp())){
                    to = (Player)sender;
                    item = plugin.getItems().get(args[1]);
                    amount = intTryParse(args[2].toLowerCase(),1);
                }
                else if(args.length == 4
                        &&  (sender.hasPermission("lbitems.give-others")
                        || sender.isOp()
                        || sender instanceof ConsoleCommandSender)){
                    to = plugin.getServer().getPlayer(args[1]);
                    item = plugin.getItems().get(args[2]);
                    amount = intTryParse(args[3].toLowerCase(),1);
                }
                else return false;

                if(to == null){
                    sender.sendMessage(ChatColor.RED + "There is no such player !");
                    return true;
                }
                if(item == null){
                    sender.sendMessage(ChatColor.RED + "There is no such item !");
                    return true;
                }

                try {
                    HashMap<Integer, ItemStack> leftOver = new HashMap<Integer, ItemStack>();
                    for (int i = 0; i < amount; i++) {
                        leftOver.putAll((to.getInventory().addItem(item)));
                    }
                    if (!leftOver.isEmpty()) {
                        Location loc = to.getLocation();
                        loc.getWorld().dropItem(loc,item);
                    }
                    to.updateInventory();
                    sender.sendMessage(ChatColor.GREEN + item.getItemMeta().getDisplayName() + " gived to " + to.getName());
                } catch (Exception e){
                    sender.sendMessage(ChatColor.RED + "An error occured. Could not give item to player !");
                }
                return true;
            default:
                return false;
        }
    }

    private int intTryParse(String s, Integer d){
        try {
            return Integer.parseInt(s);
        }catch (Exception e){
            return d;
        }
    }
}