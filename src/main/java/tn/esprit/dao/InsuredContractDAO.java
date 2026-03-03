package tn.esprit.dao;

import tn.esprit.entities.InsuredContract;
import tn.esprit.enums.ContractStatus;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for InsuredContract — maps to the insured_contract schema:
 *   id, asset_ref, boldsign_document_id, status, created_at, signed_at, local_file_path
 */
public class InsuredContractDAO implements CrudInterface<InsuredContract> {

    private final Connection connection;

    public InsuredContractDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    // ── CREATE ───────────────────────────────────────────────────────────────
    @Override
    public boolean create(InsuredContract entity) {
        String sql = "INSERT INTO insured_contract (asset_ref, boldsign_document_id, status, local_file_path) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getAssetRef());
            ps.setString(2, entity.getBoldSignDocumentId());
            ps.setString(3, entity.getStatus() != null
                    ? entity.getStatus().name()
                    : ContractStatus.NOT_SIGNED.name());
            ps.setString(4, entity.getLocalFilePath());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error creating InsuredContract: " + e.getMessage());
            return false;
        }
    }

    // ── READ BY ID ────────────────────────────────────────────────────────────
    @Override
    public InsuredContract read(int id) {
        String sql = "SELECT * FROM insured_contract WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.out.println("Error reading InsuredContract: " + e.getMessage());
        }
        return null;
    }

    // ── READ ALL ──────────────────────────────────────────────────────────────
    @Override
    public List<InsuredContract> readAll() {
        List<InsuredContract> list = new ArrayList<>();
        String sql = "SELECT * FROM insured_contract ORDER BY created_at DESC";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Error reading all InsuredContracts: " + e.getMessage());
        }
        return list;
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────
    @Override
    public boolean update(InsuredContract entity) {
        String sql = "UPDATE insured_contract SET asset_ref=?, boldsign_document_id=?, status=?, " +
                     "signed_at=?, local_file_path=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, entity.getAssetRef());
            ps.setString(2, entity.getBoldSignDocumentId());
            ps.setString(3, entity.getStatus() != null
                    ? entity.getStatus().name()
                    : ContractStatus.NOT_SIGNED.name());
            if (entity.getSignedAt() != null)
                ps.setTimestamp(4, Timestamp.valueOf(entity.getSignedAt()));
            else
                ps.setNull(4, Types.TIMESTAMP);
            ps.setString(5, entity.getLocalFilePath());
            ps.setInt(6, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating InsuredContract: " + e.getMessage());
            return false;
        }
    }

    // ── DELETE ────────────────────────────────────────────────────────────────
    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM insured_contract WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting InsuredContract: " + e.getMessage());
            return false;
        }
    }

    // ── FIND BY ASSET REFERENCE ───────────────────────────────────────────────
    /**
     * Returns all insured contracts whose asset_ref matches the given
     * insured_asset.reference value.
     *
     * @param reference the reference column value from insured_asset
     * @return list of contracts for that asset, ordered by created_at DESC
     */
    public List<InsuredContract> findByReference(String reference) {
        List<InsuredContract> list = new ArrayList<>();
        String sql = "SELECT * FROM insured_contract WHERE asset_ref = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Error finding InsuredContracts by asset reference: " + e.getMessage());
        }
        return list;
    }

    // ── FIND BY BOLDSIGN DOCUMENT ID ──────────────────────────────────────────
    /**
     * Finds a contract by its BoldSign document ID.
     * Used by the webhook to look up which contract was signed/declined.
     */
    public InsuredContract findByDocumentId(String documentId) {
        String sql = "SELECT * FROM insured_contract WHERE boldsign_document_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, documentId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.out.println("Error finding InsuredContract by documentId: " + e.getMessage());
        }
        return null;
    }

    // ── FIND BY STATUS ────────────────────────────────────────────────────────
    public List<InsuredContract> findByStatus(ContractStatus status) {
        List<InsuredContract> list = new ArrayList<>();
        String sql = "SELECT * FROM insured_contract WHERE status = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.out.println("Error finding InsuredContracts by status: " + e.getMessage());
        }
        return list;
    }

    // ── MARK AS SIGNED ────────────────────────────────────────────────────────
    /**
     * Directly sets status = 'SIGNED' and signed_at = NOW() for the given document ID.
     * Called by the webhook when BoldSign reports the document was completed.
     */
    public boolean markAsSigned(String documentId) {
        String sql = "UPDATE insured_contract SET status='SIGNED', signed_at=NOW() " +
                     "WHERE boldsign_document_id = ?";
        try (Connection c = tn.esprit.utils.MyDB.getInstance().openFreshConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, documentId);
            int rows = ps.executeUpdate();
            System.out.println("[InsuredContractDAO] markAsSigned(" + documentId + ") rows=" + rows);
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Error marking InsuredContract as signed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Creates a new InsuredContract record using a fresh connection.
     * Safe to call from any thread (e.g. Spring webhook thread).
     */
    public boolean createFresh(InsuredContract entity) {
        String sql = "INSERT INTO insured_contract (asset_ref, boldsign_document_id, status, local_file_path) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection c = tn.esprit.utils.MyDB.getInstance().openFreshConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, entity.getAssetRef());
            ps.setString(2, entity.getBoldSignDocumentId());
            ps.setString(3, entity.getStatus() != null
                    ? entity.getStatus().name()
                    : ContractStatus.NOT_SIGNED.name());
            ps.setString(4, entity.getLocalFilePath());
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error creating InsuredContract (fresh): " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates the local_file_path for a given boldsign_document_id using a fresh connection.
     */
    public boolean updateFilePath(String documentId, String filePath) {
        String sql = "UPDATE insured_contract SET local_file_path=? WHERE boldsign_document_id=?";
        try (Connection c = tn.esprit.utils.MyDB.getInstance().openFreshConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, filePath);
            ps.setString(2, documentId);
            int rows = ps.executeUpdate();
            System.out.println("[InsuredContractDAO] updateFilePath rows=" + rows);
            return rows > 0;
        } catch (Exception e) {
            System.out.println("Error updating InsuredContract file path: " + e.getMessage());
            return false;
        }
    }

    // ── MAPPER ───────────────────────────────────────────────────────────────
    private InsuredContract map(ResultSet rs) throws SQLException {
        int            id        = rs.getInt("id");
        String         assetRef  = rs.getString("asset_ref");
        String         docId     = rs.getString("boldsign_document_id");
        String         statusStr = rs.getString("status");
        ContractStatus status    = statusStr != null
                ? ContractStatus.valueOf(statusStr.toUpperCase())
                : ContractStatus.NOT_SIGNED;

        Timestamp     createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : null;

        Timestamp     signedTs  = rs.getTimestamp("signed_at");
        LocalDateTime signedAt  = signedTs != null ? signedTs.toLocalDateTime() : null;

        String filePath = rs.getString("local_file_path");

        return new InsuredContract(id, assetRef, docId, status, createdAt, signedAt, filePath);
    }
}
