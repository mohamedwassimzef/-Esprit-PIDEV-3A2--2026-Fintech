package tn.esprit.dao;

import tn.esprit.entities.Expense;
import tn.esprit.utils.MyDB;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Expense entity.
 */
public class ExpenseDAO implements CrudInterface<Expense> {

    private Connection connection;

    public ExpenseDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Expense entity) {
        String query = "INSERT INTO expense (amount, category, expense_date, description, budget_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setBigDecimal(1, entity.getAmount());
            pstmt.setString(2, entity.getCategory());
            pstmt.setDate(3, Date.valueOf(entity.getExpenseDate()));
            pstmt.setString(4, entity.getDescription());
            pstmt.setObject(5, entity.getBudgetId());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Expense: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Expense read(int id) {
        String query = "SELECT * FROM expense WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Expense: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Expense> readAll() {
        List<Expense> expenses = new ArrayList<>();
        String query = "SELECT * FROM expense";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                expenses.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Expenses: " + e.getMessage());
        }
        return expenses;
    }

    @Override
    public boolean update(Expense entity) {
        String query = "UPDATE expense SET amount = ?, category = ?, expense_date = ?, " +
                "description = ?, budget_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setBigDecimal(1, entity.getAmount());
            pstmt.setString(2, entity.getCategory());
            pstmt.setDate(3, Date.valueOf(entity.getExpenseDate()));
            pstmt.setString(4, entity.getDescription());
            pstmt.setObject(5, entity.getBudgetId());
            pstmt.setInt(6, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Expense: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM expense WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Expense: " + e.getMessage());
            return false;
        }
    }

    private Expense mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        BigDecimal amount = rs.getBigDecimal("amount");
        String category = rs.getString("category");
        LocalDate expenseDate = rs.getDate("expense_date").toLocalDate();
        String description = rs.getString("description");
        Integer budgetId = rs.getObject("budget_id") != null ? rs.getInt("budget_id") : null;
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Expense(id, amount, category, expenseDate, description, budgetId, createdAt);
    }

    public List<Expense> findByBudgetId(int budgetId) {
        List<Expense> expenses = new ArrayList<>();
        String query = "SELECT * FROM expense WHERE budget_id = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, budgetId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) expenses.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Expenses by budget: " + e.getMessage());
        }
        return expenses;
    }
}

