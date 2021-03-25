package newbank.server;

import java.util.List;

public class BankLender {

    private String lenderName;

    //private double totalAmount;//TODO - would the lender have sufficient amount to lend?

    @Override
    public String toString() {
        return "BankLender{" +
                "lenderName='" + lenderName + '\'' +
                '}';
    }

    public String getLenderName() {
        return lenderName;
    }

    public void setLenderName(String lenderName) {
        this.lenderName = lenderName;
    }

}
