package newbank.server;

import java.util.ArrayList;

public class Customer {
	
	private ArrayList<Account> accounts;
	
	public Customer() {
		accounts = new ArrayList<>();
	}
	
	public String accountsToString() {
		String s = "";
		for(Account a : accounts) {
			s += a.toString();
		}
		return s;
	}

	public void addAccount(Account account) {
		accounts.add(account);		
	}

	public boolean editAccountBalance(String accountName, double amount) {
		boolean accountExists = false;
		int accountIndex = -1;
		for (Account a : accounts) { // Search for account with the matching name
			accountIndex+=1;
			if (a.getAccountName().equals(accountName)) {
				accountExists = true;
				break;
			}
		}

		if (accountExists) {
			Account accountToEdit = accounts.get(accountIndex);
			if(accountToEdit.getOpeningBalance() + amount >= 0) {
				accountToEdit.editOpeningBalance(amount);
				return true;
			}
		}
		return false;
	}
}
