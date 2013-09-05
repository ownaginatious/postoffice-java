package postoffice.daemon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import postoffice.datatypes.CommFlags.REQ;
import postoffice.datatypes.CommFlags.RESP;
import postoffice.datatypes.CommFlags;
import postoffice.datatypes.Letter;
import postoffice.datatypes.Mailbox;
import postoffice.exception.mailbox.ExistentMailboxException;
import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.NonExistentMailboxException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class MailClerk implements Runnable {
	
	private static Logger logger = Logger.getLogger(MailClerk.class);
	
	private Socket s;
	private PostOffice po;
	private Mailbox mb = null;
	
	private DataInputStream is = null;
	private DataOutputStream os = null;
	
	private static AtomicInteger idBuilder = new AtomicInteger(0);
	
	private int id = 0;
	
	public MailClerk(Socket s, PostOffice po){

		id = idBuilder.incrementAndGet();
		
		this.s = s;
		this.po = po;
	}

	@Override
	public void run() {
		
		REQ request = null;
		REQ subrequest = null;
		Letter letter = null;
		
		log(Level.DEBUG, "A new connection has been initiated.");
		
		try {
			
			is = new DataInputStream(s.getInputStream());
			os = new DataOutputStream(s.getOutputStream());
			
			String boxname = null;
			byte[] passwordHash = new byte[16];
			
			// Start listening for incoming commands and data from the client.
			while(true){
				
				s.setSoTimeout(0);
				
				request = readRequest();
				
				if(request == null){
					
					logger.debug("Received bad command.");
					os.write(RESP.BADCOMMAND.ordinal());
					continue;
				}

				// Allow 5 seconds for the client to interact.
				s.setSoTimeout(500000);
				
				try {
					
					log(Level.DEBUG, "Received command " + request.name());
					
					switch(request){
						
						case REQBOX:
							
							if(mb != null){
								
								log(Level.DEBUG, "Already connected to a mailbox.");
								os.writeByte(RESP.ALREADYCONN.ordinal());
								continue;
							}
							else
								os.writeByte(RESP.REQDATA.ordinal());
							
							log(Level.DEBUG, "Requesting box name...");
							boxname = is.readUTF();
							
							log(Level.DEBUG, "Box name '" + boxname + "' received.");
							
							os.write(RESP.REQDATA.ordinal());

							log(Level.DEBUG, "Requesting password...");
							is.read(passwordHash);

							log(Level.DEBUG, "Password received.");
								
							try {

								mb = po.checkoutMailbox(boxname, passwordHash);
								log(Level.DEBUG, "Box '" + boxname + "' checked out successfully.");

							} catch (MailboxInUseException e) {
								
								log(Level.DEBUG, "The mailbox '" + boxname + "' is already in use.");
								os.writeByte(RESP.BOXINUSE.ordinal());
								continue;

							} catch (NonExistentMailboxException e) {

								log(Level.DEBUG, "The mailbox '" + boxname + "' already exists.");
								os.writeByte(RESP.NONEXISTBOX.ordinal());
								continue;
							}
							
							break;
							
						case RETBOX:
							
							if(mb == null){
								
								log(Level.DEBUG, "No mailbox connection.");
								os.writeByte(RESP.NOBOXCONN.ordinal());
								continue;
							}
							
							log(Level.DEBUG, "Disconnected from mailbox '" +  mb.getOwner() + "'.");
							
							mb = null;
							
							break;
							
						case CREATEBOX:

							log(Level.DEBUG, "Requesting box name...");
							os.writeByte(RESP.REQDATA.ordinal());
							
							boxname = is.readUTF();

							log(Level.DEBUG, "Box name '" + boxname + "' received.");

							log(Level.DEBUG, "Requesting password...");
							os.writeByte(RESP.REQDATA.ordinal());
							is.read(passwordHash);

							log(Level.DEBUG, "Password received.");
							
							try {
								
								po.createMailbox(boxname, passwordHash);
								log(Level.DEBUG, "Box '" + boxname + "' successfully created.");
								
							} catch (ExistentMailboxException e) {
								
								log(Level.DEBUG, "The box '" + boxname + "' already exists.");
								os.writeByte(RESP.BOXEXISTS.ordinal());
								continue;
							}
							
							log(Level.DEBUG, "Created mailbox '" + boxname + "'.");
							
							break;
							
						case REMOVEBOX:
							
							if(mb == null){
								
								log(Level.DEBUG, "No mailbox connection.");
								os.writeByte(RESP.NOBOXCONN.ordinal());
								continue;
							}

							try {
								
								po.destroyMailbox(boxname);
								
							} catch (NonExistentMailboxException e) {
								
								log(Level.DEBUG, "The mailbox '" + boxname + "' does not exist.");
								os.write(RESP.NONEXISTBOX.ordinal());
								continue;
							}
							
							log(Level.DEBUG, "Destroyed the mailbox '" + mb.getOwner() + "'.");
							
							mb = null;
							
							break;
							
						case DISCONNECT:
							
							if(mb != null)
								mb.release();
							
							log(Level.DEBUG, "Closing connection on client request...");
							os.writeByte(RESP.REQGRANTED.ordinal());
							
							s.close();
							
							return;
							
						case SENDLETTER:

							if(mb == null){
								
								log(Level.DEBUG, "No mailbox connection.");
								os.writeByte(RESP.NOBOXCONN.ordinal());
								continue;
							}
							
							os.writeByte(RESP.REQDATA.ordinal());
							letter = receiveLetter();
							
							try {
								
								po.sendLetter(letter);
								
								log(Level.DEBUG, "Message sent to '" + letter.getRecipient() + "'.");
								
							} catch (NonExistentMailboxException e) {
								
								log(Level.DEBUG, "The recipient mailbox '" + boxname + "' does not exist.");
								os.writeByte(RESP.DELFAIL.ordinal());
								continue;
							}
							
							break;
							
						case GETMAIL:
							
							if(mb == null){

								log(Level.DEBUG, "No mailbox connection.");
								os.writeByte(RESP.NOBOXCONN.ordinal());
								continue;
							}
							
							os.write(RESP.REQDATA.ordinal());
							
							// Get the timeout length (milliseconds).
							int timeout = is.readInt();
							
							long timeRemaining = 1000000 * (long) timeout;
							long maxTime = System.nanoTime() + timeRemaining;

							if(mb.getQueueSize() == 0 && timeout == 0){
								
								os.writeByte(RESP.MAILTIMEOUT.ordinal());
								continue;
							}
							else
								os.writeByte(RESP.REQDATA.ordinal());
							
							log(Level.DEBUG, "Requesting mail from a mailbox storing " + mb.getQueueSize() + " messages.");
							
							boolean noMailTerminate = false;
							
							while(!noMailTerminate && (timeout != 0 || mb.getQueueSize() > 0)){

								subrequest = readRequest();
								
								if(subrequest == REQ.NEXTLETTER){

									log(Level.DEBUG, "Client requesting next letter.");
									
									letter = mb.popMessage(timeRemaining, TimeUnit.NANOSECONDS);
									
									if(letter != null){
									
										log(Level.DEBUG, "Sending message to socket...");
										os.writeByte(RESP.INMAIL.ordinal());
										pushToSocket(letter);
										log(Level.DEBUG, "Sending complete.");
										
									} else {
										
										log(Level.DEBUG, "No more mail within alotted time.");
										os.writeByte(RESP.MAILTIMEOUT.ordinal());
										
										noMailTerminate = true;
									}
								}
								else if(subrequest == REQ.SATIATED){
								
									log(Level.DEBUG, "Client indicates satiation.");
									break;
								}
								
								timeRemaining = maxTime - System.nanoTime();
							}
							
							if(noMailTerminate){
								
								log(Level.DEBUG, "Command " + request.name() + " completed.");
								continue;
							}
							
							if(subrequest != REQ.SATIATED){
								
								subrequest = readRequest();
								
								if(subrequest != REQ.SATIATED)
									os.writeByte(RESP.MAILTIMEOUT.ordinal());
							}
							
							break;
							
						case EMPTYBOX:
							
							if(mb == null){

								log(Level.DEBUG, "No mailbox connection.");
								os.writeByte(RESP.NOBOXCONN.ordinal());
								continue;
							}
							
							mb.empty();
							
							break;
							
						default:
							
							log(Level.DEBUG, "Received non-initiating command '" + request.name() + "'.");
							os.writeByte(RESP.BADCOMMAND.ordinal());
							continue;
					}
					
					os.writeByte(RESP.REQGRANTED.ordinal());
					log(Level.DEBUG, "Command " + request.name() + " completed.");
				
				} catch(SocketTimeoutException ste){

					log(Level.DEBUG, "Client took too long to respond to command '" + request.name() + "'.");
					os.writeByte(RESP.COMMTIMEOUT.ordinal());
					continue;
					
				} catch (UnauthorizedActionException uae) {
					
					os.writeByte(RESP.NOAUTH.ordinal());
				}
			}
			
		} catch (IOException e) {
			
			log(Level.ERROR, "Problem while performing command '" + request + "'. Cause : " + e.getMessage());

			// Attempt to close the damage socket.
			try {
				
				logger.debug("[ID: " + id + "] Returning mailboxes.");
				
				if(mb != null)
					mb.release();
				
				logger.debug("[ID: " + id + "] Attempting to close the socket.");
				
				s.close();
				
			} catch (IOException ioe) {

				log(Level.DEBUG, "Failed to close the socket. Tossing instead. Cause : " + ioe.getMessage());
				
				return;
				
			} catch (UnauthorizedActionException uae) {
				
				// This can never happen in this model.
			}

			logger.debug("[ID: " + id + "] Socket closed successfully.");
			
			return;
		}
	}

	private void log(Priority priority, String message){
		
		logger.log(priority, "[ID: " + id + "] " + message);
	}
	
	private REQ readRequest() throws IOException{
		
		return CommFlags.getReqByCode(is.read());
	}
	
	/**
	 * Pushes an incoming message directly to the receiver through their socket connection to
	 * the current mailbox.
	 * 
	 * @param letter The message to push to the sender.
	 * @throws IOException 
	 */
	private void pushToSocket(Letter letter) throws IOException {

		os.writeUTF(letter.getSender());
		
		byte[] payload = letter.getPayloadBytes();

		os.writeInt(payload.length);
		os.write(payload);
	}

	private Letter receiveLetter() throws IOException {
		
		String recipient = null;

		byte[] messageData = null;
		
		recipient = is.readUTF();

		os.writeByte(RESP.REQDATA.ordinal());
		
		// Get the payload.
		int byteLength = is.readInt();

		if(byteLength > 0){

			messageData = new byte[byteLength];
			is.read(messageData);
		}
		
		return new Letter(mb.getOwner(), recipient, messageData);
	}
}
