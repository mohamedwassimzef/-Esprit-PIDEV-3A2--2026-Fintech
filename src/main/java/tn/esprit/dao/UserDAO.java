package tn.esprit.dao;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * UserDAO — responsible ONLY for SQL operations on the `user` table.
 *
 * Rules:
 *  - No password hashing here (that belongs in AuthService)
 *  - No validation here (that belongs in ValidationUtils / services)
 *  - Every method either returns data or a boolean success flag
 */
public class UserDAO implements CrudInterface<User> {

    private final Connection connection;

    public UserDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    // ── CRUD ──────────────────────────────────────────────────────────

    @Override
    public boolean create(User user) {
        String sql = """
                INSERT INTO user
                    (name, email, password_hash, role_id, is_verified,
                     phone, verification_code, google_account, facebook_account)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1,  user.getName());
            ps.setString(2,  user.getEmail());
            ps.setString(3,  user.getPasswordHash());
            ps.setInt(4,     user.getRoleId());
            ps.setBoolean(5, user.isVerified());
            ps.setString(6,  user.getPhone());
            ps.setString(7,  user.getVerificationCode());
            ps.setBoolean(8, user.isGoogleAccount());
            ps.setBoolean(9, user.isFacebookAccount());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.create: " + e.getMessage());
            return false;
        }
    }

    @Override
    public User read(int id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("UserDAO.read: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<User> readAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            System.err.println("UserDAO.readAll: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates everything EXCEPT the password.
     * Password changes go through AuthService.changePassword().
     */
    @Override
    public boolean update(User user) {
        String sql = """
                UPDATE user
                SET name = ?, email = ?, role_id = ?,
                    is_verified = ?, phone = ?
                WHERE id = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1,  user.getName());
            ps.setString(2,  user.getEmail());
            ps.setInt(3,     user.getRoleId());
            ps.setBoolean(4, user.isVerified());
            ps.setString(5,  user.getPhone());
            ps.setInt(6,     user.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.update: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.delete: " + e.getMessage());
            return false;
        }
    }

    // ── lookup helpers (used by AuthService) ─────────────────────────

    public User findByEmail(String email) {
        String sql = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        } catch (SQLException e) {
            System.err.println("UserDAO.findByEmail: " + e.getMessage());
        }
        return null;
    }

    public boolean emailExists(String email) {
        String sql = "SELECT id FROM user WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("UserDAO.emailExists: " + e.getMessage());
            return false;
        }
    }

    // ── verification helpers ──────────────────────────────────────────

    public boolean saveVerificationCode(String email, String code) {
        String sql = "UPDATE user SET verification_code = ? WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.saveVerificationCode: " + e.getMessage());
            return false;
        }
    }

    public boolean verifyUser(String email, String code) {
        String sql = """
                UPDATE user
                SET is_verified = TRUE, verification_code = NULL
                WHERE email = ? AND verification_code = ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.verifyUser: " + e.getMessage());
            return false;
        }
    }

    // ── password helpers (called only by AuthService) ─────────────────

    public boolean updatePassword(int userId, String newHashedPassword) {
        String sql = "UPDATE user SET password_hash = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.updatePassword: " + e.getMessage());
            return false;
        }
    }

    // ── session helpers ───────────────────────────────────────────────

    public boolean updateLastLogin(int userId) {
        String sql = "UPDATE user SET last_login = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.updateLastLogin: " + e.getMessage());
            return false;
        }
    }

    // ── mapping ───────────────────────────────────────────────────────

    private User map(ResultSet rs) throws SQLException {
        LocalDateTime lastLogin = null;
        Timestamp lastLoginTs = rs.getTimestamp("last_login");
        if (lastLoginTs != null) lastLogin = lastLoginTs.toLocalDateTime();

        return new User(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getInt("role_id"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime(),
                rs.getBoolean("is_verified"),
                rs.getString("phone"),
                rs.getString("verification_code"),
                rs.getBoolean("google_account"),
                rs.getBoolean("facebook_account"),
                lastLogin
        );
    }
}