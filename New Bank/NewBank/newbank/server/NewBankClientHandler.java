package newbank.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class NewBankClientHandler extends Thread{
	
	private NewBank bank;
	private BufferedReader in;
	private PrintWriter out;
	public String un = "";
	
	public NewBankClientHandler(Socket s) throws IOException {
		bank = NewBank.getBank();
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		out = new PrintWriter(s.getOutputStream(), true);
	}
	
	public void run() {
		// keep getting requests from the client and processing them
		try {
			String responce = "FAIL"; // initialise responce string
			while (responce!="SUCCESS"){ // while loop until success is achieved, will not bring up login entry until then
				// ask if login or signup
				out.println("LOGIN or SIGNUP");
				String request = in.readLine();
				responce = bank.processRequest(request);
				out.println(responce);
			}
		} catch (IOException | NumberFormatException e) {
			e.printStackTrace();
		} try {
			// ask for user name
			out.println("Enter Username");
			String userName = in.readLine();
			// ask for password
			out.println("Enter Password");
			String password = in.readLine();
			out.println("Checking Details...");
			bank.fetchUserDetails(userName);
			// authenticate user and get customer ID token from bank for use in subsequent requests
			CustomerID customer = bank.checkLogInDetails(userName, password);
			// if the user is authenticated then get requests from the user and process them
			if(customer != null) {
				out.println("Log In Successful. What do you want to do?");
				while(true) {
					String request = in.readLine();
					System.out.println("Request from " + customer.getKey());
					String responce = bank.processRequest(customer, request);
					out.println(responce);
				}
			}
			else {
				out.println("Log In Failed");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				in.close();
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
				Thread.currentThread().interrupt();
			}
		}
	}
	public String getUn(){
		return un;
	}

}
