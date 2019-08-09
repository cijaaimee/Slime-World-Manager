package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlLoader implements SlimeLoader {

    private static final String CREATE_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `worlds` (`id` INT NOT NULL AUTO_INCREMENT, " +
            "`name` VARCHAR(256) UNIQUE, `world` MEDIUMBLOB, `locked` TINYINT(1), PRIMARY KEY(id));";
    private static final String SELECT_WORLD_QUERY = "SELECT `world`, `locked` FROM `worlds` WHERE `name` = ?;";
    private static final String UPDATE_WORLD_QUERY = "INSERT INTO `worlds` (`name`, `world`, `locked`) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE `world` = ?;";
    private static final String UPDATE_LOCK_QUERY = "UPDATE `worlds` SET `locked` = ? WHERE `name` = ?;";
    private static final String DELETE_WORLD_QUERY = "DELETE FROM `worlds` WHERE `name` = ?;";
    private static final String LIST_WORLDS_QUERY = "SELECT `name` FROM `worlds`;";

    private final HikariDataSource source;

    MysqlLoader(ConfigurationSection config) throws SQLException {
        String host = config.getString("host", "127.0.0.1");
        int port = config.getInt("port", 3306);
        String username = config.getString("username", "slimeworldmanager");
        String password = config.getString("password", "");
        String database = config.getString("database", "slimeworldmanager");

        HikariConfig hikariConfig = new HikariConfig();

        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&allowMultiQueries=true");
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        source = new HikariDataSource(hikariConfig);

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(CREATE_TABLE_QUERY)) {
            statement.execute();
        }
    }

    @Override
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        try (Connection con = source.getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            if (!readOnly) {
                if (set.getBoolean("locked")) {
                    throw new WorldInUseException(worldName);
                }

                try (PreparedStatement updateStatement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
                    updateStatement.setBoolean(1, true);
                    updateStatement.setString(2, worldName);
                    updateStatement.executeUpdate();
                }
            }

            return set.getBytes("world");
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            return set.next();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(LIST_WORLDS_QUERY)) {
            ResultSet set = statement.executeQuery();

            while(set.next()) {
                worldList.add(set.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_WORLD_QUERY)) {
            statement.setString(1, worldName);
            statement.setBytes(2, serializedWorld);
            statement.setBytes(3, serializedWorld);
            statement.executeUpdate();

            if (lock) {
                try (PreparedStatement updateStatement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
                    updateStatement.setBoolean(1, true);
                    updateStatement.setString(2, worldName);
                    updateStatement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void unlockWorld(String worldName) throws IOException, UnknownWorldException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
            statement.setBoolean(1, false);
            statement.setString(2, worldName);

            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            return set.getBoolean("locked");
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_WORLD_QUERY)) {
            statement.setString(1, worldName);

            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }
}
