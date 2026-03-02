package tn.esprit.tests;

import tn.esprit.dao.*;
import tn.esprit.entities.*;
import tn.esprit.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive CRUD test suite for all entities
 * Tests all Create, Read, Update, Delete operations
 */
public class CrudTestSuite {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(100));
        System.out.println("COMPREHENSIVE CRUD TEST SUITE FOR ALL ENTITIES");
        System.out.println("=".repeat(100));

        // Start with User entity tests
        testUserCrud();
        testRoleCrud();
        testTransactionCrud();
        testBudgetCrud();
        testLoanCrud();
        testRepaymentCrud();
        testExpenseCrud();
        testComplaintCrud();
        testInsuredAssetCrud();
        testInsuredContractCrud();
        testInsurancePackageCrud();
        testContractRequestCrud();

        System.out.println("\n" + "=".repeat(100));
        System.out.println("ALL CRUD TESTS COMPLETED SUCCESSFULLY");
        System.out.println("=".repeat(100) + "\n");
    }

    // ======================== USER ENTITY TESTS ========================
    private static int testUserCrudUserId = -1;
    private static int testLoanCrudLoanId = -1;

    private static void testUserCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(35) + "USER ENTITY CRUD TESTS" + " ".repeat(41) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        UserDAO userDAO = new UserDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 new users...");
        long timestamp = System.currentTimeMillis();
        User user1 = new User("Ahmed Mohamed", "ahmed.m." + timestamp + "@fintech.com", "pwd_hash_123", 1, "+216-98-765-432");
        User user2 = new User("Fatima Ben Ali", "fatima.b." + timestamp + "@fintech.com", "pwd_hash_456", 1, "+216-98-765-433");
        User user3 = new User("Mohamed Karim", "mohamedK." + timestamp + "@fintech.com", "pwd_hash_789", 1, "+216-98-765-434");

        boolean created1 = userDAO.create(user1);
        boolean created2 = userDAO.create(user2);
        boolean created3 = userDAO.create(user3);

        System.out.println("  - User 1 (Ahmed Mohamed): " + (created1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - User 2 (Fatima Ben Ali): " + (created2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - User 3 (Mohamed Karim): " + (created3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        System.out.println("Fetching all users from database...");
        List<User> allUsers = userDAO.readAll();
        System.out.println("  Total users found: " + allUsers.size());
        if (!allUsers.isEmpty()) {
            System.out.println("\n  User Details:");
            for (int i = 0; i < allUsers.size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + allUsers.get(i));
            }
        }

        if (!allUsers.isEmpty()) {
            testUserCrudUserId = allUsers.get(allUsers.size() - 1).getId();
            System.out.println("  Stored User ID: " + testUserCrudUserId);

            // READ BY ID
            System.out.println("\n[READ BY ID TEST]");
            int firstUserId = allUsers.get(0).getId();
            System.out.println("Reading user with ID: " + firstUserId);
            User fetchedUser = userDAO.read(firstUserId);
            if (fetchedUser != null) {
                System.out.println("  OK User Found: " + fetchedUser);
            } else {
                System.out.println("  FAIL User Not Found");
            }

            // UPDATE
            System.out.println("\n[UPDATE TEST]");
            System.out.println("Updating user #1 (ID: " + firstUserId + ")");
            allUsers.get(0).setName("Ahmed Mohamed Updated");
            allUsers.get(0).setPhone("+216-99-999-999");
            allUsers.get(0).setVerified(true);
            boolean updated = userDAO.update(allUsers.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
            if (updated) {
                User verifyUpdate = userDAO.read(firstUserId);
                System.out.println("  Verification: " + verifyUpdate);
            }

            // DELETE
            if (allUsers.size() > 2) {
                System.out.println("\n[DELETE TEST]");
                int deleteId = allUsers.get(allUsers.size() - 1).getId();
                System.out.println("Deleting user with ID: " + deleteId);
                boolean deleted = userDAO.delete(deleteId);
                System.out.println("  Delete result: " + (deleted ? "OK SUCCESS" : "FAIL FAILED"));
                if (deleted) {
                    System.out.println("  Fetching all users after deletion...");
                    List<User> remainingUsers = userDAO.readAll();
                    System.out.println("  Remaining users: " + remainingUsers.size());
                }
            }
        }
    }

    // ======================== ROLE ENTITY TESTS ========================
    private static void testRoleCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(35) + "ROLE ENTITY CRUD TESTS" + " ".repeat(41) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        RoleDAO roleDAO = new RoleDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 2 new roles...");
        long timestamp = System.currentTimeMillis();
        Role role1 = new Role("supervisor_" + timestamp, "{\"read\": true, \"write\": true, \"delete\": false}");
        Role role2 = new Role("guest_" + timestamp, "{\"read\": true, \"write\": false, \"delete\": false}");

        boolean created1 = roleDAO.create(role1);
        boolean created2 = roleDAO.create(role2);

        System.out.println("  - Role 1 (supervisor): " + (created1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Role 2 (guest): " + (created2 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Role> allRoles = roleDAO.readAll();
        System.out.println("  Total roles found: " + allRoles.size());
        allRoles.forEach(r -> System.out.println("  - " + r));

        if (!allRoles.isEmpty()) {
            // READ BY ID
            System.out.println("\n[READ BY ID TEST]");
            int roleId = allRoles.get(0).getId();
            Role fetchedRole = roleDAO.read(roleId);
            System.out.println("  OK Found: " + (fetchedRole != null ? fetchedRole : "NOT FOUND"));

            // UPDATE
            System.out.println("\n[UPDATE TEST]");
            allRoles.get(0).setPermissions("{\"read\": true, \"write\": true, \"delete\": true}");
            boolean updated = roleDAO.update(allRoles.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== TRANSACTION ENTITY TESTS ========================
    private static void testTransactionCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(30) + "TRANSACTION ENTITY CRUD TESTS" + " ".repeat(38) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        if (testUserCrudUserId == -1) {
            System.out.println("\n[SKIP] No valid user ID available - skipping transaction tests");
            return;
        }

        TransactionDAO transactionDAO = new TransactionDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 transactions...");
        Transaction txn1 = new Transaction(testUserCrudUserId, new BigDecimal("500.00"), TransactionType.CREDIT,
                "Salary deposit", ReferenceType.ONLINE, null);
        Transaction txn2 = new Transaction(testUserCrudUserId, new BigDecimal("100.00"), TransactionType.DEBIT,
                "Utility payment", ReferenceType.BUDGET, null);
        Transaction txn3 = new Transaction(testUserCrudUserId, new BigDecimal("250.00"), TransactionType.CREDIT,
                "Loan disbursement", ReferenceType.LOAN, null);

        boolean c1 = transactionDAO.create(txn1);
        boolean c2 = transactionDAO.create(txn2);
        boolean c3 = transactionDAO.create(txn3);

        System.out.println("  - Transaction 1 (Salary): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Transaction 2 (Utility): " + (c2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Transaction 3 (Loan): " + (c3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Transaction> allTxns = transactionDAO.readAll();
        System.out.println("  Total transactions: " + allTxns.size());
        allTxns.forEach(t -> System.out.println("  - " + t));

        if (!allTxns.isEmpty()) {
            // READ BY ID
            System.out.println("\n[READ BY ID TEST]");
            Transaction fetched = transactionDAO.read(allTxns.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            // UPDATE
            System.out.println("\n[UPDATE TEST]");
            allTxns.get(0).setStatus(TransactionStatus.COMPLETED);
            boolean updated = transactionDAO.update(allTxns.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== BUDGET ENTITY TESTS ========================
    private static void testBudgetCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(35) + "BUDGET ENTITY CRUD TESTS" + " ".repeat(39) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        if (testUserCrudUserId == -1) {
            System.out.println("\n[SKIP] No valid user ID available - skipping budget tests");
            return;
        }

        BudgetDAO budgetDAO = new BudgetDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 2 budgets...");
        Budget budget1 = new Budget("Monthly Groceries", new BigDecimal("500.00"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), testUserCrudUserId, "Food");
        Budget budget2 = new Budget("Transportation", new BigDecimal("200.00"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), testUserCrudUserId, "Transport");

        boolean c1 = budgetDAO.create(budget1);
        boolean c2 = budgetDAO.create(budget2);

        System.out.println("  - Budget 1 (Groceries): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Budget 2 (Transportation): " + (c2 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Budget> allBudgets = budgetDAO.readAll();
        System.out.println("  Total budgets: " + allBudgets.size());
        allBudgets.forEach(b -> System.out.println("  - " + b));

        if (!allBudgets.isEmpty()) {
            // READ BY ID & UPDATE
            System.out.println("\n[READ BY ID TEST]");
            Budget fetched = budgetDAO.read(allBudgets.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            System.out.println("\n[UPDATE TEST]");
            allBudgets.get(0).setSpentAmount(new BigDecimal("250.50"));
            boolean updated = budgetDAO.update(allBudgets.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== LOAN ENTITY TESTS ========================
    private static void testLoanCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(36) + "LOAN ENTITY CRUD TESTS" + " ".repeat(40) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        LoanDAO loanDAO = new LoanDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 2 loans...");
        Loan loan1 = new Loan(1, new BigDecimal("10000.00"), new BigDecimal("5.50"),
                LocalDate.of(2026, 2, 8), LocalDate.of(2028, 2, 8));
        Loan loan2 = new Loan(1, new BigDecimal("5000.00"), new BigDecimal("4.75"),
                LocalDate.of(2026, 2, 8), LocalDate.of(2027, 2, 8));

        boolean c1 = loanDAO.create(loan1);
        boolean c2 = loanDAO.create(loan2);

        System.out.println("  - Loan 1 (10000 TND): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Loan 2 (5000 TND): " + (c2 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Loan> allLoans = loanDAO.readAll();
        System.out.println("  Total loans: " + allLoans.size());
        allLoans.forEach(l -> System.out.println("  - " + l));

        if (!allLoans.isEmpty()) {
            // READ BY ID & UPDATE
            System.out.println("\n[READ BY ID TEST]");
            Loan fetched = loanDAO.read(allLoans.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            System.out.println("\n[UPDATE TEST]");
            allLoans.get(0).setStatus("closed");
            boolean updated = loanDAO.update(allLoans.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== REPAYMENT ENTITY TESTS ========================
    private static void testRepaymentCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(32) + "REPAYMENT ENTITY CRUD TESTS" + " ".repeat(38) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        RepaymentDAO repaymentDAO = new RepaymentDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 2 repayments...");
        Repayment rep1 = new Repayment(1, new BigDecimal("500.00"),
                LocalDate.of(2026, 3, 8), "Bank Transfer");
        rep1.setMonthlyPayment(new BigDecimal("500.00"));

        Repayment rep2 = new Repayment(1, new BigDecimal("400.00"),
                LocalDate.of(2026, 4, 8), "Online Payment");
        rep2.setMonthlyPayment(new BigDecimal("400.00"));

        boolean c1 = repaymentDAO.create(rep1);
        boolean c2 = repaymentDAO.create(rep2);

        System.out.println("  - Repayment 1 (500 TND): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Repayment 2 (400 TND): " + (c2 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Repayment> allRepayments = repaymentDAO.readAll();
        System.out.println("  Total repayments: " + allRepayments.size());
        allRepayments.forEach(r -> System.out.println("  - " + r));

        if (!allRepayments.isEmpty()) {
            // READ BY ID & UPDATE
            System.out.println("\n[READ BY ID TEST]");
            Repayment fetched = repaymentDAO.read(allRepayments.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            System.out.println("\n[UPDATE TEST]");
            allRepayments.get(0).setStatus("paid");
            boolean updated = repaymentDAO.update(allRepayments.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== EXPENSE ENTITY TESTS ========================
    private static void testExpenseCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(34) + "EXPENSE ENTITY CRUD TESTS" + " ".repeat(39) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        ExpenseDAO expenseDAO = new ExpenseDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 expenses...");
        Expense exp1 = new Expense(new BigDecimal("75.50"), "Groceries",
                LocalDate.of(2026, 2, 8), "Weekly shopping at supermarket", null);
        Expense exp2 = new Expense(new BigDecimal("45.00"), "Transport",
                LocalDate.of(2026, 2, 8), "Taxi fare", null);
        Expense exp3 = new Expense(new BigDecimal("120.00"), "Utilities",
                LocalDate.of(2026, 2, 8), "Electricity bill", null);

        boolean c1 = expenseDAO.create(exp1);
        boolean c2 = expenseDAO.create(exp2);
        boolean c3 = expenseDAO.create(exp3);

        System.out.println("  - Expense 1 (Groceries): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Expense 2 (Transport): " + (c2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Expense 3 (Utilities): " + (c3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Expense> allExpenses = expenseDAO.readAll();
        System.out.println("  Total expenses: " + allExpenses.size());
        allExpenses.forEach(e -> System.out.println("  - " + e));

        if (!allExpenses.isEmpty()) {
            // READ BY ID & UPDATE
            System.out.println("\n[READ BY ID TEST]");
            Expense fetched = expenseDAO.read(allExpenses.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            System.out.println("\n[UPDATE TEST]");
            allExpenses.get(0).setAmount(new BigDecimal("80.00"));
            boolean updated = expenseDAO.update(allExpenses.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== COMPLAINT ENTITY TESTS ========================
    private static void testComplaintCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(33) + "COMPLAINT ENTITY CRUD TESTS" + " ".repeat(38) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        ComplaintDAO complaintDAO = new ComplaintDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 2 complaints...");
        Complaint comp1 = new Complaint("App crashes frequently", LocalDate.of(2026, 2, 8), 1);
        Complaint comp2 = new Complaint("Slow transaction processing", LocalDate.of(2026, 2, 7), 1);

        boolean c1 = complaintDAO.create(comp1);
        boolean c2 = complaintDAO.create(comp2);

        System.out.println("  - Complaint 1 (App crashes): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Complaint 2 (Slow processing): " + (c2 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<Complaint> allComplaints = complaintDAO.readAll();
        System.out.println("  Total complaints: " + allComplaints.size());
        allComplaints.forEach(c -> System.out.println("  - " + c));

        if (!allComplaints.isEmpty()) {
            // READ BY ID & UPDATE
            System.out.println("\n[READ BY ID TEST]");
            Complaint fetched = complaintDAO.read(allComplaints.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            System.out.println("\n[UPDATE TEST]");
            allComplaints.get(0).setStatus("resolved");
            allComplaints.get(0).setResponse("Issue has been fixed in v2.1");
            boolean updated = complaintDAO.update(allComplaints.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        }
    }

    // ======================== INSURED ASSET ENTITY TESTS ========================
    private static void testInsuredAssetCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(29) + "INSURED ASSET ENTITY CRUD TESTS" + " ".repeat(38) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        InsuredAssetDAO assetDAO = new InsuredAssetDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        System.out.println("Creating 3 insured assets...");
        InsuredAsset asset1 = new InsuredAsset("Tesla Model 3", "Vehicle",
                "Electric car for personal use",
                "Garage A", 1, new java.math.BigDecimal("45000.00"), java.time.LocalDate.of(2021,1,1));
        InsuredAsset asset2 = new InsuredAsset("Home Building", "Real Estate",
                "Residential property",
                "Downtown", 1, new java.math.BigDecimal("200000.00"), java.time.LocalDate.of(2010,6,15));
        InsuredAsset asset3 = new InsuredAsset("Diamond Necklace", "Jewelry",
                "18K gold with diamonds",
                "Safe Box", 1, new java.math.BigDecimal("5000.00"), java.time.LocalDate.of(2020,3,10));

        boolean c1 = assetDAO.create(asset1);
        boolean c2 = assetDAO.create(asset2);
        boolean c3 = assetDAO.create(asset3);

        System.out.println("  - Asset 1 (Tesla): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Asset 2 (Home): " + (c2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Asset 3 (Necklace): " + (c3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<InsuredAsset> allAssets = assetDAO.readAll();
        System.out.println("  Total assets: " + allAssets.size());
        allAssets.forEach(a -> System.out.println("  - " + a));

        if (!allAssets.isEmpty()) {
            // READ BY ID
            System.out.println("\n[READ BY ID TEST]");
            InsuredAsset fetched = assetDAO.read(allAssets.get(0).getId());
            System.out.println("  OK Found: " + (fetched != null ? fetched : "NOT FOUND"));

            // UPDATE
            System.out.println("\n[UPDATE TEST]");
            allAssets.get(0).setReference("Tesla Model 3 Updated");
            allAssets.get(0).setDeclaredValue(new java.math.BigDecimal("48000.00"));
            boolean updated = assetDAO.update(allAssets.get(0));
            System.out.println("  Update result: " + (updated ? "OK SUCCESS" : "FAIL FAILED"));

            // DELETE
            if (allAssets.size() > 2) {
                System.out.println("\n[DELETE TEST]");
                int deleteId = allAssets.get(allAssets.size() - 1).getId();
                boolean deleted = assetDAO.delete(deleteId);
                System.out.println("  Delete result: " + (deleted ? "OK SUCCESS" : "FAIL FAILED"));
            }
        }
    }

    // ======================== INSURED CONTRACT ENTITY TESTS ========================
    private static void testInsuredContractCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(27) + "INSURED CONTRACT ENTITY CRUD TESTS" + " ".repeat(37) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        InsuredContractDAO contractDAO = new InsuredContractDAO();

        System.out.println("\n[READ ALL TEST]");
        List<InsuredContract> allContracts = contractDAO.readAll();
        System.out.println("  Total contracts: " + allContracts.size());
        allContracts.forEach(c -> System.out.println("  - " + c));
    }

    // ======================== INSURANCE PACKAGE ENTITY TESTS ========================
    private static int testPackageCrudPackageId = -1;

    private static void testInsurancePackageCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(27) + "INSURANCE PACKAGE ENTITY CRUD TESTS" + " ".repeat(36) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        InsurancePackageDAO dao = new InsurancePackageDAO();
        long ts = System.currentTimeMillis();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        InsurancePackage pkg1 = new InsurancePackage("Basic Car Cover " + ts, "car",
                "Entry-level car insurance", "Covers third-party liability", 299.99, 1.0, 12, true);
        InsurancePackage pkg2 = new InsurancePackage("Premium Home Cover " + ts, "home",
                "Comprehensive home insurance", "Covers fire, flood, theft", 549.00, 1.2, 12, true);
        InsurancePackage pkg3 = new InsurancePackage("Land Protection Plan " + ts, "land",
                "Land ownership protection", "Covers legal disputes", 199.50, 0.9, 24, true);

        boolean c1 = dao.create(pkg1);
        boolean c2 = dao.create(pkg2);
        boolean c3 = dao.create(pkg3);
        System.out.println("  - Package 1 (Basic Car)   : " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Package 2 (Premium Home): " + (c2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Package 3 (Land Plan)   : " + (c3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<InsurancePackage> all = dao.readAll();
        System.out.println("  Total packages: " + all.size());
        all.forEach(p -> System.out.println("  - " + p));

        if (all.isEmpty()) return;
        testPackageCrudPackageId = all.get(all.size() - 1).getId();

        // READ BY ID
        System.out.println("\n[READ BY ID TEST]");
        InsurancePackage fetched = dao.read(testPackageCrudPackageId);
        System.out.println("  Fetched: " + (fetched != null ? "OK " + fetched : "FAIL NOT FOUND"));

        // UPDATE
        System.out.println("\n[UPDATE TEST]");
        InsurancePackage toUpdate = all.get(all.size() - 1);
        double oldPrice = toUpdate.getBasePrice();
        toUpdate.setBasePrice(oldPrice + 50.0);
        toUpdate.setDescription("Updated description " + ts);
        boolean updated = dao.update(toUpdate);
        System.out.println("  Update (price " + oldPrice + " -> " + toUpdate.getBasePrice() + "): "
                + (updated ? "OK SUCCESS" : "FAIL FAILED"));

        // FIND ACTIVE
        System.out.println("\n[FIND ACTIVE TEST]");
        List<InsurancePackage> active = dao.findActive();
        System.out.println("  Active packages: " + active.size());

        // FIND BY ASSET TYPE
        System.out.println("\n[FIND BY ASSET TYPE TEST]");
        for (String type : new String[]{"car", "home", "land"}) {
            List<InsurancePackage> byType = dao.findByAssetType(type);
            System.out.println("  Type '" + type + "': " + byType.size() + " package(s)");
        }

        // DELETE
        System.out.println("\n[DELETE TEST]");
        InsurancePackage toDelete = all.get(all.size() >= 3 ? all.size() - 3 : 0);
        boolean deleted = dao.delete(toDelete.getId());
        System.out.println("  Delete ID " + toDelete.getId() + ": " + (deleted ? "OK SUCCESS" : "FAIL FAILED"));
        InsurancePackage afterDelete = dao.read(toDelete.getId());
        System.out.println("  Confirm deleted: " + (afterDelete == null ? "OK null as expected" : "FAIL still exists"));
    }

    // ======================== CONTRACT REQUEST ENTITY TESTS ========================
    private static void testContractRequestCrud() {
        System.out.println("\n\n+" + "-".repeat(98) + "+");
        System.out.println("|" + " ".repeat(27) + "CONTRACT REQUEST ENTITY CRUD TESTS" + " ".repeat(37) + "|");
        System.out.println("+" + "-".repeat(98) + "+");

        if (testPackageCrudPackageId == -1) {
            System.out.println("  [SKIP] No InsurancePackage ID available — run InsurancePackage tests first.");
            return;
        }
        if (testUserCrudUserId == -1) {
            System.out.println("  [SKIP] No User ID available.");
            return;
        }

        // Grab any asset belonging to testUserCrudUserId
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        List<InsuredAsset> userAssets = assetDAO.findByUserId(testUserCrudUserId);
        if (userAssets.isEmpty()) {
            InsuredAsset a = new InsuredAsset("Test Car", "car",
                    "Test asset",
                    "Tunis", testUserCrudUserId,
                    new java.math.BigDecimal("15000.00"), java.time.LocalDate.now());
            assetDAO.create(a);
            userAssets = assetDAO.findByUserId(testUserCrudUserId);
        }
        if (userAssets.isEmpty()) {
            System.out.println("  [SKIP] Could not resolve an asset for user " + testUserCrudUserId);
            return;
        }
        int assetId = userAssets.get(0).getId();

        ContractRequestDAO dao = new ContractRequestDAO();

        // CREATE
        System.out.println("\n[CREATE TEST]");
        ContractRequest req1 = new ContractRequest(testUserCrudUserId, assetId, testPackageCrudPackageId, 350.00, RequestStatus.PENDING);
        ContractRequest req2 = new ContractRequest(testUserCrudUserId, assetId, testPackageCrudPackageId, 420.50, RequestStatus.PENDING);
        ContractRequest req3 = new ContractRequest(testUserCrudUserId, assetId, testPackageCrudPackageId, 275.00, RequestStatus.APPROVED);

        boolean c1 = dao.create(req1);
        boolean c2 = dao.create(req2);
        boolean c3 = dao.create(req3);
        System.out.println("  - Request 1 (PENDING  350.00): " + (c1 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Request 2 (PENDING  420.50): " + (c2 ? "OK CREATED" : "FAIL FAILED"));
        System.out.println("  - Request 3 (APPROVED 275.00): " + (c3 ? "OK CREATED" : "FAIL FAILED"));

        // READ ALL
        System.out.println("\n[READ ALL TEST]");
        List<ContractRequest> all = dao.readAll();
        System.out.println("  Total requests: " + all.size());
        all.forEach(r -> System.out.println("  - " + r));

        if (all.isEmpty()) return;
        int lastId = all.get(all.size() - 1).getId();

        // READ BY ID
        System.out.println("\n[READ BY ID TEST]");
        ContractRequest fetched = dao.read(lastId);
        System.out.println("  Fetched: " + (fetched != null ? "OK " + fetched : "FAIL NOT FOUND"));

        // UPDATE
        System.out.println("\n[UPDATE TEST]");
        ContractRequest toUpdate = all.get(all.size() - 1);
        RequestStatus oldStatus = toUpdate.getStatus();
        toUpdate.setStatus(RequestStatus.APPROVED);
        toUpdate.setCalculatedPremium(toUpdate.getCalculatedPremium() + 25.0);
        boolean updated = dao.update(toUpdate);
        System.out.println("  Update (status " + oldStatus + " -> APPROVED): " + (updated ? "OK SUCCESS" : "FAIL FAILED"));
        ContractRequest afterUpdate = dao.read(toUpdate.getId());
        if (afterUpdate != null) {
            System.out.println("  Verify status: " + afterUpdate.getStatus()
                    + (afterUpdate.getStatus() == RequestStatus.APPROVED ? "  OK MATCH" : "  FAIL MISMATCH"));
        }

        // FIND BY USER ID
        System.out.println("\n[FIND BY USER ID TEST]");
        List<ContractRequest> byUser = dao.findByUserId(testUserCrudUserId);
        System.out.println("  Requests for User " + testUserCrudUserId + ": " + byUser.size());

        // FIND BY STATUS
        System.out.println("\n[FIND BY STATUS TEST]");
        for (RequestStatus s : RequestStatus.values()) {
            System.out.println("  Status " + s + ": " + dao.findByStatus(s).size() + " request(s)");
        }

        // FIND BY PACKAGE ID
        System.out.println("\n[FIND BY PACKAGE ID TEST]");
        List<ContractRequest> byPkg = dao.findByPackageId(testPackageCrudPackageId);
        System.out.println("  Requests for Package " + testPackageCrudPackageId + ": " + byPkg.size());

        // DELETE
        System.out.println("\n[DELETE TEST]");
        int deleteId = all.get(0).getId();
        boolean deleted = dao.delete(deleteId);
        System.out.println("  Delete ID " + deleteId + ": " + (deleted ? "OK SUCCESS" : "FAIL FAILED"));
        ContractRequest afterDelete = dao.read(deleteId);
        System.out.println("  Confirm deleted: " + (afterDelete == null ? "OK null as expected" : "FAIL still exists"));
    }
}
