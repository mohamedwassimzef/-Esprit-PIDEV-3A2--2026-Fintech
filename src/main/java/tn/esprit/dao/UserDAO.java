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
        String query = "INSERT INTO user (name, email, password_hash, role_id, is_verified, phone, " +
                "verification_code, google_account, last_login, face_registered) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getEmail());
            pstmt.setString(3, entity.getPasswordHash());
            pstmt.setInt(4, entity.getRoleId());
            pstmt.setBoolean(5, entity.isVerified());
            pstmt.setString(6, entity.getPhone());
            pstmt.setString(7, entity.getVerificationCode());
            pstmt.setBoolean(8, entity.isGoogleAccount());
            pstmt.setObject(9, entity.getLastLogin() != null
                    ? Timestamp.valueOf(entity.getLastLogin()) : null);
            pstmt.setBoolean(10, entity.isFaceRegistered());

            return pstmt.executeUpdate() > 0;
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
                "is_verified = ?, phone = ?, verification_code = ?, google_account = ?, " +
                "last_login = ?, face_registered = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getName());
            pstmt.setString(2, entity.getEmail());
            pstmt.setString(3, entity.getPasswordHash());
            pstmt.setInt(4, entity.getRoleId());
            pstmt.setBoolean(5, entity.isVerified());
            pstmt.setString(6, entity.getPhone());
            pstmt.setString(7, entity.getVerificationCode());
            pstmt.setBoolean(8, entity.isGoogleAccount());
            pstmt.setObject(9, entity.getLastLogin() != null
                    ? Timestamp.valueOf(entity.getLastLogin()) : null);
            pstmt.setBoolean(10, entity.isFaceRegistered());
            pstmt.setInt(11, entity.getId());

            return pstmt.executeUpdate() > 0;
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
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting User: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    //  Extra helpers
    // -------------------------------------------------------------------------

    /** Find a user by email address. */
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

    /** Find a user by verification code (used during email verification). */
    public User findByVerificationCode(String code) {
        String query = "SELECT * FROM user WHERE verification_code = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, code);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error finding User by verification code: " + e.getMessage());
        }
        return null;
    }

    /** Stamp the last_login column to now for the given user id. */
    public boolean updateLastLogin(int userId) {
        String query = "UPDATE user SET last_login = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setInt(2, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error updating last_login: " + e.getMessage());
            return false;
        }
    }

    /** Mark a user's email as verified and clear the verification code. */
    public boolean markVerified(int userId) {
        String query = "UPDATE user SET is_verified = 1, verification_code = NULL WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.out.println("Error marking user as verified: " + e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    //  Mapping
    // -------------------------------------------------------------------------

    private User mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id                  = rs.getInt("id");
        String name             = rs.getString("name");
        String email            = rs.getString("email");
        String passwordHash     = rs.getString("password_hash");
        int roleId              = rs.getInt("role_id");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        LocalDateTime updatedAt = rs.getTimestamp("updated_at").toLocalDateTime();
        boolean isVerified      = rs.getBoolean("is_verified");
        String phone            = rs.getString("phone");
        String verificationCode = rs.getString("verification_code");
        boolean googleAccount   = rs.getBoolean("google_account");
        Timestamp lastLoginTs   = rs.getTimestamp("last_login");
        LocalDateTime lastLogin = lastLoginTs != null ? lastLoginTs.toLocalDateTime() : null;
        boolean faceRegistered  = rs.getBoolean("face_registered");

        return new User(id, name, email, passwordHash, roleId, createdAt, updatedAt,
                isVerified, phone, verificationCode, googleAccount, lastLogin, faceRegistered);
    }
}
