package controller;

import javafx.application.Platform;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;
import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsurancePackageDAO;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.RoleDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.dao.TransactionDAO;
import tn.esprit.dao.ComplaintDAO;
import tn.esprit.dao.BudgetDAO;
import tn.esprit.dao.LoanDAO;
import tn.esprit.dao.RepaymentDAO;
import tn.esprit.dao.ExpenseDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.entities.InsurancePackage;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.Role;
import tn.esprit.entities.User;
import tn.esprit.entities.Transaction;
import tn.esprit.entities.Complaint;
import tn.esprit.entities.Budget;
import tn.esprit.entities.Loan;
import tn.esprit.entities.Repayment;
import tn.esprit.entities.Expense;
import tn.esprit.enums.RequestStatus;
import tn.esprit.enums.TransactionType;
import tn.esprit.enums.TransactionStatus;
import tn.esprit.services.BoldSignService;
import tn.esprit.services.SessionManager;
import tn.esprit.services.PDFService;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * Admin Dashboard Controller.
 *  Tab 1   Contract Requests : view all, APPROVE / REJECT with one click
 *  Tab 2   Manage Packages   : full CRUD (add, edit, delete insurance packages)
 *  Tab 3   All Assets        : read-only overview of every insured asset
 */
public class AdminDashboardController {

    //    Contract Requests tab
    @FXML private TableView<ContractRequest>                  requestsTable;
    @FXML private TableColumn<ContractRequest, Integer>       reqColId, reqColUserId, reqColAssetId, reqColPackageId;
    @FXML private TableColumn<ContractRequest, Double>        reqColPremium;
    @FXML private TableColumn<ContractRequest, RequestStatus> reqColStatus;
    @FXML private TableColumn<ContractRequest, LocalDateTime> reqColCreatedAt;
    @FXML private Button        approveBtn, rejectBtn;
    @FXML private Label         pendingCountLabel, approvedCountLabel, rejectedCountLabel, signedCountLabel;
    @FXML private Label         autoRefreshLabel, lastRefreshLabel;
    @FXML private Button        manualRefreshBtn;
    @FXML private ComboBox<String> requestStatusFilter;

    /** Polls the DB every POLL_INTERVAL_SECONDS and refreshes the table if data changed. */
    private static final int POLL_INTERVAL_SECONDS = 5;
    private Timeline          autoRefreshTimeline;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    //    Manage Packages tab
    @FXML private TextField  pkgName, pkgBasePrice, pkgRiskMultiplier, pkgDurationMonths;
    @FXML private TextArea   pkgDescription, pkgCoverageDetails;
    @FXML private ComboBox<String> pkgAssetType;
    @FXML private CheckBox   pkgIsActive;
    @FXML private Button     pkgSubmitBtn, pkgClearBtn, pkgUpdateBtn, pkgDeleteBtn;

    @FXML private TableView<InsurancePackage>               adminPackagesTable;
    @FXML private TableColumn<InsurancePackage, Integer>    apkgColId;
    @FXML private TableColumn<InsurancePackage, String>     apkgColName, apkgColType;
    @FXML private TableColumn<InsurancePackage, Double>     apkgColPrice, apkgColRisk;
    @FXML private TableColumn<InsurancePackage, Integer>    apkgColDuration;
    @FXML private TableColumn<InsurancePackage, Boolean>    apkgColActive;

    //    All Assets tab                                                   
    @FXML private TableView<InsuredAsset>                   allAssetsTable;
    @FXML private TableColumn<InsuredAsset, Integer>        aColId, aColUserId;
    @FXML private TableColumn<InsuredAsset, String>         aColName, aColType, aColDescription, aColLocation;
    @FXML private TableColumn<InsuredAsset, Double>         aColValue;

    //    Manage Roles tab
    @FXML private TableView<Role>              rolesTable;
    @FXML private TableColumn<Role, Integer>   roleColId;
    @FXML private TableColumn<Role, String>    roleColName, roleColPermissions;
    @FXML private TextField  roleName;
    @FXML private CheckBox   permInsurance, permPersonalFinance, permLoans,
                             permTransactions, permComplaints, permAdmin;
    @FXML private Button     roleSubmitBtn, roleEditBtn, roleDeleteBtn;
    @FXML private Label      roleFormTitle;

    //    Tab 5 – All Transactions
    @FXML private TableView<Transaction>                     adminTxTable;
    @FXML private TableColumn<Transaction, Integer>          adminTxColId, adminTxColUserId;
    @FXML private TableColumn<Transaction, java.math.BigDecimal> adminTxColAmount;
    @FXML private TableColumn<Transaction, TransactionType>  adminTxColType;
    @FXML private TableColumn<Transaction, TransactionStatus> adminTxColStatus;
    @FXML private TableColumn<Transaction, String>           adminTxColCurrency, adminTxColDescription;
    @FXML private TableColumn<Transaction, java.time.LocalDateTime> adminTxColCreatedAt;
    @FXML private Label adminTxTotalLabel, adminTxCreditLabel, adminTxDebitLabel;

    //    Tab 6 – All Complaints
    @FXML private TableView<Complaint>                       adminComplaintsTable;
    @FXML private TableColumn<Complaint, Integer>            adminCColId, adminCColUserId;
    @FXML private TableColumn<Complaint, String>             adminCColSubject, adminCColStatus, adminCColResponse;
    @FXML private TableColumn<Complaint, java.time.LocalDate>     adminCColDate;
    @FXML private TableColumn<Complaint, java.time.LocalDateTime> adminCColCreatedAt;
    @FXML private Label adminComplaintTotalLabel, adminComplaintPendingLabel, adminComplaintResolvedLabel;

    //    Tab 7 – All Budgets
    @FXML private TableView<Budget>                          adminBudgetsTable;
    @FXML private TableColumn<Budget, Integer>               adminBColId, adminBColUserId;
    @FXML private TableColumn<Budget, String>                adminBColName, adminBColCategory;
    @FXML private TableColumn<Budget, java.math.BigDecimal>  adminBColAmount, adminBColSpent;
    @FXML private TableColumn<Budget, java.time.LocalDate>   adminBColStartDate, adminBColEndDate;
    @FXML private Label adminBudgetTotalLabel;

    //    Tab 8 – All Loans
    @FXML private TableView<Loan>                            adminLoansTable;
    @FXML private TableColumn<Loan, Integer>                 adminLColId, adminLColUserId;
    @FXML private TableColumn<Loan, java.math.BigDecimal>    adminLColAmount, adminLColInterestRate;
    @FXML private TableColumn<Loan, String>                  adminLColStatus;
    @FXML private TableColumn<Loan, java.time.LocalDate>     adminLColStartDate, adminLColEndDate;
    @FXML private TableColumn<Loan, java.time.LocalDateTime> adminLColCreatedAt;
    @FXML private Label adminLoanTotalLabel, adminLoanActiveLabel;

    //    Tab 9 – All Repayments
    @FXML private TableView<Repayment>                       adminRepaymentsTable;
    @FXML private TableColumn<Repayment, Integer>            adminRpColId, adminRpColLoanId;
    @FXML private TableColumn<Repayment, java.math.BigDecimal> adminRpColAmount, adminRpColMonthly;
    @FXML private TableColumn<Repayment, String>             adminRpColPaymentType, adminRpColStatus;
    @FXML private TableColumn<Repayment, java.time.LocalDate> adminRpColPaymentDate;
    @FXML private Label adminRepaymentTotalLabel, adminRepaymentPaidLabel, adminRepaymentPendingLabel;

    //    Tab 10 – All Expenses
    @FXML private TableView<Expense>                         adminExpensesTable;
    @FXML private TableColumn<Expense, Integer>              adminExColId, adminExColBudgetId;
    @FXML private TableColumn<Expense, String>               adminExColCategory, adminExColDescription;
    @FXML private TableColumn<Expense, java.math.BigDecimal> adminExColAmount;
    @FXML private TableColumn<Expense, java.time.LocalDate>  adminExColDate;
    @FXML private TableColumn<Expense, java.time.LocalDateTime> adminExColCreatedAt;
    @FXML private Label adminExpenseTotalLabel, adminExpenseSumLabel;

    @FXML private Label welcomeLabel;

    //    Observable lists
    private final ObservableList<ContractRequest>  requests    = FXCollections.observableArrayList();
    private final ObservableList<InsurancePackage> packages    = FXCollections.observableArrayList();
    private final ObservableList<InsuredAsset>     assets      = FXCollections.observableArrayList();
    private final ObservableList<Role>             roles       = FXCollections.observableArrayList();
    private final ObservableList<Transaction>      adminTxList      = FXCollections.observableArrayList();
    private final ObservableList<Complaint>        adminComplaintList= FXCollections.observableArrayList();
    private final ObservableList<Budget>           adminBudgetList  = FXCollections.observableArrayList();
    private final ObservableList<Loan>             adminLoanList    = FXCollections.observableArrayList();
    private final ObservableList<Repayment>        adminRepaymentList = FXCollections.observableArrayList();
    private final ObservableList<Expense>          adminExpenseList   = FXCollections.observableArrayList();

    private ContractRequest  selectedRequest;
    private InsurancePackage selectedPackage;
    private Role             selectedRole;
    private boolean pkgEditMode  = false;
    private boolean roleEditMode = false;

    //                                                                     
    //  INIT
    //                                                                     
    @FXML
    private void initialize() {
        SessionManager s = SessionManager.getInstance();
        if (s.getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + s.getCurrentUser().getName() + "  (Admin)");

        setupRequestsTable();
        setupPackagesTable();
        setupAssetsTable();
        setupRolesTable();
        setupAdminTransactionsTable();
        setupAdminComplaintsTable();
        setupAdminBudgetsTable();
        setupAdminLoansTable();
        setupAdminRepaymentsTable();
        setupAdminExpensesTable();

        pkgAssetType.setItems(FXCollections.observableArrayList("car", "home", "land"));
        requestStatusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "APPROVED", "REJECTED", "SIGNED"));

        applyInputControls();
        refreshRequestsTable();
        refreshPackagesTable();
        refreshAssetsTable();
        refreshRolesTable();
        refreshAdminTransactions();
        refreshAdminComplaints();
        refreshAdminBudgets();
        refreshAdminLoans();
        refreshAdminRepayments();
        refreshAdminExpenses();

        setPkgEditMode(false);
        startAutoRefresh();
    }

    // -- Auto-refresh (polling) --

    /**
     * Starts a JavaFX Timeline that fires every POLL_INTERVAL_SECONDS seconds on the
     * JavaFX Application Thread.  Each tick:
     *   1. Fetches fresh data from the DB in a daemon background thread.
     *   2. Back on the FX thread: compares with what is currently shown.
     *   3. Only calls setAll() if something actually changed → no visual flicker.
     *
     * The timeline is stopped when the scene's window closes so it does not
     * keep a reference to the controller after the scene is destroyed.
     */
    private void startAutoRefresh() {
        autoRefreshTimeline = new Timeline(
            new KeyFrame(Duration.seconds(POLL_INTERVAL_SECONDS), e -> pollContractRequests())
        );
        autoRefreshTimeline.setCycleCount(Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        // Stop the timer when the table's scene/window is closed
        requestsTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.setOnHidden(ev -> stopAutoRefresh());
                    }
                });
            }
        });
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            System.out.println("[AutoRefresh] Poller stopped.");
        }
    }

    /**
     * Runs the DB fetch on a background thread to avoid blocking the JavaFX thread,
     * then updates the UI on the FX thread only if data actually changed.
     */
    private void pollContractRequests() {
        Thread pollThread = new Thread(() -> {
            try {
                List<ContractRequest> fresh = new ContractRequestDAO().readAll();
                Platform.runLater(() -> {
                    // Smart diff: compare IDs+statuses to avoid unnecessary redraws
                    boolean changed = fresh.size() != requests.size();
                    if (!changed) {
                        for (int i = 0; i < fresh.size(); i++) {
                            if (fresh.get(i).getId() != requests.get(i).getId()
                                || !Objects.equals(fresh.get(i).getStatus(), requests.get(i).getStatus())) {
                                changed = true;
                                break;
                            }
                        }
                    }

                    if (changed) {
                        System.out.println("[AutoRefresh] Change detected – updating table.");
                        // Preserve selection: re-select the same row by ID after refresh
                        int selectedId = selectedRequest != null ? selectedRequest.getId() : -1;
                        requests.setAll(fresh);
                        updateStatusCountLabels(fresh);
                        // Restore selection if the row still exists
                        if (selectedId != -1) {
                            for (ContractRequest cr : requests) {
                                if (cr.getId() == selectedId) {
                                    requestsTable.getSelectionModel().select(cr);
                                    selectedRequest = cr;
                                    boolean isPending = cr.getStatus() == RequestStatus.PENDING;
                                    approveBtn.setDisable(!isPending);
                                    rejectBtn.setDisable(!isPending);
                                    break;
                                }
                            }
                        }
                        // Flash the live indicator gold briefly
                        flashLiveIndicator();
                    }

                    // Always update last-refresh timestamp
                    lastRefreshLabel.setText("last: " + LocalTime.now().format(TIME_FMT));
                });
            } catch (Exception ex) {
                System.out.println("[AutoRefresh] Poll error: " + ex.getMessage());
            }
        }, "admin-poll-thread");
        pollThread.setDaemon(true);
        pollThread.start();
    }

    /** Briefly turns the LIVE dot gold to signal a change was detected. */
    private void flashLiveIndicator() {
        autoRefreshLabel.setStyle("-fx-text-fill:#f5c800;-fx-font-size:10px;-fx-font-weight:900;-fx-letter-spacing:1.0;-fx-padding:4 6 0 0;");
        new Timeline(new KeyFrame(Duration.seconds(1), e ->
            autoRefreshLabel.setStyle("-fx-text-fill:#33dd77;-fx-font-size:10px;-fx-font-weight:900;-fx-letter-spacing:1.0;-fx-padding:4 6 0 0;")
        )).play();
    }

    /** Manual refresh button handler. */
    @FXML
    private void handleManualRefresh() {
        refreshRequestsTable();
        lastRefreshLabel.setText("last: " + LocalTime.now().format(TIME_FMT));
    }

    //                                                                     
    //  TABLE SETUP
    //                                                                     
    private void setupRequestsTable() {
        reqColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        reqColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        reqColAssetId.setCellValueFactory(new PropertyValueFactory<>("assetId"));
        reqColPackageId.setCellValueFactory(new PropertyValueFactory<>("packageId"));
        reqColPremium.setCellValueFactory(new PropertyValueFactory<>("calculatedPremium"));
        reqColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        reqColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        reqColId.setVisible(false);

        // Colour-code status
        reqColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(RequestStatus st, boolean empty) {
                super.updateItem(st, empty);
                if (empty || st == null) { setText(null); setStyle(""); return; }
                setText(st.name());
                switch (st) {
                    case APPROVED -> setStyle("-fx-text-fill:#44cc44;-fx-font-weight:700;");
                    case REJECTED -> setStyle("-fx-text-fill:#cc2200;-fx-font-weight:700;");
                    case SIGNED   -> setStyle("-fx-text-fill:#00ccff;-fx-font-weight:700;");
                    default       -> setStyle("-fx-text-fill:#f5c800;-fx-font-weight:700;");
                }
            }
        });

        requestsTable.setItems(requests);
        approveBtn.setDisable(true);
        rejectBtn.setDisable(true);

        requestsTable.setRowFactory(tv -> {
            TableRow<ContractRequest> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (!row.isEmpty()) {
                    selectedRequest = row.getItem();
                    // Only allow approve/reject on PENDING rows
                    boolean isPending = selectedRequest.getStatus() == RequestStatus.PENDING;
                    approveBtn.setDisable(!isPending);
                    rejectBtn.setDisable(!isPending);
                }
            });
            return row;
        });
    }

    private void setupPackagesTable() {
        apkgColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        apkgColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        apkgColType.setCellValueFactory(new PropertyValueFactory<>("assetType"));
        apkgColPrice.setCellValueFactory(new PropertyValueFactory<>("basePrice"));
        apkgColRisk.setCellValueFactory(new PropertyValueFactory<>("riskMultiplier"));
        apkgColDuration.setCellValueFactory(new PropertyValueFactory<>("durationMonths"));
        apkgColActive.setCellValueFactory(new PropertyValueFactory<>("active"));
        apkgColId.setVisible(false);

        // Render boolean as YES/NO with colour
        apkgColActive.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean val, boolean empty) {
                super.updateItem(val, empty);
                if (empty || val == null) { setText(null); setStyle(""); return; }
                setText(val ? "YES" : "NO");
                setStyle(val ? "-fx-text-fill:#44cc44;-fx-font-weight:700;"
                             : "-fx-text-fill:#cc2200;-fx-font-weight:700;");
            }
        });

        adminPackagesTable.setItems(packages);
        pkgUpdateBtn.setDisable(true);
        pkgDeleteBtn.setDisable(true);

        adminPackagesTable.setRowFactory(tv -> {
            TableRow<InsurancePackage> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    selectedPackage = row.getItem();
                    pkgUpdateBtn.setDisable(false);
                    pkgDeleteBtn.setDisable(false);
                }
            });
            return row;
        });
    }

    private void setupAssetsTable() {
        aColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        aColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        aColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        aColType.setCellValueFactory(new PropertyValueFactory<>("type"));
        aColValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        aColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        aColLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        aColId.setVisible(false);
        allAssetsTable.setItems(assets);
    }

    //                                                                     
    //  REFRESH
    //                                                                     
    private void refreshRequestsTable() {
        List<ContractRequest> all = new ContractRequestDAO().readAll();
        requests.setAll(all);
        selectedRequest = null;
        approveBtn.setDisable(true);
        rejectBtn.setDisable(true);
        updateStatusCountLabels(all);
    }

    private void refreshPackagesTable() {
        packages.setAll(new InsurancePackageDAO().readAll());
        selectedPackage = null;
        pkgUpdateBtn.setDisable(true);
        pkgDeleteBtn.setDisable(true);
    }

    private void refreshAssetsTable() {
        assets.setAll(new InsuredAssetDAO().readAll());
    }

    private void updateStatusCountLabels(List<ContractRequest> all) {
        long pending  = all.stream().filter(r -> r.getStatus() == RequestStatus.PENDING).count();
        long approved = all.stream().filter(r -> r.getStatus() == RequestStatus.APPROVED).count();
        long rejected = all.stream().filter(r -> r.getStatus() == RequestStatus.REJECTED).count();
        long signed   = all.stream().filter(r -> r.getStatus() == RequestStatus.SIGNED).count();
        pendingCountLabel.setText("PENDING: "  + pending);
        approvedCountLabel.setText("APPROVED: " + approved);
        rejectedCountLabel.setText("REJECTED: " + rejected);
        if (signedCountLabel != null) signedCountLabel.setText("SIGNED: " + signed);
    }

    //                                                                     
    //  TAB 1   APPROVE / REJECT
    //
    @FXML
    private void handleApproveRequest() {
        if (selectedRequest == null) return;

        // -- Snapshot the request immediately before any refresh clears selectedRequest --
        final ContractRequest req = selectedRequest;

        // 1. Update status to APPROVED in DB
        req.setStatus(RequestStatus.APPROVED);
        final ContractRequestDAO dao = new ContractRequestDAO();
        boolean ok = dao.update(req);

        if (!ok) {
            showAlert(AlertType.ERROR, "Error", "Failed to approve request.");
            return;
        }

        // 2. Refresh UI immediately so admin can see APPROVED status
        refreshRequestsTable();

        showAlert(AlertType.INFORMATION, "Approved",
                "Request #" + req.getId() + " approved.\nGenerating contract PDF and sending to user for signature...");

        // 3. BoldSign integration — runs in background thread so UI stays responsive
        Thread boldSignThread = new Thread(() -> {
            try {
                UserDAO             userDAO  = new UserDAO();
                InsuredAssetDAO     assetDAO = new InsuredAssetDAO();
                InsurancePackageDAO pkgDAO   = new InsurancePackageDAO();

                User             user  = userDAO.read(req.getUserId());
                InsuredAsset     asset = assetDAO.read(req.getAssetId());
                InsurancePackage pkg   = pkgDAO.read(req.getPackageId());

                if (user == null || asset == null || pkg == null) {
                    System.out.println("[BoldSign] Could not load user/asset/package for request #" + req.getId()
                            + "  user=" + user + "  asset=" + asset + "  pkg=" + pkg);
                    Platform.runLater(() ->
                        showAlert(AlertType.WARNING, "BoldSign Warning",
                            "Contract approved but could not load linked data.\nBoldSign email was NOT sent.")
                    );
                    return;
                }

                String approvedValue = (asset.getApprovedValue() != null
                        ? asset.getApprovedValue().toPlainString()
                        : asset.getDeclaredValue().toPlainString()) + " TND";

                String assetRef = (asset.getReference() != null && !asset.getReference().isBlank())
                        ? asset.getReference()
                        : "ASSET-" + asset.getId();

                System.out.println("[BoldSign] Sending contract to " + user.getEmail()
                        + "  asset=" + assetRef + "  pkg=" + pkg.getName()
                        + "  value=" + approvedValue);

                BoldSignService boldSign = new BoldSignService(
                        user.getName(),
                        assetRef,
                        pkg.getName(),
                        approvedValue,
                        java.time.LocalDate.now(),
                        user.getEmail()
                );

                // 4. Generate PDF + send to BoldSign → returns JSON with documentId
                String response = boldSign.sendForSignature();
                System.out.println("[BoldSign] Raw response: " + response);

                // 5. Extract documentId from BoldSign JSON response
                String documentId = extractDocumentId(response);
                if (documentId != null && !documentId.isBlank()) {
                    req.setBoldSignDocumentId(documentId);
                    dao.update(req);
                    System.out.println("[BoldSign] Document ID saved: " + documentId);
                    Platform.runLater(() ->
                        showAlert(AlertType.INFORMATION, "Contract Sent",
                            "Contract sent to " + user.getEmail() + " for signature.\nDocument ID: " + documentId)
                    );
                } else {
                    System.out.println("[BoldSign] Warning: no documentId in response: " + response);
                    Platform.runLater(() ->
                        showAlert(AlertType.WARNING, "BoldSign Warning",
                            "Contract approved and email sent, but could not store document ID.\nResponse: " + response)
                    );
                }

            } catch (Exception e) {
                System.out.println("[BoldSign] Error sending contract: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() ->
                    showAlert(AlertType.ERROR, "BoldSign Error",
                        "Contract approved but failed to send via BoldSign:\n" + e.getMessage())
                );
            }
        }, "boldsign-thread");

        boldSignThread.setDaemon(true);
        boldSignThread.start();
    }

    /** Extracts the documentId value from a BoldSign JSON response string. */
    private String extractDocumentId(String json) {
        if (json == null || json.isBlank()) return null;
        // BoldSign returns: {"documentId":"xxxx-xxxx-xxxx", ...}
        String key = "\"documentId\"";
        int idx = json.indexOf(key);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        String id = json.substring(start + 1, end);
        return id.isBlank() ? null : id;
    }

    @FXML
    private void handleRejectRequest() {
        if (selectedRequest == null) return;
        selectedRequest.setStatus(RequestStatus.REJECTED);
        boolean ok = new ContractRequestDAO().update(selectedRequest);
        showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                ok ? "Rejected" : "Error",
                ok ? "Request #" + selectedRequest.getId() + " has been REJECTED."
                   : "Failed to reject request.");
        if (ok) refreshRequestsTable();
    }

    @FXML
    private void handleRequestStatusFilter() {
        String filter = requestStatusFilter.getValue();
        if (filter == null || filter.equals("ALL")) { refreshRequestsTable(); return; }
        RequestStatus status = RequestStatus.valueOf(filter);
        List<ContractRequest> filtered = new ContractRequestDAO().findByStatus(status);
        requests.setAll(filtered);
        selectedRequest = null;
        approveBtn.setDisable(true);
        rejectBtn.setDisable(true);
    }

    //
    //  TAB 2   MANAGE PACKAGES
    //
    private void setPkgEditMode(boolean edit) {
        pkgEditMode = edit;
        pkgSubmitBtn.setText(edit ? "Update Package" : "Add Package");
    }

    @FXML
    private void handlePackageEdit() {
        if (selectedPackage == null) { showAlert(AlertType.WARNING,"No Selection","Double-click a package first."); return; }
        pkgName.setText(selectedPackage.getName());
        pkgAssetType.setValue(selectedPackage.getAssetType());
        pkgBasePrice.setText(String.valueOf(selectedPackage.getBasePrice()));
        pkgRiskMultiplier.setText(String.valueOf(selectedPackage.getRiskMultiplier()));
        pkgDurationMonths.setText(String.valueOf(selectedPackage.getDurationMonths()));
        pkgDescription.setText(selectedPackage.getDescription());
        pkgCoverageDetails.setText(selectedPackage.getCoverageDetails());
        pkgIsActive.setSelected(selectedPackage.isActive());
        setPkgEditMode(true);
    }

    @FXML
    private void handlePackageSubmit() {
        String name = pkgName.getText().trim();
        String type = pkgAssetType.getValue();
        String priceStr    = pkgBasePrice.getText().trim();
        String riskStr     = pkgRiskMultiplier.getText().trim();
        String durationStr = pkgDurationMonths.getText().trim();
        String desc        = pkgDescription.getText().trim();
        String coverage    = pkgCoverageDetails.getText().trim();
        boolean active     = pkgIsActive.isSelected();

        if (name.isEmpty() || type == null || priceStr.isEmpty() || durationStr.isEmpty()) {
            showAlert(AlertType.ERROR, "Validation", "Name, Asset Type, Base Price and Duration are required."); return;
        }
        double price, risk;
        int duration;
        try {
            price    = Double.parseDouble(priceStr);
            risk     = riskStr.isEmpty() ? 1.0 : Double.parseDouble(riskStr);
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Validation", "Price, Risk and Duration must be valid numbers."); return;
        }

        InsurancePackageDAO dao = new InsurancePackageDAO();
        boolean ok;

        if (pkgEditMode && selectedPackage != null) {
            InsurancePackage p = new InsurancePackage(selectedPackage.getId(), name, type, desc,
                    coverage, price, risk, duration, active, selectedPackage.getCreatedAt());
            ok = dao.update(p);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Updated" : "Error", ok ? "Package updated." : "Update failed.");
        } else {
            InsurancePackage p = new InsurancePackage(name, type, desc, coverage, price, risk, duration, active);
            ok = dao.create(p);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Created" : "Error", ok ? "Package added." : "Add failed.");
        }
        if (ok) { clearPkgForm(); refreshPackagesTable(); setPkgEditMode(false); }
    }

    @FXML
    private void handlePackageDelete() {
        if (selectedPackage == null) { showAlert(AlertType.WARNING,"No Selection","Double-click a package first."); return; }
        boolean ok = new InsurancePackageDAO().delete(selectedPackage.getId());
        showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                ok ? "Deleted" : "Error",
                ok ? "Package deleted." : "Delete failed (it may have linked requests).");
        if (ok) refreshPackagesTable();
    }

    @FXML
    private void handlePackageClear() { clearPkgForm(); setPkgEditMode(false); }

    private void clearPkgForm() {
        pkgName.clear(); pkgAssetType.setValue(null); pkgBasePrice.clear();
        pkgRiskMultiplier.clear(); pkgDurationMonths.clear();
        pkgDescription.clear(); pkgCoverageDetails.clear();
        pkgIsActive.setSelected(true);
        selectedPackage = null;
        pkgUpdateBtn.setDisable(true);
        pkgDeleteBtn.setDisable(true);
    }

    //                                                                     
    //  INPUT CONTROLS
    //                                                                     
    private void applyInputControls() {
        applyDecimal(pkgBasePrice);
        applyDecimal(pkgRiskMultiplier);
        applyInteger(pkgDurationMonths);
    }

    private void applyDecimal(TextField f) {
        if (f == null) return;
        UnaryOperator<TextFormatter.Change> filter =
                c -> c.getControlNewText().matches("\\d*(\\.\\d*)?") ? c : null;
        f.setTextFormatter(new TextFormatter<>(filter));
    }

    private void applyInteger(TextField f) {
        if (f == null) return;
        UnaryOperator<TextFormatter.Change> filter =
                c -> c.getControlNewText().matches("\\d*") ? c : null;
        f.setTextFormatter(new TextFormatter<>(filter));
    }

    //
    //  TAB 4 – MANAGE ROLES
    //
    private void setupRolesTable() {
        roleColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        roleColName.setCellValueFactory(new PropertyValueFactory<>("roleName"));
        roleColPermissions.setCellValueFactory(new PropertyValueFactory<>("permissions"));

        rolesTable.setItems(roles);
        roleEditBtn.setDisable(true);
        roleDeleteBtn.setDisable(true);

        rolesTable.setRowFactory(tv -> {
            TableRow<Role> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    selectedRole = row.getItem();
                    roleEditBtn.setDisable(false);
                    roleDeleteBtn.setDisable(false);
                }
            });
            return row;
        });
    }

    private void refreshRolesTable() {
        roles.setAll(new RoleDAO().readAll());
        selectedRole = null;
        roleEditBtn.setDisable(true);
        roleDeleteBtn.setDisable(true);
    }

    /** Build a comma-separated permissions string from checkbox state. */
    private String buildPermissionsJson() {
        java.util.List<String> perms = new java.util.ArrayList<>();
        if (permInsurance.isSelected())      perms.add("insurance");
        if (permPersonalFinance.isSelected()) perms.add("personal_finance");
        if (permLoans.isSelected())          perms.add("loans");
        if (permTransactions.isSelected())   perms.add("transactions");
        if (permComplaints.isSelected())     perms.add("complaints");
        if (permAdmin.isSelected())          perms.add("admin");
        return String.join(",", perms);
    }

    /** Restore checkbox state from a stored permissions string. */
    private void applyPermissionsToCheckboxes(String permissions) {
        if (permissions == null) permissions = "";
        permInsurance.setSelected(permissions.contains("insurance"));
        permPersonalFinance.setSelected(permissions.contains("personal_finance"));
        permLoans.setSelected(permissions.contains("loans"));
        permTransactions.setSelected(permissions.contains("transactions"));
        permComplaints.setSelected(permissions.contains("complaints"));
        permAdmin.setSelected(permissions.contains("admin"));
    }

    @FXML
    private void handleRoleSubmit() {
        String name = roleName.getText().trim();
        if (name.isEmpty()) {
            showAlert(AlertType.WARNING, "Validation", "Role name is required.");
            return;
        }

        String permissions = buildPermissionsJson();
        RoleDAO dao = new RoleDAO();
        boolean ok;

        if (roleEditMode && selectedRole != null) {
            selectedRole.setRoleName(name);
            selectedRole.setPermissions(permissions);
            ok = dao.update(selectedRole);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Updated" : "Error",
                    ok ? "Role \"" + name + "\" updated." : "Update failed.");
        } else {
            // Prevent duplicate role names
            boolean exists = new RoleDAO().readAll().stream()
                    .anyMatch(r -> r.getRoleName().equalsIgnoreCase(name));
            if (exists) {
                showAlert(AlertType.WARNING, "Duplicate", "A role named \"" + name + "\" already exists.");
                return;
            }
            Role newRole = new Role(name, permissions);
            ok = dao.create(newRole);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Created" : "Error",
                    ok ? "Role \"" + name + "\" added." : "Failed to add role.");
        }

        if (ok) {
            clearRoleForm();
            refreshRolesTable();
        }
    }

    @FXML
    private void handleRoleEdit() {
        if (selectedRole == null) {
            showAlert(AlertType.WARNING, "No Selection", "Double-click a role to edit it.");
            return;
        }
        roleEditMode = true;
        roleFormTitle.setText("EDIT ROLE");
        roleSubmitBtn.setText("Update Role");
        roleName.setText(selectedRole.getRoleName());
        applyPermissionsToCheckboxes(selectedRole.getPermissions());
    }

    @FXML
    private void handleRoleDelete() {
        if (selectedRole == null) {
            showAlert(AlertType.WARNING, "No Selection", "Double-click a role to delete it.");
            return;
        }
        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Delete role \"" + selectedRole.getRoleName() + "\"?\nUsers with this role will be unaffected but the role will be removed.",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                boolean ok = new RoleDAO().delete(selectedRole.getId());
                showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                        ok ? "Deleted" : "Error",
                        ok ? "Role deleted." : "Delete failed (role may be in use by users).");
                if (ok) { clearRoleForm(); refreshRolesTable(); }
            }
        });
    }

    @FXML
    private void handleRoleClear() { clearRoleForm(); }

    private void clearRoleForm() {
        roleName.clear();
        permInsurance.setSelected(false);
        permPersonalFinance.setSelected(false);
        permLoans.setSelected(false);
        permTransactions.setSelected(false);
        permComplaints.setSelected(false);
        permAdmin.setSelected(false);
        roleEditMode = false;
        selectedRole = null;
        roleFormTitle.setText("ADD NEW ROLE");
        roleSubmitBtn.setText("Add Role");
        roleEditBtn.setDisable(true);
        roleDeleteBtn.setDisable(true);
        rolesTable.getSelectionModel().clearSelection();
    }

    // -------------------------------------------------------------------------
    //  TAB 5 - ALL TRANSACTIONS
    // -------------------------------------------------------------------------
    private void setupAdminTransactionsTable() {
        adminTxColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminTxColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminTxColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        adminTxColCurrency.setCellValueFactory(new PropertyValueFactory<>("currency"));
        adminTxColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        adminTxColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        adminTxColType.setCellValueFactory(new PropertyValueFactory<>("type"));
        adminTxColType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(TransactionType item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.name());
                setStyle(item == TransactionType.CREDIT
                    ? "-fx-text-fill:#33dd77;-fx-font-weight:700;"
                    : "-fx-text-fill:#ff4422;-fx-font-weight:700;");
            }
        });

        adminTxColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        adminTxColStatus.setCellFactory(col -> new TableCell<>() {
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

        adminTxTable.setItems(adminTxList);
    }

    @FXML
    private void refreshAdminTransactions() {
        java.util.List<Transaction> all = new TransactionDAO().readAll();
        adminTxList.setAll(all);
        adminTxTotalLabel.setText("Total: " + all.size());

        java.math.BigDecimal credit = all.stream()
            .filter(t -> t.getType() == TransactionType.CREDIT && t.getStatus() == TransactionStatus.COMPLETED)
            .map(Transaction::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        java.math.BigDecimal debit = all.stream()
            .filter(t -> t.getType() == TransactionType.DEBIT && t.getStatus() == TransactionStatus.COMPLETED)
            .map(Transaction::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        adminTxCreditLabel.setText("Credit: " + credit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " TND");
        adminTxDebitLabel.setText("Debit: "  + debit.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()  + " TND");
    }

    // -------------------------------------------------------------------------
    //  TAB 6 - ALL COMPLAINTS
    // -------------------------------------------------------------------------
    private void setupAdminComplaintsTable() {
        adminCColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminCColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminCColSubject.setCellValueFactory(new PropertyValueFactory<>("subject"));
        adminCColResponse.setCellValueFactory(new PropertyValueFactory<>("response"));
        adminCColDate.setCellValueFactory(new PropertyValueFactory<>("complaintDate"));
        adminCColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        adminCColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        adminCColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String style;
                if (item.equalsIgnoreCase("resolved")) style = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if (item.equalsIgnoreCase("rejected")) style = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else style = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                setStyle(style);
            }
        });

        adminComplaintsTable.setItems(adminComplaintList);
    }

    @FXML
    private void refreshAdminComplaints() {
        java.util.List<Complaint> all = new ComplaintDAO().readAll();
        adminComplaintList.setAll(all);
        long pending  = all.stream().filter(c -> "pending".equalsIgnoreCase(c.getStatus())).count();
        long resolved = all.stream().filter(c -> "resolved".equalsIgnoreCase(c.getStatus())).count();
        adminComplaintTotalLabel.setText("Total: " + all.size());
        adminComplaintPendingLabel.setText("Pending: " + pending);
        adminComplaintResolvedLabel.setText("Resolved: " + resolved);
    }

    // -------------------------------------------------------------------------
    //  TAB 7 - ALL BUDGETS
    // -------------------------------------------------------------------------
    private void setupAdminBudgetsTable() {
        adminBColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminBColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminBColName.setCellValueFactory(new PropertyValueFactory<>("name"));
        adminBColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        adminBColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        adminBColSpent.setCellValueFactory(new PropertyValueFactory<>("spentAmount"));
        adminBColStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        adminBColEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        adminBudgetsTable.setItems(adminBudgetList);
    }

    @FXML
    private void refreshAdminBudgets() {
        java.util.List<Budget> all = new BudgetDAO().readAll();
        adminBudgetList.setAll(all);
        adminBudgetTotalLabel.setText("Total: " + all.size());
    }

    // -------------------------------------------------------------------------
    //  TAB 8 - ALL LOANS
    // -------------------------------------------------------------------------
    private void setupAdminLoansTable() {
        adminLColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminLColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        adminLColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        adminLColInterestRate.setCellValueFactory(new PropertyValueFactory<>("interestRate"));
        adminLColStartDate.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        adminLColEndDate.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        adminLColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));

        adminLColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        adminLColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String style;
                if (item.equalsIgnoreCase("active"))    style = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if (item.equalsIgnoreCase("closed"))    style = "-fx-text-fill:#888;-fx-font-weight:700;";
                else if (item.equalsIgnoreCase("defaulted")) style = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else style = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                setStyle(style);
            }
        });

        adminLoansTable.setItems(adminLoanList);
    }

    @FXML
    private void refreshAdminLoans() {
        java.util.List<Loan> all = new LoanDAO().readAll();
        adminLoanList.setAll(all);
        long active = all.stream().filter(l -> "active".equalsIgnoreCase(l.getStatus())).count();
        adminLoanTotalLabel.setText("Total: " + all.size());
        adminLoanActiveLabel.setText("Active: " + active);
    }

    // -------------------------------------------------------------------------
    //  TAB 9 - ALL REPAYMENTS
    // -------------------------------------------------------------------------
    private void setupAdminRepaymentsTable() {
        adminRpColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminRpColLoanId.setCellValueFactory(new PropertyValueFactory<>("loanId"));
        adminRpColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        adminRpColMonthly.setCellValueFactory(new PropertyValueFactory<>("monthlyPayment"));
        adminRpColPaymentType.setCellValueFactory(new PropertyValueFactory<>("paymentType"));
        adminRpColPaymentDate.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));

        adminRpColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        adminRpColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item.toUpperCase());
                String style;
                if (item.equalsIgnoreCase("paid"))       style = "-fx-text-fill:#33dd77;-fx-font-weight:700;";
                else if (item.equalsIgnoreCase("late"))  style = "-fx-text-fill:#ff4422;-fx-font-weight:700;";
                else                                     style = "-fx-text-fill:#f5c800;-fx-font-weight:700;";
                setStyle(style);
            }
        });

        adminRepaymentsTable.setItems(adminRepaymentList);
    }

    @FXML
    private void refreshAdminRepayments() {
        java.util.List<Repayment> all = new RepaymentDAO().readAll();
        adminRepaymentList.setAll(all);
        long paid    = all.stream().filter(r -> "paid".equalsIgnoreCase(r.getStatus())).count();
        long pending = all.stream().filter(r -> "pending".equalsIgnoreCase(r.getStatus())).count();
        adminRepaymentTotalLabel.setText("Total: " + all.size());
        adminRepaymentPaidLabel.setText("Paid: " + paid);
        adminRepaymentPendingLabel.setText("Pending: " + pending);
    }

    // -------------------------------------------------------------------------
    //  TAB 10 - ALL EXPENSES
    // -------------------------------------------------------------------------
    private void setupAdminExpensesTable() {
        adminExColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        adminExColBudgetId.setCellValueFactory(new PropertyValueFactory<>("budgetId"));
        adminExColCategory.setCellValueFactory(new PropertyValueFactory<>("category"));
        adminExColAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        adminExColDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        adminExColDate.setCellValueFactory(new PropertyValueFactory<>("expenseDate"));
        adminExColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        adminExpensesTable.setItems(adminExpenseList);
    }

    @FXML
    private void refreshAdminExpenses() {
        java.util.List<Expense> all = new ExpenseDAO().readAll();
        adminExpenseList.setAll(all);
        java.math.BigDecimal sum = all.stream()
            .map(Expense::getAmount)
            .filter(a -> a != null)
            .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        adminExpenseTotalLabel.setText("Total: " + all.size());
        adminExpenseSumLabel.setText("Sum: " + sum.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + " TND");
    }

    //
    //  NAVIGATION
    //                                                                     
    @FXML private void handleLogout() { SessionManager.getInstance().logout(); SceneManager.switchScene("/View/Login.fxml","Login"); }
    @FXML private void handleQuit()   { Platform.exit(); }

    //                                                                     
    //  ALERT HELPER
    //                                                                     
    private void showAlert(AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}

