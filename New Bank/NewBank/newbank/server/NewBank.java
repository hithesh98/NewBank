package newbank.server;

import java.util.HashMap;

public class NewBank {

	private static final NewBank bank = new NewBank();
	private HashMap<String, Customer> customers;

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
		customers.put("John", john);
	}

	public static NewBank getBank() {
		return bank;
	}

	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		if (customers.containsKey(userName)) {
			return new CustomerID(userName);
		}
		return null;
	}

	// login, signup, and logoff initial requests
	public synchronized String processRequest(String request) {
		String[] input = request.split(" "); // create an array of the parsed input string
		if(input[0].equals("LOGIN")) {
			return "SUCCESS";
		} else if (input[0].equals("SIGNUP")){
			if(input.length < 3){
				return "FAIL";
			}
			String name = input[1];
			if(customers.containsKey(name)){
				return "FAIL";
			}
			double initialDeposit = Double.parseDouble(input[2]);
			return signUp(name, initialDeposit);
		} else {
			return "FAIL";
		}
		if (request.startsWith("LOGOFF")){
			return "LOGOFF";
		}
	}

	private String signUp(String name, double initialDeposit) {
		Customer newCustomer = new Customer();
		newCustomer.addAccount(new Account("Main", initialDeposit));
		customers.put(name, newCustomer);
		return "SUCCESS";
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request) {
		if(customers.containsKey(customer.getKey())) {
			String[] input = request.split(" "); // create an array of the parsed input string
			if (request.startsWith("NEWACCOUNT")){
				return openAccount(customer, request.substring(request.indexOf(" ") + 1)); // +1 to remove the leading space
			}
			if(input[0].equals("MOVE")) {
				if(input.length < 4) { // return fail if not enough information is provided
					return "FAIL";
				}
				double amount = Double.parseDouble(input[1]);
				String from = input[2];
				String to = input[3];
				return moveFunds(customer,amount,from,to);
			}
			switch(request) {
			case "SHOWMYACCOUNTS" : return showMyAccounts(customer);
			default : return "FAIL";
			}
		}
		return "FAIL";
	}

	private String openAccount(CustomerID customer, String accountName) {
		(customers.get(customer.getKey())).addAccount(new Account(accountName, 0.00));
		return "SUCCESS";
	}

	private String showMyAccounts(CustomerID customer) {
		return (customers.get(customer.getKey())).accountsToString();
	}

	private String moveFunds(CustomerID customer,double amount,String from,String to) {
		if(amount > 0) { // should never need to move a negative amount
			if((customers.get(customer.getKey())).editAccountBalance(from,-amount)) { // try to remove amount
				if((customers.get(customer.getKey())).editAccountBalance(to,amount)) { // try to add amount
					return "SUCCESS";
				} else { // if adding was unsuccessful, add it back to the original account
					(customers.get(customer.getKey())).editAccountBalance(from,amount);
				}
			}
		}
		return "FAIL";
	}
}
