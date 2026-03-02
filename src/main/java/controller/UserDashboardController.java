package controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsurancePackageDAO;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.entities.InsurancePackage;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.enums.RequestStatus;
import tn.esprit.services.SessionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * User Dashboard Controller.
 *  Tab 1 - Asset Form       : add / edit assets
 *  Tab 2 - My Assets        : list with update / delete
 *  Tab 3 - Insurance Packages: browse catalogue, filter by type
 *  Tab 4 - Request Contract  : pick asset + package - auto-calculate premium
 *  Tab 5 - My Requests       : view own contract requests and their status
 */
public class UserDashboardController {

    // -- Asset Form ------------------------------------------------------
    @FXML private Button      SubAsset;
    @FXML private TextField   AssReference, AssDesc, AssDeclaredValue;
    @FXML private ComboBox<String> typeCombo;
    @FXML private DatePicker  AssCreated, AssManufDate;
    @FXML private ComboBox<String> cityCombo, areaCombo;

    // -- My Assets table -------------------------------------------------
    @FXML private TableView<InsuredAsset>                        assetsTable;
    @FXML private TableColumn<InsuredAsset, Integer>             colId;
    @FXML private TableColumn<InsuredAsset, String>              colReference, colType, colDescription, colLocation;
    @FXML private TableColumn<InsuredAsset, BigDecimal>          colDeclaredValue;
    @FXML private TableColumn<InsuredAsset, LocalDate>           colManufactureDate;
    @FXML private TableColumn<InsuredAsset, LocalDateTime>       colCreatedAt;
    @FXML private Button updateAssetButton, deleteAssetButton;

    // -- Browse Packages (card UI) -------------------------------------------
    @FXML private VBox         carCard, homeCard;
    @FXML private ScrollPane   packagesScrollPane;
    @FXML private VBox         packagesListBox;

    // -- Request Contract form -------------------------------------------
    @FXML private ComboBox<InsuredAsset>      contractAssetCombo;
    @FXML private ComboBox<InsurancePackage>  contractPackageCombo;
    @FXML private TextField  reqPremiumField, reqDurationField, reqEndDateField;
    @FXML private DatePicker reqStartDate;

    // -- My Requests table -----------------------------------------------
    @FXML private TableView<ContractRequest>                requestsTable;
    @FXML private TableColumn<ContractRequest, Integer>     reqColId, reqColAssetId, reqColPackageId;
    @FXML private TableColumn<ContractRequest, Double>      reqColPremium;
    @FXML private TableColumn<ContractRequest, RequestStatus> reqColStatus;
    @FXML private TableColumn<ContractRequest, LocalDateTime> reqColCreatedAt;

    // -- Tabs ------------------------------------------------------------
    @FXML private TabPane mainTabs;
    @FXML private Tab     assetFormTab, assetListTab, packagesTab, contractFormTab, myRequestsTab;
    @FXML private Label   welcomeLabel;

    // -- Observable lists --------------------------------------------------------
    private final ObservableList<InsuredAsset>      assets   = FXCollections.observableArrayList();
    private final ObservableList<ContractRequest>   requests = FXCollections.observableArrayList();

    private InsuredAsset      selectedAsset;
    private InsurancePackage  selectedPackage;   // set when user clicks "Select" on a card
    private String            currentPackageType = "car"; // "car" or "home"
    private boolean isEditMode = false;

    private final Map<String, List<String>> cityAreas = new HashMap<>();

    // --------------------------------------------------------------------
    //  INIT
    // --------------------------------------------------------------------
    @FXML
    private void initialize() {
        SessionManager session = SessionManager.getInstance();
        if (session.getCurrentUser() != null)
            welcomeLabel.setText("Welcome, " + session.getCurrentUser().getName());

        setupAssetTable();
        setupRequestsTable();
        setupContractAssetCombo();
        setupContractPackageCombo();
        initLocationCombos();
        applyInputControls();

        // Type dropdown: car / home
        typeCombo.setItems(FXCollections.observableArrayList("car", "home"));

        refreshAssetsTable();
        refreshRequestsTable();

        // Load car packages by default on the packages tab
        loadPackageCards("car");
        highlightTypeCard("car");

        setEditMode(false);
    }

    // --------------------------------------------------------------------
    //  TABLE SETUP HELPERS
    // --------------------------------------------------------------------
    private void setupAssetTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colReference.setCellValueFactory(new PropertyValueFactory<>("reference"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDeclaredValue.setCellValueFactory(new PropertyValueFactory<>("declaredValue"));
        colManufactureDate.setCellValueFactory(new PropertyValueFactory<>("manufactureDate"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colId.setVisible(false);
        assetsTable.setItems(assets);

        updateAssetButton.setDisable(true);
        deleteAssetButton.setDisable(true);

        assetsTable.setRowFactory(tv -> {
            TableRow<InsuredAsset> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    selectedAsset = row.getItem();
                    updateAssetButton.setDisable(false);
                    deleteAssetButton.setDisable(false);
                    System.out.println("Selected asset id: " + selectedAsset.getId());
                }
            });
            return row;
        });
    }

    // (packages now rendered as cards - no table setup needed)

    private void setupRequestsTable() {
        reqColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        reqColAssetId.setCellValueFactory(new PropertyValueFactory<>("assetId"));
        reqColPackageId.setCellValueFactory(new PropertyValueFactory<>("packageId"));
        reqColPremium.setCellValueFactory(new PropertyValueFactory<>("calculatedPremium"));
        reqColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        reqColCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        reqColId.setVisible(false);

        // Colour-code status cells
        reqColStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(RequestStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status.name());
                switch (status) {
                    case APPROVED -> setStyle("-fx-text-fill:#44cc44;-fx-font-weight:700;");
                    case REJECTED -> setStyle("-fx-text-fill:#cc2200;-fx-font-weight:700;");
                    case SIGNED   -> setStyle("-fx-text-fill:#00ccff;-fx-font-weight:700;");
                    default       -> setStyle("-fx-text-fill:#f5c800;-fx-font-weight:700;");
                }
            }
        });
        requestsTable.setItems(requests);
    }

    private void setupContractAssetCombo() {
        contractAssetCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InsuredAsset a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null : a.getReference() + "  [" + a.getType() + "]");
            }
        });
        contractAssetCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(InsuredAsset a, boolean empty) {
                super.updateItem(a, empty);
                setText(empty || a == null ? null : a.getReference() + "  [" + a.getType() + "]");
            }
        });
    }

    private void setupContractPackageCombo() {
        contractPackageCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(InsurancePackage p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getName());
            }
        });
        contractPackageCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(InsurancePackage p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null : p.getName());
            }
        });
    }

    // --------------------------------------------------------------------
    //  REFRESH helpers
    // --------------------------------------------------------------------
    private void refreshAssetsTable() {
        int userId = SessionManager.getInstance().getUserId();
        assets.setAll(new InsuredAssetDAO().findByUserId(userId));
        contractAssetCombo.setItems(FXCollections.observableArrayList(assets));
        selectedAsset = null;
        updateAssetButton.setDisable(true);
        deleteAssetButton.setDisable(true);
    }

    private void refreshRequestsTable() {
        int userId = SessionManager.getInstance().getUserId();
        requests.setAll(new ContractRequestDAO().findByUserId(userId));
    }

    // --------------------------------------------------------------------
    //  TAB 1 - ASSET FORM
    // --------------------------------------------------------------------
    private void setEditMode(boolean edit) {
        isEditMode = edit;
        SubAsset.setText(edit ? "Update Asset" : "Submit Asset");
    }

    @FXML
    private void handleSubmitButton() {
        String ref       = AssReference.getText().trim();
        String type      = typeCombo.getValue();
        String declStr   = AssDeclaredValue.getText().trim();
        String desc      = AssDesc.getText().trim();
        String loc       = buildLocationFromSelection();
        LocalDate mfDate = AssManufDate.getValue();
        int uid          = SessionManager.getInstance().getUserId();

        if (ref.isEmpty() || type == null || declStr.isEmpty()
                || loc == null || mfDate == null) {
            showAlert(AlertType.ERROR, "Validation",
                    "Reference, Type, Declared Value, City and Manufacture Date are required.");
            return;
        }
        BigDecimal declared;
        try {
            declared = new BigDecimal(declStr);
        } catch (NumberFormatException ex) {
            showAlert(AlertType.ERROR, "Validation", "Declared Value must be a number."); return;
        }

        InsuredAssetDAO dao = new InsuredAssetDAO();
        boolean ok;
        if (isEditMode) {
            InsuredAsset a = new InsuredAsset(
                    selectedAsset.getId(), ref, type, desc,
                    selectedAsset.getCreatedAt(), loc, uid,
                    declared, selectedAsset.getApprovedValue(), mfDate);
            ok = dao.update(a);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Updated" : "Error", ok ? "Asset updated." : "Update failed.");
        } else {
            InsuredAsset a = new InsuredAsset(ref, type, desc, loc, uid, declared, mfDate);
            ok = dao.create(a);
            showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                    ok ? "Created" : "Error", ok ? "Asset saved." : "Save failed.");
        }
        if (ok) { clearAssetForm(); refreshAssetsTable(); setEditMode(false);
            mainTabs.getSelectionModel().select(assetListTab); }
    }

    @FXML
    private void handleUpdateAssetButton() {
        if (selectedAsset == null) { showAlert(AlertType.WARNING, "No Selection", "Double-click an asset first."); return; }
        AssReference.setText(selectedAsset.getReference());
        typeCombo.setValue(selectedAsset.getType());
        AssDeclaredValue.setText(selectedAsset.getDeclaredValue() != null ? selectedAsset.getDeclaredValue().toPlainString() : "");
        AssDesc.setText(selectedAsset.getDescription());
        AssManufDate.setValue(selectedAsset.getManufactureDate());
        populateLocationCombos(selectedAsset.getLocation());
        setEditMode(true);
        mainTabs.getSelectionModel().select(assetFormTab);
    }

    @FXML
    private void handleDeleteAssetButton() {
        if (selectedAsset == null) { showAlert(AlertType.WARNING, "No Selection", "Double-click an asset first."); return; }
        boolean ok = new InsuredAssetDAO().delete(selectedAsset.getId());
        showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                ok ? "Deleted" : "Error", ok ? "Asset deleted." : "Delete failed.");
        if (ok) refreshAssetsTable();
    }

    // ====================================================================
    //  TAB 3 -- PACKAGE CARDS
    // ====================================================================

    @FXML
    private void handleSelectCarPackages() {
        currentPackageType = "car";
        highlightTypeCard("car");
        loadPackageCards("car");
    }

    @FXML
    private void handleSelectHomePackages() {
        currentPackageType = "home";
        highlightTypeCard("home");
        loadPackageCards("home");
    }

    private void highlightTypeCard(String type) {
        if (carCard == null || homeCard == null) return;
        if ("car".equals(type)) {
            carCard.getStyleClass().setAll("pkg-type-card-selected");
            homeCard.getStyleClass().setAll("pkg-type-card");
        } else {
            homeCard.getStyleClass().setAll("pkg-type-card-selected");
            carCard.getStyleClass().setAll("pkg-type-card");
        }
    }

    private void loadPackageCards(String assetType) {
        if (packagesListBox == null) return;
        packagesListBox.getChildren().clear();

        List<InsurancePackage> pkgList = new InsurancePackageDAO().findByAssetType(assetType);

        // Refresh the package combo in the request form with all active packages
        contractPackageCombo.setItems(FXCollections.observableArrayList(
                new InsurancePackageDAO().findActive()));

        if (pkgList.isEmpty()) {
            Label empty = new Label("No " + assetType + " packages available yet.");
            empty.setStyle("-fx-text-fill:#555555;-fx-font-size:13px;-fx-padding:20 0 0 4;");
            packagesListBox.getChildren().add(empty);
            return;
        }

        for (InsurancePackage pkg : pkgList) {
            packagesListBox.getChildren().add(buildPackageCard(pkg));
        }
    }

    private VBox buildPackageCard(InsurancePackage pkg) {
        VBox card = new VBox(10);
        card.getStyleClass().add("pkg-card");

        // Top row: name  +  price
        Label nameLabel = new Label(pkg.getName());
        nameLabel.getStyleClass().add("pkg-card-title");

        Label priceLabel = new Label(String.format("%.2f TND", pkg.getBasePrice()));
        priceLabel.getStyleClass().add("pkg-card-price");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox(8, nameLabel, spacer, priceLabel);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // Meta badges: duration  +  risk
        Label durationBadge = new Label(pkg.getDurationMonths() + " months");
        durationBadge.getStyleClass().add("pkg-card-badge");

        Label riskBadge = new Label("Risk x" + pkg.getRiskMultiplier());
        riskBadge.getStyleClass().add("pkg-card-badge");

        HBox metaRow = new HBox(8, durationBadge, riskBadge);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Description
        Label descLabel = new Label(pkg.getDescription() != null ? pkg.getDescription() : "");
        descLabel.getStyleClass().add("pkg-card-desc");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(Double.MAX_VALUE);

        // Coverage
        Label coverageHeader = new Label("COVERAGE");
        coverageHeader.setStyle("-fx-text-fill:#555555;-fx-font-size:10px;-fx-font-weight:700;");

        Label coverageLabel = new Label(pkg.getCoverageDetails() != null ? pkg.getCoverageDetails() : "");
        coverageLabel.getStyleClass().add("pkg-card-meta");
        coverageLabel.setWrapText(true);
        coverageLabel.setMaxWidth(Double.MAX_VALUE);

        // Select button
        Button selectBtn = new Button("Select This Package");
        selectBtn.getStyleClass().add("pkg-card-btn");

        boolean isSelected = selectedPackage != null && selectedPackage.getId() == pkg.getId();
        if (isSelected) {
            selectBtn.setText("Selected ->");
            selectBtn.setStyle("-fx-background-color:#44cc44;-fx-text-fill:#000;-fx-font-weight:800;"
                    + "-fx-background-radius:5;-fx-padding:8 20 8 20;");
        }
        selectBtn.setOnAction(e -> handleSelectPackageFromCard(pkg));

        card.getChildren().addAll(topRow, metaRow, descLabel, coverageHeader, coverageLabel, selectBtn);
        return card;
    }

    private void handleSelectPackageFromCard(InsurancePackage pkg) {
        selectedPackage = pkg;
        contractPackageCombo.setValue(pkg);
        recalculatePremium();
        loadPackageCards(currentPackageType); // refresh so selected card shows green
        mainTabs.getSelectionModel().select(contractFormTab);
    }

    // --------------------------------------------------------------------
    //  TAB 4 - REQUEST CONTRACT
    // --------------------------------------------------------------------
    @FXML
    private void handleAssetSelectionForRequest() { recalculatePremium(); }

    @FXML
    private void handlePackageSelectionForRequest() {
        recalculatePremium();
        // auto-fill end date when start date already set
        updateAutoEndDate();
    }

    /**
     * Premium = package.basePrice * package.riskMultiplier * (asset.value / 10000)
     * clamped to at least basePrice.
     */
    private void recalculatePremium() {
        InsuredAsset      asset = contractAssetCombo.getValue();
        InsurancePackage  pkg   = contractPackageCombo.getValue();
        if (asset == null || pkg == null) return;

        double assetVal = asset.getDeclaredValue() != null ? asset.getDeclaredValue().doubleValue() : 0.0;
        double premium  = pkg.getBasePrice() * pkg.getRiskMultiplier() * (assetVal / 10_000.0);
        if (premium < pkg.getBasePrice()) premium = pkg.getBasePrice();

        reqPremiumField.setText(String.format("%.2f", premium));
        reqDurationField.setText(pkg.getDurationMonths() + " months");
        updateAutoEndDate();
    }

    private void updateAutoEndDate() {
        InsurancePackage pkg = contractPackageCombo.getValue();
        LocalDate start      = reqStartDate.getValue();
        if (pkg == null || start == null) return;
        reqEndDateField.setText(start.plusMonths(pkg.getDurationMonths()).toString());
    }

    @FXML
    private void handleSubmitContractRequest() {
        InsuredAsset     asset = contractAssetCombo.getValue();
        InsurancePackage pkg   = contractPackageCombo.getValue();
        LocalDate        start = reqStartDate.getValue();

        if (asset == null || pkg == null) {
            showAlert(AlertType.ERROR, "Validation", "Please select both an asset and a package."); return;
        }
        if (start == null) {
            showAlert(AlertType.ERROR, "Validation", "Please choose a start date."); return;
        }

        String premText = reqPremiumField.getText().trim();
        if (premText.isEmpty()) {
            showAlert(AlertType.ERROR, "Validation", "Premium is empty - select asset & package first."); return;
        }

        double premium = Double.parseDouble(premText);
        int    userId  = SessionManager.getInstance().getUserId();

        ContractRequest req = new ContractRequest(userId, asset.getId(), pkg.getId(),
                premium, RequestStatus.PENDING);
        req.setCreatedAt(LocalDateTime.now());

        boolean ok = new ContractRequestDAO().create(req);
        showAlert(ok ? AlertType.INFORMATION : AlertType.ERROR,
                ok ? "Request Submitted" : "Error",
                ok ? "Your contract request has been submitted.\nStatus: PENDING - awaiting admin review."
                   : "Failed to submit request.");
        if (ok) {
            clearRequestForm();
            refreshRequestsTable();
            mainTabs.getSelectionModel().select(myRequestsTab);
        }
    }

    // --------------------------------------------------------------------
    //  LOCATION COMBOS
    // --------------------------------------------------------------------
    private void initLocationCombos() {
        cityAreas.put("Tunis",    Arrays.asList("Centre Ville","Lac 1","Lac 2","La Marsa","Bardo","Ariana","Ben Arous"));
        cityAreas.put("Ariana",   Arrays.asList("Ennasr","Menzah 8","Menzah 9","Raoued"));
        cityAreas.put("Ben Arous",Arrays.asList("Ezzahra","Rades","Hammam Lif","El Mourouj"));
        cityAreas.put("Manouba",  Arrays.asList("Manouba Centre","Douar Hicher","Oued Ellil"));
        cityAreas.put("Sfax",     Arrays.asList("Sakiet Ezzit","Sakiet Eddaier","Centre","Thyna","Gremda"));
        cityAreas.put("Sousse",   Arrays.asList("Khezama","Hammam Sousse","Akouda","M'saken"));
        cityAreas.put("Monastir", Arrays.asList("Monastir Ville","Jemmal","Ksar Helal","Sahline"));
        cityAreas.put("Mahdia",   Arrays.asList("Mahdia Ville","Chebba","Ksour Essef"));
        cityAreas.put("Nabeul",   Arrays.asList("Nabeul Ville","Hammamet","Kelibia","Dar Chaabane"));
        cityAreas.put("Bizerte",  Arrays.asList("Bizerte Ville","Menzel Bourguiba","Mateur","Raf Raf"));
        cityAreas.put("Beja",     Arrays.asList("Beja Ville","Medjez El Bab","Testour"));
        cityAreas.put("Jendouba", Arrays.asList("Jendouba Ville","Tabarka","Ghardimaou"));
        cityAreas.put("Kairouan", Arrays.asList("Kairouan Ville","Chebika","Oueslatia"));
        cityAreas.put("Kasserine",Arrays.asList("Kasserine Ville","Feriana","Thala"));
        cityAreas.put("Gafsa",    Arrays.asList("Gafsa Ville","Metlaoui","Redeyef"));
        cityAreas.put("Gabes",    Arrays.asList("Gabes Ville","Ghannouch","Mareth"));
        cityAreas.put("Medenine", Arrays.asList("Medenine Ville","Zarzis","Ben Guerdane","Djerba"));
        cityAreas.put("Tataouine",Arrays.asList("Tataouine Ville","Remada","Ghomrassen"));
        cityAreas.put("Tozeur",   Arrays.asList("Tozeur Ville","Nefta","Degache"));
        cityAreas.put("Kebili",   Arrays.asList("Kebili Ville","Douz","Souk Lahad"));
        cityAreas.put("Sidi Bouzid",Arrays.asList("Sidi Bouzid Ville","Regueb","Meknassy"));

        cityCombo.setItems(FXCollections.observableArrayList(cityAreas.keySet()));
        cityCombo.valueProperty().addListener((obs, old, city) -> {
            if (city != null) {
                List<String> areas = cityAreas.getOrDefault(city, List.of());
                areaCombo.setItems(FXCollections.observableArrayList(areas));
                areaCombo.setDisable(areas.isEmpty());
                areaCombo.getSelectionModel().clearSelection();
            } else { areaCombo.getItems().clear(); areaCombo.setDisable(true); }
        });
        areaCombo.setDisable(true);

        // auto-update end date when start date changes
        reqStartDate.valueProperty().addListener((obs, old, d) -> updateAutoEndDate());
    }

    private String buildLocationFromSelection() {
        String city = cityCombo != null ? cityCombo.getValue() : null;
        String area = areaCombo != null ? areaCombo.getValue() : null;
        if (city == null || city.isBlank()) return null;
        return (area == null || area.isBlank()) ? city : city + " - " + area;
    }

    private void populateLocationCombos(String location) {
        if (location == null || location.isBlank()) {
            cityCombo.getSelectionModel().clearSelection();
            areaCombo.getSelectionModel().clearSelection(); areaCombo.setDisable(true); return;
        }
        String city = location, area = null;
        if (location.contains(" - ")) { String[] p = location.split(" - ",2); city = p[0]; area = p[1]; }
        cityCombo.getSelectionModel().select(city);
        List<String> areas = cityAreas.get(city);
        if (areas != null && !areas.isEmpty()) {
            areaCombo.setItems(FXCollections.observableArrayList(areas));
            areaCombo.setDisable(false);
            if (area != null) areaCombo.getSelectionModel().select(area);
        }
    }

    // --------------------------------------------------------------------
    //  INPUT CONTROLS
    // --------------------------------------------------------------------
    private void applyInputControls() {
        applyDecimal(AssDeclaredValue);
    }

    private void applyDecimal(TextField f) {
        if (f == null) return;
        UnaryOperator<TextFormatter.Change> filter =
                c -> c.getControlNewText().matches("\\d*(\\.\\d*)?") ? c : null;
        f.setTextFormatter(new TextFormatter<>(filter));
    }

    // --------------------------------------------------------------------
    //  CLEAR helpers
    // --------------------------------------------------------------------
    private void clearAssetForm() {
        AssReference.clear(); typeCombo.setValue(null);
        AssDeclaredValue.clear(); AssDesc.clear();
        AssCreated.setValue(null); AssManufDate.setValue(null);
        cityCombo.getSelectionModel().clearSelection();
        areaCombo.getSelectionModel().clearSelection(); areaCombo.setDisable(true);
    }

    /** Yellow + button in My Assets tab */
    @FXML
    private void handleAddAssetButton() {
        clearAssetForm();
        setEditMode(false);
        mainTabs.getSelectionModel().select(assetFormTab);
    }

    /** Cancel button inside the asset form */
    @FXML
    private void handleCancelAssetForm() {
        clearAssetForm();
        setEditMode(false);
        mainTabs.getSelectionModel().select(assetListTab);
    }

    private void clearRequestForm() {
        contractAssetCombo.getSelectionModel().clearSelection();
        contractPackageCombo.getSelectionModel().clearSelection();
        reqStartDate.setValue(null);
        reqPremiumField.clear(); reqDurationField.clear(); reqEndDateField.clear();
    }

    // --------------------------------------------------------------------
    //  NAVIGATION / MENU
    // --------------------------------------------------------------------
    @FXML private void handleLogout() { SessionManager.getInstance().logout(); SceneManager.switchScene("/View/Login.fxml","Login"); }

    @FXML private void handleBackHome()    { SceneManager.switchScene("/View/Home.fxml",                     "FinTech – Home"); }
    @FXML private void goPersonalFinance() { SceneManager.switchScene("/View/PersonalFinanceDashboard.fxml", "Personal Finance"); }
    @FXML private void goLoans()           { SceneManager.switchScene("/View/LoanDashboard.fxml",            "Loans"); }
    @FXML private void goTransactions()    { SceneManager.switchScene("/View/TransactionDashboard.fxml",     "Transactions"); }
    @FXML private void handleQuit()   { Platform.exit(); }

    // --------------------------------------------------------------------
    //  ALERT HELPER
    // --------------------------------------------------------------------
    private void showAlert(AlertType type, String title, String msg) {
        Alert a = new Alert(type); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}






























