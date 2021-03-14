package newbank.server;

import java.util.HashMap;

public class NewBank {
	
	private static final NewBank bank = new NewBank();
	private HashMap<String,Customer> customers;
	private HashMap <String, Loan> creditAgreements; 

	private NewBank() {
		customers = new HashMap<>();
		addTestData();
	}

	private void addTestData() {
		Customer bhagy = new Customer();
		bhagy.addAccount(new Account("Main", 1000.0));
		customers.put("Bhagy", bhagy);
		
		Customer christina = new Customer();
		christina.addAccount(new Account("Savings", 1500.0));
		customers.put("Christina", christina);
		
		Customer john = new Customer();
		john.addAccount(new Account("Checking", 250.0));
		john.addAccount(new Account("Savings", 9999.0)); //Check if more than one account can be seen 
		customers.put("John", john);
	}
	
	public static NewBank getBank() {
		return bank;
	}
	
	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		if(customers.containsKey(userName)) {
			return new CustomerID(userName);
		}
		return null;
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request) {
		if(customers.containsKey(customer.getKey())) {
			if (request.startsWith("NEWACCOUNT")){
			return openAccount(request.substring(request.indexOf(" ")));
			}
			switch(request) {
			case "SHOWMYACCOUNTS" : return showMyAccounts(customer);
			case "NEWACCOUNT" : return addAccount(customer);
			case "PAY" : return pay(customer, commands); 
			case "LOAN" : return loan(customer, commands); 
			default : return "FAIL";
			}
		}
		return "FAIL";
	}
	

	private String pay(CustomerID customer, String[] account) {
		// PAY <Person/Company> <Amount>
		CustomerID payeeCustomerId = new CustomerID(account[1]);
		Customer payeeCustomer = customers.get(payeeCustomerId.toString());
		Customer payingCustomer = customers.get(customer.getKey());

		Double amount = Double.parseDouble(account[2]);
		// Cannot pay negative or zero amounts
		if (amount <= 0) {
			return "FAIL";
		}

		if (payeeCustomer != null && payingCustomer != null) {
			return payeeCustomer.pay(amount) && payingCustomer.pay(-amount) ? "SUCCESS" : "FAIL";
		} 
		return "FAIL";
	}

	private String openAccount(String accountName) {
		Customer.addAccount(new Account(accountName, 0.00));
		return "SUCCESS";
	}

	private String showMyAccounts(CustomerID customer) {
		return (customers.get(customer.getKey())).accountsToString();
	}

	
}
