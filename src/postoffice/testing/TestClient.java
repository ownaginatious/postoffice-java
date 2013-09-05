package postoffice.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import postoffice.connection.PostOfficeConnection;
import postoffice.exception.comm.DeliveryFailureException;
import postoffice.exception.mailbox.ExistentMailboxException;
import postoffice.exception.mailbox.ExistingMailboxConnectionException;
import postoffice.exception.mailbox.MailboxDisconnectedException;
import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.NoMailException;
import postoffice.exception.mailbox.NonExistentMailboxException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class TestClient {

	private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
	
	public static void main(String[] args) throws IOException {
		
		PostOfficeConnection<StringMessage> poc = new PostOfficeConnection<StringMessage>(StringMessage.class);

		String mailbox = null, box = null, password = null;
		
		boolean running = true;
		
		System.out.println("Client ready and awaiting commands...\n");
		
		while(running){
			
			String command = br.readLine();
			
			try {
				
				switch(command){
				
					case "connect":
						
						System.out.print("Connect to which address?: ");
						String address = getPattern(".+");
						
						System.out.print("Which port?: ");
						int port = getInt();
						
						System.out.print("Timeout?: ");
						int timeout = getInt();
						
						poc.connect(address, port, timeout);
					
						System.out.println("Now connected to " + address + ":" + port + "\n");
						break;
					
					case "disconnect":
						
						poc.disconnect();
						
						break;
						
					case "sendmessage":
						
						System.out.print("Send to whom?: ");
						String recipient = getPattern(".*");
						
						System.out.print("Message <hit return on completion>: ");
						String message = getPattern(".*");
						
						StringMessage sm = new StringMessage();
						sm.setMessage(message);
						
						poc.sendMessage(recipient, sm);
						
						break;

					case "getmessage":
						
						StringMessage resp = null;
						
						System.out.print("Apply a filter? [Y/N]: ");
						String question = getPattern("^[YN]$");
						String filter = ".*";
						
						if(question.equals("Y")) {
							
							System.out.print("Enter the filter: ");
							filter = getPattern(".*");
						}
						
						System.out.print("Wait in the event of no mail? [Y/N]: ");
						question = getPattern("^[YN]$");
						
						if(question.equals("N"))
							resp = poc.getMessage(filter);
						else {
							
							int time = 0;
							
							System.out.print("Wait for how long?: ");
							time = getInt();
							
							if(time > 0)
								resp = poc.getMessage(filter, time);
							else
								resp = poc.getMessage(filter);
						}
						
						System.out.println("\t -> From: " + resp.getSender() + ", Message: " + resp.getMessage());
						
						break;
						
					case "checkout":
						
						System.out.print("Which box do you want to check out?: ");
						box = getPattern(".*");
						System.out.print("What is the password for that box?: ");
						password = getPattern(".*");
						
						poc.checkoutMailbox(box, password);
						
						mailbox = box;
						
						break;
						
					case "return":
						
						poc.returnMailbox();
						mailbox = null;
						
						System.out.println("Mailbox successfully returned.");
						break;
					
					case "status":
						
						if(mailbox == null)
							System.out.println("Disconnected from a mailbox.");
						else
							System.out.println("Connected to mailbox '" + mailbox + "'");
						
						break;
						
					case "create":
						
						System.out.print("Which box do you want to create?: ");
						box = getPattern(".*");
						System.out.print("What will your password be?: ");
						
						password = getPattern(".*");
						poc.createMailbox(box, password);
						
						break;
						
					case "delete":
						
						poc.deleteMailbox();
						break;
						
					case "empty":
						
						poc.emptyMailQueue();
						break;
						
					case "quit":
						
						System.out.println("Good bye :)");
						
						poc.disconnect();
						
						System.out.println("Client disconnected.");
						
						running = false;
						break;
						
					default:
						System.out.println("Unknown command '" + command + "'\n");
				}
				
			} catch(MailboxDisconnectedException mde){
				
				System.out.println("You must connect to a mailbox before performing this action.");
				
			} catch (DeliveryFailureException e) {
				
				System.out.println("Delivery failed as the recipient does not exist.");
				
			} catch (ExistingMailboxConnectionException e) {
				
				System.out.println("Someone else is already connected to that mailbox.");
				
			} catch (NonExistentMailboxException e) {
				
				System.out.println("The requested mailbox does not exist.");
				
			} catch (MailboxInUseException e) {
				
				System.out.println("The requested mailbox is currently in use.");
				
			} catch (UnauthorizedActionException e) {
				
				System.out.println("The credentials provided are bad.");
				
			} catch (NoMailException e) {
				
				System.out.println("No mail discovered.");
				
			} catch (ExistentMailboxException e) {
				
				System.out.println("A mailbox by that name already exists.");
			}
		}
	}
	
	private static int getInt() throws IOException{
		
		return Integer.parseInt(getPattern("[0-9]+"));
	}
	
	private static String getPattern(String pattern) throws IOException{

		String input = br.readLine();
		
		while(!input.matches(pattern)){
			
			System.out.println("Bad input. Try again.");
			input = br.readLine();
		}
		
		return input;
	}
}
