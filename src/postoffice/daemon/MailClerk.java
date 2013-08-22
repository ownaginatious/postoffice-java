package postoffice.daemon;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

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
	
	private MessageDigest md = null;
	
	private static Logger logger = Logger.getLogger(MailClerk.class);
	
	private Socket s;
	private PostOffice po;
	private Mailbox mb = null;
	
	public MailClerk(Socket s, PostOffice po){

		this.s = s;
		this.po = po;
		
		// Initialize the password digester.
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {} // This will not ever happen, so who cares.
	}

	@Override
	public void run() {

		InputStream is;
		OutputStream os;
		
		REQ request = null;
		
		try {
			
			is = s.getInputStream();
			os = s.getOutputStream();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String boxname = null;
			byte[] passwordHash = null;
			
			// Start listening for incoming commands and data from the client.
			while(true){
				
				s.setSoTimeout(0);
				
				int command = is.read();
				request = CommFlags.getReqByCode(command);
				
				if(request == null){
					
					logger.debug("Received bad command '" + command + "'.");
					os.write(RESP.BADCOMMAND.ordinal());
					continue;
				}

				// Allow 5 seconds for the client to interact.
				s.setSoTimeout(5000);
				
				try {
					
					switch(request){
						
						case REQBOX:
							
							if(mb != null){
								
								os.write(RESP.ALREADYCONNECTED.ordinal());
								continue;
							}
							else
								os.write(RESP.REQDATA.ordinal());
							
							boxname = br.readLine();
							os.write(RESP.REQDATA.ordinal());
							passwordHash = md5Hash(br.readLine());
								
							try {

								mb = po.checkoutMailbox(boxname, passwordHash);

							} catch (MailboxInUseException e) {

								os.write(RESP.BOXINUSE.ordinal());
								continue;

							} catch (NonExistentMailboxException e) {

								os.write(RESP.NONEXISTENTBOX.ordinal());
								continue;
							}
							
							break;
							
						case DISCONNECTBOX:
							
							if(mb == null){
								
								os.write(RESP.NOBOXCONNECTION.ordinal());
								continue;
							}
							break;
							
						case CREATEBOX:
							
							os.write(RESP.REQDATA.ordinal());
							boxname = br.readLine();
							os.write(RESP.REQDATA.ordinal());
							passwordHash = md5Hash(br.readLine());

							try {
								
								po.createMailbox(boxname, passwordHash);
								
							} catch (ExistentMailboxException e) {
								
								os.write(RESP.BOXEXISTS.ordinal());
								continue;
							}
							
							break;
							
						case REMOVEBOX:
							
							if(mb == null){
								
								os.write(RESP.NOBOXCONNECTION.ordinal());
								continue;
							}

							try {
								
								po.destroyMailbox(boxname);
								
							} catch (NonExistentMailboxException e) {
								
								os.write(RESP.NONEXISTENTBOX.ordinal());
								continue;
							}
							
							break;
							
						case DISCONNECT:
							
							if(mb != null)
								mb.release();
							
							os.write(RESP.REQGRANTED.ordinal());
							
							s.close();
							
							return;
							
						case SENDLETTER:

							if(mb == null){
								
								os.write(RESP.NOBOXCONNECTION.ordinal());
								continue;
							}
							
							os.write(RESP.REQDATA.ordinal());
							Letter letter = receiveLetter(is);
								
							try {
								
								po.sendLetter(letter);
								
							} catch (NonExistentMailboxException e) {
								
								os.write(RESP.DELIVERYFAILURE.ordinal());
								continue;
							}
							
							break;
							
						case GETLETTER:
							
							if(mb == null){
								
								os.write(RESP.NOBOXCONNECTION.ordinal());
								continue;
							}
							
							if(mb.getQueueSize() == 0){

								os.write(RESP.NOMAIL.ordinal());
								continue;
							}
							else
								os.write(RESP.INCOMINGMAIL.ordinal());
							
							
							pushToSocket(mb.popMessage());
							
							break;
							
						case EMPTYBOX:
							
							if(mb == null){
								
								os.write(RESP.NOBOXCONNECTION.ordinal());
								continue;
							}
							
							mb.empty();
							
							break;
					}
					
					os.write(RESP.REQGRANTED.ordinal());
				
				} catch(SocketTimeoutException ste){

					logger.debug("Client took too long to respond to command '" + request.name() + "'.");
					os.write(RESP.COMMTIMEOUT.ordinal());
					continue;
					
				} catch (UnauthorizedActionException uae) {
					
					os.write(RESP.NOAUTH.ordinal());
				}
			}
			
		} catch (IOException e) {
			
			logger.error("Problem while performing command '" + request + "'. Cause : " + e.getMessage());

			// Attempt to close the damage socket.
			try {
				
				logger.debug("Returning mailboxes.");
				
				if(mb != null)
					mb.release();
				
				logger.debug("Attempting to close the socket.");
				
				s.close();
				
			} catch (IOException ioe) {

				logger.debug("Failed to close the socket. Tossing instead. Cause : "
						+ ioe.getMessage());
				
				return;
				
			} catch (UnauthorizedActionException uae) {
				
				// This exception can never happen in this model.
			}

			logger.debug("Socket closed successfully.");
			
			return;
		}
	}
	
	private byte[] md5Hash(String password){
		
		md.reset();
		
		try {
			return md.digest(password.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {} // This won't ever happen, so who cares.
		
		return null;
	}

	/**
	 * Pushes an incoming message directly to the receiver through their socket connection to
	 * the current mailbox.
	 * 
	 * @param letter The message to push to the sender.
	 * @throws IOException 
	 */
	private void pushToSocket(Letter letter) throws IOException{

		OutputStream os = this.s.getOutputStream();

		os.write(letter.getSender().getBytes("UTF-8"));
		os.write(CommFlags.END_OF_LINE);
		
		byte[] payload = letter.getPayloadBytes();

		os.write(ByteBuffer.allocate(4).putInt(payload.length).array());
		os.write(payload);
	}

	private Letter receiveLetter(InputStream is) throws IOException {

		// The format is <RECIPIENT><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		String recipient = null;

		byte[] messageData = null;

		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		
		recipient = br.readLine();

		// Get the payload.
		byte[] intData = new byte[4];
		is.read(intData);

		int byteLength = ByteBuffer.allocate(4).put(intData).getInt();

		if(byteLength > 0){

			messageData = new byte[byteLength];
			is.read(messageData);
		}
		
		return new Letter(mb.getOwner(), recipient, messageData);
	}
}
