package tn.esprit.dao;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User entity.
 */
public class UserDAO implements CrudInterface<User> {

    private Connection connection;

    public UserDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(User entity) {
        String query = "INSERT INTO user (name, email, password_hash, role_id, is_verified, phone) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getEmail());
            pstmt.setString(3, entity.getPasswordHash());
            pstmt.setInt(4, entity.getRoleId());
            pstmt.setBoolean(5, entity.isVerified());
            pstmt.setString(6, entity.getPhone());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating User: " + e.getMessage());
            return false;
        }
    }

    @Override
    public User read(int id) {
        String query = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading User: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> readAll() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                users.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Users: " + e.getMessage());
        }
        return users;
    }

    @Override
    public boolean update(User entity) {
        String query = "UPDATE user SET name = ?, email = ?, password_hash = ?, role_id = ?, " +
                "is_verified = ?, phone = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getEmail());
            pstmt.setString(3, entity.getPasswordHash());
            pstmt.setInt(4, entity.getRoleId());
            pstmt.setBoolean(5, entity.isVerified());
            pstmt.setString(6, entity.getPhone());
            pstmt.setInt(7, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating User: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM user WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting User: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find a user by email address.
     * @param email the email to search for
     * @return the User if found, or null
     */
    public User findByEmail(String email) {
        String query = "SELECT * FROM user WHERE email = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error finding User by email: " + e.getMessage());
        }
        return null;
    }

    private User mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String passwordHash = rs.getString("password_hash");
        int roleId = rs.getInt("role_id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
        boolean isVerified = rs.getBoolean("is_verified");
        String phone = rs.getString("phone");

        return new User(id, name, email, passwordHash, roleId, createdAt, updatedAt, isVerified, phone);
    }
}

