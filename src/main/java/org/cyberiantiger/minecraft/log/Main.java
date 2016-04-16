package org.cyberiantiger.minecraft.log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.cyberiantiger.minecraft.log.cmd.AbstractCommand;
import org.cyberiantiger.minecraft.log.cmd.AuditCommand;
import org.cyberiantiger.minecraft.log.cmd.CheckCommand;
import org.cyberiantiger.minecraft.log.cmd.CommandException;
import org.cyberiantiger.minecraft.log.cmd.DuckLogCommand;
import org.cyberiantiger.minecraft.log.cmd.FriendCommand;
import org.cyberiantiger.minecraft.log.cmd.InvalidSenderException;
import org.cyberiantiger.minecraft.log.cmd.PermissionException;
import org.cyberiantiger.minecraft.log.cmd.SeenCommand;
import org.cyberiantiger.minecraft.log.cmd.TimeTopCommand;
import org.cyberiantiger.minecraft.log.cmd.UsageException;
import org.cyberiantiger.minecraft.log.config.AutoPromote;
import org.cyberiantiger.minecraft.log.config.Config;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.introspector.BeanAccess;

import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;

import net.milkbowl.vault.permission.Permission;

public class Main extends JavaPlugin implements Listener {
    private static final String CONFIG = "config.yml";
    private static final String MESSAGES = "locale.properties";
    
    private final Properties messages = new Properties();
    private final Database database;
    private Permission permissionService;
    private Config config;

    public Main() {
        database = new Database(this);
    }

    public Database getDB() {
        return database;
    }

    public Config getRealConfig() {
        return config;
    }

    private boolean copyDefault(String source, String dest) {
        File destFile = new File(getDataFolder(), dest);
        if (!destFile.exists()) {
            try {
                destFile.getParentFile().mkdirs();
                InputStream in = getClass().getClassLoader().getResourceAsStream(source);
                if (in != null) {
                    try {
                        try (OutputStream out = new FileOutputStream(destFile)) {
                            ByteStreams.copy(in, out);
                        }
                    } finally {
                        in.close();
                    }
                    return true;
                }
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Error copying default " + dest, ex);
            }
        }
        return false;
    }
    
    public File getDataFile(String name) {
        return new File(getDataFolder(), name);
    }

    public Reader openFile(File file) throws IOException {
        return new InputStreamReader(new BufferedInputStream(new FileInputStream(file)), Charsets.UTF_8);
    }

    public Writer writeFile(File file) throws IOException {
        return new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(file)), Charsets.UTF_8);
    }

    public Reader openDataFile(String file) throws IOException {
        return openFile(getDataFile(file));
    }

    public Writer writeDataFile(String file) throws IOException {
        return writeFile(getDataFile(file));
    }

    public Reader openResource(String resource) throws IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resource);
        if (in == null) { 
            throw new FileNotFoundException(resource);
        }
        return new InputStreamReader(new BufferedInputStream(in), Charsets.UTF_8);
    }

    private void loadConfig() {
        config = new Config();
        try {
            Yaml configLoader = new Yaml(new CustomClassLoaderConstructor(Config.class, getClass().getClassLoader()));
            configLoader.setBeanAccess(BeanAccess.FIELD);
            try (Reader in = openDataFile(CONFIG)) { 
                config = configLoader.loadAs(in, Config.class);
            }
        } catch (IOException | YAMLException ex) {
            getLogger().log(Level.SEVERE, "Error loading config.yml", ex);
            getLogger().severe("Your config.yml has fatal errors, using defaults.");
        }
    }

    private void loadMessages() {
        try {
            messages.clear();
            messages.load(openDataFile(MESSAGES));
        } catch (IOException ex) {
            getLogger().log(Level.WARNING, "Could not load locale.properties", ex);
            try {
                messages.clear();
                messages.load(openResource(MESSAGES));
            } catch (IOException ex1) {
                getLogger().log(Level.SEVERE, "Could not load default locale.properties", ex);
            }
        }
    }

    public String getMessage(String msg, Object... args) {
        String result = messages.getProperty(msg);
        if (result == null) {
            return String.format("Unknown message %s with arguments %s", msg, Arrays.toString(args));
        }
        return String.format(result, args);
    }

    @Override
    public void onEnable() {
        copyDefault(CONFIG, CONFIG);
        copyDefault(MESSAGES, MESSAGES);
        permissionService = getServer().getServicesManager().getRegistration(Permission.class).getProvider();
        getLogger().log(Level.INFO, "Loaded permission interface: {0}", permissionService.getClass().getName());
        loadConfig();
        loadMessages();
        getServer().getPluginManager().registerEvents(this, this);
        long now = System.currentTimeMillis();
        String server = getServer().getServerName();
        // Delete any dangling sessions
        try {
            getDB().deleteSessionsSync(server);
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Error deleting sessions", ex);
        }
        getServer().getOnlinePlayers().stream().forEach((p) -> {
            Location l = p.getLocation();
            try {
                getDB().playerLoginSync(server, p.getName(), p.getUniqueId(), p.getAddress().getHostString(), now, l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
            } catch (SQLException ex) {
                getLogger().log(Level.WARNING, "Error performing login", ex);
            }
        });
    }

    @Override
    public void onDisable() {
        long now = System.currentTimeMillis();
        String server = getServer().getServerName();
        getServer().getOnlinePlayers().stream().forEach((p) -> {
            Location l = p.getLocation();
            try {
                getDB().playerLogoutSync(server, p.getName(), p.getUniqueId(), p.getAddress().getHostString(), now, l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
            } catch (SQLException ex) {
                getLogger().log(Level.WARNING, "Error performing login", ex);
            }
        });
        // Delete any dangling sessions
        try {
            getDB().deleteSessionsSync(server);
        } catch (SQLException ex) {
            getLogger().log(Level.WARNING, "Error deleting sessions", ex);
        }
        database.close();
    }

    public void reload() {
        onDisable();
        onEnable();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // TODO
        return null;
    }

    private final Map<String, AbstractCommand> commands = new HashMap<>();
    {
        commands.put("seen", new SeenCommand(this));
        commands.put("audit", new AuditCommand(this));
        commands.put("check", new CheckCommand(this));
        commands.put("ducklog", new DuckLogCommand(this));
        commands.put("timetop", new TimeTopCommand(this));
        commands.put("friend", new FriendCommand(this));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        AbstractCommand target = commands.get(command.getName());
        if (target != null) {
            try {
                target.execute(sender, label, args);
                return true;
            } catch (UsageException ex) {
                return false;
            } catch (InvalidSenderException ex) {
                sender.sendMessage(getMessage("invalid_sender"));
                return true;
            } catch (PermissionException ex) {
                sender.sendMessage(getMessage("permission", ex.getPermission()));
                return true;
            } catch (CommandException ex) {
                sender.sendMessage(getMessage("error"));
                getLogger().log(Level.WARNING, "An error occured executing a command", ex);
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent e) {
        long now = System.currentTimeMillis();
        Player player = e.getPlayer();
        Location l = player.getLocation();
        getDB().playerLogin(getServer().getServerName(), player.getName(), player.getUniqueId(), player.getAddress().getHostString(), now, l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(),
                (result) -> {
                    performAutorank(player, result.getLoginTime());
                }, (ex) -> {
                    getLogger().log(Level.WARNING, "Error during login", ex);
                });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent e) {
        long now = System.currentTimeMillis();
        Player player = e.getPlayer();
        Location l = player.getLocation();
        getDB().playerLogout(getServer().getServerName(), player.getName(), player.getUniqueId(), player.getAddress().getHostString(), now, l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ(),
                (result) -> {
                    performAutorank(e.getPlayer(),result.getLoginTime());
                }, (ex) -> {
                    getLogger().log(Level.WARNING, "Error during logout", ex);
                });
    }

    public static class AutorankResult {
        public static enum Status {
            SUCCESS,
            WAIT;
        }

        private final Status status;
        private final long wait;

        public AutorankResult() {
            status = Status.SUCCESS;
            wait = 0L;
        }

        public AutorankResult(long wait) {
            status = Status.WAIT;
            this.wait = wait;
        }

        public Status getStatus() {
            return status;
        }

        public long getWait() {
            return wait;
        }
    }

    public Map<String, AutorankResult> performAutorank(UUID uuid, Map<String, Long> loginTime) {
        Player player = getServer().getPlayer(uuid);
        if (player != null) {
            return performAutorank(player, loginTime);
        }
        OfflinePlayer offlinePlayer = getServer().getOfflinePlayer(uuid);
        if (offlinePlayer != null) {
            return performAutorank(offlinePlayer, loginTime);
        }
        return Collections.emptyMap();
    }

    public Map<String, AutorankResult> performAutorank(OfflinePlayer player, Map<String, Long> loginTime) {
        Map<String, AutorankResult> result = new HashMap<>();
        long totalTime = loginTime.values().stream().reduce(0L, (a, b) -> a + b);
        for (Map.Entry<String,AutoPromote> e : config.getAutopromote().entrySet()) {
            AutoPromote ap = e.getValue();
            boolean hasAll = ap.getHasGroup().stream().map((group) -> permissionService.playerInGroup(null, player, group)).reduce(true, (a,b) -> a && b);
            boolean missingAll = ap.getMissingGroup().stream().map((group) -> !permissionService.playerInGroup(null, player, group)).reduce(true, (a,b) -> a && b);
            if (hasAll && missingAll) {
                if (totalTime >= ap.getAfter()*1000) {
                    getLogger().log(Level.INFO, "Groups to remove: {0}", ap.getRemoveGroup());
                    getLogger().log(Level.INFO, "Groups to add: {0}", ap.getAddGroup());
                    for (String group : ap.getRemoveGroup()) {
                        permissionService.playerRemoveGroup(null, player, group);
                    }
                    for (String group : ap.getAddGroup()) {
                        permissionService.playerAddGroup(null, player, group); 
                    }
                    result.put(e.getKey(), new AutorankResult());
                } else {
                    result.put(e.getKey(), new AutorankResult(ap.getAfter() * 1000 - totalTime));
                }
            }
        }
        return result;
    }
}