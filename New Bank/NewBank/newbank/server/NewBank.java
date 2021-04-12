package newbank.server;

import java.io.*;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;
import java.util.*;

public class NewBank {

	private static final NewBank bank = new NewBank();
	private HashMap<String, Customer> customers;
	private static String userDetails = "";
	private static String accountusername = "";
	private static String accountpassword = "";
	private String users = ".\\New Bank\\NewBank\\newbank\\server\\users.csv";
	private String ledger = ".\\New Bank\\NewBank\\newbank\\server\\ledger.csv";
	private String lender = ".\\New Bank\\NewBank\\newbank\\server\\lenders.csv";
	private static String id = "";
	private String algorithm = "SHA-256";
	private final int MAX_LOGIN_ATTEMPTS = 3;
	

	private NewBank() {
		customers = new HashMap<>();
		if (userDetails != null) {
			addTestData();
		}
	}

	public static String readData(String search, String filepath, int col) {

		boolean idFound = false;
		try {

			BufferedReader csvReader = new BufferedReader(new FileReader(filepath));
			String row = "placeholder";
			while (row != null && !idFound) {
				row = csvReader.readLine();
				String[] data = row.split(",");
				if (data[col-1].equals(search)) {
					userDetails = row;
					idFound = true;
					return userDetails;
				}
			}
			csvReader.close();
		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();
			;
		}
		return "User not found";
	}

	public int fetchUserDetails(String username, String password) {
		try {
			readData(username, users,2);
			String user[] = userDetails.split(",");
			id = user[0];
			accountusername = user[1];
			accountpassword = user[2];
			int loginAttempts = Integer.parseInt(user[3]);
			if(loginAttempts >= MAX_LOGIN_ATTEMPTS) {
				return 2; // fail, too many failed login attempts
			}
			readData(id, ledger,1);
			addTestData();
			String newPass = generateHash(password, algorithm);
			if (newPass.equals(accountpassword)) {
				changeCSVValue(id,"0",4,users); // reset login attempts counter
				return 0; // success
			} else {
				changeCSVValue(id,String.valueOf(loginAttempts+1),4,users);
				return 1; // fail, no username/password match
			}
		} catch (Exception e) {
			//TODO: handle exception
			return -1; //undefined error
		}
	}

	private void addTestData() {
		String[] data = userDetails.split(",");
		Customer user = new Customer();
		int count = 0;
		while (count < data.length - 1) { // loads all accounts and balance from csv file
			count += 1;
			user.addAccount(new Account(data[count], Double.parseDouble(data[count + 1])));
			count += 1;
		}
		customers.put(data[0], user);
	}

	public static NewBank getBank() {
		return bank;
	}

	public synchronized CustomerID checkLogInDetails(String userName, String password) {
		
		try {
			password = generateHash(password, algorithm);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (userName.equals(accountusername) && (password.equals(accountpassword))) {
			return new CustomerID(id);
		}
		return null;
	}

	// login, signup, and logoff initial requests
	public synchronized String processRequest(String request) {
		String[] input = request.split(" "); // create an array of the parsed input string
		if (input[0].equals("LOGIN")) {
			return "SUCCESS";
		} else if (input[0].equals("SIGNUP")) {
			if (input.length < 4) {
				return "\nPlease input <username> <password> <initial deposit>\n";
			}
			String name = input[1];
			if (customers.containsKey(name)) {
				return "\nUser already exist.\nPlease try different username.\n";
			}
			//Random 7 digit number for the customers ID
			Random customerID = new Random();
			int num = customerID.nextInt(9000000) + 1000000;
			String num1 = String.valueOf(num);
			//Writing to the next line on users.csv
			try (FileWriter fw = new FileWriter(".\\New Bank\\NewBank\\newbank\\server\\users.csv", true)) {
				String hashedPass = "";
				try {
					 hashedPass = generateHash(input[2], algorithm);
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fw.append("\n");
				fw.append(num1);
				fw.append(",");
				fw.append(input[1]);
				fw.append(",");
				fw.append(hashedPass);
				fw.append(",");
				fw.append("0"); // Failed login attempts
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
			} catch (IOException e) {
				e.printStackTrace();
			}
			return "\nUSER CREATED\n";
		}
		return "\nPlease input LOGIN or SIGNUP\n";
	}

	// commands from the NewBank customer are processed in this method
	public synchronized String processRequest(CustomerID customer, String request) {
		if (customers.containsKey(customer.getKey())) {
			String[] input = request.split(" "); // create an array of the parsed input string
			// New account functionality
			if (request.startsWith("NEWACCOUNT")) {
				if (input.length != 2) { // return fail if wrong number of inputs
					return "\nPlease input correct name of the account\n";
				}	
				else {
					return openAccount(customer, request.substring(request.indexOf(" ") + 1)); // +1 to remove the leading space
				}
			}	

			// Deposit functionality
			if (request.startsWith("DEPOSIT")) {
				if (input.length != 2) { // check if wrong number of inputs
					return "\nPlease input correct format\ne.g. DEPOSIT <amount>\n";
				}	
				try { // check if amount is correct
					double depositAmount = Double.parseDouble(input[1]);
					if(depositAmount<0 || depositAmount>1000000000 || !input[1].matches("-?\\d+(\\.\\d+)?")){
						return "\nPlease input correct amount\n";
					}
					if(BigDecimal.valueOf(depositAmount).scale() > 2){ //check if over 2 decimal places
						return "\nPlease input amount to 2 decimal places\n";
					}
					if ((customers.get(customer.getKey())).editAccountBalance("Main", depositAmount)) { // try to add amount
						String toBalance = customers.get(customer.getKey()).getBalance("Main");
						editLedger(customer.getKey(), "Main", toBalance, ledger);
						return "\nDeposit of " + String.format("%.2f", depositAmount) + " accepted\n";
					}
				} catch (Exception e) {
					return "\nPlease input correct amount\n";
				}
			}

			// Withdraw functionality
			if (request.startsWith("WITHDRAW")) {
				if (input.length != 2) { // check if wrong number of inputs
					return "\nPlease input correct format\ne.g. WITHDRAW <amount>\n";
				}
				try { // check if amount is correct
					double withdrawAmount = Double.parseDouble(input[1]);
					if(withdrawAmount<0 || !input[1].matches("-?\\d+(\\.\\d+)?")){
						return "\nPlease input correct amount\n";
					}
					if(BigDecimal.valueOf(withdrawAmount).scale() > 2){ //check if over 2 decimal places
						return "\nPlease input amount to 2 decimal places\n";
					}
					if ((customers.get(customer.getKey())).editAccountBalance("Main", -withdrawAmount)) { // try to withdraw amount
						String toBalance = customers.get(customer.getKey()).getBalance("Main");
						editLedger(customer.getKey(), "Main", toBalance, ledger);
						return "\nWithdraw of " + String.format("%.2f", withdrawAmount) + " successful\n";
					} else {
						return "\nNot sufficient balance on the Main account\n";

					}
				} catch (Exception e) {
					return "\nPlease input correct amount\n";
				}
			}

			if (request.startsWith("REGISTERLENDER")){
				if(input.length < 2) { // return fail if not enough information is provided
					return "\nPlease input correct amount\n";
				}
				return registerLender(input[1]);
			}
			if (request.startsWith("BORROWMICROLOAN")){
				if(input.length < 4) { // return fail if not enough information is provided
					return "FAIL";
				}
				Customer cust = customers.get(customer.getKey());
				return borrowMicroLoan(cust, input[1], input[2], input[3], input[4]);
			}
			if (request.startsWith("SHOWMICROLENDERS")){
				return readLenders(); // +1 to remove the leading space
			}

			// MOVE functionality
			if (input[0].equals("MOVE")) {
				if (input.length != 4) { // return fail if not enough information is provided
					return "\nPlease input correct format MOVE <amount> <from> <to>\n";
				}
				try { // check if amount is correct
					String from = input[2];
					String to = input[3];
					double amount = Double.parseDouble(input[1]);
					if(amount<0 || !input[1].matches("-?\\d+(\\.\\d+)?")){ // check if correct amount
						return "\nPlease input correct amount\n";
					}
					else if(BigDecimal.valueOf(amount).scale() > 2){ //check if over 2 decimal places
						return "\nPlease input amount to 2 decimal places\n";
					}
					else if(Double.parseDouble(customers.get(customer.getKey()).getBalance(from))<amount){ //check enough balance
						return "\nNot sufficient balance on the " + from + " account\n";
					}
					else {
						return moveFunds(customer, amount, from, to);
					}
				} catch (Exception e) {
					return "\nPlease input correct amount\n";
				}
			}
			if (input[0].equals("LOGOFF")) {
				return "LOGOFF";
			}

			// PAY functionality
			if (input[0].equals("PAY")) {
				if (input.length != 3) { // return fail if not enough information is provided
					return "\nPlease input correct format PAY <ID> <amount>\n";
				}
				try { // check if amount is correct
					String customerId = input[1];
					Double amount = Double.parseDouble(input[2]);
					String fromAccount = "Main";
					String toAccount = "Main";
					if(amount<0 || !input[2].matches("-?\\d+(\\.\\d+)?")){ // check if correct amount
						return "\nPlease input correct amount\n";
					}
					else if(BigDecimal.valueOf(amount).scale() > 2){ //check if over 2 decimal places
						return "\nPlease input amount to 2 decimal places\n";
					}
					else if(Double.parseDouble(customers.get(customer.getKey()).getBalance(fromAccount))<amount){ //check enough balance
						return "\nNot sufficient balance on the " + fromAccount + " account\n";
					}
					else {
						return payment(customerId, amount, fromAccount, toAccount);
					}
				} catch (Exception e) {
					return "\nPlease input correct amount\n";
				}
			}

			switch (request) {
				case "SHOWMYACCOUNTS":
					return showMyAccounts(customer);
				default:
					return "\nWrong COMMAND\n";
			}
		}

		return "\nWrong COMMAND\n";
	}

	private String registerLender(String loanAmount){
		Double amount = Double.parseDouble(loanAmount);
		Double balance = Double.parseDouble(customers.get(id).getBalance("Main"));
		if(balance > amount){
			String newBalance = Double.toString(balance - amount);
			String record = id + "," + loanAmount;
			if(addRecord(id, record, lender)){
				customers.get(id).editAccountBalance("Main", -amount);
				editLedger(id,"Main", newBalance, ledger);
				return "\nREGISTERED AS A LENDER\n";
			} else {
				return "\nALREADY REGISTERED AS A LENDER\n";
			}
		}
		return "\nImpossible to execute\n";
	}

	private String borrowMicroLoan (Customer customer, String lender, String amount, String income, String term){
		String lenders = readLenders();
		if(!lenders.contains(lender)){
			return "\nINVALID LENDER\n";
		}
		MicroLoan loan = new MicroLoan();
		loan.setAmount(Double.valueOf(amount));
		loan.setCurrentIncome(Double.valueOf(income));
		loan.setRepaymentTerm(term);
		ArrayList<MicroLoan> existingLoan = customer.getLoans();
		if (existingLoan == null) {
			ArrayList<MicroLoan> newLoan = new ArrayList<>();
			newLoan.add(loan);
			customer.setLoans(newLoan);
		} else {
			existingLoan.add(loan);
			customer.setLoans(existingLoan);

		}
		return "\nLOAN GRANTED :)\n";
	}

	private String openAccount (CustomerID customer, String accountName){
		(customers.get(customer.getKey())).addAccount(new Account(accountName, 0.00));
		String customerName = customer.getKey();
		editEnd(customerName, accountName + ",0"); //adds the account name and balance of 0 to the csv file
		return "\nNew account ADDED\n";
	}

	// Appends to a value to the end of a record by searching for an id
	private void editEnd (String search, String stringAdd){
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
			while (row != null) {
				row = csvReader.readLine();
				if (row != null) {
					String[] data = row.split(",");
					if (data[0].equals(search)) {
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
			while (row != null) {
				row = csvReader.readLine();
				if (row != null) {
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

	private void editLedger(String search, String account, String stringAdd, String path) {
		String filepath = path;
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

	private void changeCSVValue(String id, String newValue, int col, String path) {
		// 'id' should be the unique numeric account ID, 'col' (column) starts at 1
		// method will change one CSV value. Indexed on ID and column number.
		String tempFile = ".\\New Bank\\NewBank\\newbank\\server\\temp.csv";
		File oldFile = new File(path);
		File newFile = new File(tempFile);

		try {
			// Write to a temporary file
			FileWriter fw = new FileWriter(tempFile, true);
			BufferedWriter bw = new BufferedWriter(fw);
			PrintWriter pw = new PrintWriter(bw);

			// read the original data.csv file and searches for ID and appends value to the end
			BufferedReader csvReader = new BufferedReader(new FileReader(path));
			String row = "placeholder";
			while (row!= null) {
				row = csvReader.readLine();
				if(row!=null){
					String[] data = row.split(",");
					if(data[0].equals(id)){
						row = "";
						for(int i = 0 ; i < data.length; i++){
							if(i == col-1){
								data[i] = newValue;
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
			// Overwrites the original data.csv file
			FileWriter fw2 = new FileWriter(path);
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


	private String showMyAccounts (CustomerID customer){
		return (customers.get(customer.getKey())).accountsToString();
	}

	private String moveFunds (CustomerID customer,double amount, String from, String to){
		if (amount > 0) { // should never need to move a negative amount
			if ((customers.get(customer.getKey())).editAccountBalance(from, -amount)) { // try to remove amount
				if ((customers.get(customer.getKey())).editAccountBalance(to, amount)) { // try to add amount
					String fromBalance = customers.get(customer.getKey()).getBalance(from);
					String toBalance = customers.get(customer.getKey()).getBalance(to);
					editLedger(customer.getKey(), from, fromBalance, ledger);
					editLedger(customer.getKey(), to, toBalance, ledger);
					return "\nTransfer of " + String.format("%.2f", amount) + " successful\n";
				} else { // if adding was unsuccessful, add it back to the original account
					(customers.get(customer.getKey())).editAccountBalance(from, amount);
				}
			}
		}
		return "\nWrong account name\n";
	}

	private String payment (String toID, Double amount, String from, String to){
			if (customers.get(id).editAccountBalance(from, -amount)) { // try to remove amount
				if (transfer(toID, to, amount)) { // try to add amount
					String fromBalance = customers.get(id).getBalance(from);
					editLedger(id, from, fromBalance, ledger);
					return "\nPayment of " + String.format("%.2f", amount) + " to " + toID + " successful\n";
				} else { // if adding was unsuccessful, add it back to the original account
					customers.get(id).editAccountBalance(from, amount);
				}
			}
		return "\nDestination account not recognized.\nTransfer not executed\n";
	}

	private Boolean transfer (String toID, String accountName, Double amount){
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
			while (row != null) {
				row = csvReader.readLine();
				if (row != null) {
					String[] data = row.split(",");
					if (data[0].equals(toID)) {
						row = "";
						for (int i = 0; i < data.length; i++) {
							if (data[i].equals(accountName)) {
								Double bal = Double.parseDouble(data[i + 1]);
								String newBalance = Double.toString(bal + amount);
								data[i + 1] = newBalance;
								result = true;
							}
							if (i < data.length - 1) {
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
			while (row != null) {
				row = csvReader.readLine();
				if (row != null) {
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

	private boolean addRecord(String search, String stringAdd, String path) {
		boolean result = true;
		String filepath = path;
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
				if(row!=null && result != false){
					String[] data = row.split(",");
					if(data[0].equals(search)){
						result = false;
						pw.println(row);
						break;
					}
					pw.println(row);
				} else {
					row = stringAdd;
					pw.println(row);
					result = true;
					break;
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

	private String readLenders(){
		String result = "";
		try {

			BufferedReader csvReader = new BufferedReader(new FileReader(lender));
			String row = "placeholder";
			int count = 0;
			while (row!= null) {
				row = csvReader.readLine();
				String[] data = row.split(",");
				if (count == 0){
					result += "\n" + data[0] + "\t" + "\t" + data[1];
				}
				if(count > 0){
					result += "\n" + data[0] + "\t" + "\t"  + "\t" + data[1];
				}
				count += 1;
			}
			csvReader.close();
		} catch (Exception e) {
			//TODO: handle exception
			e.printStackTrace();;
		}
		return result;
	}

	private static String generateHash(String inputdata, String algorithm) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(algorithm);
		digest.reset();
		byte[] hash = digest.digest(inputdata.getBytes());
		return bytesToHex(hash);
	}

	private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

	private static String bytesToHex(byte[] bytes){
		char[] hexChars = new char[bytes.length * 2];
		for(int j = 0; j < bytes.length;j++){
			int v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2+1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}
}