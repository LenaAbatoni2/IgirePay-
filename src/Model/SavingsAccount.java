package Model;

public class SavingsAccount extends Account {
    public SavingsAccount(int accountID, double balance) {
        super(accountID, balance);
    }


    @Override
    public String toString() {
        return "Savings account\n" + super.toString();
    }


    @Override
    public void withdraw(double amount) {
        double fee = 100;

        if (amount > 0 && amount + fee <= getBalance()) {
            super.withdraw(amount);
            setBalance(getBalance() - fee);

            System.out.println("Withdrawal fee: " + fee + " FRW");
            System.out.println("Current balance: " + getBalance() + " FRW");
        } else {
            System.out.println("Insufficient balance or invalid amount.");
        }
    }

}