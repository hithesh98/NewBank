package newbank.server;

import java.util.*;

public class NewBank {

	private static final NewBank bank = new NewBank();

	private HashMap<String,Customer> customers;

	private NewBank() {
		customers = new HashMap<>();
		addTestData();
	}

	private void addTestData() {

		Customer bhagy = new Customer();
		bhagy.setLender("Microlender1");

		bhagy.addAccount(new Account("Main", 1000.0));
		customers.put("Bhagy", bhagy);
		bhagy.setBankName("bank3");


		Customer christina = new Customer();
		christina.addAccount(new Account("Savings", 1500.0));
		christina.setBankName("bank2");
		customers.put("Christina", christina);

		Customer john = new Customer();
		john.addAccount(new Account("Savings", 250.0));
		john.setBankName("bank1");
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
	public synchronized String processRequest(CustomerID customer, String request){
		if(customers.containsKey(customer.getKey())) {
			String[] input = request.split(" "); // create an array of the parsed input string
			if (request.startsWith("NEWACCOUNT")){
				return openAccount(customer, request.substring(request.indexOf(" ") + 1)); // +1 to remove the leading space
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
			if(input[0].equals("PAY")) {
				if(input.length < 7) { // return fail if not enough information is provided
					return "FAIL";
				}
				Customer custCredit = customers.get(customer.getKey());
				String customerId = input[1];
				double amount = Double.parseDouble(input[2]);
				String fromAccount = input[3];
				String toAccount = input[4];
				String debitBank = input[5];
				String creditBank = input[6];
				Customer custDebit = customers.get(customerId);
				return payment(custCredit,amount,fromAccount,toAccount, custDebit, debitBank, creditBank);


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

	private String payment(Customer customer, double amount,String from,String to,  Customer custCredit, String debitBankName, String creditBankName ) {
		if(amount > 0) { // should never need to move a negative amount
			if(customer.editAccountBalance(from,-amount) && customer.getBankName().equals(debitBankName)) { // try to remove amount
				if(custCredit.getBankName().equals(creditBankName)&&custCredit.editAccountBalance(to,amount)) { // try to add amount
					return "SUCCESS";
				} else { // if adding was unsuccessful, add it back to the original account
					customer.editAccountBalance(from,amount);
				}
			}
		}
		return "FAIL";
	}
}
