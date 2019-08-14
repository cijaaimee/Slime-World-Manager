package com.grinderwolf.swm.plugin.update;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater implements Listener {

    private final boolean outdatedVersion;

    public Updater() {
        String currentVersionString = SWMPlugin.getInstance().getDescription().getVersion();

        if (currentVersionString.equals("${project.version}")) {
            Logging.warning("You are using a custom version of SWM. Update checking is disabled.");
            outdatedVersion = false;
            return;
        }

        Version currentVersion = new Version(currentVersionString);

        if (currentVersion.getTag().toLowerCase().endsWith("snapshot")) {
            Logging.warning("You are using a snapshot version of SWM. Update checking is disabled.");
            outdatedVersion = false;
            return;
        }

        Logging.info("Checking for updates...");
        Version latestVersion;

        try {
            latestVersion = new Version(getLatestVersion());
        } catch (IOException ex) {
            Logging.error("Failed to check for updates:");
            outdatedVersion = false;
            ex.printStackTrace();
            return;
        }

        int result = latestVersion.compareTo(currentVersion);
        outdatedVersion = result > 0;

        if (result == 0) {
            Logging.info("You are running the latest version of Slime World Manager.");
        } else if (outdatedVersion) {
            Logging.warning("You are running an outdated of Slime World Manager. Please download the latest version at SpigotMC.org.");
        } else {
            Logging.warning("You are running an unreleased version of Slime World Manager.");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (outdatedVersion && player.hasPermission("swm.updater")) {
            player.sendMessage(Logging.COMMAND_PREFIX + "This server is running an outdated of Slime World Manager. Please download the latest version at SpigotMC.org.");
        }
    }

    private static String getLatestVersion() throws IOException {
        URL url = new URL("https://api.spiget.org/v2/resources/69974/versions/latest?" + System.currentTimeMillis());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.addRequestProperty("User-Agent", "SWM " + SWMPlugin.getInstance().getDescription().getVersion());

        connection.setUseCaches(true);
        connection.setDoOutput(true);

        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String input;

            while ((input = br.readLine()) != null) {
                content.append(input);
            }
        }

        JsonObject statistics = new JsonParser().parse(content.toString()).getAsJsonObject();
        return statistics.get("name").getAsString();
    }
}
