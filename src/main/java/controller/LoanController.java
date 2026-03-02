package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.dao.LoanDAO;
import tn.esprit.dao.RepaymentDAO;
import tn.esprit.entities.Loan;
import tn.esprit.entities.Repayment;
import tn.esprit.services.SessionManager;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Controller for LoanDashboard.fxml
 *
 * Manages two entities:
 *  - Loan      : CRUD with auto loan calculator (monthly payment, total cost, interest)
 *  - Repayment : CRUD linked to a loan
 *
 * Boolean pattern (same as InsuredAsset / Personal Finance modules):
 *   editMode = false → submit performs INSERT
 *   editMode = true  → submit performs UPDATE
 */
public class LoanController {

    // ── Top bar ─────────────────────────────────────────────────
    @FXML private Label welcomeLabel;

    // ── Tabs ────────────────────────────────────────────────────
    @FXML private TabPane mainTabs;
    @FXML private Tab loanListTab, loanFormTab, repaymentListTab, repaymentFormTab;

    // ── Loan table ───────────────────────────────────────────────
    @FXML private TableView<Loan>                  loanTable;
    @FXML private TableColumn<Loan, Integer>       lColId;
    @FXML private TableColumn<Loan, BigDecimal>    lColAmount, lColInterestRate, lColMonthly, lColTotalCost;
    @FXML private TableColumn<Loan, String>        lColStatus;
    @FXML private TableColumn<Loan, LocalDate>     lColStartDate, lColEndDate;
    @FXML private TableColumn<Loan, LocalDateTime> lColCreatedAt;
    @FXML private Button updateLoanBtn, deleteLoanBtn, viewRepaymentsBtn;

    // ── Loan summary strip ───────────────────────────────────────
    @FXML private Label totalBorrowedLabel, totalRepaidLabel, outstandingLabel, activeLoansLabel;

    // ── Loan form ────────────────────────────────────────────────
    @FXML private Label     loanFormTitle;
    @FXML private TextField lAmount, lInterestRate;
    @FXML private DatePicker lStartDate, lEndDate;
    @FXML private ComboBox<String> lStatus;
    @FXML private Button    submitLoanBtn;
    // Calculated labels
    @FXML private Label calcDuration, calcMonthly, calcInterest, calcTotal;

    // ── Repayment table ──────────────────────────────────────────
    @FXML private TableView<Repayment>              repaymentTable;
    @FXML private TableColumn<Repayment, Integer>   rColId;
    @FXML private TableColumn<Repayment, String>    rColLoan, rColPaymentType, rColStatus;
    @FXML private TableColumn<Repayment, BigDecimal> rColAmount, rColMonthly;
    @FXML private TableColumn<Repayment, LocalDate> rColDate;
    @FXML private Button updateRepaymentBtn, deleteRepaymentBtn;
    @FXML private Label  repaymentTotalLabel;

    // ── Repayment form ───────────────────────────────────────────
    @FXML private Label     repaymentFormTitle;
    @FXML private ComboBox<Loan>   rLoanCombo;
    @FXML private ComboBox<String> rPaymentType, rStatus;
    @FXML private TextField rAmount, rMonthlyPayment;
    @FXML private DatePicker rDate;
    @FXML private Button    submitRepaymentBtn;

    // ── Filter ──────────────────────────────────────────────────
    @FXML private ComboBox<Loan> repaymentLoanFilter;

    // ── State ───────────────────────────────────────────────────
    private final LoanDAO      loanDAO      = new LoanDAO();
    private final RepaymentDAO repaymentDAO = new RepaymentDAO();

    private final ObservableList<Loan>      loans      = FXCollections.observableArrayList();
    private final ObservableList<Repayment> repayments = FXCollections.observableArrayList();

    private Loan      selectedLoan;
    private Repayment selectedRepayment;
    private boolean   loanEditMode      = false;
    private boolean   repaymentEditMode = false;

    private static final List<String> LOAN_STATUSES      = List.of("active", "closed", "defaulted");
    private static final List<String> PAYMENT_TYPES      = List.of("monthly", "early", "partial", "full");
    private static final List<String> REPAYMENT_STATUSES = List.of("pending", "paid", "late");

    // ────────────────────────────────────────────────────────────
    //  INIT
    // ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getName());

        setupLoanTable();
        setupRepaymentTable();
        setupFormCombos();

        refreshLoans();
        refreshRepayments(null);

        setLoanButtonState(false);
        setRepaymentButtonState(false);

        // Live loan calculator – recompute whenever amount/rate/dates change
        lAmount.textProperty().addListener((o, old, v) -> recalcLoan());
        lInterestRate.textProperty().addListener((o, old, v) -> recalcLoan());

        // Loan table selection
        loanTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedLoan = sel;
            setLoanButtonState(sel != null);
        });
        loanTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedLoan != null) openLoanForm(selectedLoan);
        });

        // Repayment table selection
        repaymentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedRepayment = sel;
            setRepaymentButtonState(sel != null);
        });
        repaymentTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedRepayment != null) openRepaymentForm(selectedRepayment);
        });

        // Auto-fill monthly when loan selected in repayment form
        rLoanCombo.valueProperty().addListener((obs, old, loan) -> {
            if (loan != null) {
                BigDecimal monthly = calcMonthlyPayment(loan.getAmount(), loan.getInterestRate(), loan.getStartDate(), loan.getEndDate());
                rMonthlyPayment.setText(monthly.setScale(2, RoundingMode.HALF_UP).toPlainString());
            } else {
                rMonthlyPayment.clear();
            }
        });
    }

    // ────────────────────────────────────────────────────────────
    //  TABLE SETUP
    // ────────────────────────────────────────────────────────────
    private void setupLoanTable() {
        lColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        lColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        lColInterestRate.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        lColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        lColStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        lColEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        lColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Calculated monthly payment column
        lColMonthly.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Loan l = getTableRow().getItem();
                BigDecimal m = calcMonthlyPayment(l.getAmount(), l.getInterestRate(), l.getStartDate(), l.getEndDate());
                setText(m.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
            }
        });

        // Total cost column
        lColTotalCost.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Loan l = getTableRow().getItem();
                long months = ChronoUnit.MONTHS.between(l.getStartDate(), l.getEndDate());
                BigDecimal m = calcMonthlyPayment(l.getAmount(), l.getInterestRate(), l.getStartDate(), l.getEndDate());
                BigDecimal total = m.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
                setText(total.toPlainString() + " TND");
            }
        });

        // Status color cell
        lColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String loanStyle;
                if ("active".equals(item.toLowerCase()))    loanStyle = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if ("closed".equals(item.toLowerCase()))    loanStyle = "-fx-text-fill:#888888;-fx-font-weight:700;";
                else if ("defaulted".equals(item.toLowerCase())) loanStyle = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else loanStyle = "-fx-text-fill:#f5c800;";
                setStyle(loanStyle);
            }
        });

        loanTable.setItems(loans);
    }

    private void setupRepaymentTable() {
        rColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        rColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        rColMonthly.setCellValueFactory(new PropertyValueFactory<>("monthlyPayment"));
        rColPaymentType.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        rColDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        // Loan display column
        rColLoan.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Repayment r = getTableRow().getItem();
                Loan loan = loans.stream().filter(l -> l.getId() == r.getLoanId()).findFirst().orElse(null);
                setText(loan != null ? "Loan #" + loan.getId() + " (" + loan.getAmount().setScale(0, RoundingMode.HALF_UP) + " TND)" : "Loan #" + r.getLoanId());
            }
        });

        // Status color cell
        rColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String repStyle;
                if ("paid".equals(item.toLowerCase()))    repStyle = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if ("pending".equals(item.toLowerCase())) repStyle = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                else if ("late".equals(item.toLowerCase()))    repStyle = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else repStyle = "";
                setStyle(repStyle);
            }
        });
        rColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        repaymentTable.setItems(repayments);
    }

    private void setupFormCombos() {
        lStatus.setItems(FXCollections.observableArrayList(LOAN_STATUSES));
        rPaymentType.setItems(FXCollections.observableArrayList(PAYMENT_TYPES));
        rStatus.setItems(FXCollections.observableArrayList(REPAYMENT_STATUSES));
    }

    // ────────────────────────────────────────────────────────────
    //  REFRESH
    // ────────────────────────────────────────────────────────────
    private void refreshLoans() {
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        loans.setAll(loanDAO.findByUserId(userId));

        BigDecimal totalBorrowed = loans.stream().map(Loan::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long active = loans.stream().filter(l -> "active".equals(l.getStatus())).count();

        // Total repaid = sum of all paid repayments
        BigDecimal totalRepaid = repaymentDAO.readAll().stream()
            .filter(r -> "paid".equals(r.getStatus()) && loans.stream().anyMatch(l -> l.getId() == r.getLoanId()))
            .map(Repayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal outstanding = totalBorrowed.subtract(totalRepaid).max(BigDecimal.ZERO);

        totalBorrowedLabel.setText(totalBorrowed.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        totalRepaidLabel.setText(totalRepaid.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        outstandingLabel.setText(outstanding.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        activeLoansLabel.setText(String.valueOf(active));

        // Populate combos
        javafx.util.StringConverter<Loan> loanConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Loan l) {
                return l == null ? "" : "Loan #" + l.getId() + " – " + l.getAmount().setScale(0, RoundingMode.HALF_UP) + " TND (" + l.getStatus() + ")";
            }
            @Override public Loan fromString(String s) { return null; }
        };
        ObservableList<Loan> loanOptions = FXCollections.observableArrayList(loans);
        repaymentLoanFilter.setItems(loanOptions);
        rLoanCombo.setItems(loanOptions);
        repaymentLoanFilter.setConverter(loanConverter);
        rLoanCombo.setConverter(loanConverter);
    }

    private void refreshRepayments(Loan filterLoan) {
        List<Repayment> list = filterLoan != null
            ? repaymentDAO.findByLoanId(filterLoan.getId())
            : repaymentDAO.readAll().stream()
                .filter(r -> loans.stream().anyMatch(l -> l.getId() == r.getLoanId()))
                .toList();
        repayments.setAll(list);

        BigDecimal total = list.stream().filter(r -> "paid".equals(r.getStatus()))
            .map(Repayment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        repaymentTotalLabel.setText(total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
    }

    // ────────────────────────────────────────────────────────────
    //  LOAN CRUD
    // ────────────────────────────────────────────────────────────
    @FXML private void handleAddLoan() {
        clearLoanForm();
        loanEditMode = false;
        loanFormTitle.setText("NEW LOAN APPLICATION");
        submitLoanBtn.setText("Apply for Loan");
        lStatus.setValue("active");
        mainTabs.getSelectionModel().select(loanFormTab);
    }

    @FXML private void handleUpdateLoan() {
        if (selectedLoan != null) openLoanForm(selectedLoan);
    }

    private void openLoanForm(Loan l) {
        loanEditMode = true;
        loanFormTitle.setText("EDIT LOAN");
        submitLoanBtn.setText("Update Loan");
        lAmount.setText(l.getAmount().toPlainString());
        lInterestRate.setText(l.getInterestRate().toPlainString());
        lStartDate.setValue(l.getStartDate());
        lEndDate.setValue(l.getEndDate());
        lStatus.setValue(l.getStatus());
        recalcLoan();
        mainTabs.getSelectionModel().select(loanFormTab);
    }

    @FXML private void handleLoanDateChange() { recalcLoan(); }

    @FXML private void handleSubmitLoan() {
        String amountStr = lAmount.getText().trim();
        String rateStr   = lInterestRate.getText().trim();
        LocalDate start  = lStartDate.getValue();
        LocalDate end    = lEndDate.getValue();
        String status    = lStatus.getValue();

        if (amountStr.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Loan amount is required."); return; }
        if (rateStr.isEmpty())   { showAlert(AlertType.WARNING, "Validation", "Interest rate is required."); return; }
        if (start == null || end == null) { showAlert(AlertType.WARNING, "Validation", "Start and end dates are required."); return; }
        if (!end.isAfter(start)) { showAlert(AlertType.WARNING, "Validation", "End date must be after start date."); return; }
        if (status == null) { showAlert(AlertType.WARNING, "Validation", "Please select a status."); return; }

        BigDecimal amount, rate;
        try {
            amount = new BigDecimal(amountStr); if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { showAlert(AlertType.WARNING, "Validation", "Amount must be a positive number."); return; }
        try {
            rate = new BigDecimal(rateStr); if (rate.compareTo(BigDecimal.ZERO) < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { showAlert(AlertType.WARNING, "Validation", "Interest rate must be a non-negative number."); return; }

        int userId = SessionManager.getInstance().getCurrentUser().getId();

        if (loanEditMode && selectedLoan != null) {
            selectedLoan.setAmount(amount);
            selectedLoan.setInterestRate(rate);
            selectedLoan.setStartDate(start);
            selectedLoan.setEndDate(end);
            selectedLoan.setStatus(status);
            selectedLoan.setUserId(userId);
            boolean ok = loanDAO.update(selectedLoan);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Loan", ok ? "Loan updated." : "Update failed.");
        } else {
            Loan l = new Loan(userId, amount, rate, start, end);
            l.setStatus(status);
            boolean ok = loanDAO.create(l);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Loan", ok ? "Loan application submitted." : "Creation failed.");
        }

        refreshLoans();
        refreshRepayments(repaymentLoanFilter.getValue());
        mainTabs.getSelectionModel().select(loanListTab);
        clearLoanForm();
    }

    @FXML private void handleDeleteLoan() {
        if (selectedLoan == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION,
            "Delete Loan #" + selectedLoan.getId() + "?\nAll associated repayments will also be deleted.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                // Delete associated repayments first
                repaymentDAO.findByLoanId(selectedLoan.getId()).forEach(rep -> repaymentDAO.delete(rep.getId()));
                boolean ok = loanDAO.delete(selectedLoan.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Loan", ok ? "Loan deleted." : "Delete failed.");
                refreshLoans();
                refreshRepayments(null);
                setLoanButtonState(false);
            }
        });
    }

    @FXML private void handleCancelLoanForm() {
        clearLoanForm();
        mainTabs.getSelectionModel().select(loanListTab);
    }

    @FXML private void handleViewRepayments() {
        if (selectedLoan != null) {
            repaymentLoanFilter.setValue(selectedLoan);
            refreshRepayments(selectedLoan);
            mainTabs.getSelectionModel().select(repaymentListTab);
        }
    }

    // ────────────────────────────────────────────────────────────
    //  REPAYMENT CRUD
    // ────────────────────────────────────────────────────────────
    @FXML private void handleAddRepayment() {
        clearRepaymentForm();
        repaymentEditMode = false;
        repaymentFormTitle.setText("NEW REPAYMENT");
        submitRepaymentBtn.setText("Save Repayment");
        rStatus.setValue("pending");
        rDate.setValue(LocalDate.now());
        mainTabs.getSelectionModel().select(repaymentFormTab);
    }

    @FXML private void handleUpdateRepayment() {
        if (selectedRepayment != null) openRepaymentForm(selectedRepayment);
    }

    private void openRepaymentForm(Repayment r) {
        repaymentEditMode = true;
        repaymentFormTitle.setText("EDIT REPAYMENT");
        submitRepaymentBtn.setText("Update Repayment");
        loans.stream().filter(l -> l.getId() == r.getLoanId()).findFirst().ifPresent(rLoanCombo::setValue);
        rAmount.setText(r.getAmount().toPlainString());
        if (r.getMonthlyPayment() != null) rMonthlyPayment.setText(r.getMonthlyPayment().toPlainString());
        rPaymentType.setValue(r.getPaymentType());
        rStatus.setValue(r.getStatus());
        rDate.setValue(r.getPaymentDate());
        mainTabs.getSelectionModel().select(repaymentFormTab);
    }

    @FXML private void handleSubmitRepayment() {
        Loan linkedLoan  = rLoanCombo.getValue();
        String amountStr = rAmount.getText().trim();
        String type      = rPaymentType.getValue();
        String status    = rStatus.getValue();
        LocalDate date   = rDate.getValue();

        if (linkedLoan == null) { showAlert(AlertType.WARNING, "Validation", "Please select a loan."); return; }
        if (amountStr.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Amount is required."); return; }
        if (type == null)   { showAlert(AlertType.WARNING, "Validation", "Please select a payment type."); return; }
        if (status == null) { showAlert(AlertType.WARNING, "Validation", "Please select a status."); return; }
        if (date == null)   { showAlert(AlertType.WARNING, "Validation", "Payment date is required."); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr); if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) { showAlert(AlertType.WARNING, "Validation", "Amount must be a positive number."); return; }

        BigDecimal monthly = calcMonthlyPayment(linkedLoan.getAmount(), linkedLoan.getInterestRate(),
            linkedLoan.getStartDate(), linkedLoan.getEndDate());

        if (repaymentEditMode && selectedRepayment != null) {
            selectedRepayment.setLoanId(linkedLoan.getId());
            selectedRepayment.setAmount(amount);
            selectedRepayment.setMonthlyPayment(monthly);
            selectedRepayment.setPaymentType(type);
            selectedRepayment.setStatus(status);
            selectedRepayment.setPaymentDate(date);
            boolean ok = repaymentDAO.update(selectedRepayment);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Repayment", ok ? "Repayment updated." : "Update failed.");
        } else {
            Repayment rep = new Repayment(linkedLoan.getId(), amount, date, type);
            rep.setStatus(status);
            rep.setMonthlyPayment(monthly);
            boolean ok = repaymentDAO.create(rep);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Repayment", ok ? "Repayment recorded." : "Creation failed.");
        }

        refreshLoans();
        refreshRepayments(repaymentLoanFilter.getValue());
        mainTabs.getSelectionModel().select(repaymentListTab);
        clearRepaymentForm();
    }

    @FXML private void handleDeleteRepayment() {
        if (selectedRepayment == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete this repayment?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = repaymentDAO.delete(selectedRepayment.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Repayment", ok ? "Repayment deleted." : "Delete failed.");
                refreshLoans();
                refreshRepayments(repaymentLoanFilter.getValue());
                setRepaymentButtonState(false);
            }
        });
    }

    @FXML private void handleCancelRepaymentForm() {
        clearRepaymentForm();
        mainTabs.getSelectionModel().select(repaymentListTab);
    }

    @FXML private void handleLoanFilterChange() {
        refreshRepayments(repaymentLoanFilter.getValue());
    }

    // ────────────────────────────────────────────────────────────
    //  LOAN CALCULATOR
    // ────────────────────────────────────────────────────────────

    /**
     * Recalculates the loan summary panel in real-time as the user types.
     * Uses standard amortization formula:
     *   M = P * [r(1+r)^n] / [(1+r)^n - 1]
     *   P = principal, r = monthly rate, n = number of months
     */
    private void recalcLoan() {
        try {
            BigDecimal principal = new BigDecimal(lAmount.getText().trim());
            BigDecimal annualRate = new BigDecimal(lInterestRate.getText().trim());
            LocalDate start = lStartDate.getValue();
            LocalDate end = lEndDate.getValue();

            if (start == null || end == null || !end.isAfter(start)) {
                clearCalcLabels(); return;
            }
            long months = ChronoUnit.MONTHS.between(start, end);
            if (months <= 0) { clearCalcLabels(); return; }

            BigDecimal monthly = calcMonthlyPayment(principal, annualRate, start, end);
            BigDecimal totalCost = monthly.multiply(BigDecimal.valueOf(months)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalInterest = totalCost.subtract(principal).setScale(2, RoundingMode.HALF_UP);

            calcDuration.setText(months + " months");
            calcMonthly.setText(monthly.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
            calcInterest.setText(totalInterest.toPlainString() + " TND");
            calcTotal.setText(totalCost.toPlainString() + " TND");
        } catch (Exception e) {
            clearCalcLabels();
        }
    }

    private void clearCalcLabels() {
        calcDuration.setText("—");
        calcMonthly.setText("—");
        calcInterest.setText("—");
        calcTotal.setText("—");
    }

    /**
     * Standard amortization monthly payment formula.
     */
    private BigDecimal calcMonthlyPayment(BigDecimal principal, BigDecimal annualRate,
                                          LocalDate start, LocalDate end) {
        if (principal == null || annualRate == null || start == null || end == null) return BigDecimal.ZERO;
        long n = ChronoUnit.MONTHS.between(start, end);
        if (n <= 0) return BigDecimal.ZERO;
        if (annualRate.compareTo(BigDecimal.ZERO) == 0)
            return principal.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);

        double r = annualRate.doubleValue() / 100.0 / 12.0;
        double p = principal.doubleValue();
        double monthly = p * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        return BigDecimal.valueOf(monthly).setScale(2, RoundingMode.HALF_UP);
    }

    // ────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────
    private void clearLoanForm() {
        lAmount.clear(); lInterestRate.clear();
        lStartDate.setValue(null); lEndDate.setValue(null);
        lStatus.setValue(null);
        clearCalcLabels();
        loanEditMode = false;
        selectedLoan = null;
        loanTable.getSelectionModel().clearSelection();
        setLoanButtonState(false);
    }

    private void clearRepaymentForm() {
        rLoanCombo.setValue(null); rAmount.clear();
        rMonthlyPayment.clear(); rPaymentType.setValue(null);
        rStatus.setValue(null); rDate.setValue(null);
        repaymentEditMode = false;
        selectedRepayment = null;
        repaymentTable.getSelectionModel().clearSelection();
        setRepaymentButtonState(false);
    }

    private void setLoanButtonState(boolean enabled) {
        updateLoanBtn.setDisable(!enabled);
        deleteLoanBtn.setDisable(!enabled);
        viewRepaymentsBtn.setDisable(!enabled);
    }

    private void setRepaymentButtonState(boolean enabled) {
        updateRepaymentBtn.setDisable(!enabled);
        deleteRepaymentBtn.setDisable(!enabled);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    // ── Navigation ──────────────────────────────────────────────
    @FXML private void goInsurance()       { SceneManager.switchScene("/View/UserDashboard.fxml",           "Insurance"); }
    @FXML private void goPersonalFinance() { SceneManager.switchScene("/View/PersonalFinanceDashboard.fxml","Personal Finance"); }
    @FXML private void goTransactions()    { SceneManager.switchScene("/View/TransactionDashboard.fxml",    "Transactions"); }
    @FXML private void handleBackHome()    { SceneManager.switchScene("/View/Home.fxml",                    "FinTech – Home"); }
    @FXML private void handleLogout()      { SessionManager.getInstance().logout(); SceneManager.switchScene("/View/Login.fxml", "Login"); }
}




