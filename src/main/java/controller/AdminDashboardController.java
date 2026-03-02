package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsurancePackageDAO;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.entities.InsurancePackage;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.enums.RequestStatus;
import tn.esprit.services.SessionManager;
import tn.esprit.services.PDFService;
import java.time.LocalDateTime;
import java.util.List;
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
    @FXML private Label         pendingCountLabel, approvedCountLabel, rejectedCountLabel;
    @FXML private ComboBox<String> requestStatusFilter;

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

    @FXML private Label welcomeLabel;

    //    Observable lists                                                 
    private final ObservableList<ContractRequest>  requests = FXCollections.observableArrayList();
    private final ObservableList<InsurancePackage> packages = FXCollections.observableArrayList();
    private final ObservableList<InsuredAsset>     assets   = FXCollections.observableArrayList();

    private ContractRequest  selectedRequest;
    private InsurancePackage selectedPackage;
    private boolean pkgEditMode = false;

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

        pkgAssetType.setItems(FXCollections.observableArrayList("car", "home", "land"));
        requestStatusFilter.setItems(FXCollections.observableArrayList("ALL", "PENDING", "APPROVED", "REJECTED"));

        applyInputControls();
        refreshRequestsTable();
        refreshPackagesTable();
        refreshAssetsTable();

        setPkgEditMode(false);
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
        pendingCountLabel.setText("PENDING: " + pending);
        approvedCountLabel.setText("APPROVED: " + approved);
        rejectedCountLabel.setText("REJECTED: " + rejected);
    }

    //                                                                     
    //  TAB 1   APPROVE / REJECT
    //                                                                     
    @FXML
    private void handleApproveRequest() {
        if (selectedRequest == null) return;
        selectedRequest.setStatus(RequestStatus.APPROVED);
        boolean ok = new ContractRequestDAO().update(selectedRequest);
        showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                ok ? "Approved" : "Error",
                ok ? "Request #" + selectedRequest.getId() + " has been APPROVED."
                   : "Failed to approve request.");


        if (ok) refreshRequestsTable();
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

