package tn.esprit.tests;

import tn.esprit.dao.*;
import tn.esprit.entities.*;
import java.util.List;

public class RetrieveOperationTest {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("RETRIEVE (READ) OPERATION TEST - ALL TABLES");
        System.out.println("=".repeat(80) + "\n");

        testRetrieveRole();
        testRetrieveUser();
        testRetrieveTransaction();
        testRetrieveBudget();
        testRetrieveLoan();
        testRetrieveRepayment();
        testRetrieveExpense();
        testRetrieveComplaint();
        testRetrieveInsuredAsset();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("RETRIEVE TESTS COMPLETED");
        System.out.println("=".repeat(80) + "\n");
    }

    // ======================== RETRIEVE TESTS ========================

    private static void testRetrieveRole() {
        System.out.println("\n>>> RETRIEVE ROLE TEST");
        System.out.println("─".repeat(80));

        RoleDAO roleDAO = new RoleDAO();

        // Test 1: Retrieve All Roles
        System.out.println("\n[READ ALL ROLES]");
        List<Role> allRoles = roleDAO.readAll();
        System.out.println("Total Roles found: " + allRoles.size());

        if (!allRoles.isEmpty()) {
            System.out.println("\nRoles List:");
            for (int i = 0; i < Math.min(5, allRoles.size()); i++) {
                System.out.println("  [" + (i + 1) + "] " + allRoles.get(i));
            }
            if (allRoles.size() > 5) {
                System.out.println("  ... and " + (allRoles.size() - 5) + " more roles");
            }

            // Test 2: Retrieve Role by ID
            System.out.println("\n[READ ROLE BY ID]");
            int roleId = allRoles.get(0).getId();
            Role roleById = roleDAO.read(roleId);
            System.out.println("Role ID " + roleId + ": " + (roleById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (roleById != null) {
                System.out.println("  Details: " + roleById);
            }
        } else {
            System.out.println("No roles found in database");
        }
    }

    private static void testRetrieveUser() {
        System.out.println("\n>>> RETRIEVE USER TEST");
        System.out.println("─".repeat(80));

        UserDAO userDAO = new UserDAO();

        // Test 1: Retrieve All Users
        System.out.println("\n[READ ALL USERS]");
        List<User> allUsers = userDAO.readAll();
        System.out.println("Total Users found: " + allUsers.size());

        if (!allUsers.isEmpty()) {
            System.out.println("\nUsers List:");
            for (int i = 0; i < Math.min(5, allUsers.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allUsers.get(i).getId() +
                                   " | Name: " + allUsers.get(i).getName() +
                                   " | Email: " + allUsers.get(i).getEmail());
            }
            if (allUsers.size() > 5) {
                System.out.println("  ... and " + (allUsers.size() - 5) + " more users");
            }

            // Test 2: Retrieve User by ID
            System.out.println("\n[READ USER BY ID]");
            int userId = allUsers.get(0).getId();
            User userById = userDAO.read(userId);
            System.out.println("User ID " + userId + ": " + (userById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (userById != null) {
                System.out.println("  Details: " + userById);
            }
        } else {
            System.out.println("No users found in database");
        }
    }

    private static void testRetrieveTransaction() {
        System.out.println("\n>>> RETRIEVE TRANSACTION TEST");
        System.out.println("─".repeat(80));

        TransactionDAO transactionDAO = new TransactionDAO();

        // Test 1: Retrieve All Transactions
        System.out.println("\n[READ ALL TRANSACTIONS]");
        List<Transaction> allTransactions = transactionDAO.readAll();
        System.out.println("Total Transactions found: " + allTransactions.size());

        if (!allTransactions.isEmpty()) {
            System.out.println("\nTransactions List:");
            for (int i = 0; i < Math.min(5, allTransactions.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allTransactions.get(i).getId() +
                                   " | Amount: " + allTransactions.get(i).getAmount() +
                                   " | Type: " + allTransactions.get(i).getType());
            }
            if (allTransactions.size() > 5) {
                System.out.println("  ... and " + (allTransactions.size() - 5) + " more transactions");
            }

            // Test 2: Retrieve Transaction by ID
            System.out.println("\n[READ TRANSACTION BY ID]");
            int txnId = allTransactions.get(0).getId();
            Transaction txnById = transactionDAO.read(txnId);
            System.out.println("Transaction ID " + txnId + ": " + (txnById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (txnById != null) {
                System.out.println("  Details: " + txnById);
            }
        } else {
            System.out.println("No transactions found in database");
        }
    }

    private static void testRetrieveBudget() {
        System.out.println("\n>>> RETRIEVE BUDGET TEST");
        System.out.println("─".repeat(80));

        BudgetDAO budgetDAO = new BudgetDAO();

        // Test 1: Retrieve All Budgets
        System.out.println("\n[READ ALL BUDGETS]");
        List<Budget> allBudgets = budgetDAO.readAll();
        System.out.println("Total Budgets found: " + allBudgets.size());

        if (!allBudgets.isEmpty()) {
            System.out.println("\nBudgets List:");
            for (int i = 0; i < Math.min(5, allBudgets.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allBudgets.get(i).getId() +
                                   " | Name: " + allBudgets.get(i).getName() +
                                   " | Amount: " + allBudgets.get(i).getAmount());
            }
            if (allBudgets.size() > 5) {
                System.out.println("  ... and " + (allBudgets.size() - 5) + " more budgets");
            }

            // Test 2: Retrieve Budget by ID
            System.out.println("\n[READ BUDGET BY ID]");
            int budgetId = allBudgets.get(0).getId();
            Budget budgetById = budgetDAO.read(budgetId);
            System.out.println("Budget ID " + budgetId + ": " + (budgetById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (budgetById != null) {
                System.out.println("  Details: " + budgetById);
            }
        } else {
            System.out.println("No budgets found in database");
        }
    }

    private static void testRetrieveLoan() {
        System.out.println("\n>>> RETRIEVE LOAN TEST");
        System.out.println("─".repeat(80));

        LoanDAO loanDAO = new LoanDAO();

        // Test 1: Retrieve All Loans
        System.out.println("\n[READ ALL LOANS]");
        List<Loan> allLoans = loanDAO.readAll();
        System.out.println("Total Loans found: " + allLoans.size());

        if (!allLoans.isEmpty()) {
            System.out.println("\nLoans List:");
            for (int i = 0; i < Math.min(5, allLoans.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allLoans.get(i).getId() +
                                   " | Amount: " + allLoans.get(i).getAmount() +
                                   " | Interest Rate: " + allLoans.get(i).getInterestRate());
            }
            if (allLoans.size() > 5) {
                System.out.println("  ... and " + (allLoans.size() - 5) + " more loans");
            }

            // Test 2: Retrieve Loan by ID
            System.out.println("\n[READ LOAN BY ID]");
            int loanId = allLoans.get(0).getId();
            Loan loanById = loanDAO.read(loanId);
            System.out.println("Loan ID " + loanId + ": " + (loanById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (loanById != null) {
                System.out.println("  Details: " + loanById);
            }
        } else {
            System.out.println("No loans found in database");
        }
    }

    private static void testRetrieveRepayment() {
        System.out.println("\n>>> RETRIEVE REPAYMENT TEST");
        System.out.println("─".repeat(80));

        RepaymentDAO repaymentDAO = new RepaymentDAO();

        // Test 1: Retrieve All Repayments
        System.out.println("\n[READ ALL REPAYMENTS]");
        List<Repayment> allRepayments = repaymentDAO.readAll();
        System.out.println("Total Repayments found: " + allRepayments.size());

        if (!allRepayments.isEmpty()) {
            System.out.println("\nRepayments List:");
            for (int i = 0; i < Math.min(5, allRepayments.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allRepayments.get(i).getId() +
                                   " | Amount: " + allRepayments.get(i).getAmount() +
                                   " | Status: " + allRepayments.get(i).getStatus());
            }
            if (allRepayments.size() > 5) {
                System.out.println("  ... and " + (allRepayments.size() - 5) + " more repayments");
            }

            // Test 2: Retrieve Repayment by ID
            System.out.println("\n[READ REPAYMENT BY ID]");
            int repaymentId = allRepayments.get(0).getId();
            Repayment repaymentById = repaymentDAO.read(repaymentId);
            System.out.println("Repayment ID " + repaymentId + ": " + (repaymentById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (repaymentById != null) {
                System.out.println("  Details: " + repaymentById);
            }
        } else {
            System.out.println("No repayments found in database");
        }
    }

    private static void testRetrieveExpense() {
        System.out.println("\n>>> RETRIEVE EXPENSE TEST");
        System.out.println("─".repeat(80));

        ExpenseDAO expenseDAO = new ExpenseDAO();

        // Test 1: Retrieve All Expenses
        System.out.println("\n[READ ALL EXPENSES]");
        List<Expense> allExpenses = expenseDAO.readAll();
        System.out.println("Total Expenses found: " + allExpenses.size());

        if (!allExpenses.isEmpty()) {
            System.out.println("\nExpenses List:");
            for (int i = 0; i < Math.min(5, allExpenses.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allExpenses.get(i).getId() +
                                   " | Amount: " + allExpenses.get(i).getAmount() +
                                   " | Category: " + allExpenses.get(i).getCategory());
            }
            if (allExpenses.size() > 5) {
                System.out.println("  ... and " + (allExpenses.size() - 5) + " more expenses");
            }

            // Test 2: Retrieve Expense by ID
            System.out.println("\n[READ EXPENSE BY ID]");
            int expenseId = allExpenses.get(0).getId();
            Expense expenseById = expenseDAO.read(expenseId);
            System.out.println("Expense ID " + expenseId + ": " + (expenseById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (expenseById != null) {
                System.out.println("  Details: " + expenseById);
            }
        } else {
            System.out.println("No expenses found in database");
        }
    }

    private static void testRetrieveComplaint() {
        System.out.println("\n>>> RETRIEVE COMPLAINT TEST");
        System.out.println("─".repeat(80));

        ComplaintDAO complaintDAO = new ComplaintDAO();

        // Test 1: Retrieve All Complaints
        System.out.println("\n[READ ALL COMPLAINTS]");
        List<Complaint> allComplaints = complaintDAO.readAll();
        System.out.println("Total Complaints found: " + allComplaints.size());

        if (!allComplaints.isEmpty()) {
            System.out.println("\nComplaints List:");
            for (int i = 0; i < Math.min(5, allComplaints.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allComplaints.get(i).getId() +
                                   " | Subject: " + allComplaints.get(i).getSubject() +
                                   " | Status: " + allComplaints.get(i).getStatus());
            }
            if (allComplaints.size() > 5) {
                System.out.println("  ... and " + (allComplaints.size() - 5) + " more complaints");
            }

            // Test 2: Retrieve Complaint by ID
            System.out.println("\n[READ COMPLAINT BY ID]");
            int complaintId = allComplaints.get(0).getId();
            Complaint complaintById = complaintDAO.read(complaintId);
            System.out.println("Complaint ID " + complaintId + ": " + (complaintById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (complaintById != null) {
                System.out.println("  Details: " + complaintById);
            }
        } else {
            System.out.println("No complaints found in database");
        }
    }

    private static void testRetrieveInsuredAsset() {
        System.out.println("\n>>> RETRIEVE INSURED ASSET TEST");
        System.out.println("─".repeat(80));

        InsuredAssetDAO assetDAO = new InsuredAssetDAO();

        // Test 1: Retrieve All Assets
        System.out.println("\n[READ ALL INSURED ASSETS]");
        List<InsuredAsset> allAssets = assetDAO.readAll();
        System.out.println("Total Insured Assets found: " + allAssets.size());

        if (!allAssets.isEmpty()) {
            System.out.println("\nInsured Assets List:");
            for (int i = 0; i < Math.min(5, allAssets.size()); i++) {
                System.out.println("  [" + (i + 1) + "] ID: " + allAssets.get(i).getId() +
                                   " | Reference: " + allAssets.get(i).getReference() +
                                   " | Type: " + allAssets.get(i).getType() +
                                   " | DeclaredValue: " + allAssets.get(i).getDeclaredValue());
            }
            if (allAssets.size() > 5) {
                System.out.println("  ... and " + (allAssets.size() - 5) + " more assets");
            }

            // Test 2: Retrieve Asset by ID
            System.out.println("\n[READ INSURED ASSET BY ID]");
            int assetId = allAssets.get(0).getId();
            InsuredAsset assetById = assetDAO.read(assetId);
            System.out.println("Insured Asset ID " + assetId + ": " + (assetById != null ? "✓ FOUND" : "✗ NOT FOUND"));
            if (assetById != null) {
                System.out.println("  Details: " + assetById);
            }
        } else {
            System.out.println("No insured assets found in database");
        }
    }
}

