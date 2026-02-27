package tn.esprit.tests;

import tn.esprit.dao.*;
import tn.esprit.entities.*;
import tn.esprit.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class CreateOperationTest {
    private static int createdUserId = -1;
    private static int createdLoanId = -1;

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CREATE OPERATION TEST - ALL TABLES");
        System.out.println("=".repeat(80) + "\n");

        testCreateRole();
        testCreateUser();

        if (createdUserId != -1) {
            testCreateTransaction();
            testCreateBudget();
            testCreateLoan();

            if (createdLoanId != -1) {
                testCreateRepayment();
            }

            testCreateComplaint();
            testCreateInsuredAsset();
        }

        testCreateExpense();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("CREATE TESTS COMPLETED");
        System.out.println("=".repeat(80) + "\n");
    }

    private static void testCreateRole() {
        System.out.println("\n>>> CREATE ROLE TEST");
        RoleDAO roleDAO = new RoleDAO();
        long timestamp = System.currentTimeMillis();
        Role role = new Role("test_role_" + timestamp, "{\"permissions\": \"read,write\"}");
        boolean result = roleDAO.create(role);
        System.out.println("Create Role: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateUser() {
        System.out.println("\n>>> CREATE USER TEST");
        UserDAO userDAO = new UserDAO();
        long timestamp = System.currentTimeMillis();
        User user = new User("Test User", "test.user." + timestamp + "@example.com", "hashed_password_123", 1, "+216-99-123-456");
        boolean result = userDAO.create(user);
        System.out.println("Create User: " + (result ? "✓ SUCCESS" : "✗ FAILED"));

        if (result) {
            List<User> allUsers = userDAO.readAll();
            if (!allUsers.isEmpty()) {
                createdUserId = allUsers.get(allUsers.size() - 1).getId();
                System.out.println("  Stored User ID: " + createdUserId);
            }
        }
    }

    private static void testCreateTransaction() {
        System.out.println("\n>>> CREATE TRANSACTION TEST");
        TransactionDAO transactionDAO = new TransactionDAO();
        Transaction txn = new Transaction(createdUserId, new BigDecimal("1000.50"), TransactionType.CREDIT,
                "Test transaction", ReferenceType.ONLINE, null);
        boolean result = transactionDAO.create(txn);
        System.out.println("Create Transaction: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateBudget() {
        System.out.println("\n>>> CREATE BUDGET TEST");
        BudgetDAO budgetDAO = new BudgetDAO();
        long timestamp = System.currentTimeMillis();
        Budget budget = new Budget("Test Budget " + timestamp, new BigDecimal("1000.00"),
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28), createdUserId, "Testing");
        boolean result = budgetDAO.create(budget);
        System.out.println("Create Budget: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateLoan() {
        System.out.println("\n>>> CREATE LOAN TEST");
        LoanDAO loanDAO = new LoanDAO();
        Loan loan = new Loan(createdUserId, new BigDecimal("5000.00"), new BigDecimal("4.50"),
                LocalDate.of(2026, 2, 8), LocalDate.of(2027, 2, 8));
        boolean result = loanDAO.create(loan);
        System.out.println("Create Loan: " + (result ? "✓ SUCCESS" : "✗ FAILED"));

        if (result) {
            List<Loan> allLoans = loanDAO.readAll();
            if (!allLoans.isEmpty()) {
                createdLoanId = allLoans.get(allLoans.size() - 1).getId();
                System.out.println("  Stored Loan ID: " + createdLoanId);
            }
        }
    }

    private static void testCreateRepayment() {
        System.out.println("\n>>> CREATE REPAYMENT TEST");
        RepaymentDAO repaymentDAO = new RepaymentDAO();
        Repayment repayment = new Repayment(createdLoanId, new BigDecimal("250.00"),
                LocalDate.of(2026, 3, 8), "Bank Transfer");
        repayment.setMonthlyPayment(new BigDecimal("250.00"));
        boolean result = repaymentDAO.create(repayment);
        System.out.println("Create Repayment: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateExpense() {
        System.out.println("\n>>> CREATE EXPENSE TEST");
        ExpenseDAO expenseDAO = new ExpenseDAO();
        Expense expense = new Expense(new BigDecimal("99.99"), "Food",
                LocalDate.of(2026, 2, 8), "Test expense", null);
        boolean result = expenseDAO.create(expense);
        System.out.println("Create Expense: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateComplaint() {
        System.out.println("\n>>> CREATE COMPLAINT TEST");
        ComplaintDAO complaintDAO = new ComplaintDAO();
        long timestamp = System.currentTimeMillis();
        Complaint complaint = new Complaint("Test complaint subject " + timestamp, LocalDate.of(2026, 2, 8), createdUserId);
        boolean result = complaintDAO.create(complaint);
        System.out.println("Create Complaint: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }

    private static void testCreateInsuredAsset() {
        System.out.println("\n>>> CREATE INSURED ASSET TEST");
        InsuredAssetDAO assetDAO = new InsuredAssetDAO();
        long timestamp = System.currentTimeMillis();
        InsuredAsset asset = new InsuredAsset("Test Asset " + timestamp, "TestType", 9999.99,
                "Test asset description", "Test Location", createdUserId);
        boolean result = assetDAO.create(asset);
        System.out.println("Create InsuredAsset: " + (result ? "✓ SUCCESS" : "✗ FAILED"));
    }
}

