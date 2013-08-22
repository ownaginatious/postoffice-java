package postoffice.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;

import postoffice.datatypes.CommFlags.REQ;
import postoffice.datatypes.CommFlags.RESP;
import postoffice.datatypes.CommFlags;
import postoffice.datatypes.Letter;
import postoffice.datatypes.Message;
import postoffice.exception.PostOfficeException;
import postoffice.exception.comm.CommunicationException;
import postoffice.exception.comm.CommunicationFailureException;
import postoffice.exception.comm.UnknownResponseException;

public class PostOfficeConnection<T extends Message> {

	private Socket s = null;
	private InputStream is = null;
	private OutputStream os = null;
	
	private String mailboxId = null;
	
	private String address = null;
	private int port;
	
	private Class<T> messageClass;
	
	private List<byte[]> writeBuffer = new ArrayList<byte[]>();
	
	private RESP lastResponse;
	
	public PostOfficeConnection(String address, int port, Class<T> messageClass){
		
		this.address = address;
		this.port = port;
		
		this.messageClass = messageClass;
	}
	
	public String getMailboxId(){
		
		return this.mailboxId;
	}
	
	public void connect() throws CommunicationFailureException {
		
		try {
			
			s = SocketFactory.getDefault().createSocket(address, port);
			
			is = s.getInputStream();
			os = s.getOutputStream();
			
		} catch (IOException e) {
			
			throw new CommunicationFailureException("Problem establishing post office connection.", e);
		}
	}
	
	public void disconnect() throws IOException, UnknownResponseException {
		
		if(!execute(REQ.DISCONNECT, RESP.REQGRANTED)){
			
			s.close();
			
			throw new UnknownResponseException("Unclean disconnected: Expected response "
					+ RESP.REQGRANTED.name() + " from the server, but received '" 
					+ lastResponse.name() + " instead. Forcing socket closed anyway." );
		}
		
		s.close();
	}
	
	public T getMessage() throws IOException {
		
		return getMessage(".*", 0);
	}
	
	public T getMessage(int waitTime) throws IOException {
		
		return getMessage(".*", waitTime);
	}
	
	public T getMessage(String filter) throws IOException {
	
		return getMessage(filter, 0);
	}
	
	public T getMessage(String filter, int waitTime) throws IOException, UnknownResponseException {
		
		if(!execute(REQ.GETLETTER, RESP.REQDATA))
			throw new UnknownResponseException("");
		
		addData(filter);
		executeWithData(RESP.REQDATA);
		
		addData(ByteBuffer.allocate(4).putLong(waitTime).array());
		executeWithData(RESP.INCOMINGMAIL, waitTime);
		
		try {
			
			Letter letter = receiveLetter();
			
			T emptyMessage = messageClass.newInstance();
			
			emptyMessage.initialize(letter.getSender(), letter.getPayloadBytes());
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
	public void sendMessage(String recipient, T message) throws PostOfficeException, IOException {

		// The format is <RECIPIENT><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		execute(REQ.SENDLETTER, RESP.REQDATA);
		
		addData(recipient);
		executeWithData(RESP.REQDATA);
		
		byte[] payload = message.marshal();
		
		addData(ByteBuffer.allocate(4).putInt(payload.length).array());
		addData(payload);
		
		executeWithData(RESP.REQGRANTED);
	}

	public void emptyMailQueue() throws CommunicationException {

		execute(REQ.EMPTYBOX, RESP.REQDATA);
	}
	
	public void createMailbox(String identifier, String password) throws PostOfficeException, IOException {
		
		execute(REQ.CREATEBOX, RESP.REQDATA);
		
		addData(identifier);
		executeWithData(RESP.REQDATA);
		
		addData(password);
		executeWithData(RESP.REQGRANTED);
	}
	
	public void deleteMailbox(){
		
		execute(REQ.REMOVEBOX, RESP.REQDATA);
		
		mailboxId = null;
	}
	
	public void checkoutMailbox(String identifier, String password) throws PostOfficeException, IOException {
		
		execute(REQ.REQBOX, RESP.REQDATA);

		addData(identifier);
		executeWithData(RESP.REQDATA);
		
		addData(password);
		executeWithData(RESP.REQGRANTED);
		
		mailboxId = identifier;
	}
	
	public void returnMailbox(){
		
		execute(REQ.DISCONNECTBOX, RESP.REQDATA);
		
		mailboxId = null;
	}
	
	public Letter receiveLetter() throws IOException {

		// The format is <SENDER><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		String sender = null;
		
		InputStream is = s.getInputStream();
		
		byte[] messageData = null;

		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		sender = br.readLine();

		// Get the payload.
		byte[] intData = new byte[4];
		is.read(intData);

		int byteLength = ByteBuffer.allocate(4).put(intData).getInt();

		if(byteLength > 0){

			messageData = new byte[byteLength];
			is.read(messageData);
		}

		return new Letter(sender, mailboxId, messageData);
	}
	
	public boolean execute(REQ req, RESP resp){
		
		return execute(req, resp, -1);
	}
	
	//FIXME:
	public boolean execute(REQ req, RESP resp, int timeout){
		
		return false;
	}
	
	public void addData(String payload){

		try {
			
			addData(payload.getBytes("UTF-8"));
			addData(new byte[]{ CommFlags.END_OF_LINE });
		
		} catch (UnsupportedEncodingException e) {} // This can never happen.
	}
	
	public void addData(byte[] payload){
		
		writeBuffer.add(payload);
	}
	
	public boolean executeWithData(RESP resp) throws IOException {
		
		return executeWithData(resp, -1);
	}
	
	public boolean executeWithData(RESP resp, int timeout) throws IOException {

		int previousTimeout = s.getSoTimeout();

		if(timeout != -1)
			s.setSoTimeout(timeout + 1000); // Leave 1000 seconds longer than the server to act.

		for(byte[] data : writeBuffer){

			os.write(data);
			os.write(CommFlags.END_OF_LINE);
		}

		writeBuffer.clear();

		RESP response = getResponse();

		s.setSoTimeout(previousTimeout);

		return response == resp;
	}

	/*
	private void checkResponse(RESP response, RESP[] responses) throws CommunicationException, MailboxException {
		
		if(Arrays.asList(responses).contains(response))
			return;
		
		switch(response){
		
			case BADCOMMAND:
				throw new UnknownRequestException("The server received a command it did not understand.");
				
			case BOXEXISTS:
				throw new ExistentMailboxException("The mailbox already exists.");
				
			case BOXINUSE:
				throw new MailboxInUseException("The mailbox has already been checked out.");
				
			case DELIVERYFAILURE:
				throw new DeliveryFailureException("The target recipient does not exist.");
				
			case NOAUTH:
				throw new UnauthorizedActionException("Attempted to perform an action without authorization.");
		
			case NOBOXCONNECTION:
				throw new MailboxDisconnectedException("No connection to a mailbox exists.");
				
			case NONEXISTENTBOX:
				throw new NonExistentMailboxException("The requested mailbox does not exist.");
				
			case ALREADYCONNECTED:
				throw new ExistingMailboxConnectionException("Actions involving a different mailbox cannot be performed while connected to a different mailbox.");
				
			case MAILTIMEOUT:
				throw new MailTimeoutException("The server indicates a message was not received in the allotted time frame.");
				
			case COMMTIMEOUT:
				throw new CommunicationFailureException("The server indicates a message was not received in the allotted time frame.");
		
			default:
				
				throw new SynchronizationException("A response from the set "
						+ responses + " was expected, but " + response
						+ " was received.");
		}
	}
	*/
	
	private RESP getResponse() throws IOException {

		int code = is.read();

		RESP resp = CommFlags.getRespByCode(code);

		if(resp == null)
			throw new IOException("Unknown response code '" + code + "'. Likely, the client and server are out of sync.");

		lastResponse = resp;
		
		return resp;
	}
}
