/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.cyberiantiger.minecraft.log.config.Config;

/**
 *
 * @author antony
 */
public class Database {
    private static final int DB_VERSION = 1;
    private static final String GET_VERSION_SQL = "SELECT id FROM version";
    private static final String UPDATE_PLAYERNAME_SQL = "INSERT INTO playernames (uuid, name, last_seen) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE name = ?, last_seen = ?";
    private static final String LOGIN_SQL = "INSERT INTO login_events (uuid, server, time, ip, type) VALUES (?, ?, ?, ?, 'LOGIN')";
    private static final String LOGOUT_SQL = "INSERT INTO login_events (uuid, server, time, ip, type) VALUES (?, ?, ?, ?, 'LOGOUT')";
    private static final String UPDATE_LOGIN_TIME_SQL = "INSERT INTO login_time (uuid, server, time) values (?, ?, ?) ON DUPLICATE KEY UPDATE time = time + ?";
    private static final String QUERY_LOGIN_TIME_SQL = "SELECT server, time FROM login_time WHERE uuid = ? ORDER BY server";
    private static final String QUERY_CURRENT_SESSION = "SELECT server, start FROM login_sessions WHERE uuid = ? ORDER BY server";
    private static final String UPDATE_SESSION_SQL = "INSERT INTO login_sessions (server, uuid, start) values (?, ?, ?) ON DUPLICATE KEY UPDATE start = ?";

    public static class PlayerLoginResult {
        private final Map<String, Long> loginTime;
        public PlayerLoginResult(Map<String, Long> loginTime) {
            this.loginTime = loginTime;
        }

        public Map<String, Long> getLoginTime() {
            return loginTime;
        }
    }

    public static class PlayerLogoutResult {
        private final Map<String, Long> loginTime;
        public PlayerLogoutResult(Map<String, Long> loginTime) {
            this.loginTime = loginTime;
        }

        public Map<String, Long> getLoginTime() {
            return loginTime;
        }
    }

    private final Main main;
    private Connection conn;

    public Database(Main main) {
        this.main = main;
    }

    private synchronized Connection getConnection() throws SQLException {
        if (conn != null) {
            if (conn.isValid(1000)) {
                return conn;
            } else {
                conn.close();
                conn = null;
            }
        }
        Config config = main.getRealConfig();
        conn = DriverManager.getConnection(config.getDbUrl(), config.getDbUser(), config.getDbPassword());
        conn.setAutoCommit(true); // Who needs transactions anyway.
        try (
                Statement testVersion = conn.createStatement();
                ResultSet rs = testVersion.executeQuery(GET_VERSION_SQL)
                ) {
            if (rs.next()) {
                int version = rs.getInt(1);
                if (version != DB_VERSION) throw new SQLException("Database version does not match expected version");
            } else {
                throw new SQLException("Database version field not set");
            }
        } catch (SQLException ex) {
            main.getLogger().log(Level.INFO, "Error initialising database: {0}", ex.getMessage());
            try (
                    Statement batch = conn.createStatement();
                    Reader in = main.openResource("schema.sql");
                    ) {
                BufferedReader inn = new BufferedReader(in);
                StringBuilder query = new StringBuilder();
                String line;
                while ( (line = inn.readLine()) != null ) {
                    if (line.endsWith(";")) {
                        query.append(line.substring(0, line.length() - 1));
                        batch.addBatch(query.toString());
                        query.setLength(0);
                    } else {
                        query.append(line);
                        query.append(System.lineSeparator());
                    }
                }
                batch.executeBatch();
            } catch (IOException ex2) {
                throw new SQLException("Fatal error initialising database", ex2);
            }
        }
        return conn;
    }

    public synchronized void close() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ex) {
                main.getLogger().log(Level.SEVERE, "Error closing database", ex);
            }
            conn = null;
        }
    }

    synchronized void deleteSessionsSync(String serverName) throws SQLException {
        Connection conn = getConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("DELETE FROM login_sessions WHERE server = ?");
                )
        {
            ps.setString(1, serverName);
            ps.executeUpdate();
        }
    }

    synchronized PlayerLoginResult playerLoginSync(String serverName, String name, UUID uuid, String ip, long time) throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        try (
                PreparedStatement login = conn.prepareStatement(LOGIN_SQL);
                PreparedStatement updateNames = conn.prepareStatement(UPDATE_PLAYERNAME_SQL);
                PreparedStatement updateSession = conn.prepareStatement(UPDATE_SESSION_SQL);
                )
        {
            conn.setAutoCommit(false);
            login.setString(1, uuid.toString());
            login.setString(2, serverName);
            login.setLong(3, time);
            login.setString(4, ip);
            updateNames.setString(1, uuid.toString());
            updateNames.setString(2, name);
            updateNames.setLong(3, time);
            updateNames.setString(4, name);
            updateNames.setLong(5, time);
            updateSession.setString(1, serverName);
            updateSession.setString(2, uuid.toString());
            updateSession.setLong(3, time);
            updateSession.setLong(4, time);
            login.executeUpdate();
            updateNames.executeUpdate();
            updateSession.executeUpdate();
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
        return new PlayerLoginResult(getLoginTime(uuid, time));
    }

    synchronized PlayerLogoutResult playerLogoutSync(String serverName, String name, UUID uuid, String ip, long time) throws SQLException {
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        try (
                PreparedStatement getSessionStartTime = conn.prepareStatement("SELECT start FROM login_sessions WHERE server = ? AND uuid = ?");
                )
        {
            getSessionStartTime.setString(1, serverName);
            getSessionStartTime.setString(2, uuid.toString());
            try (ResultSet rs = getSessionStartTime.executeQuery()) {
                if (rs.next()) {
                    long start = rs.getLong(1);
                    try (
                            PreparedStatement deleteSession = conn.prepareStatement("DELETE FROM login_sessions WHERE server = ? AND uuid = ?");
                            PreparedStatement updateTimes = conn.prepareStatement(UPDATE_LOGIN_TIME_SQL);
                            ) 
                    {
                        deleteSession.setString(1, serverName);
                        deleteSession.setString(2, uuid.toString());
                        updateTimes.setString(1, uuid.toString());
                        updateTimes.setString(2, serverName);
                        updateTimes.setLong(3, time - start);
                        updateTimes.setLong(4, time - start);
                        deleteSession.executeUpdate();
                        updateTimes.executeUpdate();
                    }
                }
            }
        }
        try (
                PreparedStatement logout = conn.prepareStatement(LOGOUT_SQL);
                PreparedStatement updateNames = conn.prepareStatement(UPDATE_PLAYERNAME_SQL);
                )
        {
            logout.setString(1, uuid.toString());
            logout.setString(2, serverName);
            logout.setLong(3, time);
            logout.setString(4, ip);
            updateNames.setString(1, uuid.toString());
            updateNames.setString(2, name);
            updateNames.setLong(3, time);
            updateNames.setString(4, name);
            updateNames.setLong(5, time);
            logout.executeUpdate();
            updateNames.executeUpdate();
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
        return new PlayerLogoutResult(getLoginTime(uuid, time));
    }

    private synchronized Map<String, Long> getLoginTime(UUID uuid, long time) throws SQLException {
        Map<String, Long> result = new TreeMap<>();
        Connection conn = getConnection();
        conn.setAutoCommit(false);
        try (
                PreparedStatement loginTime = conn.prepareStatement(QUERY_LOGIN_TIME_SQL);
                PreparedStatement session = conn.prepareStatement(QUERY_CURRENT_SESSION);
                )
        {
            session.setString(1, uuid.toString());
            loginTime.setString(1, uuid.toString());
            try (
                    ResultSet rs = loginTime.executeQuery();
                    )
            {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getLong(2));
                }
            }
            try (
                    ResultSet rs = session.executeQuery();
                    )
            {
                while (rs.next()) {
                    String server = rs.getString(1);
                    long start = rs.getLong(2);
                    if (result.containsKey(server)) {
                        result.put(server, result.get(server) + time - start);
                    } else {
                        result.put(server, time - start);
                    }
                }
            }
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }
        return result;
    }

    private synchronized List<UUID> getUUIDsByName(String name) throws SQLException {
        Connection conn = getConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM playernames WHERE name = ?");
                )
        {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(UUID.fromString(rs.getString(1)));
                }
                return result;
            }
        }
    }

    private synchronized List<UUID> getUUIDsByIP(String ip) throws SQLException {
        Connection conn = getConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT uuid FROM login_events WHERE ip = ? GROUP BY uuid");
                )
        {
            ps.setString(1, ip);
            try (ResultSet rs = ps.executeQuery()) {
                List<UUID> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(UUID.fromString(rs.getString(1)));
                }
                return result;
            }
        }
    }

    private synchronized Map<String, LoginEvent> getNamesByUUID(UUID uuid) throws SQLException {
        Connection conn = getConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT p.name, l.server, l.time, l.ip, l.type FROM playernames p, login_events l WHERE p.uuid = l.uuid and p.last_seen = l.time and p.uuid = ? ORDER BY p.last_seen DESC");
                )
        {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                Map<String,LoginEvent> result = new LinkedHashMap<>();
                while (rs.next()) {
                    result.put(rs.getString(1), new LoginEvent(LoginEvent.Type.valueOf(rs.getString(5).toUpperCase()), rs.getString(2), rs.getString(4), rs.getLong(3)));
                }
                return result;
            }
        }
    }

    private synchronized List<LoginEvent> getLoginEventsByUUID(UUID uuid, int offset, int limit) throws SQLException {
        Connection conn = getConnection();
        try (
                PreparedStatement ps = conn.prepareStatement("SELECT server, time, ip, type FROM login_events WHERE uuid = ? ORDER BY time DESC LIMIT ? OFFSET ?");
                )
        {
            ps.setString(1, uuid.toString());
            ps.setInt(2, limit);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<LoginEvent> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new LoginEvent(LoginEvent.Type.valueOf(rs.getString(4).toUpperCase()), rs.getString(1), rs.getString(3), rs.getLong(2)));
                }
                return result;
            }
        }
    }

    private synchronized Map<UUID, Map<String, LoginEvent>> seenNameSync(String name) throws SQLException {
        Connection conn = getConnection();
        List<UUID> accounts = getUUIDsByName(name);
        Map<UUID, Map<String, LoginEvent>> alts = new HashMap<>();
        for (UUID uuid : accounts) {
            alts.put(uuid, getNamesByUUID(uuid));
        }
        return alts;
    }

    private synchronized Map<UUID, Map<String, LoginEvent>> seenIpSync(String ip) throws SQLException {
        Connection conn = getConnection();
        List<UUID> accounts = getUUIDsByIP(ip);
        Map<UUID, Map<String, LoginEvent>> alts = new HashMap<>();
        for (UUID uuid : accounts) {
            alts.put(uuid, getNamesByUUID(uuid));
        }
        return alts;
    }

    private synchronized Map<UUID, List<LoginEvent>> auditSync(String name, int offset, int limit) throws SQLException {
        Connection conn = getConnection();
        Map<UUID,List<LoginEvent>> result = new HashMap<>();
        List<UUID> uuids = getUUIDsByName(name);
        for (UUID uuid : uuids) {
            result.put(uuid, getLoginEventsByUUID(uuid, offset, limit));
        }
        return result;
    }

    private synchronized Map<UUID, Map<String, Long>> checkSync(String name) throws SQLException {
        Connection conn = getConnection();
        List<UUID> uuids = getUUIDsByName(name);
        Map<UUID, Map<String,Long>> result = new HashMap<>();
        for (UUID uuid : uuids) {
            result.put(uuid, getLoginTime(uuid, System.currentTimeMillis()));
        }
        return result;
    }

    private synchronized Map<String, Long> checkSync(UUID uuid) throws SQLException {
        return getLoginTime(uuid, System.currentTimeMillis());
    }

    private synchronized Map<String, Long> timetopSync() throws SQLException {
        Connection conn = getConnection();
        Map<String, Long> result = new LinkedHashMap<>();
        try (
                PreparedStatement ps = conn.prepareStatement( "select p.name, p.uuid, sum(l.time) as total from playernames p, login_time l where p.uuid = l.uuid and p.last_seen = (select max(p2.last_seen) from playernames p2 where p2.uuid = p.uuid) group by p.name, p.uuid order by total desc limit 10" );
                )
        {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getLong(3));
                }
            }
        }
        return result;
    }

    private synchronized Map<String, Long> timetopSync(String server) throws SQLException {
        Connection conn = getConnection();
        Map<String, Long> result = new LinkedHashMap<>();
        try (
                PreparedStatement ps = conn.prepareStatement( "select p.name, p.uuid, l.time as total from playernames p, login_time l where p.uuid = l.uuid and p.last_seen = (select max(p2.last_seen) from playernames p2 where p2.uuid = p.uuid) and l.server = ? order by total desc limit 10;" );
                )
        {
            ps.setString(1, server);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString(1), rs.getLong(3));
                }
            }
        }

        return result;
    }

    public void playerLogin(String serverName, String name, UUID uuid, String ip, long time, Consumer<PlayerLoginResult> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                playerLogin(serverName, name, uuid, ip, time, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(playerLoginSync(serverName, name, uuid, ip, time));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void playerLogout(String serverName, String name, UUID uuid, String ip, long time, Consumer<PlayerLogoutResult> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                playerLogout(serverName, name, uuid, ip, time, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(playerLogoutSync(serverName, name, uuid, ip, time));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void seenName(String name, Consumer<Map<UUID, Map<String, LoginEvent>>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                seenName(name, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(seenNameSync(name));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void seenIp(String ip, Consumer<Map<UUID, Map<String, LoginEvent>>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                seenIp(ip, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(seenIpSync(ip));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void audit(String name, int offset, int limit, Consumer<Map<UUID, List<LoginEvent>>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                audit(name, offset, limit, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(auditSync(name, offset, limit));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void check(String name, Consumer<Map<UUID, Map<String,Long>>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                check(name, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(checkSync(name));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void check(UUID uuid, Consumer<Map<UUID, Map<String, Long>>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                check(uuid, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(Collections.singletonMap(uuid, checkSync(uuid)));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void timetop(Consumer<Map<String, Long>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                timetop(new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(timetopSync());
        } catch (SQLException e) {
            ex.accept(e);
        }
    }

    public void timetop(String server, Consumer<Map<String, Long>> result, Consumer<Exception> ex) {
        if (Bukkit.isPrimaryThread()) {
            main.getServer().getScheduler().runTaskAsynchronously(main, () -> {
                timetop(server, new CallSyncConsumer<>(main, result), new CallSyncConsumer<>(main, ex));
            });
            return;
        }
        try {
            result.accept(timetopSync(server));
        } catch (SQLException e) {
            ex.accept(e);
        }
    }
}