package tn.esprit.dao;

import tn.esprit.entities.Role;
import tn.esprit.utils.MyDB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Role entity.
 */
public class RoleDAO implements CrudInterface<Role> {

    private Connection connection;

    public RoleDAO() {
        this.connection = MyDB.getInstance().getConx();
    }

    @Override
    public boolean create(Role entity) {
        String query = "INSERT INTO role (role_name, permissions) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getRoleName());
            pstmt.setString(2, entity.getPermissions());

            int rowsInserted = pstmt.executeUpdate();
            return rowsInserted > 0;
        } catch (SQLException e) {
            System.out.println("Error creating Role: " + e.getMessage());
            return false;
        }
    }

    @Override
    public Role read(int id) {
        String query = "SELECT * FROM role WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToEntity(rs);
            }
        } catch (SQLException e) {
            System.out.println("Error reading Role: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Role> readAll() {
        List<Role> roles = new ArrayList<>();
        String query = "SELECT * FROM role";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                roles.add(mapResultSetToEntity(rs));
            }
        } catch (SQLException e) {
            System.out.println("Error reading all Roles: " + e.getMessage());
        }
        return roles;
    }

    @Override
    public boolean update(Role entity) {
        String query = "UPDATE role SET role_name = ?, permissions = ? WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, entity.getRoleName());
            pstmt.setString(2, entity.getPermissions());
            pstmt.setInt(3, entity.getId());

            int rowsUpdated = pstmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.out.println("Error updating Role: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(int id) {
        String query = "DELETE FROM role WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, id);
            int rowsDeleted = pstmt.executeUpdate();
            return rowsDeleted > 0;
        } catch (SQLException e) {
            System.out.println("Error deleting Role: " + e.getMessage());
            return false;
        }
    }

    private Role mapResultSetToEntity(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String roleName = rs.getString("role_name");
        String permissions = rs.getString("permissions");

        return new Role(id, roleName, permissions);
    }
}

