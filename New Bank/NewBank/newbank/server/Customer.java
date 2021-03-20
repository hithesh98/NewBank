package newbank.server;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
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
		String accountString = account.toString();
		accountString.replaceAll("\\s", "");
		String[] newAcc = accountString.split(":");

		try {
			FileWriter fw = new FileWriter(".\\New Bank\\NewBank\\newbank\\server\\data.csv", true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			for(String val : newAcc){
				pw.print("," + val);
			}
			pw.flush();
			pw.close();
		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();
		}

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
