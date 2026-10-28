package Model;

import java.time.LocalDateTime;

public class Transactions {
    private int transactionID;
    private String referenceID;
    private double amount;
    private String transactionType;
    private LocalDateTime timestamp;


public Transactions(int transactionID, String referenceID,double amount, String transactionType,
                    LocalDateTime timestamp){
    this.transactionID=transactionID;
    this.referenceID=referenceID;
    this.amount=amount;
    this. transactionType=transactionType;
    this.timestamp=timestamp;
}

    public int getTransactionID(){
    return transactionID;
    }

    public void setTransactionID(int transactionID){
    this.transactionID=transactionID;
    }

    public String getReferenceID() {
        return referenceID;
    }

    public void setReferenceID(String referenceID) {
        this.referenceID = referenceID;
    }


    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;

    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString(){
    return " TransactionID:"+transactionID
            +"\nReferenceID:"+referenceID
            +"\nAmount:"+amount
            +"\nTransaction Type:"+transactionType
            +"\n Timestamp:"+timestamp;
    }
}
