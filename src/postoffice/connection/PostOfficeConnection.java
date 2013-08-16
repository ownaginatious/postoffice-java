package postoffice.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;

import postoffice.daemon.MailClerk.REQ;
import postoffice.datatypes.Letter;
import postoffice.exception.PostOfficeException;

public class PostOfficeConnection {

	private Socket s = null;
	private OutputStream os = null;
	private InputStream is = null;
	
	private String address = null;
	private int port;
	
	public PostOfficeConnection(String address, int port){
		
		this.address = address;
		this.port = port;
	}
	
	public void connect() throws PostOfficeException {
		
		try {
			
			s = SocketFactory.getDefault().createSocket(address, port);
			
			os = s.getOutputStream();
			is = s.getInputStream();
			
		} catch (IOException e) {
			
			throw new PostOfficeException("Problem establishing post office connection.", e);
		}
	}
	
	public void disconnect() throws PostOfficeException{
		
		try {
			
			s.close();
			
		} catch (IOException e) {
			
			throw new PostOfficeException("Problem closing post office connection.", e);
		}
	}
	
	public Letter getMessage(){
		
	}
	
	public Letter getMessage(int tamount, TimeUnit tu){
		
	}
	
	public Letter getMessage(String filter){
		
	}
	
	public Letter getMessage(String filter, int tamount, TimeUnit tu) throws IOException{
		
		os.write(REQ.GETLETTER.ordinal());
		
		
	}
	
	public Future<Letter> createFuture()
	{
	    ExecutorService service = Executors.newSingleThreadExecutor();
	    
	    Callable<Letter> callable = new Callable<Letter>(){

			@Override
			public Letter call() throws Exception {
				
				s.setSoTimeout(0);
				
				byte[] messageData = null;

				BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				
				String sender = br.readLine();
				String recipient = br.readLine();

				// Get the payload.
				byte[] intData = new byte[4];
				is.read(intData);

				int byteLength = ByteBuffer.allocate(4).put(intData).getInt();

				if(byteLength > 0){

					messageData = new byte[byteLength];
					is.read(messageData);
				}
				
				return new Letter(sender, recipient, messageData);
			}
	    };
	    
	    Future<Letter> ret = service.submit(callable);
	    
	    // Let the thread die when the callable has finished.
	    service.shutdown();
	    
	    return ret;
	}
	
	public void sendMessage(String recipient, byte[] message){
		
	}
	
	public void emptyMailQueue(){
		
	}
	
	public void createMailbox(String identifier, String password){
		
	}
	
	public void deleteMailbox(String identifier, String password){
		
	}
	
	public void deleteMailbox(){
		
	}
	
	public void checkoutMailbox(String identifier, String password){
		
	}
	
	public void returnMailbox(){
		
	}
}
