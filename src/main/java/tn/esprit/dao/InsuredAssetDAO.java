package tn.esprit.dao;

import tn.esprit.entities.InsuredAsset;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for InsuredAsset entity.
 * Implements CRUD operations for InsuredAsset in the database.
 */
public class InsuredAssetDAO implements CrudInterface<InsuredAsset> {

    private Connection connection;

    public InsuredAssetDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(InsuredAsset entity) {
        String query = "INSERT INTO insured_asset (name, type, value, description, created_at, location, user_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getType());
            pstmt.setDouble(3, entity.getValue());
            pstmt.setString(4, entity.getDescription());
            pstmt.setTimestamp(5, Timestamp.valueOf(entity.getCreatedAt() != null ?
                    entity.getCreatedAt() : LocalDateTime.now()));
            pstmt.setString(6, entity.getLocation());
            pstmt.setInt(7, entity.getUserId());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating InsuredAsset: " + e.getMessage());
            return false;
        }
    }

    @Override
    public InsuredAsset read(int id) {
        String query = "SELECT * FROM insured_asset WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading InsuredAsset: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<InsuredAsset> readAll() {
        List<InsuredAsset> assets = new ArrayList<>();
        String query = "SELECT * FROM insured_asset";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                assets.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all InsuredAssets: " + e.getMessage());
        }
        return assets;
    }

    @Override
    public boolean update(InsuredAsset entity) {
        String query = "UPDATE insured_asset SET name = ?, type = ?, value = ?, " +
                "description = ?, created_at = ?, location = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getType());
            pstmt.setDouble(3, entity.getValue());
            pstmt.setString(4, entity.getDescription());
            pstmt.setTimestamp(5, Timestamp.valueOf(entity.getCreatedAt() != null ?
                    entity.getCreatedAt() : LocalDateTime.now()));
            pstmt.setString(6, entity.getLocation());
            pstmt.setInt(7, entity.getUserId());
            pstmt.setInt(8, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating InsuredAsset: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM insured_asset WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting InsuredAsset: " + e.getMessage());
            return false;
        }
    }

    /**
     * Maps a ResultSet row to an InsuredAsset entity.
     *
     * @param rs The ResultSet to map
     * @return The mapped InsuredAsset entity
     * @throws SQLException if there's an error accessing the ResultSet
     */
    private InsuredAsset mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String type = rs.getString("type");
        double value = rs.getDouble("value");
        String description = rs.getString("description");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        String location = rs.getString("location");
        int userId = rs.getInt("user_id");

        return new InsuredAsset(id, name, type, value, description, createdAt, location, userId);
    }
}
