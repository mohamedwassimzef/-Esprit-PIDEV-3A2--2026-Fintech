package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.dao.BudgetDAO;
import tn.esprit.dao.ExpenseDAO;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Expense;
import tn.esprit.services.SessionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for PersonalFinanceDashboard.fxml
 *
 * Manages two entities:
 *  - Budget  : CRUD with progress tracking (amount vs spent)
 *  - Expense : CRUD linked to a budget
 *
 * Pattern: same boolean isEditMode flag used in InsuredAsset module.
 *   isEditMode = false  → submit creates new record
 *   isEditMode = true   → submit updates existing record
 */
public class PersonalFinanceController {

    // ── Top bar ────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;

    // ── Tabs ───────────────────────────────────────────────────────
    @FXML private TabPane mainTabs;
    @FXML private Tab budgetListTab, budgetFormTab, expenseListTab, expenseFormTab;

    // ── Budget table ───────────────────────────────────────────────
    @FXML private TableView<Budget>                   budgetTable;
    @FXML private TableColumn<Budget, Integer>        bColId;
    @FXML private TableColumn<Budget, String>         bColName, bColCategory;
    @FXML private TableColumn<Budget, BigDecimal>     bColAmount, bColSpent, bColRemaining;
    @FXML private TableColumn<Budget, Double>         bColProgress;
    @FXML private TableColumn<Budget, LocalDate>      bColStartDate, bColEndDate;
    @FXML private Button updateBudgetBtn, deleteBudgetBtn, viewExpensesBtn;

    // ── Budget summary ─────────────────────────────────────────────
    @FXML private Label totalBudgetedLabel, totalSpentLabel, remainingLabel;

    // ── Budget form ────────────────────────────────────────────────
    @FXML private Label     budgetFormTitle;
    @FXML private TextField bName, bAmount;
    @FXML private ComboBox<String> bCategory;
    @FXML private DatePicker bStartDate, bEndDate;
    @FXML private Button    submitBudgetBtn;

    // ── Expense table ──────────────────────────────────────────────
    @FXML private TableView<Expense>                  expenseTable;
    @FXML private TableColumn<Expense, Integer>       eColId;
    @FXML private TableColumn<Expense, String>        eColDescription, eColCategory;
    @FXML private TableColumn<Expense, BigDecimal>    eColAmount;
    @FXML private TableColumn<Expense, LocalDate>     eColDate;
    @FXML private TableColumn<Expense, String>        eColBudget;
    @FXML private TableColumn<Expense, LocalDateTime> eColCreatedAt;
    @FXML private Button updateExpenseBtn, deleteExpenseBtn;
    @FXML private Label  expenseTotalLabel;

    // ── Expense form ───────────────────────────────────────────────
    @FXML private Label     expenseFormTitle;
    @FXML private TextField eDescription, eAmount;
    @FXML private ComboBox<String> eCategory;
    @FXML private ComboBox<Budget> eBudgetCombo;
    @FXML private DatePicker eDate;
    @FXML private Button    submitExpenseBtn;

    // ── Expense filter ─────────────────────────────────────────────
    @FXML private ComboBox<Budget> expenseBudgetFilter;

    // ── State ──────────────────────────────────────────────────────
    private final BudgetDAO  budgetDAO  = new BudgetDAO();
    private final ExpenseDAO expenseDAO = new ExpenseDAO();

    private final ObservableList<Budget>  budgets  = FXCollections.observableArrayList();
    private final ObservableList<Expense> expenses = FXCollections.observableArrayList();

    private Budget  selectedBudget;
    private Expense selectedExpense;
    private boolean budgetEditMode  = false;
    private boolean expenseEditMode = false;

    private static final List<String> CATEGORIES = List.of(
        "Food", "Transport", "Housing", "Healthcare",
        "Education", "Entertainment", "Shopping", "Utilities", "Other"
    );

    // ──────────────────────────────────────────────────────────────
    //  INIT
    // ──────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getName());

        setupBudgetTable();
        setupExpenseTable();
        setupFormCombos();

        refreshBudgets();
        refreshExpenses(null);

        setBudgetButtonState(false);
        setExpenseButtonState(false);

        // Budget table row selection
        budgetTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedBudget = sel;
            setBudgetButtonState(sel != null);
        });

        // Double-click budget → fill form for update
        budgetTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedBudget != null)
                openBudgetForm(selectedBudget);
        });

        // Expense table row selection
        expenseTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedExpense = sel;
            setExpenseButtonState(sel != null);
        });

        // Double-click expense → fill form for update
        expenseTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedExpense != null)
                openExpenseForm(selectedExpense);
        });
    }

    // ──────────────────────────────────────────────────────────────
    //  TABLE SETUP
    // ──────────────────────────────────────────────────────────────
    private void setupBudgetTable() {
        bColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        bColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        bColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        bColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        bColSpent.setCellValueFactory(new PropertyValueFactory<>("spentAmount"));
        bColStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        bColEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        // Remaining = amount - spentAmount
        bColRemaining.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null); setStyle("");
                } else {
                    Budget b = getTableRow().getItem();
                    BigDecimal rem = b.getAmount().subtract(b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO);
                    setText(rem.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
                    setStyle(rem.compareTo(BigDecimal.ZERO) < 0
                        ? "-fx-text-fill:#ff4422;-fx-font-weight:700;"
                        : "-fx-text-fill:#33dd77;-fx-font-weight:700;");
                }
            }
        });

        // Progress bar column
        bColProgress.setCellFactory(col -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar(0);
            {
                bar.setMaxWidth(Double.MAX_VALUE);
                bar.setStyle("-fx-accent:#f5c800;");
            }
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Budget b = getTableRow().getItem();
                    if (b.getAmount().compareTo(BigDecimal.ZERO) == 0) {
                        bar.setProgress(0);
                    } else {
                        double pct = b.getSpentAmount() != null
                            ? b.getSpentAmount().doubleValue() / b.getAmount().doubleValue()
                            : 0.0;
                        bar.setProgress(Math.min(pct, 1.0));
                        bar.setStyle(pct > 1.0 ? "-fx-accent:#ff4422;" : "-fx-accent:#f5c800;");
                    }
                    setGraphic(bar);
                }
            }
        });

        budgetTable.setItems(budgets);
    }

    private void setupExpenseTable() {
        eColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        eColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        eColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        eColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        eColDate.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));
        eColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Budget name column (resolved from budgetId)
        eColBudget.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    Expense exp = getTableRow().getItem();
                    if (exp.getBudgetId() == null) {
                        setText("—");
                        setStyle("-fx-text-fill:#555555;");
                    } else {
                        Budget b = budgets.stream()
                            .filter(bud -> bud.getId() == exp.getBudgetId())
                            .findFirst().orElse(null);
                        setText(b != null ? b.getName() : "Budget #" + exp.getBudgetId());
                        setStyle("");
                    }
                }
            }
        });

        expenseTable.setItems(expenses);
    }

    private void setupFormCombos() {
        bCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
        eCategory.setItems(FXCollections.observableArrayList(CATEGORIES));
    }

    // ──────────────────────────────────────────────────────────────
    //  REFRESH
    // ──────────────────────────────────────────────────────────────
    private void refreshBudgets() {
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        budgets.setAll(budgetDAO.findByUserId(userId));

        // Summary
        BigDecimal totalBudgeted = budgets.stream()
            .map(Budget::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream()
            .map(b -> b.getSpentAmount() != null ? b.getSpentAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remaining = totalBudgeted.subtract(totalSpent);

        totalBudgetedLabel.setText(totalBudgeted.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        totalSpentLabel.setText(totalSpent.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        remainingLabel.setText(remaining.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");

        // Populate filter combo & eBudgetCombo
        ObservableList<Budget> budgetOptions = FXCollections.observableArrayList(budgets);
        expenseBudgetFilter.setItems(budgetOptions);
        eBudgetCombo.setItems(budgetOptions);

        // Budget display in combos
        javafx.util.StringConverter<Budget> budgetConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Budget b) { return b == null ? "" : b.getName() + " (" + b.getCategory() + ")"; }
            @Override public Budget fromString(String s) { return null; }
        };
        expenseBudgetFilter.setConverter(budgetConverter);
        eBudgetCombo.setConverter(budgetConverter);
    }

    private void refreshExpenses(Budget filterBudget) {
        List<Expense> all;
        if (filterBudget != null) {
            all = expenseDAO.findByBudgetId(filterBudget.getId());
        } else {
            all = expenseDAO.readAll();
        }
        expenses.setAll(all);

        BigDecimal total = all.stream()
            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        expenseTotalLabel.setText(total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
    }

    // ──────────────────────────────────────────────────────────────
    //  BUDGET CRUD HANDLERS
    // ──────────────────────────────────────────────────────────────
    @FXML private void handleAddBudget() {
        clearBudgetForm();
        budgetEditMode = false;
        budgetFormTitle.setText("NEW BUDGET");
        submitBudgetBtn.setText("Save Budget");
        mainTabs.getSelectionModel().select(budgetFormTab);
    }

    @FXML private void handleUpdateBudget() {
        if (selectedBudget != null) openBudgetForm(selectedBudget);
    }

    private void openBudgetForm(Budget b) {
        budgetEditMode = true;
        budgetFormTitle.setText("EDIT BUDGET");
        submitBudgetBtn.setText("Update Budget");
        bName.setText(b.getName());
        bAmount.setText(b.getAmount().toPlainString());
        bCategory.setValue(b.getCategory());
        bStartDate.setValue(b.getStartDate());
        bEndDate.setValue(b.getEndDate());
        mainTabs.getSelectionModel().select(budgetFormTab);
    }

    @FXML private void handleSubmitBudget() {
        // ── Validation ──
        String name = bName.getText().trim();
        String amountStr = bAmount.getText().trim();
        String category = bCategory.getValue();
        LocalDate start = bStartDate.getValue();
        LocalDate end = bEndDate.getValue();

        if (name.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Budget name is required."); return; }
        if (amountStr.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Amount is required."); return; }
        if (category == null) { showAlert(AlertType.WARNING, "Validation", "Please select a category."); return; }
        if (start == null || end == null) { showAlert(AlertType.WARNING, "Validation", "Start and end dates are required."); return; }
        if (!end.isAfter(start)) { showAlert(AlertType.WARNING, "Validation", "End date must be after start date."); return; }

        BigDecimal amount;
        try { amount = new BigDecimal(amountStr); if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { showAlert(AlertType.WARNING, "Validation", "Amount must be a positive number."); return; }

        int userId = SessionManager.getInstance().getCurrentUser().getId();

        if (budgetEditMode && selectedBudget != null) {
            selectedBudget.setName(name);
            selectedBudget.setAmount(amount);
            selectedBudget.setCategory(category);
            selectedBudget.setStartDate(start);
            selectedBudget.setEndDate(end);
            selectedBudget.setUserId(userId);
            boolean ok = budgetDAO.update(selectedBudget);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Budget", ok ? "Budget updated." : "Update failed.");
        } else {
            Budget b = new Budget(name, amount, start, end, userId, category);
            boolean ok = budgetDAO.create(b);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Budget", ok ? "Budget created." : "Creation failed.");
        }

        refreshBudgets();
        mainTabs.getSelectionModel().select(budgetListTab);
        clearBudgetForm();
    }

    @FXML private void handleDeleteBudget() {
        if (selectedBudget == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete budget \"" + selectedBudget.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = budgetDAO.delete(selectedBudget.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Budget", ok ? "Budget deleted." : "Delete failed.");
                refreshBudgets();
                refreshExpenses(null);
                setBudgetButtonState(false);
            }
        });
    }

    @FXML private void handleCancelBudgetForm() {
        clearBudgetForm();
        mainTabs.getSelectionModel().select(budgetListTab);
    }

    @FXML private void handleViewExpenses() {
        if (selectedBudget != null) {
            expenseBudgetFilter.setValue(selectedBudget);
            refreshExpenses(selectedBudget);
            mainTabs.getSelectionModel().select(expenseListTab);
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  EXPENSE CRUD HANDLERS
    // ──────────────────────────────────────────────────────────────
    @FXML private void handleAddExpense() {
        clearExpenseForm();
        expenseEditMode = false;
        expenseFormTitle.setText("NEW EXPENSE");
        submitExpenseBtn.setText("Save Expense");
        mainTabs.getSelectionModel().select(expenseFormTab);
    }

    @FXML private void handleUpdateExpense() {
        if (selectedExpense != null) openExpenseForm(selectedExpense);
    }

    private void openExpenseForm(Expense e) {
        expenseEditMode = true;
        expenseFormTitle.setText("EDIT EXPENSE");
        submitExpenseBtn.setText("Update Expense");
        eDescription.setText(e.getDescription() != null ? e.getDescription() : "");
        eAmount.setText(e.getAmount().toPlainString());
        eCategory.setValue(e.getCategory());
        eDate.setValue(e.getExpenseDate());
        if (e.getBudgetId() != null) {
            budgets.stream().filter(b -> b.getId() == e.getBudgetId()).findFirst()
                .ifPresent(eBudgetCombo::setValue);
        } else {
            eBudgetCombo.setValue(null);
        }
        mainTabs.getSelectionModel().select(expenseFormTab);
    }

    @FXML private void handleSubmitExpense() {
        // ── Validation ──
        String desc = eDescription.getText().trim();
        String amountStr = eAmount.getText().trim();
        String category = eCategory.getValue();
        LocalDate date = eDate.getValue();
        Budget linkedBudget = eBudgetCombo.getValue();

        if (amountStr.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Amount is required."); return; }
        if (category == null) { showAlert(AlertType.WARNING, "Validation", "Please select a category."); return; }
        if (date == null) { showAlert(AlertType.WARNING, "Validation", "Date is required."); return; }

        BigDecimal amount;
        try { amount = new BigDecimal(amountStr); if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { showAlert(AlertType.WARNING, "Validation", "Amount must be a positive number."); return; }

        Integer budgetId = linkedBudget != null ? linkedBudget.getId() : null;

        if (expenseEditMode && selectedExpense != null) {
            selectedExpense.setDescription(desc);
            selectedExpense.setAmount(amount);
            selectedExpense.setCategory(category);
            selectedExpense.setExpenseDate(date);
            selectedExpense.setBudgetId(budgetId);
            boolean ok = expenseDAO.update(selectedExpense);
            if (ok) updateBudgetSpent(budgetId);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Expense", ok ? "Expense updated." : "Update failed.");
        } else {
            Expense exp = new Expense(amount, category, date, desc, budgetId);
            boolean ok = expenseDAO.create(exp);
            if (ok) updateBudgetSpent(budgetId);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Expense", ok ? "Expense recorded." : "Creation failed.");
        }

        refreshBudgets();
        refreshExpenses(expenseBudgetFilter.getValue());
        mainTabs.getSelectionModel().select(expenseListTab);
        clearExpenseForm();
    }

    @FXML private void handleDeleteExpense() {
        if (selectedExpense == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete this expense?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                Integer budgetId = selectedExpense.getBudgetId();
                boolean ok = expenseDAO.delete(selectedExpense.getId());
                if (ok) updateBudgetSpent(budgetId);
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Expense", ok ? "Expense deleted." : "Delete failed.");
                refreshBudgets();
                refreshExpenses(expenseBudgetFilter.getValue());
                setExpenseButtonState(false);
            }
        });
    }

    @FXML private void handleCancelExpenseForm() {
        clearExpenseForm();
        mainTabs.getSelectionModel().select(expenseListTab);
    }

    @FXML private void handleBudgetFilterChange() {
        refreshExpenses(expenseBudgetFilter.getValue());
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────

    /**
     * Recalculates and persists the spentAmount for a budget
     * whenever an expense is created, updated, or deleted.
     */
    private void updateBudgetSpent(Integer budgetId) {
        if (budgetId == null) return;
        Budget b = budgetDAO.read(budgetId);
        if (b == null) return;
        BigDecimal totalSpent = expenseDAO.findByBudgetId(budgetId).stream()
            .map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        b.setSpentAmount(totalSpent);
        budgetDAO.update(b);
    }

    private void clearBudgetForm() {
        bName.clear();
        bAmount.clear();
        bCategory.setValue(null);
        bStartDate.setValue(null);
        bEndDate.setValue(null);
        budgetEditMode = false;
        selectedBudget = null;
        budgetTable.getSelectionModel().clearSelection();
        setBudgetButtonState(false);
    }

    private void clearExpenseForm() {
        eDescription.clear();
        eAmount.clear();
        eCategory.setValue(null);
        eDate.setValue(null);
        eBudgetCombo.setValue(null);
        expenseEditMode = false;
        selectedExpense = null;
        expenseTable.getSelectionModel().clearSelection();
        setExpenseButtonState(false);
    }

    private void setBudgetButtonState(boolean enabled) {
        updateBudgetBtn.setDisable(!enabled);
        deleteBudgetBtn.setDisable(!enabled);
        viewExpensesBtn.setDisable(!enabled);
    }

    private void setExpenseButtonState(boolean enabled) {
        updateExpenseBtn.setDisable(!enabled);
        deleteExpenseBtn.setDisable(!enabled);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ── Navigation ─────────────────────────────────────────────────
    @FXML private void goInsurance()       { SceneManager.switchScene("/View/UserDashboard.fxml",            "Insurance"); }
    @FXML private void goLoans()           { SceneManager.switchScene("/View/LoanDashboard.fxml",            "Loans"); }
    @FXML private void goTransactions()    { SceneManager.switchScene("/View/TransactionDashboard.fxml",     "Transactions"); }
    @FXML private void handleBackHome()    { SceneManager.switchScene("/View/Home.fxml",                     "FinTech – Home"); }
    @FXML private void handleLogout()      { SessionManager.getInstance().logout(); SceneManager.switchScene("/View/Login.fxml", "Login"); }
}



