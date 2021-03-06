package me.bymartrixx.playerevents.config;

import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import eu.pb4.placeholders.PlaceholderAPI;
import me.bymartrixx.playerevents.PlayerEvents;
import me.bymartrixx.playerevents.Utils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class PlayerEventsConfig {
    public static class Manager {
        private static File configFile;

        public static void prepareConfigFile() {
            if (configFile != null) {
                return;
            }
            configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), PlayerEvents.MOD_ID + ".json");
        }

        public static void createConfigFile() {
            PlayerEvents.CONFIG = new PlayerEventsConfig();

            saveConfig();
        }

        public static void saveConfig() {
            prepareConfigFile();

            String jsonString = PlayerEvents.GSON.toJson(PlayerEvents.CONFIG);
            try (FileWriter fileWriter = new FileWriter(configFile)) {
                fileWriter.write(jsonString);
            } catch (IOException e) {
                PlayerEvents.LOGGER.error("Couldn't save Player Events config.", e);
            }
        }

        public static void loadConfig() {
            prepareConfigFile();

            try {
                if (!configFile.exists()) {
                    createConfigFile();
                }
                if (configFile.exists()) {
                    BufferedReader bReader = new BufferedReader(new FileReader(configFile));

                    PlayerEventsConfig savedConfig = PlayerEvents.GSON.fromJson(bReader, PlayerEventsConfig.class);
                    if (savedConfig != null) {
                        PlayerEvents.CONFIG = savedConfig;
                    }
                }
            } catch (FileNotFoundException e) {
                PlayerEvents.LOGGER.error("Couldn't load configuration for Player Events. Reverting to default.", e);
                createConfigFile();
            }
        }
    }

    private final ActionList death;
    private final ActionList firstJoin;
    private final ActionList join;
    private final ActionList killEntity;
    private final ActionList killPlayer;
    private final ActionList leave;
    private final CustomCommandActionList[] customCommands;

    public PlayerEventsConfig() {
        this.death = new ActionList();
        this.firstJoin = new ActionList();
        this.join = new ActionList();
        this.killPlayer = new ActionList();
        this.killEntity = new ActionList();
        this.leave = new ActionList();
        this.customCommands = new CustomCommandActionList[]{};
    }

    private static void doSimpleAction(ActionList actionList, ServerPlayerEntity player) {
        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addEntityPlaceholders(stringPlaceholders, player, "player");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addEntityTextPlaceholders(textPlaceholders, player, "player");
        for (String action : actionList.actions) {
            doAction(action, player, stringPlaceholders, textPlaceholders, actionList.broadcastToEveryone);
        }
    }

    private static void doSimpleAction(ActionList actionList, ServerPlayerEntity player,
                                       MinecraftServer server) {
        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addEntityPlaceholders(stringPlaceholders, player, "player");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addEntityTextPlaceholders(textPlaceholders, player, "player");
        for (String action : actionList.actions) {
            doAction(action, player, server, stringPlaceholders, textPlaceholders, actionList.broadcastToEveryone);
        }
    }

    private static void doAction(String action, ServerPlayerEntity player,
                                 Map<String, String> strPlaceholders, Map<String, Text> textPlaceholders, boolean broadcast) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return; // Shouldn't happen
        }

        doAction(action, player, server, strPlaceholders, textPlaceholders, broadcast);
    }

    private static void doAction(String action, ServerPlayerEntity player, MinecraftServer server,
                                 Map<String, String> strPlaceholders, Map<String, Text> textPlaceholders, boolean broadcast) {
        action = action.trim();

        if (action.startsWith("/")) {
            String command = Utils.replacePlaceholders(action, strPlaceholders);
            if (PlayerEvents.isPlaceholderApiLoaded()) {
                command = PlaceholderAPI.parseString(PlaceholderAPI.parseString(command, player), server);
            }
            server.getCommandManager().execute(server.getCommandSource(), command);
        } else {
            Text message = Utils.replaceTextPlaceholders(action, textPlaceholders);
            if (PlayerEvents.isPlaceholderApiLoaded()) {
                message = PlaceholderAPI.parseText(PlaceholderAPI.parseText(message, player), server);
            }
            Utils.message(player, message, broadcast);
        }
    }

    public static void testSimpleAction(ActionList actionList, ServerCommandSource source, String title) {
        String message = String.format(title, actionList.broadcastToEveryone ? "Send to everyone" : "Send only to the player");
        source.sendFeedback(new LiteralText("" + Formatting.GRAY + Formatting.ITALIC + message), false);

        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addCommandSourcePlaceholders(stringPlaceholders, source, "player");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addCommandSourceTextPlaceholders(textPlaceholders, source, "player");
        for (String action : actionList.actions) {
            testAction(action, source, stringPlaceholders, textPlaceholders);
        }
    }

    public static void testAction(String action, ServerCommandSource source,
                                  Map<String, String> strPlaceholders, Map<String, Text> textPlaceholders) {
        action = action.trim();

        if (action.startsWith("/")) {
            String command = Utils.replacePlaceholders(action, strPlaceholders);
            if (PlayerEvents.isPlaceholderApiLoaded()) {
                command = PlaceholderAPI.parseString(command, source.getMinecraftServer());
                try {
                    command = PlaceholderAPI.parseString(command, source.getPlayer());
                } catch (CommandSyntaxException ignored) {
                    // Ignore
                }
            }
            source.sendFeedback(new LiteralText("[COMMAND] " + command), false);
        } else {
            Text message = Utils.replaceTextPlaceholders(action, textPlaceholders);
            if (PlayerEvents.isPlaceholderApiLoaded()) {
                message = PlaceholderAPI.parseText(message, source.getMinecraftServer());
                try {
                    message = PlaceholderAPI.parseText(message, source.getPlayer());
                } catch (CommandSyntaxException ignored) {
                    // Ignore
                }
            }
            source.sendFeedback(message, false);
        }
    }

    public String[] getDeathActions() {
        return this.death.actions;
    }

    public String[] getFirstJoinActions() {
        return this.firstJoin.actions;
    }

    public String[] getJoinActions() {
        return this.join.actions;
    }

    public String[] getKillEntityActions() {
        return this.killEntity.actions;
    }

    public String[] getKillPlayerActions() {
        return this.killPlayer.actions;
    }

    public String[] getLeaveActions() {
        return this.leave.actions;
    }

    public void doDeathActions(ServerPlayerEntity player) {
        doSimpleAction(this.death, player);
    }

    public void doFirstJoinActions(ServerPlayerEntity player, MinecraftServer server) {
        doSimpleAction(this.firstJoin, player, server);
    }

    public void doJoinActions(ServerPlayerEntity player, MinecraftServer server) {
        doSimpleAction(this.join, player, server);
    }

    public void doLeaveActions(ServerPlayerEntity player, MinecraftServer server) {
        doSimpleAction(this.leave, player, server);
    }

    public void doKillEntityActions(ServerPlayerEntity player, Entity killedEntity) {
        Map<String, String> strPlaceholders = Maps.newHashMap();
        Utils.addEntityPlaceholders(strPlaceholders, player, "player");
        Utils.addEntityPlaceholders(strPlaceholders, killedEntity, "killedEntity");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addEntityTextPlaceholders(textPlaceholders, player, "player");
        Utils.addEntityTextPlaceholders(textPlaceholders, killedEntity, "killedEntity");

        for (String action : this.killEntity.actions) {
            doAction(action, player, strPlaceholders, textPlaceholders, this.killEntity.broadcastToEveryone);
        }
    }

    public void doKillPlayerActions(ServerPlayerEntity player, ServerPlayerEntity killedPlayer) {
        Map<String, String> strPlaceholders = Maps.newHashMap();
        Utils.addEntityPlaceholders(strPlaceholders, player, "player");
        Utils.addEntityPlaceholders(strPlaceholders, killedPlayer, "killedPlayer");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addEntityTextPlaceholders(textPlaceholders, player, "player");
        Utils.addEntityTextPlaceholders(textPlaceholders, killedPlayer, "killedPlayer");

        for (String action : this.killPlayer.actions) {
            doAction(action, player, strPlaceholders, textPlaceholders, this.killPlayer.broadcastToEveryone);
        }
    }

    public void doCustomCommandsActions(String command, ServerCommandSource source) {
        for (CustomCommandActionList actionList : this.customCommands) {
            if (actionList.getCommandStr().matches(command)) {
                try {
                    doSimpleAction(actionList, source.getPlayer());
                } catch (CommandSyntaxException e) {
                    PlayerEvents.LOGGER.error("This should not happen, please report it to the mod author, attaching the logs", e);
                }
            }
        }
    }

    public void testDeathActions(ServerCommandSource source) {
        testSimpleAction(this.death, source, "Death actions (%s):");
    }

    public void testFirstJoinActions(ServerCommandSource source) {
        testSimpleAction(this.firstJoin, source, "First time join actions (%s):");
    }

    public void testJoinActions(ServerCommandSource source) {
        testSimpleAction(this.join, source, "Join actions (%s):");
    }

    public void testLeaveActions(ServerCommandSource source) {
        testSimpleAction(this.leave, source, "Leave actions:");
    }

    public void testKillEntityActions(ServerCommandSource source) {
        String message = String.format("Kill entity actions (%s):", this.killEntity.broadcastToEveryone ? "Send to everyone" : "Send only to the player");
        source.sendFeedback(new LiteralText("" + Formatting.GRAY + Formatting.ITALIC + message), false);

        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addCommandSourcePlaceholders(stringPlaceholders, source, "player");
        stringPlaceholders.put("killedEntity", "dummyEntity");
        stringPlaceholders.put("killedEntity.display", "Dummy entity");
        stringPlaceholders.put("killedEntity.entityName", "Dummy");
        stringPlaceholders.put("killedEntity.x", "0.0");
        stringPlaceholders.put("killedEntity.y", "0.0");
        stringPlaceholders.put("killedEntity.z", "0.0");

        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addCommandSourceTextPlaceholders(textPlaceholders, source, "player");
        textPlaceholders.put("killedEntity", new LiteralText("dummyEntity"));
        textPlaceholders.put("killedEntity.display", new LiteralText("Dummy entity"));
        textPlaceholders.put("killedEntity.entityName", new LiteralText("Dummy"));
        textPlaceholders.put("killedEntity.x", new LiteralText("0.0"));
        textPlaceholders.put("killedEntity.y", new LiteralText("0.0"));
        textPlaceholders.put("killedEntity.z", new LiteralText("0.0"));

        for (String action : this.killEntity.actions) {
            testAction(action, source, stringPlaceholders, textPlaceholders);
        }
    }

    public void testKillPlayerActions(ServerCommandSource source) {
        String message = String.format("Kill player actions (%s):", this.killPlayer.broadcastToEveryone ? "Send to everyone" : "Send only to the player");
        source.sendFeedback(new LiteralText("" + Formatting.GRAY + Formatting.ITALIC + message), false);

        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addCommandSourcePlaceholders(stringPlaceholders, source, "player");
        Utils.addCommandSourcePlaceholders(stringPlaceholders, source, "killedPlayer");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addCommandSourceTextPlaceholders(textPlaceholders, source, "player");
        Utils.addCommandSourceTextPlaceholders(textPlaceholders, source, "killedPlayer");

        for (String action : this.killPlayer.actions) {
            testAction(action, source, stringPlaceholders, textPlaceholders);
        }
    }

    public void testCustomCommandsActions(ServerCommandSource source) {
        Map<String, String> stringPlaceholders = Maps.newHashMap();
        Utils.addCommandSourcePlaceholders(stringPlaceholders, source, "player");
        Map<String, Text> textPlaceholders = Maps.newHashMap();
        Utils.addCommandSourceTextPlaceholders(textPlaceholders, source, "player");

        for (CustomCommandActionList actionList : this.customCommands) {
            String message = String.format("'%s' actions ('%s'):", actionList.getCommandStr(), actionList.broadcastToEveryone ? "Send to everyone" : "Send only to the player");
            source.sendFeedback(new LiteralText("§7§o" + message), false);

            for (String action : actionList.actions) {
                testAction(action, source, stringPlaceholders, textPlaceholders);
            }
        }
    }

    public void testEveryActionGroup(ServerCommandSource source) {
        this.testDeathActions(source);
        this.testFirstJoinActions(source);
        this.testJoinActions(source);
        this.testKillEntityActions(source);
        this.testKillPlayerActions(source);
        this.testLeaveActions(source);
        this.testCustomCommandsActions(source);
    }

    public void registerCustomCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        CommandNode<ServerCommandSource> root = dispatcher.getRoot();
        for (CustomCommandActionList actionList : this.customCommands) {
            CommandNode<ServerCommandSource> node = root.getChild(actionList.getCommand());
            if (node == null) {
                dispatcher.register(CommandManager.literal(actionList.getCommand()).executes(ctx -> 1));
            }
        }
    }

    public static class ActionList {
        protected final String[] actions;
        protected final boolean broadcastToEveryone;

        public ActionList() {
            this.actions = new String[] {};
            this.broadcastToEveryone = true;
        }
    }

    public static class CustomCommandActionList extends ActionList {
        protected final String command;

        public CustomCommandActionList() {
            super();
            this.command = "";
        }

        public String getCommandStr() {
            return this.command.startsWith("/") ? this.command : "/" + this.command;
        }

        public String getCommand() {
            return !this.command.startsWith("/") ? this.command : this.command.substring(1);
        }
    }
}
