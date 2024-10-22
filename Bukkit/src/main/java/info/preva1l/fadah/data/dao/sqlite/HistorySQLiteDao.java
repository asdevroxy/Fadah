package info.preva1l.fadah.data.dao.sqlite;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zaxxer.hikari.HikariDataSource;
import info.preva1l.fadah.Fadah;
import info.preva1l.fadah.data.dao.Dao;
import info.preva1l.fadah.data.gson.ConfigurationSerializableAdapter;
import info.preva1l.fadah.records.HistoricItem;
import info.preva1l.fadah.records.History;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

@RequiredArgsConstructor
public class HistorySQLiteDao implements Dao<History> {
    private final HikariDataSource dataSource;
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(ConfigurationSerializable.class, new ConfigurationSerializableAdapter())
            .serializeNulls().create();
    private static final Type HISTORY_LIST_TYPE = new TypeToken<ArrayList<HistoricItem>>(){}.getType();

    /**
     * Get an object from the database by its id.
     *
     * @param id the id of the object to get.
     * @return an optional containing the object if it exists, or an empty optional if it does not.
     */
    @Override
    public Optional<History> get(UUID id) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                        SELECT `items`
                        FROM `historyV2`
                        WHERE `playerUUID`=?;""")) {
                statement.setString(1, id.toString());
                final ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    List<HistoricItem> items;
                    try {
                        items = GSON.fromJson(Arrays.toString(Base64.getDecoder().decode(resultSet.getString("items"))), HISTORY_LIST_TYPE);
                    } catch (IllegalArgumentException e) {
                        items = GSON.fromJson(resultSet.getString("items"), HISTORY_LIST_TYPE);
                    }
                    return Optional.of(new History(id, items));
                }
            }
        } catch (SQLException e) {
            Fadah.getConsole().log(Level.SEVERE, "Failed to get item from history!", e);
        }
        return Optional.empty();
    }

    /**
     * Get all objects of type T from the database.
     *
     * @return a list of all objects of type T in the database.
     */
    @Override
    public List<History> getAll() {
        throw new NotImplementedException();
    }

    /**
     * Save an object of type T to the database.
     *
     * @param history the object to save.
     */
    @Override
    public void save(History history) {
        try (Connection connection = getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO `historyV2`
                    (`playerUUID`,`items`)
                    VALUES (?,?)
                    ON CONFLICT(`playerUUID`) DO UPDATE SET
                        `items` = excluded.`items`;""")) {
                statement.setString(1, history.owner().toString());
                statement.setString(2, Base64.getEncoder().encodeToString(GSON.toJson(history.collectableItems(), HISTORY_LIST_TYPE).getBytes()));
                statement.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            Fadah.getConsole().log(Level.SEVERE, "Failed to add item to history!", e);
        }
    }

    /**
     * Update an object of type T in the database.
     *
     * @param history the object to update.
     * @param params          the parameters to update the object with.
     */
    @Override
    public void update(History history, String[] params) {
        throw new NotImplementedException();
    }

    /**
     * Delete an object of type T from the database.
     *
     * @param collectableItem the object to delete.
     */
    @Override
    public void delete(History collectableItem) {
        throw new NotImplementedException();
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}