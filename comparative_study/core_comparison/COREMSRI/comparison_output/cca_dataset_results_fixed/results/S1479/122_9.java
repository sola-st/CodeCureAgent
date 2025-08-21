package cn.nukkit.command.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandEnum;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.TextFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Pub4Game on 23.01.2016.
 */
public class EnchantCommand extends VanillaCommand {

    private static final Map<String, Integer> ENCHANT_NAME_TO_ID = new HashMap<>();

    static {
        ENCHANT_NAME_TO_ID.put("protection", 0);
        ENCHANT_NAME_TO_ID.put("fire_protection", 1);
        ENCHANT_NAME_TO_ID.put("feather_falling", 2);
        ENCHANT_NAME_TO_ID.put("blast_protection", 3);
        ENCHANT_NAME_TO_ID.put("projectile_projection", 4);
        ENCHANT_NAME_TO_ID.put("thorns", 5);
        ENCHANT_NAME_TO_ID.put("respiration", 6);
        ENCHANT_NAME_TO_ID.put("aqua_affinity", 7);
        ENCHANT_NAME_TO_ID.put("depth_strider", 8);
        ENCHANT_NAME_TO_ID.put("sharpness", 9);
        ENCHANT_NAME_TO_ID.put("smite", 10);
        ENCHANT_NAME_TO_ID.put("bane_of_arthropods", 11);
        ENCHANT_NAME_TO_ID.put("knockback", 12);
        ENCHANT_NAME_TO_ID.put("fire_aspect", 13);
        ENCHANT_NAME_TO_ID.put("looting", 14);
        ENCHANT_NAME_TO_ID.put("efficiency", 15);
        ENCHANT_NAME_TO_ID.put("silk_touch", 16);
        ENCHANT_NAME_TO_ID.put("durability", 17);
        ENCHANT_NAME_TO_ID.put("fortune", 18);
        ENCHANT_NAME_TO_ID.put("power", 19);
        ENCHANT_NAME_TO_ID.put("punch", 20);
        ENCHANT_NAME_TO_ID.put("flame", 21);
        ENCHANT_NAME_TO_ID.put("infinity", 22);
        ENCHANT_NAME_TO_ID.put("luck_of_the_sea", 23);
        ENCHANT_NAME_TO_ID.put("lure", 24);
        ENCHANT_NAME_TO_ID.put("frost_walker", 25);
        ENCHANT_NAME_TO_ID.put("mending", 26);
        ENCHANT_NAME_TO_ID.put("binding_curse", 27);
        ENCHANT_NAME_TO_ID.put("vanishing_curse", 28);
        ENCHANT_NAME_TO_ID.put("impaling", 29);
        ENCHANT_NAME_TO_ID.put("loyality", 30);
        ENCHANT_NAME_TO_ID.put("riptide", 31);
        ENCHANT_NAME_TO_ID.put("channeling", 32);
    }

    public EnchantCommand(String name) {
        super(name, "%nukkit.command.enchant.description", "%commands.enchant.usage");
        this.setPermission("nukkit.command.enchant");
        this.commandParameters.clear();
        this.commandParameters.put("default", new CommandParameter[]{
                CommandParameter.newType("player", CommandParamType.TARGET),
                CommandParameter.newType("enchantmentId", CommandParamType.INT),
                CommandParameter.newType("level", true, CommandParamType.INT)
        });
        this.commandParameters.put("byName", new CommandParameter[]{
                CommandParameter.newType("player", CommandParamType.TARGET),
                CommandParameter.newEnum("enchantmentName", new CommandEnum("Enchant",
                        "protection", "fire_protection", "feather_falling", "blast_protection", "projectile_projection", "thorns", "respiration",
                        "aqua_affinity", "depth_strider", "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "efficiency",
                        "silk_touch", "durability", "fortune", "power", "punch", "flame", "infinity", "luck_of_the_sea", "lure", "frost_walker", "mending",
                        "binding_curse", "vanishing_curse", "impaling", "loyality", "riptide", "channeling")),
                CommandParameter.newType("level", true, CommandParamType.INT)
        });
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.testPermission(sender)) {
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.usageMessage));
            return true;
        }
        Player player = sender.getServer().getPlayer(args[0]);
        if (player == null) {
            sender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.player.notFound"));
            return true;
        }
        int enchantId;
        int enchantLevel;
        try {
            enchantId = getIdByName(args[1]);
            enchantLevel = args.length == 3 ? Integer.parseInt(args[2]) : 1;
        } catch (NumberFormatException e) {
            sender.sendMessage(new TranslationContainer("commands.generic.usage", this.usageMessage));
            return true;
        }
        Enchantment enchantment = Enchantment.getEnchantment(enchantId);
        if (enchantment == null) {
            sender.sendMessage(new TranslationContainer("commands.enchant.notFound", String.valueOf(enchantId)));
            return true;
        }
        enchantment.setLevel(enchantLevel);
        Item item = player.getInventory().getItemInHand();
        if (item.getId() <= 0) {
            sender.sendMessage(new TranslationContainer("commands.enchant.noItem"));
            return true;
        }
        item.addEnchantment(enchantment);
        player.getInventory().setItemInHand(item);
        Command.broadcastCommandMessage(sender, new TranslationContainer("%commands.enchant.success"));
        return true;
    }

    public int getIdByName(String value) throws NumberFormatException {
        Integer id = ENCHANT_NAME_TO_ID.get(value);
        if (id != null) {
            return id;
        }
        return Integer.parseInt(value);
    }
}

