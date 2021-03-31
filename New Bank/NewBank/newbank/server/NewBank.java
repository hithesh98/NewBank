package newbank.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;



public class NewBank {

	private static final NewBank bank = new NewBank();
	private HashMap<String, Customer> customers;

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
		int count = 0;
		while(count < data.length-1){ // loads all accounts and balance from csv file
			count += 1;
			user.addAccount(new Account(data[count], Double.parseDouble(data[count+1])));
			count += 1;
		}
		customers.put(data[0], user);
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
			if (input[0].equals("LOGOFF")){
				return "LOGOFF";
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
		String customerName = customer.getKey();
		editEnd(customerName, accountName + ",0"); //adds the account name and balance of 0 to the csv file
		return "SUCCESS";
	}

	// Appends to a value to the end of a record by searching for an id
	private void editEnd(String search, String stringAdd) {
		String filepath = ".\\New Bank\\NewBank\\newbank\\server\\data.csv";
		String tempFile = ".\\New Bank\\NewBank\\newbank\\server\\temp.csv";
		File oldFile = new File(filepath);
		File newFile = new File(tempFile);
		

		try {
			// Write to a temperory file
			FileWriter fw = new FileWriter(tempFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);
			
			// read the orginal data.csv file and searches for ID and appends value to the end
			BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
			String row = "placeholder";
			while (row!= null) {
				row = csvReader.readLine();
				if(row!=null){
					String[] data = row.split(",");
					if(data[0].equals(search)){
						row += "," + stringAdd;
					}
					pw.println(row);
				}
			}
			pw.flush();
			pw.close();
			csvReader.close();

		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();
		}
		try {
			// Overwrittes the original data.csv file
			FileWriter fw2 = new FileWriter(filepath);
			BufferedWriter bw2 = new BufferedWriter(fw2);
			PrintWriter pw2 = new PrintWriter(bw2);
			
			// Copies all data from temporary file
			BufferedReader csvReader = new BufferedReader(new FileReader(tempFile));
			String row = "placeholder";
			while (row!= null) {
				row = csvReader.readLine();
				if(row!=null){
					pw2.println(row);
				}
			}
			pw2.flush();
			pw2.close();
			csvReader.close();

		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();
		}
		// deletes the temporary file
		boolean b = newFile.delete();
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
