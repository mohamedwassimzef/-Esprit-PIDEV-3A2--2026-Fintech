package tn.esprit.dao;
import tn.esprit.entities.ContractRequest;
import tn.esprit.enums.RequestStatus;
import tn.esprit.utils.MyDB;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
/**
 * Data Access Object for ContractRequest entity.
 * Implements CRUD operations for ContractRequest in the database.
 */
public class ContractRequestDAO implements CrudInterface<ContractRequest> {
    private Connection connection;
    public ContractRequestDAO() {
        this.connection = MyDB.getInstance().getConx();
    }
    @Override
    public boolean create(ContractRequest entity) {
        String query = "INSERT INTO contract_request (user_id, asset_id, package_id, calculated_premium, status) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setInt(2, entity.getAssetId());
            pstmt.setInt(3, entity.getPackageId());
            pstmt.setDouble(4, entity.getCalculatedPremium());
            pstmt.setString(5, entity.getStatus() != null ? entity.getStatus().toDbValue() : RequestStatus.PENDING.toDbValue());
            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating ContractRequest: " + e.getMessage());
            return false;
        }
    }
    @Override
    public ContractRequest read(int id) {
        String query = "SELECT * FROM contract_request WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading ContractRequest: " + e.getMessage());
        }
        return null;
    }
    @Override
    public List<ContractRequest> readAll() {
        List<ContractRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM contract_request";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                requests.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all ContractRequests: " + e.getMessage());
        }
        return requests;
    }
    @Override
    public boolean update(ContractRequest entity) {
        String query = "UPDATE contract_request SET user_id = ?, asset_id = ?, package_id = ?, " +
                "calculated_premium = ?, status = ?, boldsign_document_id = ? WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setInt(2, entity.getAssetId());
            pstmt.setInt(3, entity.getPackageId());
            pstmt.setDouble(4, entity.getCalculatedPremium());
            pstmt.setString(5, entity.getStatus() != null ? entity.getStatus().toDbValue() : RequestStatus.PENDING.toDbValue());
            pstmt.setString(6, entity.getBoldSignDocumentId());
            pstmt.setInt(7, entity.getId());
            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating ContractRequest: " + e.getMessage());
            return false;
        }
    }
    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM contract_request WHERE id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting ContractRequest: " + e.getMessage());
            return false;
        }
    }
    /**
     * Find contract requests by user ID.
     * @param userId the user ID to filter by
     * @return list of requests belonging to the user
     */
    public List<ContractRequest> findByUserId(int userId) {
        List<ContractRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM contract_request WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding ContractRequests by user ID: " + e.getMessage());
        }
        return requests;
    }
    /**
     * Find contract requests by status.
     * @param status the request status to filter by
     * @return list of requests with the given status
     */
    public List<ContractRequest> findByStatus(RequestStatus status) {
        List<ContractRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM contract_request WHERE status = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, status.toDbValue());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding ContractRequests by status: " + e.getMessage());
        }
        return requests;
    }
    /**
     * Find contract requests by package ID.
     * @param packageId the package ID to filter by
     * @return list of requests for the given package
     */
    public List<ContractRequest> findByPackageId(int packageId) {
        List<ContractRequest> requests = new ArrayList<>();
        String query = "SELECT * FROM contract_request WHERE package_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, packageId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                requests.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error finding ContractRequests by package ID: " + e.getMessage());
        }
        return requests;
    }
    /**
     * Find a contract request by its BoldSign document ID.
     * Used by the webhook to resolve which request was signed.
     * Opens a fresh connection to be thread-safe (webhook runs on Spring thread).
     */
    public ContractRequest findByBoldSignDocumentId(String documentId) {
        String query = "SELECT * FROM contract_request WHERE boldsign_document_id = ?";
        try (Connection conn = openFreshConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, documentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToEntity(rs);
        } catch (SQLException e) {
            System.out.println("Error finding ContractRequest by BoldSign document ID: " + e.getMessage());
        }
        return null;
    }

    /**
     * Updates only the status column for the given contract request ID.
     * Used by the webhook — opens a fresh connection to be thread-safe.
     *
     * @param requestId the contract_request.id to update
     * @param status    the new status value
     * @return true if the row was updated
     */
    public boolean updateStatus(int requestId, RequestStatus status) {
        String query = "UPDATE contract_request SET status = ? WHERE id = ?";
        try (Connection conn = openFreshConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, status.toDbValue());
            pstmt.setInt(2, requestId);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Error updating ContractRequest status: " + e.getMessage());
            return false;
        }
    }

    /** Opens a brand-new JDBC connection using the same credentials as MyDB. */
    private Connection openFreshConnection() throws SQLException {
        MyDB db = MyDB.getInstance();
        return java.sql.DriverManager.getConnection(
                db.getUrl(), db.getUsername(), db.getPassword()
        );
    }

    private ContractRequest mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        int assetId = rs.getInt("asset_id");
        int packageId = rs.getInt("package_id");
        double calculatedPremium = rs.getDouble("calculated_premium");
        String statusStr = rs.getString("status");
        RequestStatus status = statusStr != null ? RequestStatus.fromString(statusStr) : RequestStatus.PENDING;
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime createdAt = ts != null ? ts.toLocalDateTime() : null;
        ContractRequest req = new ContractRequest(id, userId, assetId, packageId, calculatedPremium, status, createdAt);
        req.setBoldSignDocumentId(rs.getString("boldsign_document_id"));
        return req;
    }
}
