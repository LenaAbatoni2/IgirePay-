package Model;

public class Customer {
    private int customerid;
    private String fullnames;
    private String pin;
    private String phone_number;
    private String email;


    public Customer(int customerid, String fullnames,String pin,String phone_number, String email){
        this.customerid=customerid;
        this.fullnames=fullnames;
        this.pin=pin;
        this.phone_number=phone_number;
        this.email=email;
    }

    public int getCustomerid() {
        return customerid;
    }

    public void setCustomerid(int customerid) {
        this.customerid = customerid;
    }

    public String getFullnames() {
        return fullnames;
    }

    public void setFullnames(String fullnames) {
        this.fullnames = fullnames;
    }

    public String getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = pin;
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public String toString() {
        return "Customer" +
                "customerid=" + customerid +
                "\nfullnames='" + fullnames + '\'' +
                "\nphone_number='" + phone_number + '\'' +
                "\nemail='" + email;
    }
}
