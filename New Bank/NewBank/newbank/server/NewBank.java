package newbank.server;

import java.io.*;
import java.util.HashMap;
import java.util.Random;
import java.util.*;

public class NewBank {

	private static final NewBank bank = new NewBank();
	private HashMap<String, Customer> customers;
	private static String userDetails = "";
	private static String accountusername = "";
	private static String accountpassword= "";
	private static String id= "";

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

	public Boolean fetchUserDetails(String username, String password){
		try {
			readData(username, ".\\New Bank\\NewBank\\newbank\\server\\users.csv");
			String user[] = userDetails.split(",");
			id = user[user.length-1];
			accountusername = user[0];
			accountpassword = user[1];
			readData(id, ".\\New Bank\\NewBank\\newbank\\server\\ledger.csv");
			addTestData();
			if(password.equals(accountpassword)){
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			//TODO: handle exception
			return false;
		}
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
		if (userName.equals(accountusername) && (password.equals(accountpassword))) {
			return new CustomerID(id);
		}
		return null;
	}

	// login, signup, and logoff initial requests
	public synchronized String processRequest(String request) {
		String[] input = request.split(" "); // create an array of the parsed input string
		if(input[0].equals("LOGIN")) {
			return "SUCCESS";
		} else if (input[0].equals("SIGNUP")){
			if(input.length < 4){
				return "FAIL";
			}
			String name = input[1];
			if(customers.containsKey(name)){
				return "FAIL";
			}
			//Random 7 digit number for the customers ID
			Random customerID = new Random();
			int num = customerID.nextInt(9000000) + 1000000;
			String num1 = String.valueOf(num);
			//Writing to the next line on users.csv
			try (FileWriter fw = new FileWriter(".\\New Bank\\NewBank\\newbank\\server\\users.csv", true)) {
				fw.append("\n");
				fw.append(input[1]);
				fw.append(",");
				fw.append(input[2]);
				fw.append(",");
				fw.append(num1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			try (FileWriter fw1 = new FileWriter(".\\New Bank\\NewBank\\newbank\\server\\ledger.csv", true)) {
				fw1.append("\n");
				fw1.append(num1);
				fw1.append(",");
				fw1.append("Main");
				fw1.append(",");
				fw1.append(input[3]);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return "SUCCESS";
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request){
		if(customers.containsKey(customer.getKey())) {
			String[] input = request.split(" "); // create an array of the parsed input string
			if(request.startsWith("NEWACCOUNT")){
				return openAccount(customer, request.substring(request.indexOf(" ") + 1)); // +1 to remove the leading space
			}
			if(request.startsWith("DEPOSIT")){
				if(input.length != 2) { // return fail if wrong number of inputs
					return "FAIL - wrong format";
				}
				try {
					double depositAmount = Double.parseDouble(input[1]);
					if((customers.get(customer.getKey())).editAccountBalance("Main",depositAmount)) { // try to add amount
						//String fromBalance = customers.get(customer.getKey()).getBalance(from);
						String toBalance = customers.get(customer.getKey()).getBalance("Main");
						//editLedger(customer.getKey(), from, fromBalance);
						editLedger(customer.getKey(), "Main", toBalance);
						return "SUCCESS";
					}
				}	
				catch (Exception e){
					return "FAIL - please input number";
				}
			if (request.startsWith("REGISTERLENDER")){
				if(input.length < 2) { // return fail if not enough information is provided
					return "FAIL";
				}
				Customer cust = customers.get(customer.getKey());
				return registerLender(cust, input[1]);
			}
			if (request.startsWith("BORROWMICROLOAN")){
				if(input.length < 4) { // return fail if not enough information is provided
					return "FAIL";
				}
				Customer cust = customers.get(customer.getKey());
				return borrowMicroLoan(cust, input[1], input[2], input[3], input[4]);
			}
			if (request.startsWith("SHOWMICROLENDERS")){
				return showMicroLenders(); // +1 to remove the leading space
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
			if(input[0].equals("LOGOFF")){
				return "LOGOFF";
			}

			if(input[0].equals("PAY")) {
				if(input.length < 5) { // return fail if not enough information is provided
					return "FAIL";
				}
				String customerId = input[1];
				Double amount = Double.parseDouble(input[2]);
				String fromAccount = input[3];
				String toAccount = input[4];
				return payment(customerId,amount,fromAccount,toAccount);
				}

			switch(request) {
				case "SHOWMYACCOUNTS" : return showMyAccounts(customer);
				default : return "FAIL";
			}
		}

		return "FAIL";
	}

	private String borrowMicroLoan(Customer customer, String lender, String amount, String income, String term){
		Set<String> str = new HashSet<>();
		for(Customer map: customers.values()){

			str.add(map.getLender());
		}
		if(!str.contains(lender)){
			return  "INVALID LENDER";
		}
		MicroLoan loan = new MicroLoan();
		loan.setAmount(Double.valueOf(amount));
		loan.setCurrentIncome(Double.valueOf(income));
		loan.setRepaymentTerm(term);
		ArrayList<MicroLoan> existingLoan = customer.getLoans();
		if(existingLoan == null){
			ArrayList<MicroLoan> newLoan  = new ArrayList<>();
			newLoan.add(loan);
			customer.setLoans(newLoan);
		}else {
			existingLoan.add(loan);
			customer.setLoans(existingLoan);

		}
		return "LOAN GRANTED";
	}

	private String registerLender(Customer customer, String lender){
		customer.setLender(lender);
		return "SUCCESS";
	}

	private String showMicroLenders(){
		String s = "";
		for(Customer map: customers.values()){

			s += map.getLender();
		}
		return s;
	}

	private String openAccount(CustomerID customer, String accountName) {
		(customers.get(customer.getKey())).addAccount(new Account(accountName, 0.00));
		String customerName = customer.getKey();
		editEnd(customerName, accountName + ",0"); //adds the account name and balance of 0 to the csv file
		return "SUCCESS";
	}

	// Appends to a value to the end of a record by searching for an id
	private void editEnd(String search, String stringAdd) {
		String filepath = ".\\New Bank\\NewBank\\newbank\\server\\ledger.csv";
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

	private void editLedger(String search, String account, String stringAdd) {
		String filepath = ".\\New Bank\\NewBank\\newbank\\server\\ledger.csv";
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
						row = "";
						for(int i = 0 ; i < data.length; i++){
							if(data[i].equals(account)){
								data[i + 1] = stringAdd;
							}
							if(i<data.length-1){
								row += data[i] + ",";
							} else {
								row += data[i];
							}
						}
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
					String fromBalance = customers.get(customer.getKey()).getBalance(from);
					String toBalance = customers.get(customer.getKey()).getBalance(to);
					editLedger(customer.getKey(), from, fromBalance);
					editLedger(customer.getKey(), to, toBalance);
					return "SUCCESS";
				} else { // if adding was unsuccessful, add it back to the original account
					(customers.get(customer.getKey())).editAccountBalance(from,amount);
				}
			}
		}
		return "FAIL";
	}

	private String payment(String toID, Double amount,String from, String to) {
		if(amount > 0) { // should never need to move a negative amount
			if(customers.get(id).editAccountBalance(from,-amount)) { // try to remove amount
				if(transfer(toID, to, amount)) { // try to add amount
					String fromBalance = customers.get(id).getBalance(from);
					editLedger(id, from, fromBalance);
					return "SUCCESS";
				} else { // if adding was unsuccessful, add it back to the original account
					customers.get(id).editAccountBalance(from,amount);
				}
			}
		}
		return "FAIL";
	}

	private Boolean transfer(String toID, String accountName, Double amount) {
		Boolean result = false;
		String filepath = ".\\New Bank\\NewBank\\newbank\\server\\ledger.csv";
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
					if(data[0].equals(toID)){
						row = "";
						for(int i = 0 ; i < data.length; i++){
							if(data[i].equals(accountName)){
								Double bal = Double.parseDouble(data[i + 1]);
								String newBalance = Double.toString(bal + amount);
								data[i+1] = newBalance;
								result = true;
							}
							if(i<data.length-1){
								row += data[i] + ",";
							} else {
								row += data[i];
							}
						}
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
		return result;
	}
}
