
package Model;

public class Account {
    private int accountID;
    private double balance;

    public Account(int accountID, double balance) {
        this.accountID = accountID;
        this.balance = balance;
    }

    public int getAccountID() {
        return accountID;
    }

    public void setAccountID(int accountID) {
        this.accountID = accountID;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account ID:" + accountID
                + "\nBalance:" + balance;
    }

    public void deposit(double amount) {
        if (amount > 0) {
            balance = balance + amount;
            System.out.println("You have successfully deposited:" + amount);
            System.out.println("Your current balance is" + balance);
        } else {
            System.out.println("Insufficient amount to deposit");
        }
    }

    public void withdraw(double amount) {
        if (amount > 0 && amount <= balance) {
            balance = balance - amount;
            System.out.println("You have successfully withdrew:" + amount);
        } else {
            System.out.println("Insufficient amount to withdraw");
        }
    }

    public void processTransaction(String transactionType, double amount) {
        if (transactionType.equalsIgnoreCase("deposit")) {
            deposit(amount);
        } else if (transactionType.equalsIgnoreCase("withdraw")) {
            withdraw(amount);
        } else {
            System.out.println("Invalid transaction type");
        }
    }

    // Transfer money to another account
    public boolean transferTo(Account receiver, double amount) {
        if (amount > 0 && amount <= balance) {
            balance = balance - amount;
            receiver.balance = receiver.balance + amount;
            return true;
        }

        return false;
    }
}
