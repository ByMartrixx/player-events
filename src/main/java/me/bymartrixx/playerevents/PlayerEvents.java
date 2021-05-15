package me.bymartrixx.playerevents;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.bymartrixx.playerevents.api.event.PlayerDeathCallback;
import me.bymartrixx.playerevents.api.event.PlayerJoinCallback;
import me.bymartrixx.playerevents.api.event.PlayerKillEntityCallback;
import me.bymartrixx.playerevents.api.event.PlayerKillPlayerCallback;
import me.bymartrixx.playerevents.api.event.PlayerLeaveCallback;
import me.bymartrixx.playerevents.command.PlayerEventsCommand;
import me.bymartrixx.playerevents.config.PlayerEventsConfig;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PlayerEvents implements DedicatedServerModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();
    public static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).setPrettyPrinting().create();

    public static final String MOD_ID = "player_events";
    public static final String MOD_NAME = "Player Events";

    public static PlayerEventsConfig CONFIG;

    @Override
    public void onInitializeServer() {
        PlayerEventsConfig.Manager.loadConfig();

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> PlayerEventsCommand.register(dispatcher));

        PlayerDeathCallback.EVENT.register((player, source) -> CONFIG.runDeathActions(player));

        PlayerJoinCallback.EVENT.register((player, server) -> CONFIG.runJoinActions(server, player));

        PlayerLeaveCallback.EVENT.register((player, server) -> CONFIG.runLeaveActions(server, player));

        PlayerKillPlayerCallback.EVENT.register((player, killedPlayer) ->
                CONFIG.runKillPlayerActions(player.getServer(), player, killedPlayer));

        PlayerKillEntityCallback.EVENT.register((player, killedEntity) ->
                CONFIG.runKillEntityActions(player.getServer(), player, killedEntity));
    }

    public static void log(Level level, String message) {
        log(level, message, (Object) null);
    }

    public static void log(Level level, String message, Object ... fields){
        LOGGER.log(level, "[" + MOD_NAME + "] " + message, fields);
    }
}
