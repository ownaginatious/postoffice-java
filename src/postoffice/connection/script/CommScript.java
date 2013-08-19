package postoffice.connection.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import postoffice.datatypes.CommFlags;
import postoffice.datatypes.CommFlags.REQ;
import postoffice.datatypes.CommFlags.RESP;
import postoffice.datatypes.Letter;
import postoffice.exception.comm.CommunicationException;
import postoffice.exception.comm.CommunicationFailureException;
import postoffice.exception.comm.DeliveryFailureException;
import postoffice.exception.comm.MailTimeoutException;
import postoffice.exception.comm.SynchronizationException;
import postoffice.exception.comm.UnknownRequestException;
import postoffice.exception.comm.UnknownResponseException;
import postoffice.exception.mailbox.ExistentMailboxException;
import postoffice.exception.mailbox.MailboxDisconnectedException;
import postoffice.exception.mailbox.MailboxException;
import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.NonExistentMailboxException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class CommScript {

	private InputStream is;
	private OutputStream os;
	
	private Queue<Letter> messageBuffer = new LinkedList<Letter>();
	private List<byte[]> writeBuffer = new ArrayList<byte[]>();
	
	public CommScript(InputStream is, OutputStream os){
		
		this.is = is;
		this.os = os;
	}
	
	public void execute(REQ req, RESP resp){
		
		execute(req, resp, -1);
	}
	
	public boolean execute(REQ req, RESP resp, int timeout){
		
		return execute(req, new RESP[]{ resp }, timeout);
	}
	
	public void execute(REQ req, RESP[] resp){
		
		execute(req, resp, -1);
	}
	
	public boolean execute(REQ req, RESP[] resp, int timeout){
		
		
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
	
	public void executeWithData(RESP resp){
		
		executeWithData(resp, -1);
	}
	
	public boolean executeWithData(RESP resp, int timeout){
		

		return executeWithData(new RESP[]{ resp }, timeout);
	}
	
	public void executeWithData(RESP[] resp){
		
		executeWithData(resp, -1);
	}
	
	public boolean executeWithData(RESP[] resp, int timeout){
		
		try {
			
			for(byte[] data : writeBuffer){
				
				os.write(data);
				os.write(CommFlags.END_OF_LINE);
			}
			
			writeBuffer.clear();
			
			RESP response = getResponse();
			
			checkResponse(response, resp);
		}
	}
	
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
	
	private RESP getResponse() throws IOException, UnknownResponseException {

		int code = is.read();

		RESP resp = CommFlags.getRespByCode(code);

		if(resp == null)
			throw new UnknownResponseException("Unknown response code '" + code + "'.");

		return resp;
	}
	
	public void clearLetterBuffer(){
		
		messageBuffer.clear();
	}
	
	public Letter getMessage(){
		
		return messageBuffer.poll();
	}
	
	private Letter receiveLetter() throws IOException {

		// The format is <SENDER><EOL><LENGTH><-- PAYLOAD BYTES -->
		
		String sender = null;

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

		return new Letter(sender, null, messageData);
	}
	
}
