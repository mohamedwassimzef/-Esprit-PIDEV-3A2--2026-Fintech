package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.PropertyValueFactory;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.InsuredContractDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.InsuredContract;
import tn.esprit.entities.User;
import tn.esprit.enums.ContractStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class MainController {

    @FXML
    private Button SubAsset;

    @FXML
    private TextField AssName;

    @FXML
    private TextField AssType;

    @FXML
    private TextField AssValue;

    @FXML
    private TextField AssDesc;

    @FXML
    private DatePicker AssCreated;

    @FXML
    private TextField AssUser;

    // Replaced free-text location with city + area selectors
    @FXML
    private ComboBox<String> cityCombo;

    @FXML
    private ComboBox<String> areaCombo;

    @FXML
    private TextField nameField;

    @FXML
    private TableView<InsuredAsset> assetsTable;

    @FXML
    private TableColumn<InsuredAsset, Integer> colId;

    @FXML
    private TableColumn<InsuredAsset, String> colName;

    @FXML
    private TableColumn<InsuredAsset, String> colType;

    @FXML
    private TableColumn<InsuredAsset, Double> colValue;

    @FXML
    private TableColumn<InsuredAsset, String> colDescription;

    @FXML
    private TableColumn<InsuredAsset, LocalDateTime> colCreatedAt;

    @FXML
    private TableColumn<InsuredAsset, String> colLocation;

    @FXML
    private TableColumn<InsuredAsset, Integer> colUserId;

    @FXML
    private Button updateAssetButton;

    @FXML
    private Button deleteAssetButton;

    @FXML
    private TabPane mainTabs;

    @FXML
    private Tab assetFormTab;

    @FXML
    private Tab assetListTab;

    @FXML
    private Tab contractFormTab;

    @FXML
    private Tab contractListTab;

    @FXML
    private TextField contractNumberField;

    @FXML
    private TextField contractAssetIdField;

    @FXML
    private TextField contractUserIdField;

    @FXML
    private DatePicker contractStartDatePicker;

    @FXML
    private DatePicker contractEndDatePicker;

    @FXML
    private TextField contractPremiumField;

    @FXML
    private TextField contractCoverageField;

    @FXML
    private ComboBox<ContractStatus> contractStatusCombo;

    @FXML
    private TextField contractApprovedByField;

    @FXML
    private Button contractSubmitButton;

    @FXML
    private TableView<InsuredContract> contractsTable;

    @FXML
    private TableColumn<InsuredContract, Integer> contractColId;

    @FXML
    private TableColumn<InsuredContract, String> contractColNumber;

    @FXML
    private TableColumn<InsuredContract, Integer> contractColAssetId;

    @FXML
    private TableColumn<InsuredContract, Integer> contractColUserId;

    @FXML
    private TableColumn<InsuredContract, LocalDate> contractColStart;

    @FXML
    private TableColumn<InsuredContract, LocalDate> contractColEnd;

    @FXML
    private TableColumn<InsuredContract, Double> contractColPremium;

    @FXML
    private TableColumn<InsuredContract, Double> contractColCoverage;

    @FXML
    private TableColumn<InsuredContract, ContractStatus> contractColStatus;

    @FXML
    private TableColumn<InsuredContract, Integer> contractColApprovedBy;

    @FXML
    private Button contractUpdateButton;

    @FXML
    private Button contractDeleteButton;

    private final ObservableList<InsuredAsset> assets = FXCollections.observableArrayList();
    private InsuredAsset selectedAsset;
    private boolean isEditMode = false;

    private final ObservableList<InsuredContract> contracts = FXCollections.observableArrayList();
    private InsuredContract selectedContract;
    private boolean isContractEditMode = false;

    // Static city -> areas map for demo; could be loaded from DB later
    private final Map<String, List<String>> cityAreas = new HashMap<>();

    @FXML
    public void handleAdd() {
        System.out.println(nameField.getText());
    }

    @FXML
    private void initialize() {
        // Wire table columns to entity properties.
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        colCreatedAt.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));

        colId.setVisible(false);

        setAssetActionButtonsEnabled(false);

        assetsTable.setItems(assets);
        assetsTable.setRowFactory(table -> {
            TableRow<InsuredAsset> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    InsuredAsset item = row.getItem();
                    selectedAsset = item;
                    setAssetActionButtonsEnabled(true);
                    System.out.println("Selected asset id: " + item.getId());
                }
            });
            return row;
        });

        setupContractTable();
        refreshContractsTable();
        setContractEditMode(false);

        refreshAssetsTable();
        setEditMode(false);

        // Initialize city/area data and bindings
        initLocationCombos();

        applyInputControls();
    }

    private void initLocationCombos() {
        // Define Tunisian cities and some example areas/neighborhoods
        cityAreas.put("Tunis", Arrays.asList("Centre Ville", "Lac 1", "Lac 2", "La Marsa", "Bardo", "Ariana", "Ben Arous"));
        cityAreas.put("Ariana", Arrays.asList("Ennasr", "Menzah 8", "Menzah 9", "Raoued"));
        cityAreas.put("Ben Arous", Arrays.asList("Ezzahra", "Rades", "Hammam Lif", "El Mourouj"));
        cityAreas.put("Manouba", Arrays.asList("Manouba Centre", "Douar Hicher", "Oued Ellil"));

        cityAreas.put("Sfax", Arrays.asList("Sakiet Ezzit", "Sakiet Eddaier", "Centre", "Thyna", "Gremda"));
        cityAreas.put("Sousse", Arrays.asList("Khezama", "Hammam Sousse", "Akouda", "M'saken"));
        cityAreas.put("Monastir", Arrays.asList("Monastir Ville", "Jemmal", "Ksar Helal", "Sahline"));
        cityAreas.put("Mahdia", Arrays.asList("Mahdia Ville", "Chebba", "Ksour Essef"));

        cityAreas.put("Nabeul", Arrays.asList("Nabeul Ville", "Hammamet", "Kelibia", "Dar Chaabane"));
        cityAreas.put("Bizerte", Arrays.asList("Bizerte Ville", "Menzel Bourguiba", "Mateur", "Raf Raf"));
        cityAreas.put("Beja", Arrays.asList("Beja Ville", "Medjez El Bab", "Testour"));
        cityAreas.put("Jendouba", Arrays.asList("Jendouba Ville", "Tabarka", "Ghardimaou"));

        cityAreas.put("Kairouan", Arrays.asList("Kairouan Ville", "Chebika", "Oueslatia"));
        cityAreas.put("Sidi Bouzid", Arrays.asList("Sidi Bouzid Ville", "Regueb", "Meknassy"));
        cityAreas.put("Kasserine", Arrays.asList("Kasserine Ville", "Feriana", "Thala"));
        cityAreas.put("Gafsa", Arrays.asList("Gafsa Ville", "Metlaoui", "Redeyef"));

        cityAreas.put("Gabes", Arrays.asList("Gabes Ville", "Ghannouch", "Mareth"));
        cityAreas.put("Medenine", Arrays.asList("Medenine Ville", "Zarzis", "Ben Guerdane", "Djerba"));
        cityAreas.put("Tataouine", Arrays.asList("Tataouine Ville", "Remada", "Ghomrassen"));
        cityAreas.put("Tozeur", Arrays.asList("Tozeur Ville", "Nefta", "Degache"));
        cityAreas.put("Kebili", Arrays.asList("Kebili Ville", "Douz", "Souk Lahad"));

        if (cityCombo != null) {
            cityCombo.setItems(FXCollections.observableArrayList(cityAreas.keySet()));
            cityCombo.valueProperty().addListener((obs, oldCity, newCity) -> {
                if (newCity != null) {
                    List<String> areas = cityAreas.getOrDefault(newCity, List.of());
                    areaCombo.setItems(FXCollections.observableArrayList(areas));
                    areaCombo.setDisable(areas.isEmpty());
                    areaCombo.getSelectionModel().clearSelection();
                } else {
                    areaCombo.getItems().clear();
                    areaCombo.setDisable(true);
                }
            });
        }
        if (areaCombo != null) {
            areaCombo.setDisable(true); // disabled until a city is selected
        }
    }

    private String buildLocationFromSelection() {
        String city = cityCombo != null ? cityCombo.getValue() : null;
        String area = areaCombo != null ? areaCombo.getValue() : null;
        if (city == null || city.isBlank()) {
            return null;
        }
        if (area == null || area.isBlank()) {
            return city; // only city selected
        }
        return city + " - " + area;
    }

    private void populateLocationCombosFromLocationString(String location) {
        if (location == null || location.isBlank() || cityCombo == null || areaCombo == null) {
            if (cityCombo != null) cityCombo.getSelectionModel().clearSelection();
            if (areaCombo != null) {
                areaCombo.getSelectionModel().clearSelection();
                areaCombo.setDisable(true);
            }
            return;
        }
        String city = location;
        String area = null;
        if (location.contains(" - ")) {
            String[] parts = location.split(" - ", 2);
            city = parts[0];
            area = parts[1];
        }
        // Set city first to populate areas
        cityCombo.getSelectionModel().select(city);
        List<String> areas = cityAreas.get(city);
        if (areas != null && !areas.isEmpty()) {
            areaCombo.setItems(FXCollections.observableArrayList(areas));
            areaCombo.setDisable(false);
            if (area != null && areas.contains(area)) {
                areaCombo.getSelectionModel().select(area);
            }
        } else {
            areaCombo.getSelectionModel().clearSelection();
            areaCombo.setDisable(true);
        }
    }

    private void setupContractTable() {
        contractColId.setCellValueFactory(new PropertyValueFactory<>("id"));
        contractColNumber.setCellValueFactory(new PropertyValueFactory<>("contractNumber"));
        contractColAssetId.setCellValueFactory(new PropertyValueFactory<>("assetId"));
        contractColUserId.setCellValueFactory(new PropertyValueFactory<>("userId"));
        contractColStart.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        contractColEnd.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        contractColPremium.setCellValueFactory(new PropertyValueFactory<>("premiumAmount"));
        contractColCoverage.setCellValueFactory(new PropertyValueFactory<>("coverageAmount"));
        contractColStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        contractColApprovedBy.setCellValueFactory(new PropertyValueFactory<>("approvedBy"));

        contractColId.setVisible(false);

        contractStatusCombo.setItems(FXCollections.observableArrayList(ContractStatus.values()));

        setContractActionButtonsEnabled(false);
        contractsTable.setItems(contracts);
        contractsTable.setRowFactory(table -> {
            TableRow<InsuredContract> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    InsuredContract item = row.getItem();
                    selectedContract = item;
                    setContractActionButtonsEnabled(true);
                    System.out.println("Selected contract id: " + item.getId());
                }
            });
            return row;
        });
    }

    private void setEditMode(boolean enabled) {
        isEditMode = enabled;
        if (enabled) {
            SubAsset.setText("Update");
        } else {
            SubAsset.setText("Submit");
        }
    }

    private void setAssetActionButtonsEnabled(boolean enabled) {
        updateAssetButton.setDisable(!enabled);
        deleteAssetButton.setDisable(!enabled);
    }

    private void setContractEditMode(boolean enabled) {
        isContractEditMode = enabled;
        if (enabled) {
            contractSubmitButton.setText("Update");
        } else {
            contractSubmitButton.setText("Submit");
        }
    }

    private void setContractActionButtonsEnabled(boolean enabled) {
        contractUpdateButton.setDisable(!enabled);
        contractDeleteButton.setDisable(!enabled);
    }

    private void refreshAssetsTable() {
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        List<InsuredAsset> allAssets = assetDAO.readAll();
        assets.setAll(allAssets);
        selectedAsset = null;
        setAssetActionButtonsEnabled(false);
        setEditMode(false);
    }

    private void refreshContractsTable() {
        InsuredContractDAO contractDAO = new InsuredContractDAO();
        List<InsuredContract> allContracts = contractDAO.readAll();
        contracts.setAll(allContracts);
        selectedContract = null;
        setContractActionButtonsEnabled(false);
        setContractEditMode(false);
    }

    @FXML
    private void handleUpdateAssetButton() {
        if (selectedAsset == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
            return;
        }

        AssName.setText(selectedAsset.getName());
        AssType.setText(selectedAsset.getType());
        AssValue.setText(String.valueOf(selectedAsset.getValue()));
        AssDesc.setText(selectedAsset.getDescription());
        AssUser.setText(String.valueOf(selectedAsset.getUserId()));

        // Fill city/area from stored location string
        populateLocationCombosFromLocationString(selectedAsset.getLocation());

        if (selectedAsset.getCreatedAt() != null) {
            AssCreated.setValue(selectedAsset.getCreatedAt().toLocalDate());
        } else {
            AssCreated.setValue(null);
        }

        setEditMode(true);
        mainTabs.getSelectionModel().select(assetFormTab);
    }

    @FXML
    private void handleContractUpdateButton() {
        if (selectedContract == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click a contract first.");
            return;
        }

        contractNumberField.setText(selectedContract.getContractNumber());
        contractAssetIdField.setText(String.valueOf(selectedContract.getAssetId()));
        contractUserIdField.setText(String.valueOf(selectedContract.getUserId()));
        contractPremiumField.setText(String.valueOf(selectedContract.getPremiumAmount()));
        contractCoverageField.setText(String.valueOf(selectedContract.getCoverageAmount()));
        contractStatusCombo.setValue(selectedContract.getStatus());

        if (selectedContract.getStartDate() != null) {
            contractStartDatePicker.setValue(selectedContract.getStartDate());
        } else {
            contractStartDatePicker.setValue(null);
        }

        if (selectedContract.getEndDate() != null) {
            contractEndDatePicker.setValue(selectedContract.getEndDate());
        } else {
            contractEndDatePicker.setValue(null);
        }

        if (selectedContract.getApprovedBy() != null) {
            contractApprovedByField.setText(String.valueOf(selectedContract.getApprovedBy()));
        } else {
            contractApprovedByField.clear();
        }

        setContractEditMode(true);
        mainTabs.getSelectionModel().select(contractFormTab);
    }

    @FXML
    private void handleSubmitButton() {
        System.out.println("Submit button clicked!");

        try {
            // Get form values
            String name = AssName.getText().trim();
            String type = AssType.getText().trim();
            String value = AssValue.getText().trim();
            String description = AssDesc.getText().trim();
            String userId = AssUser.getText().trim();

            // Build location from city + area selectors
            String location = buildLocationFromSelection();

            // Validate required fields
            if (name.isEmpty() || type.isEmpty() || value.isEmpty() || userId.isEmpty() || location == null) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "Please fill in all required fields (Name, Type, Value, City, User ID). Area is optional.");
                return;
            }

            // Parse numeric values
            double assetValue;
            int userIdInt;

            try {
                assetValue = Double.parseDouble(value);
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "Value must be a valid number");
                return;
            }

            try {
                userIdInt = Integer.parseInt(userId);
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "User ID must be a valid integer");
                return;
            }

            // Verify user exists
            UserDAO userDAO = new UserDAO();
            User user = userDAO.read(userIdInt);

            if (user == null) {
                showAlert(AlertType.ERROR, "User Not Found",
                         "User with ID " + userIdInt + " does not exist in the database.\n" +
                         "Please enter a valid User ID or create a new user first.");
                System.out.println("ERROR: User ID " + userIdInt + " not found in database");
                return;
            }

            LocalDate createdDate = AssCreated.getValue();

            InsuredAssetDAO assetDAO = new InsuredAssetDAO();
            boolean success;

            if (isEditMode) {
                if (selectedAsset == null) {
                    showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
                    return;
                }

                InsuredAsset asset = new InsuredAsset(
                    selectedAsset.getId(),
                    name,
                    type,
                    assetValue,
                    description,
                    selectedAsset.getCreatedAt(),
                    location,
                    userIdInt
                );

                if (createdDate != null) {
                    asset.setCreatedAt(createdDate.atStartOfDay());
                }

                success = assetDAO.update(asset);

                if (success) {
                    showAlert(AlertType.INFORMATION, "Updated", "Insured Asset updated successfully!");
                    System.out.println("Asset updated successfully: " + asset);
                } else {
                    showAlert(AlertType.ERROR, "Update Failed", "Failed to update Insured Asset.");
                    System.out.println("Failed to update asset: " + asset);
                }
            } else {
                InsuredAsset asset = new InsuredAsset(
                    name,
                    type,
                    assetValue,
                    description,
                    location,
                    userIdInt
                );

                if (createdDate != null) {
                    asset.setCreatedAt(createdDate.atStartOfDay());
                }

                success = assetDAO.create(asset);

                if (success) {
                    showAlert(AlertType.INFORMATION, "Success",
                             "Insured Asset created successfully!");
                    System.out.println("Asset created successfully: " + asset);
                } else {
                    showAlert(AlertType.ERROR, "Database Error",
                             "Failed to create Insured Asset. Please check the console for details.");
                    System.out.println("Failed to create asset: " + asset);
                }
            }

            if (success) {
                clearForm();
                refreshAssetsTable();
                setEditMode(false);
                mainTabs.getSelectionModel().select(assetListTab);
            }

        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Unexpected Error",
                     "An error occurred: " + e.getMessage());
            System.out.println("Error in handleSubmitButton: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleContractSubmitButton() {
        try {
            String contractNumber = contractNumberField.getText().trim();
            String assetIdText = contractAssetIdField.getText().trim();
            String userIdText = contractUserIdField.getText().trim();
            String premiumText = contractPremiumField.getText().trim();
            String coverageText = contractCoverageField.getText().trim();
            ContractStatus status = contractStatusCombo.getValue();

            // Validate required fields
            if (contractNumber.isEmpty() || assetIdText.isEmpty() || userIdText.isEmpty() ||
                premiumText.isEmpty() || coverageText.isEmpty() || status == null) {
                showAlert(AlertType.ERROR, "Validation Error",
                         "Please fill in all required fields.");
                return;
            }

            LocalDate startDate = contractStartDatePicker.getValue();
            LocalDate endDate = contractEndDatePicker.getValue();

            if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
                showAlert(AlertType.ERROR, "Validation Error", "End date must be after start date.");
                return;
            }

            int assetId;
            int userId;
            double premiumAmount;
            double coverageAmount;

            try {
                assetId = Integer.parseInt(assetIdText);
                userId = Integer.parseInt(userIdText);
                premiumAmount = Double.parseDouble(premiumText);
                coverageAmount = Double.parseDouble(coverageText);
            } catch (NumberFormatException e) {
                showAlert(AlertType.ERROR, "Validation Error", "Invalid numeric values.");
                return;
            }

            // Parse optional Approved By
            Integer approvedBy = null;
            String approvedByText = contractApprovedByField.getText().trim();
            if (!approvedByText.isEmpty()) {
                try {
                    approvedBy = Integer.parseInt(approvedByText);
                } catch (NumberFormatException e) {
                    showAlert(AlertType.ERROR, "Validation Error", "Approved By must be a valid integer.");
                    return;
                }
            }

            // Verify asset and user exist
            InsuredAssetDAO assetDAO = new InsuredAssetDAO();
            if (assetDAO.read(assetId) == null) {
                showAlert(AlertType.ERROR, "Asset Not Found", "Asset ID does not exist.");
                return;
            }

            UserDAO userDAO = new UserDAO();
            // Verify main contract user exists
            if (userDAO.read(userId) == null) {
                showAlert(AlertType.ERROR, "User Not Found", "User ID does not exist.");
                return;
            }

            // Verify Approved By user exists when provided to avoid FK violation
            if (approvedBy != null && userDAO.read(approvedBy) == null) {
                showAlert(AlertType.ERROR, "User Not Found",
                         "Approved By user ID " + approvedBy + " does not exist.");
                return;
            }

            InsuredContractDAO contractDAO = new InsuredContractDAO();
            boolean success;

            // contractNumber is passed directly as String
            if (isContractEditMode) {
                if (selectedContract == null) {
                    showAlert(AlertType.WARNING, "No Selection", "Please double-click a contract first.");
                    return;
                }

                InsuredContract contract = new InsuredContract(
                    selectedContract.getId(),
                    contractNumber,
                    assetId,
                    userId,
                    startDate,
                    endDate,
                    premiumAmount,
                    coverageAmount,
                    status,
                    selectedContract.getCreatedAt(),
                    approvedBy
                );

                success = contractDAO.update(contract);
            } else {
                InsuredContract contract = new InsuredContract(
                    0,
                    contractNumber,
                    assetId,
                    userId,
                    startDate,
                    endDate,
                    premiumAmount,
                    coverageAmount,
                    status,
                    LocalDateTime.now(),
                    approvedBy
                );

                success = contractDAO.create(contract);
            }

            if (success) {
                showAlert(AlertType.INFORMATION, "Success", "Insured Contract saved successfully.");
                clearContractForm();
                refreshContractsTable();
                mainTabs.getSelectionModel().select(contractListTab);
            } else {
                showAlert(AlertType.ERROR, "Database Error", "Failed to save Insured Contract.");
            }
        } catch (Exception e) {
            showAlert(AlertType.ERROR, "Unexpected Error", "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteAssetButton() {
        if (selectedAsset == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click an asset first.");
            return;
        }

        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        boolean deleted = assetDAO.delete(selectedAsset.getId());

        if (deleted) {
            System.out.println("Deleted asset id: " + selectedAsset.getId());
            showAlert(AlertType.INFORMATION, "Deleted", "Insured Asset deleted successfully.");
            refreshAssetsTable();
        } else {
            showAlert(AlertType.ERROR, "Delete Failed", "Failed to delete the selected asset.");
        }
    }

    @FXML
    private void handleContractDeleteButton() {
        if (selectedContract == null) {
            showAlert(AlertType.WARNING, "No Selection", "Please double-click a contract first.");
            return;
        }

        InsuredContractDAO contractDAO = new InsuredContractDAO();
        boolean deleted = contractDAO.delete(selectedContract.getId());

        if (deleted) {
            showAlert(AlertType.INFORMATION, "Deleted", "Insured Contract deleted successfully.");
            refreshContractsTable();
        } else {
            showAlert(AlertType.ERROR, "Delete Failed", "Failed to delete the selected contract.");
        }
    }

    /**
     * Show an alert dialog to the user
     */
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Clear all form fields after successful submission
     */
    private void clearForm() {
        AssName.clear();
        AssType.clear();
        AssValue.clear();
        AssDesc.clear();
        AssUser.clear();
        AssCreated.setValue(null);
        if (cityCombo != null) {
            cityCombo.getSelectionModel().clearSelection();
        }
        if (areaCombo != null) {
            areaCombo.getSelectionModel().clearSelection();
            areaCombo.setDisable(true);
        }
    }

    private void clearContractForm() {
        contractNumberField.clear();
        contractAssetIdField.clear();
        contractUserIdField.clear();
        contractStartDatePicker.setValue(null);
        contractEndDatePicker.setValue(null);
        contractPremiumField.clear();
        contractCoverageField.clear();
        contractStatusCombo.setValue(null);
        contractApprovedByField.clear();
        setContractEditMode(false);
    }

    private void applyInputControls() {
        // Asset fields
        applyDecimalOnly(AssValue);
        applyIntegerOnly(AssUser);

        // Contract fields
        // Contract number back to alphanumeric with dash/underscore (string-based IDs)
        applyAlphanumericWithDash(contractNumberField);
        applyIntegerOnly(contractAssetIdField);
        applyIntegerOnly(contractUserIdField);
        applyDecimalOnly(contractPremiumField);
        applyDecimalOnly(contractCoverageField);
        applyIntegerOnly(contractApprovedByField);
    }

    private void applyIntegerOnly(TextField field) {
        if (field == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            return text.matches("\\d*") ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void applyDecimalOnly(TextField field) {
        if (field == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            return text.matches("\\d*(\\.\\d*)?") ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void applyAlphanumericWithDash(TextField field) {
        if (field == null) {
            return;
        }
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String text = change.getControlNewText();
            // Allow letters, numbers, dashes, and underscores for contract numbers
            return text.matches("[A-Za-z0-9\\-_]*") ? change : null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }
}
