package postoffice.connection;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.SocketFactory;

import postoffice.datatypes.CommFlags.REQ;
import postoffice.datatypes.CommFlags.RESP;
import postoffice.datatypes.CommFlags;
import postoffice.datatypes.Message;
import postoffice.exception.PostOfficeException;
import postoffice.exception.comm.CommunicationException;
import postoffice.exception.comm.DeliveryFailureException;
import postoffice.exception.comm.MailTimeoutException;
import postoffice.exception.mailbox.ExistentMailboxException;
import postoffice.exception.mailbox.ExistingMailboxConnectionException;
import postoffice.exception.mailbox.MailboxDisconnectedException;
import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.NoMailException;
import postoffice.exception.mailbox.NonExistentMailboxException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class PostOfficeConnection<T extends Message> {

	private MessageDigest md = null;
	private Socket s = null;
	
	private BufferedReader br = null;
	private InputStream is = null;
	private OutputStream os = null;
	
	private String mailboxId = null;
	
	private Class<T> messageClass;
	
	private List<byte[]> writeBuffer = new ArrayList<byte[]>();
	
	private Map<String, Queue<T>> mailBuffer = new HashMap<String, Queue<T>>();
	
	public PostOfficeConnection(Class<T> messageClass){

		this.messageClass = messageClass;
		
		// Initialize the password digester.
		try {
			
			md = MessageDigest.getInstance("MD5");
			
		} catch (NoSuchAlgorithmException e) {} // This will not ever happen, so who cares.
	}
	
	public String getMailboxId(){
		
		return this.mailboxId;
	}
	
	public void connect(String address, int port, int timeout) throws IOException {
		
		try {
			
			s = SocketFactory.getDefault().createSocket(address, port);
			
			s.setSoTimeout(timeout);
			
			is = s.getInputStream();
			br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			os = s.getOutputStream();
			
		} catch (IOException e) {
			
			throw new IOException("Problem establishing post office connection.", e);
		}
	}
	
	public void disconnect() throws IOException {

		try {
			
			execute(REQ.DISCONNECT, RESP.REQGRANTED);
			
		}
		catch(IOException ioe){
			
			throw new IOException(ioe.getMessage() + " An attempt was made to force the connection closed anyway.");
		}
		finally {

			br.close();
			s.close();
		}
	}
	
	private T newMessage(String sender, byte[] data){
		
		try {
			
			T message = messageClass.newInstance();
			message.initialize(sender, data);
			
			return message;
		
		} catch (InstantiationException | IllegalAccessException e) {} // Message type (T) must follow a strict interface,
																	   // so this can never happen.
		return null;
	}
	
	public T getMessage() throws IOException, MailboxDisconnectedException, NoMailException {
		
		try {
			
			return getMessage(".*", -1);
			
		} catch (MailTimeoutException e) {} // This cannot happen here.
		
		return null;
	}
	
	public T getMessage(int waitTime) throws IOException, MailboxDisconnectedException, MailTimeoutException, NoMailException {
		
		return getMessage(".*", waitTime);
	}
	
	public T getMessage(String filter) throws IOException, MailboxDisconnectedException, MailTimeoutException, NoMailException {
	
		return getMessage(filter, 0);
	}
	
	public T getMessage(String filter, int waitTime) throws MailboxDisconnectedException, IOException, MailTimeoutException, NoMailException {
		
		if(execute(REQ.GETMAIL, RESP.REQDATA, RESP.NOBOXCONNECTION) != RESP.REQDATA)
			throw new MailboxDisconnectedException("No mailbox connection exists to receive mail from");
		
		// Send the data timeout.
		
		if(waitTime < 0)
			waitTime = 0;
		
		addData(ByteBuffer.allocate(4).putInt(waitTime).array());
		executeWithData(RESP.REQDATA);
		
		// Create a pattern to match the sender.
		Pattern p = Pattern.compile(filter);
		
		// First search existing buffered mail to see if there are already incoming messages
		// from that sender.
		for(String sender : mailBuffer.keySet()){
			
			Matcher m = p.matcher(sender);
			
			if(m.find()){
				
				Queue<T> mailFromSender = mailBuffer.get(m);
				
				T message = mailFromSender.remove();
				
				execute(REQ.SATIATED, RESP.REQGRANTED);
				
				return message;
			}
		}
		
		// If there are no buffered messages, start receiving mail.
		while(true){
		
			if(execute(REQ.NEXTLETTER, RESP.INCOMINGMAIL, RESP.MAILTIMEOUT) != RESP.INCOMINGMAIL){
				
				if(waitTime == -1)
					throw new NoMailException("There is no mail matching the specified filter in the mailbox.");
				else
					throw new MailTimeoutException("The waiting period has been exceeded, no messages matching the pattern discovered.");
			}
			
			// The format is <SENDER><EOL><LENGTH><-- PAYLOAD BYTES -->
			
			String sender = null;
			
			byte[] messageData = null;

			sender = br.readLine();

			// Get the payload.
			byte[] intData = new byte[4];
			
			is.read(intData);

			int byteLength = ByteBuffer.allocate(4).put(intData).getInt();

			messageData = new byte[byteLength];
			is.read(messageData);
				
			// Check if the message is one we want.
			Matcher m = p.matcher(sender);
			
			T message = newMessage(sender, messageData);
			
			if(m.find()){
				
				execute(REQ.SATIATED, RESP.REQGRANTED);
				
				return message;
			}
			else {
				
				// If the message is not the one we are interested in, buffer it
				// for later.
				
				Queue<T> messageQueue = mailBuffer.get(sender);
				
				if(messageQueue == null)
					messageQueue = new LinkedList<T>();
					
				messageQueue.add(message);
				
				mailBuffer.put(sender, messageQueue);
			}
		}
	}
	
	public void sendMessage(String recipient, T message) throws MailboxDisconnectedException, IOException, DeliveryFailureException {

		// The format is <RECIPIENT><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		if(execute(REQ.SENDLETTER, RESP.REQDATA, RESP.NOBOXCONNECTION) != RESP.REQDATA)
			throw new MailboxDisconnectedException("Cannot send a message as there is no mailbox connection.");
		
		addData(recipient);
		executeWithData(RESP.REQDATA);
		
		byte[] payload = message.marshal();
		
		// Load the message to send into the buffer.
		addData(ByteBuffer.allocate(4).putInt(payload.length).array());
		addData(payload);
		
		if(executeWithData(RESP.REQGRANTED, RESP.DELIVERYFAILURE) != RESP.REQGRANTED)
			throw new DeliveryFailureException("");
	}

	public void emptyMailQueue() throws CommunicationException, MailboxDisconnectedException, IOException {

		mailBuffer.clear();
		
		if(execute(REQ.EMPTYBOX, RESP.REQGRANTED, RESP.NOBOXCONNECTION) != RESP.REQGRANTED)
			throw new MailboxDisconnectedException("Cannot empty the mail queue as there is no connection.");
	}
	
	public void createMailbox(String identifier, String password) throws PostOfficeException, IOException {
		
		execute(REQ.CREATEBOX, RESP.REQDATA);
		
		addData(identifier);
		executeWithData(RESP.REQDATA);
		
		addData(md5Hash(password));
		
		if(executeWithData(RESP.REQGRANTED, RESP.BOXEXISTS) != RESP.REQGRANTED)
			throw new ExistentMailboxException("A mailbox with the identifier '" + identifier + "' already exists.");
	}
	
	public void deleteMailbox() throws IOException, MailboxDisconnectedException {
		
		if(execute(REQ.REMOVEBOX, RESP.REQGRANTED, RESP.NOBOXCONNECTION) != RESP.REQGRANTED)
			throw new MailboxDisconnectedException("Cannot delete the current mailbox as no mailbox has been checked out.");
		
		mailBuffer.clear();
		
		mailboxId = null;
	}
	
	public void checkoutMailbox(String identifier, String password) throws ExistingMailboxConnectionException, IOException, NonExistentMailboxException, MailboxInUseException, UnauthorizedActionException {
		
		if(execute(REQ.REQBOX, RESP.REQDATA, RESP.ALREADYCONNECTED) != RESP.REQDATA)
			throw new ExistingMailboxConnectionException("A connection to mailbox already exists. Disconnect before checking out a different one.");

		addData(identifier);
		executeWithData(RESP.REQDATA);
		
		addData(md5Hash(password));
		
		RESP response = executeWithData(RESP.REQGRANTED, RESP.NONEXISTENTBOX, RESP.BOXINUSE, RESP.NOAUTH);
		
		if(response != RESP.REQGRANTED){
			
			switch(response){
			
				case NONEXISTENTBOX:
					throw new NonExistentMailboxException("The mailbox with the identifier '" 
							+ identifier + "' does not exist, and therefore cannot be checked out.");
				case BOXINUSE:
					throw new MailboxInUseException("The mailbox with the identifier '"
							+ identifier + "' has already been checked out by someone else.");
				case NOAUTH:
					throw new UnauthorizedActionException("Bad credentials for mailbox '" + identifier + "'.");
					
				default:
					// No action as this will not happen.
			}
		}
		
		mailboxId = identifier;
	}
	
	public void returnMailbox() throws IOException, MailboxDisconnectedException{
		
		if(execute(REQ.DISCONNECTBOX, RESP.REQGRANTED, RESP.NOBOXCONNECTION) != RESP.REQGRANTED)
			throw new MailboxDisconnectedException("Cannot disconnect from the present mailbox as there is no connection.");
		
		mailBuffer.clear();
		
		mailboxId = null;
	}
	
	private RESP execute(REQ req, RESP... resp) throws IOException {
		
		return execute(-1, req, resp);
	}
	
	private RESP execute(int timeout, REQ req, RESP... resp) throws IOException {
		
		// Clears the write buffer and adds the command to execute to it.
		writeBuffer.clear();
		writeBuffer.add(new byte[]{ (byte) req.ordinal() });
		
		return executeWithData(timeout, resp);
	}
	
	private RESP executeWithData(RESP... resp) throws IOException {
		
		return executeWithData(-1, resp);
	}
	
	private RESP executeWithData(int timeout, RESP... resp) throws IOException {

		int previousTimeout = s.getSoTimeout();
		
		try {

			if(timeout != -1)
				s.setSoTimeout(timeout + 1000); // Leave 1000 seconds longer than the server to act.

			for(byte[] data : writeBuffer){

				os.write(data);
				os.write(CommFlags.END_OF_LINE);
			}

			writeBuffer.clear();

			RESP response = getResponse();

			if(!Arrays.asList(resp).contains(response))
				throw new IOException("Unexpected response '" 
						+ response.name() + "'. Expected  element of " + resp + ". Likely, the client and server are out of sync.");
			
			return response;
		}
		catch(SocketTimeoutException ste){
			
			throw new IOException("Server failed to respond within the alotted time.");
		}
		finally {
			
			s.setSoTimeout(previousTimeout);
		}
	}
	
	private void addData(String payload){

		try {
			
			addData(payload.getBytes("UTF-8"));
			addData(new byte[]{ CommFlags.END_OF_LINE });
		
		} catch (UnsupportedEncodingException e) {} // This can never happen.
	}
	
	public void addData(byte[] payload){
		
		writeBuffer.add(payload);
	}
	
	
	private RESP getResponse() throws IOException {

		int code = is.read();

		RESP resp = CommFlags.getRespByCode(code);

		if(resp == null)
			throw new IOException("Unknown response code '" + code + "'. Likely, the client and server are out of sync.");

		return resp;
	}
	
	private byte[] md5Hash(String password){
		
		md.reset();
		
		try {
			return md.digest(password.getBytes("UTF-8"));
		} catch (UnsupportedEncodingException e) {} // This won't ever happen, so who cares.
		
		return null;
	}
}
