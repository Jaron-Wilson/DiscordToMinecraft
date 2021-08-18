package me.jaron.dja.discordsmpplugin;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;

public final class DiscordSMPPlugin extends JavaPlugin {

    private JDA jda;
    private TextChannel chatChannel;

    private final Map <String, String> advancementToDisplayMap = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String botToken = getConfig().getString("bot-token");
        try {
            jda = JDABuilder.createDefault(botToken)
                    .build()
                    .awaitReady();
        } catch (InterruptedException | LoginException exception) {
            exception.printStackTrace();
        }

        if (jda == null) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        String chatChannelId = getConfig().getString("chat-channel-id");
        if (chatChannelId != null) {
            chatChannel = jda.getTextChannelById(chatChannelId);
        }

        ConfigurationSection advancementMap = getConfig().getConfigurationSection("advancementMap");
        if (advancementMap != null) {
            for (String key : advancementMap.getKeys(false)) {
                advancementToDisplayMap.put(key, advancementMap.getString(key));
            }
        }

        jda.addEventListener(new DiscordListener());
        getServer().getPluginManager().registerEvents(new SpigotListener(), this);
    }

    @Override
    public void onDisable() {
        if (jda != null) jda.shutdownNow();
    }

    private void sendMessage(Player player , String content, boolean contentInAuthorLine, Color color){
        if (chatChannel == null) return;

        EmbedBuilder builder = new EmbedBuilder()
                .setAuthor(
                        contentInAuthorLine ? content : player.getDisplayName(),
                        null,
                        "https://crafatar.com/avatars/" + player.getUniqueId().toString() + "?overlay=1"
                );

        if (!contentInAuthorLine) {
            builder.setDescription(content);
        }

        chatChannel.sendMessage(builder.build()).queue();
    }

    public final class SpigotListener implements Listener {
        @EventHandler
        private void onChat(AsyncPlayerChatEvent event) {
            sendMessage(event.getPlayer(), event.getMessage(), false, Color.GRAY);
        }

        @EventHandler
        private void onJoin(PlayerJoinEvent event) {
            sendMessage(event.getPlayer(), event.getPlayer().getDisplayName() + " joined the game.", true, Color.GREEN);
        }

        @EventHandler
        private void onQuit(PlayerQuitEvent event) {
            sendMessage(event.getPlayer(), event.getPlayer().getDisplayName() + " left the game.", true, Color.RED);
        }

        @EventHandler
        private void onDeath(PlayerDeathEvent event) {
            Player player = event.getEntity();
            String deathMessage = event.getDeathMessage() == null ? player.getDisplayName() + " died." : event.getDeathMessage();
            sendMessage(player, deathMessage, true, Color.RED);
        }

        @EventHandler
        private void onAdvancement(PlayerAdvancementDoneEvent event) {
            String advancementKey = event.getAdvancement().getKey().getKey();
            String display = advancementToDisplayMap.get(advancementKey);
            if (display == null) return;

            sendMessage(event.getPlayer(), event.getPlayer().getDisplayName() + " has made the advancement [" + display + "]", true, Color.BLUE);
        }
    }
    public final class DiscordListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(@NotNull MessageReceivedEvent event) {
           if (!event.getChannel().equals(chatChannel)) return;

            Member member = event.getMember();
            if (member == null || member.getUser().isBot()) return;
            String message = event.getMessage().getContentDisplay();
            Bukkit.broadcastMessage(ChatColor.DARK_BLUE + "<" + member.getEffectiveName() + ">" + ChatColor.RESET + " " + message);
        }
    }
}
