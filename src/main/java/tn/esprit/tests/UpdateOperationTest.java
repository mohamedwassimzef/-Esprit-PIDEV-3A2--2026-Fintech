package tn.esprit.tests;

import tn.esprit.dao.*;
import tn.esprit.entities.*;
import tn.esprit.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class UpdateOperationTest {

    public static void main(String[] args) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("UPDATE OPERATION TEST - ALL TABLES");
        System.out.println("=".repeat(80) + "\n");

        testUpdateRole();
        testUpdateUser();
        testUpdateTransaction();
        testUpdateBudget();
        testUpdateLoan();
        testUpdateRepayment();
        testUpdateExpense();
        testUpdateComplaint();
        testUpdateInsuredAsset();

        System.out.println("\n" + "=".repeat(80));
        System.out.println("UPDATE TESTS COMPLETED");
        System.out.println("=".repeat(80) + "\n");
    }

    // ======================== UPDATE TESTS ========================

    private static void testUpdateRole() {
        System.out.println("\n>>> UPDATE ROLE TEST");
        System.out.println("─".repeat(80));

        RoleDAO roleDAO = new RoleDAO();

        // Get first role to update
        System.out.println("\n[SELECT ROLE FOR UPDATE]");
        List<Role> allRoles = roleDAO.readAll();
        System.out.println("Total Roles found: " + allRoles.size());

        if (!allRoles.isEmpty()) {
            Role roleToUpdate = allRoles.get(0);
            int roleId = roleToUpdate.getId();
            System.out.println("Selected Role ID: " + roleId);
            System.out.println("Before Update: " + roleToUpdate);

            // Update role
            System.out.println("\n[PERFORMING UPDATE]");
            String newPermissions = "{\"read\": true, \"write\": true, \"delete\": true, \"admin\": true}";
            roleToUpdate.setPermissions(newPermissions);
            boolean updated = roleDAO.update(roleToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Role updatedRole = roleDAO.read(roleId);
            if (updatedRole != null) {
                System.out.println("After Update: " + updatedRole);
                System.out.println("Verification: " + (updatedRole.getPermissions().contains("admin") ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No roles found in database");
        }
    }

    private static void testUpdateUser() {
        System.out.println("\n>>> UPDATE USER TEST");
        System.out.println("─".repeat(80));

        UserDAO userDAO = new UserDAO();

        // Get first user to update
        System.out.println("\n[SELECT USER FOR UPDATE]");
        List<User> allUsers = userDAO.readAll();
        System.out.println("Total Users found: " + allUsers.size());

        if (!allUsers.isEmpty()) {
            User userToUpdate = allUsers.get(0);
            int userId = userToUpdate.getId();
            System.out.println("Selected User ID: " + userId);
            System.out.println("Before Update: Name=" + userToUpdate.getName() +
                             " | Phone=" + userToUpdate.getPhone() +
                             " | Verified=" + userToUpdate.isVerified());

            // Update user
            System.out.println("\n[PERFORMING UPDATE]");
            userToUpdate.setName("Updated User Name");
            userToUpdate.setPhone("+216-99-888-777");
            userToUpdate.setVerified(true);
            boolean updated = userDAO.update(userToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            User updatedUser = userDAO.read(userId);
            if (updatedUser != null) {
                System.out.println("After Update: Name=" + updatedUser.getName() +
                                 " | Phone=" + updatedUser.getPhone() +
                                 " | Verified=" + updatedUser.isVerified());
                boolean nameMatch = updatedUser.getName().equals("Updated User Name");
                boolean phoneMatch = updatedUser.getPhone().equals("+216-99-888-777");
                boolean verifiedMatch = updatedUser.isVerified();
                System.out.println("Verification: " + (nameMatch && phoneMatch && verifiedMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No users found in database");
        }
    }

    private static void testUpdateTransaction() {
        System.out.println("\n>>> UPDATE TRANSACTION TEST");
        System.out.println("─".repeat(80));

        TransactionDAO transactionDAO = new TransactionDAO();

        // Get first transaction to update
        System.out.println("\n[SELECT TRANSACTION FOR UPDATE]");
        List<Transaction> allTransactions = transactionDAO.readAll();
        System.out.println("Total Transactions found: " + allTransactions.size());

        if (!allTransactions.isEmpty()) {
            Transaction txnToUpdate = allTransactions.get(0);
            int txnId = txnToUpdate.getId();
            System.out.println("Selected Transaction ID: " + txnId);
            System.out.println("Before Update: Amount=" + txnToUpdate.getAmount() +
                             " | Status=" + txnToUpdate.getStatus() +
                             " | Description=" + txnToUpdate.getDescription());

            // Update transaction
            System.out.println("\n[PERFORMING UPDATE]");
            txnToUpdate.setStatus(TransactionStatus.COMPLETED);
            txnToUpdate.setDescription("Updated - Transaction completed successfully");
            boolean updated = transactionDAO.update(txnToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Transaction updatedTxn = transactionDAO.read(txnId);
            if (updatedTxn != null) {
                System.out.println("After Update: Amount=" + updatedTxn.getAmount() +
                                 " | Status=" + updatedTxn.getStatus() +
                                 " | Description=" + updatedTxn.getDescription());
                boolean statusMatch = updatedTxn.getStatus() == TransactionStatus.COMPLETED;
                boolean descMatch = updatedTxn.getDescription().contains("Updated");
                System.out.println("Verification: " + (statusMatch && descMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No transactions found in database");
        }
    }

    private static void testUpdateBudget() {
        System.out.println("\n>>> UPDATE BUDGET TEST");
        System.out.println("─".repeat(80));

        BudgetDAO budgetDAO = new BudgetDAO();

        // Get first budget to update
        System.out.println("\n[SELECT BUDGET FOR UPDATE]");
        List<Budget> allBudgets = budgetDAO.readAll();
        System.out.println("Total Budgets found: " + allBudgets.size());

        if (!allBudgets.isEmpty()) {
            Budget budgetToUpdate = allBudgets.get(0);
            int budgetId = budgetToUpdate.getId();
            System.out.println("Selected Budget ID: " + budgetId);
            System.out.println("Before Update: Name=" + budgetToUpdate.getName() +
                             " | Amount=" + budgetToUpdate.getAmount() +
                             " | SpentAmount=" + budgetToUpdate.getSpentAmount());

            // Update budget
            System.out.println("\n[PERFORMING UPDATE]");
            budgetToUpdate.setName("Updated Budget - " + System.currentTimeMillis());
            budgetToUpdate.setAmount(new BigDecimal("2500.00"));
            budgetToUpdate.setSpentAmount(new BigDecimal("1200.50"));
            boolean updated = budgetDAO.update(budgetToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Budget updatedBudget = budgetDAO.read(budgetId);
            if (updatedBudget != null) {
                System.out.println("After Update: Name=" + updatedBudget.getName() +
                                 " | Amount=" + updatedBudget.getAmount() +
                                 " | SpentAmount=" + updatedBudget.getSpentAmount());
                boolean amountMatch = updatedBudget.getAmount().compareTo(new BigDecimal("2500.00")) == 0;
                boolean spentMatch = updatedBudget.getSpentAmount().compareTo(new BigDecimal("1200.50")) == 0;
                System.out.println("Verification: " + (amountMatch && spentMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No budgets found in database");
        }
    }

    private static void testUpdateLoan() {
        System.out.println("\n>>> UPDATE LOAN TEST");
        System.out.println("─".repeat(80));

        LoanDAO loanDAO = new LoanDAO();

        // Get first loan to update
        System.out.println("\n[SELECT LOAN FOR UPDATE]");
        List<Loan> allLoans = loanDAO.readAll();
        System.out.println("Total Loans found: " + allLoans.size());

        if (!allLoans.isEmpty()) {
            Loan loanToUpdate = allLoans.get(0);
            int loanId = loanToUpdate.getId();
            System.out.println("Selected Loan ID: " + loanId);
            System.out.println("Before Update: Amount=" + loanToUpdate.getAmount() +
                             " | InterestRate=" + loanToUpdate.getInterestRate() +
                             " | Status=" + loanToUpdate.getStatus());

            // Update loan
            System.out.println("\n[PERFORMING UPDATE]");
            loanToUpdate.setStatus("partially_paid");
            boolean updated = loanDAO.update(loanToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Loan updatedLoan = loanDAO.read(loanId);
            if (updatedLoan != null) {
                System.out.println("After Update: Amount=" + updatedLoan.getAmount() +
                                 " | InterestRate=" + updatedLoan.getInterestRate() +
                                 " | Status=" + updatedLoan.getStatus());
                System.out.println("Verification: " + (updatedLoan.getStatus().equals("partially_paid") ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No loans found in database");
        }
    }

    private static void testUpdateRepayment() {
        System.out.println("\n>>> UPDATE REPAYMENT TEST");
        System.out.println("─".repeat(80));

        RepaymentDAO repaymentDAO = new RepaymentDAO();

        // Get first repayment to update
        System.out.println("\n[SELECT REPAYMENT FOR UPDATE]");
        List<Repayment> allRepayments = repaymentDAO.readAll();
        System.out.println("Total Repayments found: " + allRepayments.size());

        if (!allRepayments.isEmpty()) {
            Repayment repaymentToUpdate = allRepayments.get(0);
            int repaymentId = repaymentToUpdate.getId();
            System.out.println("Selected Repayment ID: " + repaymentId);
            System.out.println("Before Update: Amount=" + repaymentToUpdate.getAmount() +
                             " | Status=" + repaymentToUpdate.getStatus() +
                             " | PaymentType=" + repaymentToUpdate.getPaymentType());

            // Update repayment
            System.out.println("\n[PERFORMING UPDATE]");
            repaymentToUpdate.setStatus("paid");
            repaymentToUpdate.setPaymentType("Bank Transfer - Updated");
            boolean updated = repaymentDAO.update(repaymentToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Repayment updatedRepayment = repaymentDAO.read(repaymentId);
            if (updatedRepayment != null) {
                System.out.println("After Update: Amount=" + updatedRepayment.getAmount() +
                                 " | Status=" + updatedRepayment.getStatus() +
                                 " | PaymentType=" + updatedRepayment.getPaymentType());
                boolean statusMatch = updatedRepayment.getStatus().equals("paid");
                boolean paymentMatch = updatedRepayment.getPaymentType().contains("Updated");
                System.out.println("Verification: " + (statusMatch && paymentMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No repayments found in database");
        }
    }

    private static void testUpdateExpense() {
        System.out.println("\n>>> UPDATE EXPENSE TEST");
        System.out.println("─".repeat(80));

        ExpenseDAO expenseDAO = new ExpenseDAO();

        // Get first expense to update
        System.out.println("\n[SELECT EXPENSE FOR UPDATE]");
        List<Expense> allExpenses = expenseDAO.readAll();
        System.out.println("Total Expenses found: " + allExpenses.size());

        if (!allExpenses.isEmpty()) {
            Expense expenseToUpdate = allExpenses.get(0);
            int expenseId = expenseToUpdate.getId();
            System.out.println("Selected Expense ID: " + expenseId);
            System.out.println("Before Update: Amount=" + expenseToUpdate.getAmount() +
                             " | Category=" + expenseToUpdate.getCategory() +
                             " | Description=" + expenseToUpdate.getDescription());

            // Update expense
            System.out.println("\n[PERFORMING UPDATE]");
            expenseToUpdate.setAmount(new BigDecimal("150.75"));
            expenseToUpdate.setCategory("Updated - Food & Groceries");
            expenseToUpdate.setDescription("Updated expense description");
            boolean updated = expenseDAO.update(expenseToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Expense updatedExpense = expenseDAO.read(expenseId);
            if (updatedExpense != null) {
                System.out.println("After Update: Amount=" + updatedExpense.getAmount() +
                                 " | Category=" + updatedExpense.getCategory() +
                                 " | Description=" + updatedExpense.getDescription());
                boolean amountMatch = updatedExpense.getAmount().compareTo(new BigDecimal("150.75")) == 0;
                boolean categoryMatch = updatedExpense.getCategory().contains("Updated");
                System.out.println("Verification: " + (amountMatch && categoryMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No expenses found in database");
        }
    }

    private static void testUpdateComplaint() {
        System.out.println("\n>>> UPDATE COMPLAINT TEST");
        System.out.println("─".repeat(80));

        ComplaintDAO complaintDAO = new ComplaintDAO();

        // Get first complaint to update
        System.out.println("\n[SELECT COMPLAINT FOR UPDATE]");
        List<Complaint> allComplaints = complaintDAO.readAll();
        System.out.println("Total Complaints found: " + allComplaints.size());

        if (!allComplaints.isEmpty()) {
            Complaint complaintToUpdate = allComplaints.get(0);
            int complaintId = complaintToUpdate.getId();
            System.out.println("Selected Complaint ID: " + complaintId);
            System.out.println("Before Update: Subject=" + complaintToUpdate.getSubject() +
                             " | Status=" + complaintToUpdate.getStatus() +
                             " | Response=" + complaintToUpdate.getResponse());

            // Update complaint
            System.out.println("\n[PERFORMING UPDATE]");
            complaintToUpdate.setStatus("resolved");
            complaintToUpdate.setResponse("Issue has been resolved. Thank you for your patience.");
            boolean updated = complaintDAO.update(complaintToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            Complaint updatedComplaint = complaintDAO.read(complaintId);
            if (updatedComplaint != null) {
                System.out.println("After Update: Subject=" + updatedComplaint.getSubject() +
                                 " | Status=" + updatedComplaint.getStatus() +
                                 " | Response=" + updatedComplaint.getResponse());
                boolean statusMatch = updatedComplaint.getStatus().equals("resolved");
                boolean responseMatch = updatedComplaint.getResponse() != null &&
                                      updatedComplaint.getResponse().contains("resolved");
                System.out.println("Verification: " + (statusMatch && responseMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No complaints found in database");
        }
    }

    private static void testUpdateInsuredAsset() {
        System.out.println("\n>>> UPDATE INSURED ASSET TEST");
        System.out.println("─".repeat(80));

        InsuredAssetDAO assetDAO = new InsuredAssetDAO();

        // Get first asset to update
        System.out.println("\n[SELECT INSURED ASSET FOR UPDATE]");
        List<InsuredAsset> allAssets = assetDAO.readAll();
        System.out.println("Total Insured Assets found: " + allAssets.size());

        if (!allAssets.isEmpty()) {
            InsuredAsset assetToUpdate = allAssets.get(0);
            int assetId = assetToUpdate.getId();
            System.out.println("Selected Asset ID: " + assetId);
            System.out.println("Before Update: Reference=" + assetToUpdate.getReference() +
                             " | Type=" + assetToUpdate.getType() +
                             " | DeclaredValue=" + assetToUpdate.getDeclaredValue());

            // Update asset
            System.out.println("\n[PERFORMING UPDATE]");
            assetToUpdate.setReference("Updated Asset - " + System.currentTimeMillis());
            assetToUpdate.setType("Premium Vehicle");
            assetToUpdate.setDeclaredValue(new java.math.BigDecimal("75000.00"));
            assetToUpdate.setDescription("Updated: High-value asset with premium coverage");
            boolean updated = assetDAO.update(assetToUpdate);
            System.out.println("Update Status: " + (updated ? "✓ SUCCESS" : "✗ FAILED"));

            // Verify update
            System.out.println("\n[VERIFYING UPDATE]");
            InsuredAsset updatedAsset = assetDAO.read(assetId);
            if (updatedAsset != null) {
                System.out.println("After Update: Reference=" + updatedAsset.getReference() +
                                 " | Type=" + updatedAsset.getType() +
                                 " | DeclaredValue=" + updatedAsset.getDeclaredValue());
                boolean typeMatch  = updatedAsset.getType().equals("Premium Vehicle");
                boolean valueMatch = new java.math.BigDecimal("75000.00").compareTo(updatedAsset.getDeclaredValue()) == 0;
                System.out.println("Verification: " + (typeMatch && valueMatch ? "✓ VERIFIED" : "✗ NOT VERIFIED"));
            }
        } else {
            System.out.println("No insured assets found in database");
        }
    }
}

