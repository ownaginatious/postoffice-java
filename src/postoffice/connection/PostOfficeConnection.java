package postoffice.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.SocketFactory;

import postoffice.connection.script.CommScript;
import postoffice.datatypes.CommFlags.REQ;
import postoffice.datatypes.CommFlags.RESP;
import postoffice.datatypes.Message;
import postoffice.exception.PostOfficeException;
import postoffice.exception.comm.CommunicationException;
import postoffice.exception.comm.MailTimeoutException;

public class PostOfficeConnection {

	private Socket s = null;
	private OutputStream os = null;
	private InputStream is = null;
	private CommScript cs = null;
	
	private String mailboxId = null;
	
	private String address = null;
	private int port;
	
	public PostOfficeConnection(String address, int port){
		
		this.address = address;
		this.port = port;
	}
	
	public String getMailboxId(){
		
		return this.mailboxId;
	}
	
	public void connect() throws PostOfficeException {
		
		try {
			
			s = SocketFactory.getDefault().createSocket(address, port);
			
			os = s.getOutputStream();
			is = s.getInputStream();
			
			cs = new CommScript(is, os);
			
		} catch (IOException e) {
			
			throw new PostOfficeException("Problem establishing post office connection.", e);
		}
	}
	
	public void disconnect() throws PostOfficeException{
		
		cs.execute(REQ.DISCONNECT, RESP.REQGRANTED);
		
		try {
			
			s.close();
			
		} catch (IOException e) {
			
			throw new PostOfficeException("Problem closing post office connection.", e);
		}
	}
	
	public byte[] getMessage() throws MailTimeoutException {
		
		return getMessage(".*", 0);
	}
	
	public byte[] getMessage(int waitTime) throws MailTimeoutException {
		
		return getMessage(".*", waitTime);
	}
	
	public byte[] getMessage(String filter) throws MailTimeoutException {
	
		return getMessage(filter, 0);
	}
	
	public byte[] getMessage(String filter, int waitTime) {
		
		cs.execute(REQ.GETLETTER, RESP.REQDATA);
		
		cs.addData(filter);
		cs.executeWithData(RESP.REQDATA);
		
		cs.addData(ByteBuffer.allocate(4).putLong(waitTime).array());
		cs.executeWithData(RESP.INCOMINGMAIL, waitTime);
		
		return cs.getMessage().getPayloadBytes();
	}
	
	public void sendMessage(String recipient, Message<?> message){

		// The format is <RECIPIENT><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		cs.execute(REQ.SENDLETTER, RESP.REQDATA);
		
		cs.addData(recipient);
		cs.executeWithData(RESP.REQDATA);
		
		byte[] payload = message.marshall();
		
		cs.addData(ByteBuffer.allocate(4).putInt(payload.length).array());
		cs.addData(payload);
		
		cs.executeWithData(RESP.REQGRANTED);
	}

	public void emptyMailQueue() throws CommunicationException {

		cs.clearLetterBuffer();
		cs.execute(REQ.EMPTYBOX, RESP.REQDATA);
	}
	
	public void createMailbox(String identifier, String password) throws CommunicationException {
		
		cs.execute(REQ.CREATEBOX, RESP.REQDATA);
		
		cs.addData(identifier);
		cs.executeWithData(RESP.REQDATA);
		
		cs.addData(password);
		cs.executeWithData(RESP.REQGRANTED);
	}
	
	public void deleteMailbox(){
		
		cs.execute(REQ.REMOVEBOX, RESP.REQDATA);
		
		mailboxId = null;
	}
	
	public void checkoutMailbox(String identifier, String password){
		
		cs.execute(REQ.REQBOX, RESP.REQDATA);

		cs.addData(identifier);
		cs.executeWithData(RESP.REQDATA);
		
		cs.addData(password);
		cs.executeWithData(RESP.REQGRANTED);
		
		mailboxId = identifier;
	}
	
	public void returnMailbox(){
		
		cs.execute(REQ.DISCONNECTBOX, RESP.REQDATA);
		
		mailboxId = null;
	}
}
