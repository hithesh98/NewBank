package newbank.server;

import java.util.ArrayList;

public class Customer {
	
	private static ArrayList<Account> accounts;
	
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

	public static void addAccount(Account account) {
		accounts.add(account);		
	}
}
