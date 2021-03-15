package newbank.server;

public class Account {

	private String accountName;
	private double openingBalance;

	public Account(String accountName, double openingBalance) {
		this.accountName = accountName;
		this.openingBalance = openingBalance;
	}

	public String toString() {
		return (accountName + ": " + openingBalance + " ");
	}

	public String getAccountName() {
		return accountName;
	}

	public double getOpeningBalance() {
		return openingBalance;
	}

	public void editOpeningBalance(double amount) {
		this.openingBalance += amount;
	}

}
