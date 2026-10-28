package Model;

public class WalletAccount extends Account {
        public WalletAccount(int accountID, double balance){
            super(accountID,balance);
        }

@Override
    public String toString(){
            return "Wallet Account\n" + super.toString();
}

@Override
    public void withdraw(double amount){
    System.out.println("Proccesing withdraw");
    super.withdraw(amount);
}



    }




