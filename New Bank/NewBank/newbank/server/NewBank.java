package newbank.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;


public class NewBank {
	
	private static final NewBank bank = new NewBank();
	private HashMap<String,Customer> customers;

	private static String userDetails = "";

	private NewBank() {
		customers = new HashMap<>();
		if (userDetails!=null){
			addTestData();
		}
	}

	public static String readData(String search, String  filepath) {

		boolean idFound = false;
		try {

			BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
			String row = "placeholder";
			while (row!= null && !idFound) {
				row = csvReader.readLine();
				String[] data = row.split(",");
				if(data[0].equals(search)){
					userDetails = row;
					idFound = true;
					return userDetails;
				}
			}
			csvReader.close();
		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();;
		}
		return "User not found";
	}

	public void fetchUserDetails(String username){
		readData(username, ".\\New Bank\\NewBank\\newbank\\server\\data.csv");
		addTestData();
	}

	private void addTestData() {
		String[] data = userDetails.split(",");
		Customer user = new Customer();
		user.addAccount(new Account(data[1], Double.parseDouble(data[2]) ));
		customers.put(data[0], user);
		
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
