package newbank.server;

public class MicroLoan {

    private double amount;
    private String repaymentTerm;

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getRepaymentTerm() {
        return repaymentTerm;
    }

    public void setRepaymentTerm(String repaymentTerm) {
        this.repaymentTerm = repaymentTerm;
    }

    public double getCurrentIncome() {
        return currentIncome;
    }

    public void setCurrentIncome(double currentIncome) {
        this.currentIncome = currentIncome;
    }

    private double currentIncome;


}
