package tn.esprit.dao;

import tn.esprit.entities.Loan;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Loan entity.
 */
public class LoanDAO implements CrudInterface<Loan> {

    private Connection connection;

    public LoanDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Loan entity) {
        String query = "INSERT INTO loan (user_id, amount, interest_rate, start_date, end_date, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setBigDecimal(3, entity.getInterestRate());
            pstmt.setDate(4, Date.valueOf(entity.getStartDate()));
            pstmt.setDate(5, Date.valueOf(entity.getEndDate()));
            pstmt.setString(6, entity.getStatus());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Loan: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Loan read(int id) {
        String query = "SELECT * FROM loan WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Loan: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Loan> readAll() {
        List<Loan> loans = new ArrayList<>();
        String query = "SELECT * FROM loan";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                loans.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Loans: " + e.getMessage());
        }
        return loans;
    }

    @Override
    public boolean update(Loan entity) {
        String query = "UPDATE loan SET user_id = ?, amount = ?, interest_rate = ?, " +
                "start_date = ?, end_date = ?, status = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getUserId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setBigDecimal(3, entity.getInterestRate());
            pstmt.setDate(4, Date.valueOf(entity.getStartDate()));
            pstmt.setDate(5, Date.valueOf(entity.getEndDate()));
            pstmt.setString(6, entity.getStatus());
            pstmt.setInt(7, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Loan: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM loan WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Loan: " + e.getMessage());
            return false;
        }
    }

    private Loan mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int userId = rs.getInt("user_id");
        BigDecimal amount = rs.getBigDecimal("amount");
        BigDecimal interestRate = rs.getBigDecimal("interest_rate");
        LocalDate startDate = rs.getDate("start_date").toLocalDate();
        LocalDate endDate = rs.getDate("end_date").toLocalDate();
        String status = rs.getString("status");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Loan(id, userId, amount, interestRate, startDate, endDate, status, createdAt);
    }

    public List<Loan> findByUserId(int userId) {
        List<Loan> loans = new ArrayList<>();
        String query = "SELECT * FROM loan WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) loans.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Loans by user: " + e.getMessage());
        }
        return loans;
    }
}

