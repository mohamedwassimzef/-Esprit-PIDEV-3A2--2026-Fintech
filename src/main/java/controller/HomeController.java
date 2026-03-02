package controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import tn.esprit.services.SessionManager;

/**
 * Controller for Home.fxml
 *
 * Shows service cards after login.
 * Only Insurance is active; the rest show "Coming soon".
 */
public class HomeController {

    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null) {
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getName());
        }
    }

    /** Open the Insurance module (UserDashboard). */
    @FXML
    private void goInsurance() {
        SceneManager.switchScene("/View/UserDashboard.fxml", "Insurance – My Dashboard");
    }

    /** Open the Personal Finance module. */
    @FXML
    private void goPersonalFinance() {
        SceneManager.switchScene("/View/PersonalFinanceDashboard.fxml", "Personal Finance");
    }

    /** Open the Loans module. */
    @FXML
    private void goLoans() {
        SceneManager.switchScene("/View/LoanDashboard.fxml", "Loans");
    }

    /** Open the Transactions module. */
    @FXML
    private void goTransactions() {
        SceneManager.switchScene("/View/TransactionDashboard.fxml", "Transactions");
    }

    @FXML
    private void handleLogout() {
        SessionManager.getInstance().logout();
        SceneManager.switchScene("/View/Login.fxml", "FinTech – Login");
    }
}




