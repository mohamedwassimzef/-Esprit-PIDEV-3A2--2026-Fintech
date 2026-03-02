package tn.esprit.tests;
import tn.esprit.dao.ContractRequestDAO;
import tn.esprit.dao.InsurancePackageDAO;
import tn.esprit.dao.InsuredAssetDAO;
import tn.esprit.dao.UserDAO;
import tn.esprit.entities.ContractRequest;
import tn.esprit.entities.InsurancePackage;
import tn.esprit.entities.InsuredAsset;
import tn.esprit.entities.User;
import tn.esprit.enums.RequestStatus;
import java.util.List;
/**
 * CRUD test suite for InsurancePackage and ContractRequest entities.
 * Covers Create, Read (all + by id), Update, Delete,
 * and the extra query methods (findActive, findByAssetType,
 * findByUserId, findByStatus, findByPackageId).
 */
public class InsurancePackageAndContractRequestTest {
    /* IDs shared between test sections */
    private static int testUserId     = -1;
    private static int testAssetId    = -1;
    private static int testPackageId  = -1;
    private static int testRequestId  = -1;
    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("   INSURANCE PACKAGE & CONTRACT REQUEST  --  CRUD TEST SUITE");
        System.out.println("=".repeat(100));
        setupPrerequisites();
        if (testUserId == -1 || testAssetId == -1) {
            System.out.println("\n[ABORT] Could not find prerequisite user/asset records. " +
                    "Make sure at least one User and InsuredAsset exist in the DB.");
            return;
        }
        testInsurancePackageCrud();
        testContractRequestCrud();
        System.out.println("\n" + "=".repeat(100));
        System.out.println("   ALL TESTS FINISHED");
        System.out.println("=".repeat(100) + "\n");
    }
    // ---------------------------------------------------------------
    // SETUP: grab an existing user + asset (or create fresh ones)
    // ---------------------------------------------------------------
    private static void setupPrerequisites() {
        System.out.println("\n[SETUP] Fetching prerequisite User and InsuredAsset records...");
        UserDAO userDAO = new UserDAO();
        List<User> users = userDAO.readAll();
        if (!users.isEmpty()) {
            testUserId = users.get(0).getId();
            System.out.println("  Using User ID : " + testUserId + "  (" + users.get(0).getName() + ")");
        } else {
            // create a minimal user so the FK is satisfied
            long ts = System.currentTimeMillis();
            User u = new User("Test User", "testuser." + ts + "@fintech.com", "hash_" + ts, 2, "+21600000000");
            userDAO.create(u);
            List<User> fresh = userDAO.readAll();
            if (!fresh.isEmpty()) {
                testUserId = fresh.get(fresh.size() - 1).getId();
                System.out.println("  Created new User ID : " + testUserId);
            }
        }
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        if (testUserId != -1) {
            List<InsuredAsset> assets = assetDAO.findByUserId(testUserId);
            if (assets.isEmpty()) {
                // create one asset for this user
                InsuredAsset a = new InsuredAsset(
                        "Test Car", "car",
                        "Test asset for DAO tests",
                        "Tunis - Centre Ville", testUserId,
                        new java.math.BigDecimal("15000.00"),
                        java.time.LocalDate.of(2022, 1, 1));
                assetDAO.create(a);
                assets = assetDAO.findByUserId(testUserId);
            }
            if (!assets.isEmpty()) {
                testAssetId = assets.get(0).getId();
                System.out.println("  Using Asset ID: " + testAssetId + "  (" + assets.get(0).getReference() + ")");
            }
        }
    }
    // ===============================================================
    //  INSURANCE PACKAGE CRUD TESTS
    // ===============================================================
    private static void testInsurancePackageCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(28) + "INSURANCE PACKAGE  CRUD TESTS" + " ".repeat(41) + "|");
        System.out.println("+" + "-".repeat(98) + "+");
        InsurancePackageDAO dao = new InsurancePackageDAO();
        long ts = System.currentTimeMillis();
        // ---- CREATE ------------------------------------------------
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 insurance packages...");
        InsurancePackage pkg1 = new InsurancePackage(
                "Basic Car Cover " + ts, "car",
                "Entry-level car insurance",
                "Covers third-party liability and basic damage",
                299.99, 1.0, 12, true);
        InsurancePackage pkg2 = new InsurancePackage(
                "Premium Home Cover " + ts, "home",
                "Comprehensive home insurance",
                "Covers fire, flood, theft and structural damage",
                549.00, 1.2, 12, true);
        InsurancePackage pkg3 = new InsurancePackage(
                "Land Protection Plan " + ts, "land",
                "Land ownership protection plan",
                "Covers legal disputes and natural disasters",
                199.50, 0.9, 24, true);
        boolean c1 = dao.create(pkg1);
        boolean c2 = dao.create(pkg2);
        boolean c3 = dao.create(pkg3);
        System.out.println("  - Package 1 (Basic Car)   : " + result(c1));
        System.out.println("  - Package 2 (Premium Home): " + result(c2));
        System.out.println("  - Package 3 (Land Plan)   : " + result(c3));
        // ---- READ ALL ----------------------------------------------
        System.out.println("\n[READ ALL TEST]");
        List<InsurancePackage> all = dao.readAll();
        System.out.println("  Total packages in DB: " + all.size());
        all.forEach(p -> System.out.println("  - " + p));
        if (all.isEmpty()) {
            System.out.println("  [SKIP] No packages found, skipping remaining tests.");
            return;
        }
        // save id for ContractRequest tests
        testPackageId = all.get(all.size() - 1).getId();
        // ---- READ BY ID --------------------------------------------
        System.out.println("\n[READ BY ID TEST]");
        InsurancePackage fetched = dao.read(testPackageId);
        System.out.println("  Fetched ID " + testPackageId + " -> " +
                (fetched != null ? "OK  " + fetched : "FAIL  NOT FOUND"));
        // ---- UPDATE ------------------------------------------------
        System.out.println("\n[UPDATE TEST]");
        InsurancePackage toUpdate = all.get(all.size() - 1);
        double oldPrice = toUpdate.getBasePrice();
        toUpdate.setBasePrice(oldPrice + 50.0);
        toUpdate.setDescription("Updated description - " + ts);
        boolean updated = dao.update(toUpdate);
        System.out.println("  Update (basePrice " + oldPrice + " -> " + toUpdate.getBasePrice() + "): " + result(updated));
        // verify the change was persisted
        InsurancePackage afterUpdate = dao.read(toUpdate.getId());
        if (afterUpdate != null) {
            System.out.println("  Verify persisted price: " + afterUpdate.getBasePrice() +
                    (Math.abs(afterUpdate.getBasePrice() - toUpdate.getBasePrice()) < 0.01 ? "  OK MATCH" : "  FAIL MISMATCH"));
        }
        // ---- FIND ACTIVE -------------------------------------------
        System.out.println("\n[FIND ACTIVE TEST]");
        List<InsurancePackage> active = dao.findActive();
        System.out.println("  Active packages: " + active.size());
        active.forEach(p -> System.out.println("  - [" + p.getId() + "] " + p.getName() + " | active=" + p.isActive()));
        // ---- FIND BY ASSET TYPE ------------------------------------
        System.out.println("\n[FIND BY ASSET TYPE TEST]");
        for (String type : new String[]{"car", "home", "land"}) {
            List<InsurancePackage> byType = dao.findByAssetType(type);
            System.out.println("  Type '" + type + "': " + byType.size() + " package(s)");
            byType.forEach(p -> System.out.println("    - [" + p.getId() + "] " + p.getName()));
        }
        // ---- DELETE ------------------------------------------------
        System.out.println("\n[DELETE TEST]");
        // Delete the first of the newly created packages (leave the others for contract request tests)
        InsurancePackage firstCreated = all.get(all.size() >= 3 ? all.size() - 3 : 0);
        boolean deleted = dao.delete(firstCreated.getId());
        System.out.println("  Delete ID " + firstCreated.getId() + ": " + result(deleted));
        // confirm deletion
        InsurancePackage afterDelete = dao.read(firstCreated.getId());
        System.out.println("  Confirm deleted (should be null): " +
                (afterDelete == null ? "OK  null as expected" : "FAIL  still exists: " + afterDelete));
    }
    // ===============================================================
    //  CONTRACT REQUEST CRUD TESTS
    // ===============================================================
    private static void testContractRequestCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(28) + "CONTRACT REQUEST  CRUD TESTS" + " ".repeat(42) + "|");
        System.out.println("+" + "-".repeat(98) + "+");
        if (testPackageId == -1) {
            System.out.println("  [SKIP] No InsurancePackage ID available. Run InsurancePackage tests first.");
            return;
        }
        ContractRequestDAO dao = new ContractRequestDAO();
        // ---- CREATE ------------------------------------------------
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 contract requests...");
        ContractRequest req1 = new ContractRequest(
                testUserId, testAssetId, testPackageId,
                350.00, RequestStatus.PENDING);
        ContractRequest req2 = new ContractRequest(
                testUserId, testAssetId, testPackageId,
                420.50, RequestStatus.PENDING);
        ContractRequest req3 = new ContractRequest(
                testUserId, testAssetId, testPackageId,
                275.00, RequestStatus.APPROVED);
        boolean c1 = dao.create(req1);
        boolean c2 = dao.create(req2);
        boolean c3 = dao.create(req3);
        System.out.println("  - Request 1 (PENDING  350.00): " + result(c1));
        System.out.println("  - Request 2 (PENDING  420.50): " + result(c2));
        System.out.println("  - Request 3 (APPROVED 275.00): " + result(c3));
        // ---- READ ALL ----------------------------------------------
        System.out.println("\n[READ ALL TEST]");
        List<ContractRequest> all = dao.readAll();
        System.out.println("  Total requests in DB: " + all.size());
        all.forEach(r -> System.out.println("  - " + r));
        if (all.isEmpty()) {
            System.out.println("  [SKIP] No requests found, skipping remaining tests.");
            return;
        }
        testRequestId = all.get(all.size() - 1).getId();
        // ---- READ BY ID --------------------------------------------
        System.out.println("\n[READ BY ID TEST]");
        ContractRequest fetched = dao.read(testRequestId);
        System.out.println("  Fetched ID " + testRequestId + " -> " +
                (fetched != null ? "OK  " + fetched : "FAIL  NOT FOUND"));
        // ---- UPDATE ------------------------------------------------
        System.out.println("\n[UPDATE TEST]");
        ContractRequest toUpdate = all.get(all.size() - 1);
        RequestStatus oldStatus = toUpdate.getStatus();
        toUpdate.setStatus(RequestStatus.APPROVED);
        toUpdate.setCalculatedPremium(toUpdate.getCalculatedPremium() + 25.0);
        boolean updated = dao.update(toUpdate);
        System.out.println("  Update (status " + oldStatus + " -> APPROVED, premium +" + 25.0 + "): " + result(updated));
        // verify
        ContractRequest afterUpdate = dao.read(toUpdate.getId());
        if (afterUpdate != null) {
            System.out.println("  Verify status persisted: " + afterUpdate.getStatus() +
                    (afterUpdate.getStatus() == RequestStatus.APPROVED ? "  OK MATCH" : "  FAIL MISMATCH"));
        }
        // ---- FIND BY USER ID ---------------------------------------
        System.out.println("\n[FIND BY USER ID TEST]");
        List<ContractRequest> byUser = dao.findByUserId(testUserId);
        System.out.println("  Requests for User ID " + testUserId + ": " + byUser.size());
        byUser.forEach(r -> System.out.println("  - [" + r.getId() + "] premium=" + r.getCalculatedPremium()
                + " status=" + r.getStatus()));
        // ---- FIND BY STATUS ----------------------------------------
        System.out.println("\n[FIND BY STATUS TEST]");
        for (RequestStatus status : RequestStatus.values()) {
            List<ContractRequest> byStatus = dao.findByStatus(status);
            System.out.println("  Status " + status + ": " + byStatus.size() + " request(s)");
        }
        // ---- FIND BY PACKAGE ID ------------------------------------
        System.out.println("\n[FIND BY PACKAGE ID TEST]");
        List<ContractRequest> byPkg = dao.findByPackageId(testPackageId);
        System.out.println("  Requests for Package ID " + testPackageId + ": " + byPkg.size());
        byPkg.forEach(r -> System.out.println("  - [" + r.getId() + "] " + r));
        // ---- DELETE ------------------------------------------------
        System.out.println("\n[DELETE TEST]");
        int deleteId = all.get(0).getId();
        boolean deleted = dao.delete(deleteId);
        System.out.println("  Delete ID " + deleteId + ": " + result(deleted));
        ContractRequest afterDelete = dao.read(deleteId);
        System.out.println("  Confirm deleted (should be null): " +
                (afterDelete == null ? "OK  null as expected" : "FAIL  still exists: " + afterDelete));
    }
    // ---------------------------------------------------------------
    // Helper
    // ---------------------------------------------------------------
    private static String result(boolean ok) {
        return ok ? "OK  SUCCESS" : "FAIL  FAILED";
    }
}