package com.garbagemule.MobArena.commands.setup;

import com.garbagemule.MobArena.Msg;
import com.garbagemule.MobArena.commands.Command;
import com.garbagemule.MobArena.commands.CommandInfo;
import com.garbagemule.MobArena.commands.Commands;
import com.garbagemule.MobArena.framework.Arena;
import com.garbagemule.MobArena.framework.ArenaMaster;
import com.garbagemule.MobArena.region.ArenaRegion;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandInfo(
    name    = "setup",
    pattern = "setup",
    usage   = "/ma setup <arena>",
    desc    = "enter setup mode for an arena",
    permission = "mobarena.setup.setup"
)
public class SetupCommand implements Command, Listener {
    @Override
    public boolean execute(ArenaMaster am, CommandSender sender, String... args) {
        if (!Commands.isPlayer(sender)) {
            am.getGlobalMessenger().tell(sender, Msg.MISC_NOT_FROM_CONSOLE);
            return true;
        }

        // Get the arena
        Arena arena;
        if (args.length == 0) {
            List<Arena> arenas = am.getArenas();
            if (arenas.size() > 1) {
                return false;
            }
            arena = arenas.get(0);
        } else {
            arena = am.getArenaWithName(args[0]);
            if (arena == null) {
                am.getGlobalMessenger().tell(sender, "There is no arena with the name " + ChatColor.RED + args[0] + ChatColor.RESET + ".");
                am.getGlobalMessenger().tell(sender, "Type " + ChatColor.YELLOW + "/ma addarena " + args[0] + ChatColor.RESET + " to create it!");
                return true;
            }
        }
        Player player = Commands.unwrap(sender);

        // Create the setup object
        Setup setup = new Setup(player, arena);

        // Register it as an event listener
        am.getPlugin().getServer().getPluginManager().registerEvents(setup, am.getPlugin());

        // Set up the conversation
        Conversation convo = new Conversation(am.getPlugin(), player, setup);
        setup.convo = convo;
        convo.addConversationAbandonedListener(setup);
        convo.setLocalEchoEnabled(false);
        // ... existing code unchanged ...

        private boolean region(Action action, String lower, String upper, Location loc) {
            if (action == Action.LEFT_CLICK_BLOCK) {
                regions(lower, loc);
                return true;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                regions(upper, loc);
                return true;
            }
            return false;
        }

        private boolean warps(PlayerInteractEvent event) {
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_BLOCK) {
                Location loc = event.getClickedBlock().getLocation();
                loc.setYaw(player.getLocation().getYaw());
                loc.setPitch(0);
                fix(loc);
                String warp = warpArray[warpIndex];
                warps(warp, loc);
                return true;
            } else if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
                warpIndex++;
                if (warpIndex == warpArray.length) {
                    warpIndex = 0;
                }
                next = formatYellow("Current warp: %s", warpArray[warpIndex]);
                return true;
            }
            return false;
        }

        private boolean spawns(PlayerInteractEvent event) {
            if (!event.hasBlock()) {
                return false;
            }

            Location l = event.getClickedBlock().getLocation();
            fix(l);
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_BLOCK) {
                spawns(l, true);
                return true;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                spawns(l, false);
                return true;
            }
            return false;
        }

        private boolean chests(PlayerInteractEvent event) {
            if (!event.hasBlock()) {
                return false;
            }

            Block b = event.getClickedBlock();
            Action action = event.getAction();
            if (action == Action.LEFT_CLICK_BLOCK) {
                chests(b, true);
                return true;
            } else if (action == Action.RIGHT_CLICK_BLOCK) {
                chests(b, false);
                return true;
            }
            return false;
        }

        // ... rest of the code unchanged ...

                        next = formatYellow("Showing both %s.", "regions");
                    } else {
                        next = formatYellow("Showing %s (lobby region not defined).", "arena region");
                    }
                } else if (region.isLobbyDefined()) {
                    region.showLobbyRegion(player);
                    next = formatYellow("Showing %s (arena region not defined).", "lobby region");
                } else {
                    next = "No regions have been defined yet.";
                }
                return this;
            } else if (toShow.equalsIgnoreCase("ar")) {
                if (region.isDefined()) {
                    next = formatYellow("Showing %s.", "arena region");
                    region.showRegion(player);
                } else {
                    next = "The region has not been defined yet.";
                }
                return this;
            } else if (toShow.equalsIgnoreCase("lr")) {
                if (region.isLobbyDefined()) {
                    next = formatYellow("Showing %s.", "lobby region");
                    region.showLobbyRegion(player);
                } else {
                    next = "The lobby region has not been defined yet.";
                }
                return this;
            }

            // Warps
            if (toShow.matches("arena|lobby|spec(tator)?|exit")) {
                next = formatYellow("Showing %s warp.", toShow);
                Location loc;
                loc = toShow.equals("arena")     ? region.getArenaWarp() :
                      toShow.equals("lobby")     ? region.getLobbyWarp() :
                      toShow.equals("spec")      ? region.getSpecWarp()  :
                      toShow.equals("spectator") ? region.getSpecWarp()  :
                      toShow.equals("exit")      ? region.getExitWarp()  : null;
                region.showBlock(player, loc);
                return this;
            }

            // Spawnpoints
            if (toShow.matches("sp(awn(point)?s?)?")) {
                next = formatYellow("Showing %s.", "spawnpoints");
                region.showSpawns(player);
                return this;
            }

            // Chests
            if (toShow.matches("c((hest(s)?)?|on(tainer(s)?)?)")) {
                next = formatYellow("Showing %s.", "containers");
                region.showChests(player);
                return this;
            }

            // Show the "show help", if invalid thing
            return acceptInput(context, "show ?");
        }

        /**
         * Missing points and warps
         */
        private Prompt missing() {
            if (missing.isEmpty()) {
                next = "All required points and warps have been set!";
            } else {
                next = "Missing points and warps: " + getMissing();
            }
            return this;
        }

        /**
         * Expand options
         */
        private Prompt expandOptions() {
            StringBuilder buffy = new StringBuilder();
            buffy.append("\nUsage: &eexp <region> <amount> <direction>");

            buffy.append("\n\n&r&7Variable details:");
            buffy.append("\n&r&7 region: &rar&7 (arena region) or &rlr&7 (lobby region)");
            buffy.append("\n&r&7 amount: number of blocks to expand by");
            buffy.append("\n&r&7 direction: &rup&7, &rdown&7, or &routs&7");

            buffy.append("\n\n&r&7Examples:");
            buffy.append("\n&r exp ar 5 up   &7expand arena region up by 5");
            buffy.append("\n&r exp lr 10 out   &7expand lobby region out by 10");
            next = color(buffy.toString());
            return this;
        }

        /**
         * Show options
         */
        private Prompt showOptions() {
            StringBuilder buffy = new StringBuilder();
            buffy.append("\nUsage: &eshow <thing>");

            buffy.append("\n\n&r&7Possible things to show:");
            buffy.append("\n&r&7 regions: &rar&7 (arena region) or &rlr&7 (lobby region) or &rr&7 (both)");
            buffy.append("\n&r&7 warps: &rarena&7, &rlobby&7, &rspec&7, or &rexit");
            buffy.append("\n&r&7 points: &rspawns&7 or &rchests&7");

            buffy.append("\n\n&r&7Examples:");
            buffy.append("\n&r show spawns   &7show spawnpoints");
            buffy.append("\n&r show ar   &7show arena region");
            next = color(buffy.toString());
            return this;
        }

        /**
         * Done!
         */
        private Prompt done() {
            if (missing.isEmpty()) {
                tell(player, "Setup complete! Arena is ready to be used!");
            } else {
                tell(player, "Setup incomplete. Missing points and warps: " + getMissing());
            }
            return Prompt.END_OF_CONVERSATION;
        }

        /**
         * Invalid input
         */
        private Prompt invalidInput() {
            next = formatYellow("Invalid input. Type %s for help", "?");
            return this;
        }


        // ====================================================================
        //  Auxiliary methods
        // ====================================================================

        private String getMissing() {
            StringBuilder buffy = new StringBuilder();
            for (String m : missing) {
                buffy.append("\n").append(m);
            }
            return buffy.toString();
        }

        private String color(String s) {
            return ChatColor.translateAlternateColorCodes('&', s);
        }

        private boolean inArenaWorld() {
            return player.getWorld().getName().equals(arena.getWorld().getName());
        }

        private void tell(Conversable whom, String msg) {
            whom.sendRawMessage(ChatColor.GREEN + "[MobArena] " + ChatColor.RESET + msg);
        }

        private String formatYellow(String msg, String arg) {
            return String.format(msg, ChatColor.YELLOW + arg + ChatColor.RESET);
        }

        private String getName(Location l) {
            return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
        }


        // ====================================================================
        //  Regular expressions for the input
        // ====================================================================

        private static final String HELP     = "[?]|h(elp)?";
        private static final String MISSING  = "miss(ing)?";
        private static final String EXPAND   = "exp(and)? (a|l)r [1-9][0-9]* (up|down|out)";
        private static final String EXPHELP  = "exp(and)?";
        private static final String SHOW     = "show (r|ar|lr|arena|lobby|spec(tator)?|exit|sp(awn(point)?s?)?|c((hest(s)?)?|on(tainer(s)?)?))";
        private static final String SHOWHELP = "show";
        private static final String DONE     = "done|quit|stop|end";
    }
}