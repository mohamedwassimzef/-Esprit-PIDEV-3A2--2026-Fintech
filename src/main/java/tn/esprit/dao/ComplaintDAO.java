package tn.esprit.dao;

import tn.esprit.entities.Complaint;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Complaint entity.
 */
public class ComplaintDAO implements CrudInterface<Complaint> {

    private Connection connection;

    public ComplaintDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Complaint entity) {
        String query = "INSERT INTO complaint (subject, status, complaint_date, response, user_id) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getSubject());
            pstmt.setString(2, entity.getStatus());
            pstmt.setDate(3, Date.valueOf(entity.getComplaintDate()));
            pstmt.setString(4, entity.getResponse());
            pstmt.setObject(5, entity.getUserId());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Complaint: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Complaint read(int id) {
        String query = "SELECT * FROM complaint WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Complaint: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Complaint> readAll() {
        List<Complaint> complaints = new ArrayList<>();
        String query = "SELECT * FROM complaint";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                complaints.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Complaints: " + e.getMessage());
        }
        return complaints;
    }

    @Override
    public boolean update(Complaint entity) {
        String query = "UPDATE complaint SET subject = ?, status = ?, complaint_date = ?, " +
                "response = ?, user_id = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getSubject());
            pstmt.setString(2, entity.getStatus());
            pstmt.setDate(3, Date.valueOf(entity.getComplaintDate()));
            pstmt.setString(4, entity.getResponse());
            pstmt.setObject(5, entity.getUserId());
            pstmt.setInt(6, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Complaint: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM complaint WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Complaint: " + e.getMessage());
            return false;
        }
    }

    private Complaint mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String subject = rs.getString("subject");
        String status = rs.getString("status");
        LocalDate complaintDate = rs.getDate("complaint_date").toLocalDate();
        String response = rs.getString("response");
        Integer userId = rs.getObject("user_id") != null ? rs.getInt("user_id") : null;
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Complaint(id, subject, status, complaintDate, response, userId, createdAt);
    }

    public List<Complaint> findByUserId(int userId) {
        List<Complaint> list = new ArrayList<>();
        String query = "SELECT * FROM complaint WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToEntity(rs));
        } catch (SQLException e) {
            System.out.println("Error finding Complaints by user: " + e.getMessage());
        }
        return list;
    }
}

