package tn.esprit.dao;

import tn.esprit.entities.Transaction;
import tn.esprit.enums.Currency;
import tn.esprit.enums.ReferenceType;
import tn.esprit.enums.TransactionStatus;
import tn.esprit.enums.TransactionType;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Transaction entity.
 */
public class TransactionDAO implements CrudInterface<Transaction> {

    private Connection connection;

    public TransactionDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Transaction entity) {
        String query = "INSERT INTO transaction (user_id, amount, type, status, description, " +
                "reference_type, reference_id, currency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setString(3, entity.getType().toString());
            pstmt.setString(4, entity.getStatus() != null ? entity.getStatus().toString() : "PENDING");
            pstmt.setString(5, entity.getDescription());
            pstmt.setString(6, entity.getReferenceType() != null ? entity.getReferenceType().toString() : null);
            pstmt.setObject(7, entity.getReferenceId());
            pstmt.setString(8, entity.getCurrency() != null ? entity.getCurrency().toString() : "TND");

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Transaction: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Transaction read(int id) {
        String query = "SELECT * FROM transaction WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Transaction: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Transaction> readAll() {
        List<Transaction> transactions = new ArrayList<>();
        String query = "SELECT * FROM transaction";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                transactions.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Transactions: " + e.getMessage());
        }
        return transactions;
    }

    @Override
    public boolean update(Transaction entity) {
        String query = "UPDATE transaction SET user_id = ?, amount = ?, type = ?, status = ?, " +
                "description = ?, reference_type = ?, reference_id = ?, currency = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setString(3, entity.getType().toString());
            pstmt.setString(4, entity.getStatus() != null ? entity.getStatus().toString() : "PENDING");
            pstmt.setString(5, entity.getDescription());
            pstmt.setString(6, entity.getReferenceType() != null ? entity.getReferenceType().toString() : null);
            pstmt.setObject(7, entity.getReferenceId());
            pstmt.setString(8, entity.getCurrency() != null ? entity.getCurrency().toString() : "TND");
            pstmt.setInt(9, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Transaction: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM transaction WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Transaction: " + e.getMessage());
            return false;
        }
    }

    private Transaction mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        BigDecimal amount = rs.getBigDecimal("amount");
        TransactionType type = TransactionType.valueOf(rs.getString("type").toUpperCase());
        TransactionStatus status = TransactionStatus.valueOf(rs.getString("status").toUpperCase());
        String description = rs.getString("description");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        String refTypeStr = rs.getString("reference_type");
        ReferenceType referenceType = refTypeStr != null ? ReferenceType.valueOf(refTypeStr.toUpperCase()) : null;

        Integer referenceId = rs.getObject("reference_id") != null ? rs.getInt("reference_id") : null;

        String currencyStr = rs.getString("currency");
        Currency currency = currencyStr != null ? Currency.valueOf(currencyStr) : Currency.TND;

        return new Transaction(id, userId, amount, type, status, description, createdAt, referenceType, referenceId, currency);
    }

    public List<Transaction> findByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        String query = "SELECT * FROM transaction WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Transactions by user: " + e.getMessage());
        }
        return list;
    }

    /** Saves a created transaction and returns the generated DB id (-1 on failure). */
    public int createAndGetId(Transaction entity) {
        String query = "INSERT INTO transaction (user_id, amount, type, status, description, " +
                "reference_type, reference_id, currency) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setString(3, entity.getType().toString());
            pstmt.setString(4, entity.getStatus() != null ? entity.getStatus().toString() : "PENDING");
            pstmt.setString(5, entity.getDescription());
            pstmt.setString(6, entity.getReferenceType() != null ? entity.getReferenceType().toString() : null);
            pstmt.setObject(7, entity.getReferenceId());
            pstmt.setString(8, entity.getCurrency() != null ? entity.getCurrency().toString() : "TND");
            pstmt.executeUpdate();
            ResultSet keys = pstmt.getGeneratedKeys();
            if (keys.next()) return keys.getInt(1);
        } catch (SQLException e) {
            System.out.println("Error creating Transaction: " + e.getMessage());
        }
        return -1;
    }
}

