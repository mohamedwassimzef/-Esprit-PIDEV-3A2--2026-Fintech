package tn.esprit.dao;

import tn.esprit.entities.Repayment;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Repayment entity.
 */
public class RepaymentDAO implements CrudInterface<Repayment> {

    private Connection connection;

    public RepaymentDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Repayment entity) {
        String query = "INSERT INTO repayment (loan_id, amount, payment_date, payment_type, status, monthly_payment) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getLoanId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setDate(3, Date.valueOf(entity.getPaymentDate()));
            pstmt.setString(4, entity.getPaymentType());
            pstmt.setString(5, entity.getStatus());
            pstmt.setBigDecimal(6, entity.getMonthlyPayment());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Repayment: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Repayment read(int id) {
        String query = "SELECT * FROM repayment WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Repayment: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Repayment> readAll() {
        List<Repayment> repayments = new ArrayList<>();
        String query = "SELECT * FROM repayment";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                repayments.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Repayments: " + e.getMessage());
        }
        return repayments;
    }

    @Override
    public boolean update(Repayment entity) {
        String query = "UPDATE repayment SET loan_id = ?, amount = ?, payment_date = ?, " +
                "payment_type = ?, status = ?, monthly_payment = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, entity.getLoanId());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setDate(3, Date.valueOf(entity.getPaymentDate()));
            pstmt.setString(4, entity.getPaymentType());
            pstmt.setString(5, entity.getStatus());
            pstmt.setBigDecimal(6, entity.getMonthlyPayment());
            pstmt.setInt(7, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Repayment: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM repayment WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Repayment: " + e.getMessage());
            return false;
        }
    }

    private Repayment mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int loanId = rs.getInt("loan_id");
        BigDecimal amount = rs.getBigDecimal("amount");
        LocalDate paymentDate = rs.getDate("payment_date").toLocalDate();
        String paymentType = rs.getString("payment_type");
        String status = rs.getString("status");
        BigDecimal monthlyPayment = rs.getBigDecimal("monthly_payment");

        return new Repayment(id, loanId, amount, paymentDate, paymentType, status, monthlyPayment);
    }

    public List<Repayment> findByLoanId(int loanId) {
        List<Repayment> list = new ArrayList<>();
        String query = "SELECT * FROM repayment WHERE loan_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, loanId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Repayments by loan: " + e.getMessage());
        }
        return list;
    }
}

