package tn.esprit.dao;

import tn.esprit.entities.InsuredAsset;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for InsuredAsset entity.
 * Implements CRUD operations for InsuredAsset in the database.
 */
public class InsuredAssetDAO implements CrudInterface<InsuredAsset> {

    private final Connection connection;

    public InsuredAssetDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(InsuredAsset e) {
        String sql = "INSERT INTO insured_asset " +
                "(reference, type, description, created_at, location, user_id, declared_value, approved_value, manufacture_date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getReference());
            ps.setString(2, e.getType());
            ps.setString(3, e.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now()));
            ps.setString(5, e.getLocation());
            ps.setInt(6, e.getUserId());
            ps.setBigDecimal(7, e.getDeclaredValue());
            ps.setBigDecimal(8, e.getApprovedValue());
            ps.setDate(9, e.getManufactureDate() != null ? Date.valueOf(e.getManufactureDate()) : null);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Error creating InsuredAsset: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public InsuredAsset read(int id) {
        String sql = "SELECT * FROM insured_asset WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException ex) {
            System.out.println("Error reading InsuredAsset: " + ex.getMessage());
        }
        return null;
    }

    @Override
    public List<InsuredAsset> readAll() {
        List<InsuredAsset> list = new ArrayList<>();
        String sql = "SELECT * FROM insured_asset";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException ex) {
            System.out.println("Error reading all InsuredAssets: " + ex.getMessage());
        }
        return list;
    }

    @Override
    public boolean update(InsuredAsset e) {
        String sql = "UPDATE insured_asset SET " +
                "reference=?, type=?, description=?, created_at=?, location=?, " +
                "user_id=?, declared_value=?, approved_value=?, manufacture_date=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, e.getReference());
            ps.setString(2, e.getType());
            ps.setString(3, e.getDescription());
            ps.setTimestamp(4, Timestamp.valueOf(e.getCreatedAt() != null ? e.getCreatedAt() : LocalDateTime.now()));
            ps.setString(5, e.getLocation());
            ps.setInt(6, e.getUserId());
            ps.setBigDecimal(7, e.getDeclaredValue());
            ps.setBigDecimal(8, e.getApprovedValue());
            ps.setDate(9, e.getManufactureDate() != null ? Date.valueOf(e.getManufactureDate()) : null);
            ps.setInt(10, e.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Error updating InsuredAsset: " + ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM insured_asset WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.out.println("Error deleting InsuredAsset: " + ex.getMessage());
            return false;
        }
    }

    /**
     * Find assets belonging to a specific user.
     * @param userId the user ID to filter by
     * @return list of assets owned by the user
     */
    public List<InsuredAsset> findByUserId(int userId) {
        List<InsuredAsset> list = new ArrayList<>();
        String sql = "SELECT * FROM insured_asset WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException ex) {
            System.out.println("Error finding assets by userId: " + ex.getMessage());
        }
        return list;
    }

    /**
     * Looks up the reference string for a given asset ID.
     * Opens a fresh connection — safe to call from any thread (e.g. webhook).
     *
     * @param assetId the insured_asset.id
     * @return the reference string, or null if not found
     */
    public String findReferenceByAssetId(int assetId) {
        String sql = "SELECT reference FROM insured_asset WHERE id = ?";
        try (Connection c = tn.esprit.utils.MyDB.getInstance().openFreshConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, assetId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("reference");
        } catch (SQLException ex) {
            System.out.println("Error finding reference by assetId: " + ex.getMessage());
        }
        return null;
    }

    // ── private helper ────────────────────────────────────────────────────
    private InsuredAsset map(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        Date mfDate  = rs.getDate("manufacture_date");
        return new InsuredAsset(
                rs.getInt("id"),
                rs.getString("reference"),
                rs.getString("type"),
                rs.getString("description"),
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("location"),
                rs.getInt("user_id"),
                rs.getBigDecimal("declared_value"),
                rs.getBigDecimal("approved_value"),
                mfDate != null ? mfDate.toLocalDate() : null
        );
    }
}
