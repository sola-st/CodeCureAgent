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

import java.util.Map;

/**
 * Created by Pub4Game on 23.01.2016.
 */
public class EnchantCommand extends VanillaCommand {

    private static final Map<String, Integer> ENCHANTMENT_NAME_TO_ID = Map.ofEntries(
            Map.entry("protection", 0),
            Map.entry("fire_protection", 1),
            Map.entry("feather_falling", 2),
            Map.entry("blast_protection", 3),
            Map.entry("projectile_projection", 4),
            Map.entry("thorns", 5),
            Map.entry("respiration", 6),
            Map.entry("aqua_affinity", 7),
            Map.entry("depth_strider", 8),
            Map.entry("sharpness", 9),
            Map.entry("smite", 10),
            Map.entry("bane_of_arthropods", 11),
            Map.entry("knockback", 12),
            Map.entry("fire_aspect", 13),
            Map.entry("looting", 14),
            Map.entry("efficiency", 15),
            Map.entry("silk_touch", 16),
            Map.entry("durability", 17),
            Map.entry("fortune", 18),
            Map.entry("power", 19),
            Map.entry("punch", 20),
            Map.entry("flame", 21),
            Map.entry("infinity", 22),
            Map.entry("luck_of_the_sea", 23),
            Map.entry("lure", 24),
            Map.entry("frost_walker", 25),
            Map.entry("mending", 26),
            Map.entry("binding_curse", 27),
            Map.entry("vanishing_curse", 28),
            Map.entry("impaling", 29),
            Map.entry("loyality", 30),
            Map.entry("riptide", 31),
            Map.entry("channeling", 32)
    );

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
        Integer id = ENCHANTMENT_NAME_TO_ID.get(value);
        if (id != null) {
            return id;
        }
        return Integer.parseInt(value);
    }
}