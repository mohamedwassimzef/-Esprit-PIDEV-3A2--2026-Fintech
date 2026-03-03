package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import tn.esprit.dao.ComplaintDAO;
import tn.esprit.dao.TransactionDAO;
import tn.esprit.entities.Complaint;
import tn.esprit.entities.Transaction;
import tn.esprit.enums.Currency;
import tn.esprit.enums.ReferenceType;
import tn.esprit.enums.TransactionStatus;
import tn.esprit.enums.TransactionType;
import tn.esprit.services.ContentModerationService;
import tn.esprit.services.Payment;
import tn.esprit.services.SessionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller for TransactionDashboard.fxml
 *
 * Tab 1 – My Transactions  : list all user transactions with summary stats
 * Tab 2 – New Transaction   : Paymee payment form → calls Payment.createPayment() → saves to DB
 * Tab 3 – My Complaints     : list complaints with status filter
 * Tab 4 – Complaint Form    : file a new complaint, optionally linked to a transaction
 *
 * Boolean pattern (same as all other modules):
 *   editMode = false → submit = INSERT
 *   editMode = true  → submit = UPDATE
 */
public class TransactionController {

    // ── Top bar ─────────────────────────────────────────────────────
    @FXML private Label welcomeLabel;

    // ── Tabs ────────────────────────────────────────────────────────
    @FXML private TabPane mainTabs;
    @FXML private Tab txListTab, txFormTab, complaintListTab, complaintFormTab;

    // ── Transaction table ────────────────────────────────────────────
    @FXML private TableView<Transaction>                  txTable;
    @FXML private TableColumn<Transaction, Integer>       txColId;
    @FXML private TableColumn<Transaction, TransactionType>   txColType;
    @FXML private TableColumn<Transaction, TransactionStatus> txColStatus;
    @FXML private TableColumn<Transaction, String>        txColCurrency, txColDescription, txColRefType;
    @FXML private TableColumn<Transaction, BigDecimal>    txColAmount;
    @FXML private TableColumn<Transaction, LocalDateTime> txColCreatedAt;
    @FXML private Button complainBtn, deleteTxBtn;

    // ── Transaction summary ──────────────────────────────────────────
    @FXML private Label totalCreditLabel, totalDebitLabel, pendingCountLabel, completedCountLabel;

    // ── Transaction form ─────────────────────────────────────────────
    @FXML private TextField  txAmount, txDescription, txFirstName, txLastName,
                             txEmail, txPhone, txOrderId, txReturnUrl, txCancelUrl;
    @FXML private ComboBox<String>          txType;
    @FXML private ComboBox<Currency>        txCurrency;
    @FXML private ComboBox<ReferenceType>   txRefType;
    @FXML private Button     submitTxBtn;
    @FXML private VBox       apiResponseBox;
    @FXML private Label      apiStatusLabel, apiPaymentUrlLabel;

    // ── Complaint table ──────────────────────────────────────────────
    @FXML private TableView<Complaint>                  complaintTable;
    @FXML private TableColumn<Complaint, Integer>       cColId;
    @FXML private TableColumn<Complaint, String>        cColSubject, cColStatus, cColResponse;
    @FXML private TableColumn<Complaint, LocalDate>     cColDate;
    @FXML private TableColumn<Complaint, LocalDateTime> cColCreatedAt;
    @FXML private Button updateComplaintBtn, deleteComplaintBtn;
    @FXML private Label  complaintCountLabel;
    @FXML private ComboBox<String> complaintStatusFilter;

    // ── Complaint form ────────────────────────────────────────────────
    @FXML private Label     complaintFormTitle;
    @FXML private TextField cSubject;
    @FXML private TextArea  cDetails;
    @FXML private DatePicker cDate;
    @FXML private ComboBox<Transaction> cTxCombo;
    @FXML private Button    submitComplaintBtn;

    // ── State ─────────────────────────────────────────────────────────
    private final TransactionDAO txDAO        = new TransactionDAO();
    private final ComplaintDAO   complaintDAO = new ComplaintDAO();

    private final ObservableList<Transaction> transactions = FXCollections.observableArrayList();
    private final ObservableList<Complaint>   complaints   = FXCollections.observableArrayList();

    private Transaction selectedTx;
    private Complaint   selectedComplaint;
    private boolean     complaintEditMode = false;

    // last Paymee payment URL so user can click to open
    private String lastPaymentUrl = null;

    // ────────────────────────────────────────────────────────────────
    //  INIT
    // ────────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        if (SessionManager.getInstance().getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUser().getName());

        setupTransactionTable();
        setupComplaintTable();
        setupFormCombos();

        refreshTransactions();
        refreshComplaints(null);

        setTxButtonState(false);
        setComplaintButtonState(false);

        // Transaction row selection
        txTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedTx = sel;
            setTxButtonState(sel != null);
        });

        // Complaint row selection
        complaintTable.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            selectedComplaint = sel;
            setComplaintButtonState(sel != null);
        });
        complaintTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && selectedComplaint != null)
                openComplaintForm(selectedComplaint);
        });
    }

    // ────────────────────────────────────────────────────────────────
    //  TABLE SETUP
    // ────────────────────────────────────────────────────────────────
    private void setupTransactionTable() {
        txColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        txColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        txColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        txColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        txColCurrency.setCellValueFactory(new PropertyValueFactory<>("currency"));
        txColRefType.setCellValueFactory(new PropertyValueFactory<>("referenceType"));

        // Type column — enum type matches column declaration
        txColType.setCellValueFactory(new PropertyValueFactory<>("type"));
        txColType.setCellFactory(col -> new TableCell<Transaction, TransactionType>() {
            @Override protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.name());
                setStyle(item == TransactionType.CREDIT
                    ? "-fx-text-fill:#33dd77;-fx-font-weight:700;"
                    : "-fx-text-fill:#ff4422;-fx-font-weight:700;");
            }
        });

        // Status column — enum type matches column declaration
        txColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        txColStatus.setCellFactory(col -> new TableCell<Transaction, TransactionStatus>() {
            @Override protected void updateItem(TransactionStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.name());
                String style;
                if (item == TransactionStatus.COMPLETED)   style = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if (item == TransactionStatus.FAILED) style = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else                                       style = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                setStyle(style);
            }
        });

        txTable.setItems(transactions);
    }

    private void setupComplaintTable() {
        cColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        cColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        cColDate.setCellValueFactory(new PropertyValueFactory<>("complaintDate"));
        cColResponse.setCellValueFactory(new PropertyValueFactory<>("response"));
        cColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        // Status with color
        cColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String style;
                if ("resolved".equals(item.toLowerCase())) style = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if ("rejected".equals(item.toLowerCase())) style = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else style = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                setStyle(style);
            }
        });
        cColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        complaintTable.setItems(complaints);
    }

    private void setupFormCombos() {
        txType.setItems(FXCollections.observableArrayList("DEBIT", "CREDIT"));
        txCurrency.setItems(FXCollections.observableArrayList(Currency.values()));
        txRefType.setItems(FXCollections.observableArrayList(ReferenceType.values()));
        txCurrency.setValue(Currency.TND);

        complaintStatusFilter.setItems(FXCollections.observableArrayList("ALL", "pending", "resolved", "rejected"));
        complaintStatusFilter.setValue("ALL");

        // Transaction display in complaint combo
        javafx.util.StringConverter<Transaction> txConverter = new javafx.util.StringConverter<>() {
            @Override public String toString(Transaction t) {
                if (t == null) return "None";
                return "TX #" + t.getId() + " – " + t.getAmount().setScale(2, RoundingMode.HALF_UP) + " TND (" + t.getType() + ")";
            }
            @Override public Transaction fromString(String s) { return null; }
        };
        cTxCombo.setConverter(txConverter);
    }

    // ────────────────────────────────────────────────────────────────
    //  REFRESH
    // ────────────────────────────────────────────────────────────────
    private void refreshTransactions() {
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        transactions.setAll(txDAO.findByUserId(userId));

        BigDecimal credit = transactions.stream()
            .filter(t -> t.getType() == TransactionType.CREDIT && t.getStatus() == TransactionStatus.COMPLETED)
            .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal debit = transactions.stream()
            .filter(t -> t.getType() == TransactionType.DEBIT && t.getStatus() == TransactionStatus.COMPLETED)
            .map(Transaction::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long pending   = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.PENDING).count();
        long completed = transactions.stream().filter(t -> t.getStatus() == TransactionStatus.COMPLETED).count();

        totalCreditLabel.setText(credit.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        totalDebitLabel.setText(debit.setScale(2, RoundingMode.HALF_UP).toPlainString() + " TND");
        pendingCountLabel.setText(String.valueOf(pending));
        completedCountLabel.setText(String.valueOf(completed));

        // Populate complaint TX combo
        cTxCombo.setItems(FXCollections.observableArrayList(transactions));
    }

    private void refreshComplaints(String statusFilter) {
        int userId = SessionManager.getInstance().getCurrentUser().getId();
        List<Complaint> all = complaintDAO.findByUserId(userId);
        if (statusFilter != null && !"ALL".equals(statusFilter)) {
            all = all.stream().filter(c -> statusFilter.equals(c.getStatus())).toList();
        }
        complaints.setAll(all);
        complaintCountLabel.setText(String.valueOf(complaints.size()));
    }

    // ────────────────────────────────────────────────────────────────
    //  TRANSACTION CRUD
    // ────────────────────────────────────────────────────────────────
    @FXML private void handleAddTransaction() {
        clearTxForm();
        mainTabs.getSelectionModel().select(txFormTab);
    }

    @FXML private void handleSubmitTransaction() {
        // ── Validation ──
        String amountStr  = txAmount.getText().trim();
        String type       = txType.getValue();
        Currency currency = txCurrency.getValue();
        String desc       = txDescription.getText().trim();
        String firstName  = txFirstName.getText().trim();
        String lastName   = txLastName.getText().trim();
        String email      = txEmail.getText().trim();
        String phone      = txPhone.getText().trim();
        String returnUrl  = txReturnUrl.getText().trim();
        String cancelUrl  = txCancelUrl.getText().trim();
        String orderId    = txOrderId.getText().trim();
        ReferenceType refType = txRefType.getValue();

        if (amountStr.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Amount is required."); return; }
        if (type == null)        { showAlert(AlertType.WARNING, "Validation", "Transaction type is required."); return; }
        if (email.isEmpty())     { showAlert(AlertType.WARNING, "Validation", "Email is required."); return; }
        if (firstName.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "First name is required."); return; }
        if (lastName.isEmpty())  { showAlert(AlertType.WARNING, "Validation", "Last name is required."); return; }
        if (phone.isEmpty())     { showAlert(AlertType.WARNING, "Validation", "Phone is required."); return; }

        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showAlert(AlertType.WARNING, "Validation", "Amount must be a positive number."); return;
        }

        // Auto-generate orderId if empty
        if (orderId.isEmpty()) orderId = "ORD-" + System.currentTimeMillis();

        int userId = SessionManager.getInstance().getCurrentUser().getId();
        /// ////////////////////////////////////////////////////////////////////
        /// /////////////////////////////////////////////////////////////////////
        /// ////////
        // ── Save transaction to DB with PENDING status ──
        Transaction tx = new Transaction(userId, amount, TransactionType.valueOf(type),
                desc.isEmpty() ? "Payment via Paymee" : desc, refType, null);
        if (currency != null) tx.setCurrency(currency);
        int txId = txDAO.createAndGetId(tx);

        if (txId == -1) {
            showAlert(AlertType.ERROR, "Error", "Failed to save transaction to database."); return;
        }

        // Disable button while API call is in progress
        submitTxBtn.setDisable(true);
        submitTxBtn.setText("Processing...");

        // ── Call Paymee API in background thread ──
        final String finalOrderId = orderId;
        final int    finalTxId    = txId;
        final String finalDesc    = desc.isEmpty() ? "Payment via Paymee" : desc;

        Thread apiThread = new Thread(() -> {
            try {
                // Capture response via redirected output
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                java.io.PrintStream oldOut = System.out;
                System.setOut(new java.io.PrintStream(baos));

                Payment.createPayment(
                    amount.doubleValue(), finalDesc,
                    firstName, lastName, email, phone,
                    returnUrl, cancelUrl,
                    "https://iridaceous-misty-vivaciously.ngrok-free.dev/webhook/paymee",
                    finalOrderId
                );

                System.out.flush();
                System.setOut(oldOut);
                String apiOutput = baos.toString();
                System.out.println("[Paymee] API response:\n" + apiOutput);

                // Parse payment_url from JSON response
                String paymentUrl = extractJsonValue(apiOutput, "payment_url");
                String token      = extractJsonValue(apiOutput, "token");
                boolean success   = apiOutput.contains("\"status\": true") || apiOutput.contains("\"status\":true");

                // Update transaction status in DB
                Transaction saved = txDAO.read(finalTxId);
                if (saved != null) {
                    saved.setStatus(success ? TransactionStatus.COMPLETED : TransactionStatus.FAILED);
                    txDAO.update(saved);
                }

                final String finalPaymentUrl = paymentUrl;
                final boolean finalSuccess   = success;

                Platform.runLater(() -> {
                    submitTxBtn.setDisable(false);
                    submitTxBtn.setText("Send Payment via Paymee");
                    refreshTransactions();

                    // Show API response panel
                    apiResponseBox.setVisible(true);
                    apiResponseBox.setManaged(true);
                    apiStatusLabel.setText(finalSuccess ? "Payment created successfully!" : "Payment creation failed.");
                    apiStatusLabel.setStyle(finalSuccess
                        ? "-fx-text-fill:#33dd77;-fx-font-weight:700;"
                        : "-fx-text-fill:#ff4422;-fx-font-weight:700;");

                    if (finalPaymentUrl != null && !finalPaymentUrl.isBlank()) {
                        lastPaymentUrl = finalPaymentUrl;
                        apiPaymentUrlLabel.setText("Click to open payment page: " + finalPaymentUrl);
                    } else {
                        apiPaymentUrlLabel.setText("");
                    }

                    if (finalSuccess) {
                        showAlert(AlertType.INFORMATION, "Payment Sent",
                            "Transaction created and sent via Paymee.\n" +
                            (finalPaymentUrl != null ? "Payment URL: " + finalPaymentUrl : ""));
                    } else {
                        showAlert(AlertType.WARNING, "Paymee Warning",
                            "Transaction saved locally but Paymee returned an error.\nCheck the API response panel.");
                    }
                });
            } catch (Exception ex) {
                System.out.println("[Paymee] Exception: " + ex.getMessage());
                Platform.runLater(() -> {
                    submitTxBtn.setDisable(false);
                    submitTxBtn.setText("Send Payment via Paymee");
                    // Mark as failed in DB
                    Transaction saved = txDAO.read(finalTxId);
                    if (saved != null) {
                        saved.setStatus(TransactionStatus.FAILED);
                        txDAO.update(saved);
                    }
                    refreshTransactions();
                    showAlert(AlertType.ERROR, "API Error",
                        "Failed to reach Paymee API:\n" + ex.getMessage());
                });
            }
        }, "paymee-api-thread");
        apiThread.setDaemon(true);
        apiThread.start();
    }

    @FXML private void handleOpenPaymentUrl() {
        if (lastPaymentUrl != null && !lastPaymentUrl.isBlank()) {
            try {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(lastPaymentUrl));
            } catch (Exception e) {
                showAlert(AlertType.INFORMATION, "Payment URL", lastPaymentUrl);
            }
        }
    }

    @FXML private void handleDeleteTransaction() {
        if (selectedTx == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete transaction #" + selectedTx.getId() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = txDAO.delete(selectedTx.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Transaction",
                    ok ? "Transaction deleted." : "Delete failed.");
                refreshTransactions();
                setTxButtonState(false);
            }
        });
    }

    @FXML private void handleCancelTxForm() {
        clearTxForm();
        mainTabs.getSelectionModel().select(txListTab);
    }

    // Called from "File Complaint" button in transaction list
    @FXML private void handleFileComplaint() {
        clearComplaintForm();
        if (selectedTx != null) cTxCombo.setValue(selectedTx);
        complaintEditMode = false;
        complaintFormTitle.setText("FILE A COMPLAINT");
        submitComplaintBtn.setText("Submit Complaint");
        cDate.setValue(LocalDate.now());
        mainTabs.getSelectionModel().select(complaintFormTab);
    }

    // ────────────────────────────────────────────────────────────────
    //  COMPLAINT CRUD
    // ────────────────────────────────────────────────────────────────
    @FXML private void handleAddComplaint() {
        clearComplaintForm();
        complaintEditMode = false;
        complaintFormTitle.setText("FILE A COMPLAINT");
        submitComplaintBtn.setText("Submit Complaint");
        cDate.setValue(LocalDate.now());
        mainTabs.getSelectionModel().select(complaintFormTab);
    }

    @FXML private void handleUpdateComplaint() {
        if (selectedComplaint != null) openComplaintForm(selectedComplaint);
    }

    private void openComplaintForm(Complaint c) {
        complaintEditMode = true;
        complaintFormTitle.setText("EDIT COMPLAINT");
        submitComplaintBtn.setText("Update Complaint");
        cSubject.setText(c.getSubject() != null ? c.getSubject() : "");
        cDetails.setText(c.getResponse() != null ? c.getResponse() : "");
        cDate.setValue(c.getComplaintDate());
        mainTabs.getSelectionModel().select(complaintFormTab);
    }

    @FXML private void handleSubmitComplaint() {
        String subject = cSubject.getText().trim();
        String details = cDetails.getText().trim();
        LocalDate date = cDate.getValue();

        // ── Basic validation (fast, before the API call) ──────────────
        if (subject.isEmpty()) { showAlert(AlertType.WARNING, "Validation", "Subject is required."); return; }
        if (date == null)      { showAlert(AlertType.WARNING, "Validation", "Complaint date is required."); return; }

        // ── Text to moderate: subject + details combined ──────────────
        String textToCheck = subject + (details.isEmpty() ? "" : "\n" + details);

        // ── Disable button & show loading state ───────────────────────
        submitComplaintBtn.setDisable(true);
        submitComplaintBtn.setText("Checking content...");

        int userId = SessionManager.getInstance().getCurrentUser().getId();
        boolean editMode  = complaintEditMode;
        Complaint editing = selectedComplaint;

        // ── Run moderation in background thread ───────────────────────
        Thread moderationThread = new Thread(() -> {
            ContentModerationService moderator = new ContentModerationService();
            ContentModerationService.ModerationResult result;

            try {
                result = moderator.analyse(textToCheck);
            } catch (Exception ex) {
                // API unreachable or misconfigured — log and allow submission
                System.out.println("[Moderation] API error (allowing submission): " + ex.getMessage());
                result = null;
            }

            final ContentModerationService.ModerationResult finalResult = result;

            Platform.runLater(() -> {
                // Re-enable the button
                submitComplaintBtn.setDisable(false);
                submitComplaintBtn.setText(editMode ? "Update Complaint" : "Submit Complaint");

                // ── If flagged → show reason and stop ──────────────────
                if (finalResult != null && finalResult.isFlagged()) {
                    showAlert(AlertType.ERROR, "Content Moderation", finalResult.getReason());
                    return;
                }

                // ── Not flagged (or API unavailable) → save ────────────
                if (editMode && editing != null) {
                    editing.setSubject(subject);
                    editing.setResponse(details.isEmpty() ? null : details);
                    editing.setComplaintDate(date);
                    boolean ok = complaintDAO.update(editing);
                    showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Complaint",
                        ok ? "Complaint updated." : "Update failed.");
                } else {
                    Complaint c = new Complaint(subject, date, userId);
                    c.setResponse(details.isEmpty() ? null : details);
                    boolean ok = complaintDAO.create(c);
                    showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Complaint",
                        ok ? "Complaint submitted successfully." : "Submission failed.");
                }

                refreshComplaints(getComplaintFilter());
                mainTabs.getSelectionModel().select(complaintListTab);
                clearComplaintForm();
            });
        }, "moderation-thread");

        moderationThread.setDaemon(true);
        moderationThread.start();
    }

    @FXML private void handleDeleteComplaint() {
        if (selectedComplaint == null) return;
        Alert confirm = new Alert(AlertType.CONFIRMATION, "Delete this complaint?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                boolean ok = complaintDAO.delete(selectedComplaint.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR, "Complaint",
                    ok ? "Complaint deleted." : "Delete failed.");
                refreshComplaints(getComplaintFilter());
                setComplaintButtonState(false);
            }
        });
    }

    @FXML private void handleCancelComplaintForm() {
        clearComplaintForm();
        mainTabs.getSelectionModel().select(complaintListTab);
    }

    @FXML private void handleComplaintFilterChange() {
        refreshComplaints(getComplaintFilter());
    }

    // ────────────────────────────────────────────────────────────────
    //  HELPERS
    // ────────────────────────────────────────────────────────────────
    private String getComplaintFilter() {
        String v = complaintStatusFilter.getValue();
        return (v == null || "ALL".equals(v)) ? null : v;
    }

    private void clearTxForm() {
        txAmount.clear(); txDescription.clear();
        txFirstName.clear(); txLastName.clear();
        txEmail.clear(); txPhone.clear(); txOrderId.clear();
        txType.setValue(null);
        txCurrency.setValue(Currency.TND);
        txRefType.setValue(null);
        txReturnUrl.setText("https://app.fintech.tn/payment/success");
        txCancelUrl.setText("https://app.fintech.tn/payment/cancel");
        apiResponseBox.setVisible(false);
        apiResponseBox.setManaged(false);
        apiStatusLabel.setText("");
        apiPaymentUrlLabel.setText("");
        lastPaymentUrl = null;
        submitTxBtn.setDisable(false);
        submitTxBtn.setText("Send Payment via Paymee");
    }

    private void clearComplaintForm() {
        cSubject.clear(); cDetails.clear(); cDate.setValue(null); cTxCombo.setValue(null);
        complaintEditMode = false;
        selectedComplaint = null;
        complaintTable.getSelectionModel().clearSelection();
        setComplaintButtonState(false);
    }

    private void setTxButtonState(boolean enabled) {
        complainBtn.setDisable(!enabled);
        deleteTxBtn.setDisable(!enabled);
    }

    private void setComplaintButtonState(boolean enabled) {
        updateComplaintBtn.setDisable(!enabled);
        deleteComplaintBtn.setDisable(!enabled);
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert a = new Alert(type, message, ButtonType.OK);
        a.setTitle(title); a.setHeaderText(null); a.showAndWait();
    }

    /**
     * Lightweight JSON value extractor (same approach as WebhookController).
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) return null;
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + search.length());
        if (colon < 0) return null;
        int i = colon + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
        if (i >= json.length()) return null;
        char ch = json.charAt(i);
        if (ch == '{' || ch == '[') return null;
        if (ch == '"') {
            int end = json.indexOf('"', i + 1);
            return (end > i + 1) ? json.substring(i + 1, end) : null;
        }
        int end = i;
        while (end < json.length() && ",}\n\r] ".indexOf(json.charAt(end)) < 0) end++;
        String val = json.substring(i, end).trim();
        return (val.isEmpty() || "null".equals(val)) ? null : val;
    }

    // ── Navigation ───────────────────────────────────────────────────
    @FXML private void goInsurance()       { SceneManager.switchScene("/View/UserDashboard.fxml",            "Insurance"); }
    @FXML private void goPersonalFinance() { SceneManager.switchScene("/View/PersonalFinanceDashboard.fxml", "Personal Finance"); }
    @FXML private void goLoans()           { SceneManager.switchScene("/View/LoanDashboard.fxml",            "Loans"); }
    @FXML private void handleBackHome()    { SceneManager.switchScene("/View/Home.fxml",                     "FinTech – Home"); }
    @FXML private void handleLogout()      { SessionManager.getInstance().logout(); SceneManager.switchScene("/View/Login.fxml", "Login"); }
}





