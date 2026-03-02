package tn.esprit.dao;
import tn.esprit.entities.InsurancePackage;
import tn.esprit.utils.MyDB;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * Data Access Object for InsurancePackage entity.
 * Implements CRUD operations for InsurancePackage in the database.
 */
public class InsurancePackageDAO implements CrudInterface<InsurancePackage> {
    private Connection connection;
    public InsurancePackageDAO() {
        this.connection = MyDB.getInstance().getConx();
    }
    @Override
    public boolean create(InsurancePackage entity) {
        String query = "INSERT INTO insurance_package (name, asset_type, description, coverage_details, " +
                "base_price, risk_multiplier, duration_months, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getAssetType());
            pstmt.setString(3, entity.getDescription());
            pstmt.setString(4, entity.getCoverageDetails());
            pstmt.setDouble(5, entity.getBasePrice());
            pstmt.setDouble(6, entity.getRiskMultiplier());
            pstmt.setInt(7, entity.getDurationMonths());
            pstmt.setBoolean(8, entity.isActive());
            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating InsurancePackage: " + e.getMessage());
            return false;
        }
    }
    @Override
    public InsurancePackage read(int id) {
        String query = "SELECT * FROM insurance_package WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading InsurancePackage: " + e.getMessage());
        }
        return null;
    }
    @Override
    public List<InsurancePackage> readAll() {
        List<InsurancePackage> packages = new ArrayList<>();
        String query = "SELECT * FROM insurance_package";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                packages.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all InsurancePackages: " + e.getMessage());
        }
        return packages;
    }
    @Override
    public boolean update(InsurancePackage entity) {
        String query = "UPDATE insurance_package SET name = ?, asset_type = ?, description = ?, " +
                "coverage_details = ?, base_price = ?, risk_multiplier = ?, duration_months = ?, " +
                "is_active = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getAssetType());
            pstmt.setString(3, entity.getDescription());
            pstmt.setString(4, entity.getCoverageDetails());
            pstmt.setDouble(5, entity.getBasePrice());
            pstmt.setDouble(6, entity.getRiskMultiplier());
            pstmt.setInt(7, entity.getDurationMonths());
            pstmt.setBoolean(8, entity.isActive());
            pstmt.setInt(9, entity.getId());
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating InsurancePackage: " + e.getMessage());
            return false;
        }
    }
    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM insurance_package WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting InsurancePackage: " + e.getMessage());
            return false;
        }
    }
    /**
     * Find all active insurance packages.
     * @return list of active packages
     */
    public List<InsurancePackage> findActive() {
        List<InsurancePackage> packages = new ArrayList<>();
        String query = "SELECT * FROM insurance_package WHERE is_active = TRUE";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                packages.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding active InsurancePackages: " + e.getMessage());
        }
        return packages;
    }
    /**
     * Find insurance packages by asset type (car, home, land).
     * @param assetType the asset type to filter by
     * @return list of packages matching the asset type
     */
    public List<InsurancePackage> findByAssetType(String assetType) {
        List<InsurancePackage> packages = new ArrayList<>();
        String query = "SELECT * FROM insurance_package WHERE asset_type = ? AND is_active = TRUE";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, assetType);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                packages.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding InsurancePackages by asset type: " + e.getMessage());
        }
        return packages;
    }
    private InsurancePackage mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String assetType = rs.getString("asset_type");
        String description = rs.getString("description");
        String coverageDetails = rs.getString("coverage_details");
        double basePrice = rs.getDouble("base_price");
        double riskMultiplier = rs.getDouble("risk_multiplier");
        int durationMonths = rs.getInt("duration_months");
        boolean isActive = rs.getBoolean("is_active");
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        return new InsurancePackage(id, name, assetType, description, coverageDetails,
                basePrice, riskMultiplier, durationMonths, isActive, createdAt);
    }
}
