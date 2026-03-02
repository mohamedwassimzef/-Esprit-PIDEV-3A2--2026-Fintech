package tn.esprit.dao;

import tn.esprit.entities.Budget;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Budget entity.
 */
public class BudgetDAO implements CrudInterface<Budget> {

    private Connection connection;

    public BudgetDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Budget entity) {
        String query = "INSERT INTO budget (name, amount, start_date, end_date, user_id, category, spent_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setDate(3, Date.valueOf(entity.getStartDate()));
            pstmt.setDate(4, Date.valueOf(entity.getEndDate()));
            pstmt.setInt(5, entity.getUserId());
            pstmt.setString(6, entity.getCategory());
            pstmt.setBigDecimal(7, entity.getSpentAmount());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Budget: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Budget read(int id) {
        String query = "SELECT * FROM budget WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Budget: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Budget> readAll() {
        List<Budget> budgets = new ArrayList<>();
        String query = "SELECT * FROM budget";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                budgets.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Budgets: " + e.getMessage());
        }
        return budgets;
    }

    @Override
    public boolean update(Budget entity) {
        String query = "UPDATE budget SET name = ?, amount = ?, start_date = ?, end_date = ?, " +
                "user_id = ?, category = ?, spent_amount = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setBigDecimal(2, entity.getAmount());
            pstmt.setDate(3, Date.valueOf(entity.getStartDate()));
            pstmt.setDate(4, Date.valueOf(entity.getEndDate()));
            pstmt.setInt(5, entity.getUserId());
            pstmt.setString(6, entity.getCategory());
            pstmt.setBigDecimal(7, entity.getSpentAmount());
            pstmt.setInt(8, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Budget: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM budget WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Budget: " + e.getMessage());
            return false;
        }
    }

    private Budget mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        BigDecimal amount = rs.getBigDecimal("amount");
        LocalDate startDate = rs.getDate("start_date").toLocalDate();
        LocalDate endDate = rs.getDate("end_date").toLocalDate();
        int userId = rs.getInt("user_id");
        String category = rs.getString("category");
        BigDecimal spentAmount = rs.getBigDecimal("spent_amount");

        return new Budget(id, name, amount, startDate, endDate, userId, category, spentAmount);
    }

    public List<Budget> findByUserId(int userId) {
        List<Budget> budgets = new ArrayList<>();
        String query = "SELECT * FROM budget WHERE user_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) budgets.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Budgets by user: " + e.getMessage());
        }
        return budgets;
    }
}

