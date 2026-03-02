package tn.esprit.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import tn.esprit.entities.Role;
import tn.esprit.entities.User;
import tn.esprit.services.AuthService;
import tn.esprit.services.UserService;
import tn.esprit.utils.SessionManager;
import tn.esprit.utils.ValidationUtils;

import java.io.IOException;
import java.util.Optional;

/**
 * Admin panel — manages all user accounts.
 *
 * Fixes applied vs original:
 *  1. Password field removed from form — admin cannot set/see passwords
 *  2. Admin-created users get a generated temp password (shown once in a dialog)
 *  3. Role is a ComboBox loaded from DB — no magic numbers
 *  4. Delete requires confirmation dialog
 *  5. Real-time search/filter on the table
 *  6. is_verified shown as a column and editable via CheckBox in the form
 *  7. SessionManager used instead of passing null
 */
public class UserManagementController {

    // ── table ─────────────────────────────────────────────────────────
    @FXML private TableView<User>             userTable;
    @FXML private TableColumn<User, Integer>  colId;
    @FXML private TableColumn<User, String>   colName;
    @FXML private TableColumn<User, String>   colEmail;
    @FXML private TableColumn<User, String>   colPhone;
    @FXML private TableColumn<User, String>   colRole;
    @FXML private TableColumn<User, Boolean>  colVerified;

    // ── search ────────────────────────────────────────────────────────
    @FXML private TextField searchField;

    // ── form ──────────────────────────────────────────────────────────
    @FXML private TextField        nameField;
    @FXML private TextField        emailField;
    @FXML private TextField        phoneField;
    @FXML private ComboBox<Role>   roleCombo;
    @FXML private CheckBox         verifiedCheck;

    // ── feedback ──────────────────────────────────────────────────────
    @FXML private Label messageLabel;

    // ── state ─────────────────────────────────────────────────────────
    private final UserService  userService  = new UserService();
    private final AuthService  authService  = new AuthService();

    private ObservableList<User> masterList;
    private FilteredList<User>   filteredList;

    // ── init ──────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        setupTableColumns();
        setupRoleCombo();
        setupSearch();
        setupKeyboardFilters();
        setupRowClickFill();
        loadUsers();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(d ->
                new javafx.beans.property.SimpleIntegerProperty(d.getValue().getId()).asObject());

        colName.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getName()));

        colEmail.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getEmail()));

        colPhone.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getPhone()));

        // Show role name instead of the raw integer
        colRole.setCellValueFactory(d -> {
            Role role = userService.getRoleById(d.getValue().getRoleId());
            String roleName = role != null ? role.getRoleName() : "Unknown";
            return new javafx.beans.property.SimpleStringProperty(roleName);
        });

        // Verified: read-only checkmark column
        colVerified.setCellValueFactory(d ->
                new javafx.beans.property.SimpleBooleanProperty(d.getValue().isVerified()).asObject());
        colVerified.setCellFactory(CheckBoxTableCell.forTableColumn(colVerified));
    }

    private void setupRoleCombo() {
        roleCombo.setItems(FXCollections.observableArrayList(userService.getAllRoles()));

        // Display role name in the ComboBox dropdown
        roleCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getRoleName());
            }
        });
        roleCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Role r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.getRoleName());
            }
        });
    }

    private void setupSearch() {
        masterList   = FXCollections.observableArrayList();
        filteredList = new FilteredList<>(masterList, p -> true);
        userTable.setItems(filteredList);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filter = newVal == null ? "" : newVal.trim().toLowerCase();
            filteredList.setPredicate(user -> {
                if (filter.isEmpty()) return true;
                return user.getName().toLowerCase().contains(filter)
                        || user.getEmail().toLowerCase().contains(filter)
                        || user.getPhone().contains(filter);
            });
        });
    }

    private void setupKeyboardFilters() {
        // Phone: digits only, max 8
        phoneField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[0-9]")) { e.consume(); return; }
            if (phoneField.getText().length() >= 8) e.consume();
        });

        // Name: letters and spaces only
        nameField.addEventFilter(KeyEvent.KEY_TYPED, e -> {
            if (!e.getCharacter().matches("[a-zA-Z ]")) e.consume();
        });
    }

    private void setupRowClickFill() {
        userTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldUser, newUser) -> {
                    if (newUser != null) fillForm(newUser);
                });
    }

    // ── data loading ──────────────────────────────────────────────────

    private void loadUsers() {
        masterList.setAll(userService.getAllUsers());
    }

    // ── form helpers ──────────────────────────────────────────────────

    private void fillForm(User user) {
        nameField.setText(user.getName());
        emailField.setText(user.getEmail());
        phoneField.setText(user.getPhone());
        verifiedCheck.setSelected(user.isVerified());

        // Select the matching role in the ComboBox
        roleCombo.getItems().stream()
                .filter(r -> r.getId() == user.getRoleId())
                .findFirst()
                .ifPresent(roleCombo::setValue);
    }

    private void clearForm() {
        nameField.clear();
        emailField.clear();
        phoneField.clear();
        roleCombo.setValue(null);
        verifiedCheck.setSelected(false);
        userTable.getSelectionModel().clearSelection();
        messageLabel.setText("");
    }

    // ── validation ────────────────────────────────────────────────────

    private boolean validateForm(boolean requireEmail) {
        if (!ValidationUtils.isValidName(nameField.getText().trim())) {
            showMessage("Name must be letters only, at least 2 characters.", "red");
            return false;
        }
        if (requireEmail) {
            if (!ValidationUtils.isValidEmail(emailField.getText().trim())) {
                showMessage("Invalid email format.", "red");
                return false;
            }
        }
        if (!ValidationUtils.isValidPhone(phoneField.getText().trim())) {
            showMessage("Phone must be exactly 8 digits.", "red");
            return false;
        }
        if (roleCombo.getValue() == null) {
            showMessage("Please select a role.", "red");
            return false;
        }
        return true;
    }

    // ── CRUD handlers ─────────────────────────────────────────────────

    @FXML
    private void handleAdd() {
        if (!validateForm(true)) return;

        String email = emailField.getText().trim().toLowerCase();
        if (userService.emailExists(email)) {
            showMessage("This email is already registered.", "red");
            return;
        }

        User user = new User(
                nameField.getText().trim(),
                email,
                "",   // password set below
                roleCombo.getValue().getId(),
                phoneField.getText().trim()
        );
        user.setVerified(verifiedCheck.isSelected());

        // Generate a temporary password, hash it, store plain text in temp variable
        String tempPassword = authService.generateAndHashTempPassword(user);

        if (userService.adminCreateUser(user)) {
            // Show the temp password to the admin once — they must communicate it to the user
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("User Created");
            alert.setHeaderText("Account created successfully.");
            alert.setContentText(
                    "A temporary password has been generated.\n\n" +
                            "Temporary password: " + tempPassword + "\n\n" +
                            "Please share this with the user. They should change it after first login."
            );
            alert.showAndWait();

            loadUsers();
            clearForm();
            showMessage("✅ User added successfully.", "green");
        } else {
            showMessage("❌ Failed to add user. Try again.", "red");
        }
    }

    @FXML
    private void handleUpdate() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a user from the table first.", "red");
            return;
        }

        // On update we don't re-validate email format if unchanged
        if (!validateForm(false)) return;

        selected.setName(nameField.getText().trim());
        selected.setPhone(phoneField.getText().trim());
        selected.setRoleId(roleCombo.getValue().getId());
        selected.setVerified(verifiedCheck.isSelected());
        // Email intentionally not updated here — changing email needs re-verification

        if (userService.adminUpdateUser(selected)) {
            loadUsers();
            clearForm();
            showMessage("✅ User updated successfully.", "green");
        } else {
            showMessage("❌ Update failed. Try again.", "red");
        }
    }

    @FXML
    private void handleDelete() {
        User selected = userTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showMessage("Select a user from the table first.", "red");
            return;
        }

        // Prevent admin from deleting themselves
        if (SessionManager.isLoggedIn() &&
                SessionManager.getCurrentUser().getId() == selected.getId()) {
            showMessage("You cannot delete your own account.", "red");
            return;
        }

        // Confirmation dialog
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete User");
        confirm.setHeaderText("Delete " + selected.getName() + "?");
        confirm.setContentText("This action cannot be undone.");
        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (userService.deleteUser(selected.getId())) {
                loadUsers();
                clearForm();
                showMessage("✅ User deleted.", "green");
            } else {
                showMessage("❌ Delete failed.", "red");
            }
        }
    }

    @FXML
    private void handleClear() {
        clearForm();
    }

    // ── logout ────────────────────────────────────────────────────────

    @FXML
    private void handleLogout() {
        SessionManager.logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) userTable.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("FinTech Portal – Login");
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ── helper ────────────────────────────────────────────────────────

    private void showMessage(String text, String color) {
        messageLabel.setText(text);
        messageLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}