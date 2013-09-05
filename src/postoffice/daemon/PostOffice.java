package postoffice.daemon;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import postoffice.datatypes.Letter;
import postoffice.datatypes.Mailbox;
import postoffice.exception.mailbox.ExistentMailboxException;
import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.NonExistentMailboxException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class PostOffice implements Runnable {

	public static final String OFFICE_ADDRESS = "postoffice.daemon";
	public static final int DEFAULT_PORT = 8228;

	private static Logger logger = Logger.getLogger(PostOffice.class);

	private Map<String, Mailbox> mailboxes = new ConcurrentHashMap<String, Mailbox>();

	private boolean running = true;
	
	private ServerSocket ss = null;
	
	public static PostOffice createPostOffice() throws IOException {
		
		return PostOffice.createPostOffice(DEFAULT_PORT);
	}
	
	public static PostOffice createPostOffice(int port) throws IOException {
		
		PostOffice postOffice = new PostOffice(port);
		
		(new Thread(postOffice)).start();
		
		return postOffice;
	}

	private PostOffice(int port) throws IOException {
		
		ss = new ServerSocket(port);
		logger.debug("Daemon started on port " + port + ".");
		
	}
	
	/**
	 * Checks if a mailbox has been checked out or not.
	 * 
	 * @param mailbox The mailbox to check the status of.
	 * @return True if the mailbox is checked out, false otherwise.
	 * @throws NonExistentMailboxException Thrown if the mailbox does not exist.
	 */
	protected boolean isCheckedOut(String mailbox) throws NonExistentMailboxException {

		Mailbox mb = mailboxes.get(mailbox);

		if(mb == null)
			throw new NonExistentMailboxException("The mailbox '" + mailbox + "' does not exist.");

		return mb.isCheckedOut();
	}
	
	/**
	 * Creates a password protected mailbox.
	 * 
	 * @param id Identifier for the mailbox.
	 * @param passwordHash A hash of a password for the mailbox.
	 * @throws ExistentMailboxException Thrown if the mailbox already exists.
	 */
	protected synchronized void createMailbox(String id, byte[] passwordHash) throws ExistentMailboxException {

		Mailbox currentBox = mailboxes.get(id);

		if(currentBox != null)
			throw new ExistentMailboxException("Cannot create the mailbox with identifier '" + id + "' as it already exists.");

		mailboxes.put(id, new Mailbox(id, Arrays.copyOf(passwordHash, passwordHash.length)));
	}

	/**
	 * Destroys an existing mailbox.
	 * 
	 * @param id Identifier for the mailbox.
	 * @throws NonExistentMailboxException Thrown if the mailbox to destroy does not exist.
	 * @throws UnauthorizedActionException Thrown if a thread does not first checkout a mailbox before attempting to destroy it.
	 */
	protected synchronized void destroyMailbox(String id) throws NonExistentMailboxException, UnauthorizedActionException {

		Mailbox currentBox = mailboxes.get(id);

		if(currentBox == null)
			throw new NonExistentMailboxException("Cannot destroy the mailbox '" + id + "' as it does not exist.");
		else
			currentBox.release();
		
		mailboxes.remove(id);
	}

	/**
	 * Checks if a certain mailbox exists.
	 * 
	 * @param id Identifier for the mailbox.
	 * @return True if it does, false otherwise.
	 */
	protected boolean mailboxExists(String id){

		return mailboxes.containsKey(id);
	}

	/**
	 * Checks out a mailbox and locks it for use only by the current thread.
	 * 
	 * @param id Identifier for the mailbox.
	 * @param passwordHash A hash of the password for the mailbox.
	 * @return The checked out mailbox.
	 * @throws MailboxInUseException Thrown if the mailbox has already been checked out.
	 * @throws BadCredentialsException Thrown if an incorrect password was used to access the mailbox.
	 * @throws NonExistentMailboxException Thrown if the mailbox does not exist.
	 */
	protected Mailbox checkoutMailbox(String id, byte[] passwordHash) throws UnauthorizedActionException, MailboxInUseException, NonExistentMailboxException {

		Mailbox currentBox = mailboxes.get(id);

		if(currentBox == null)
			throw new NonExistentMailboxException("The mailbox with the identifier '" + id + "' cannot be checked out as it does not exist.");
		
		currentBox.checkout(passwordHash);
		
		return currentBox;
	}

	/**
	 * Returns a mailbox; unlocking it for other threads to access.
	 * 
	 * @param id Identifier for the mailbox.
	 * @throws UnauthorizedActionException Thrown if a thread not currently holding the mailbox tries to unlock it.
	 * @throws NonExistentMailboxException Thrown if the mailbox in question does not exist.
	 */
	protected void returnMailbox(String id) throws UnauthorizedActionException, NonExistentMailboxException{
		
		Mailbox currentBox = mailboxes.get(id);
		
		if(currentBox == null)
			throw new NonExistentMailboxException("The mailbox with the identifier '" + id + "' cannot be returned as it does not exist.");
		
		currentBox.release();
	}
	
	/**
	 * Sends a message to a mailbox connected to the post office server.
	 * 
	 * @param letter The letter to send.
	 * @throws NonExistentMailboxException Thrown if the recipient mailbox of the letter does not exist.
	 */
	protected void sendLetter(Letter letter) throws NonExistentMailboxException {
		
		String recipient = letter.getRecipient();
		
		Mailbox recBox = mailboxes.get(recipient);
		
		if(recBox == null)
			throw new NonExistentMailboxException("The recipient '" + recBox + "' does not exist.");
		else
			recBox.deliver(letter);
	}

	/**
	 * Shuts down the post office network and disconnects all clients.
	 * 
	 * @param running 
	 */
	public void shutdown(){

		this.running = false;
		
		try {
			ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	@Override
	public void run() {

		ExecutorService executor = Executors.newCachedThreadPool();
		
		while(running){

			long connId = 0;

			try {

				connId++;

				MailClerk mc = new MailClerk(ss.accept(), this);
				executor.execute(mc);

			} catch (SocketException e){

				// This exception is only thrown when the server's socket is
				// terminated.

				logger.info(e.getMessage());

			} catch (IOException e) {

				logger.debug("Server failed when accepting connection number " + connId + ".");
				logger.debug(e.getStackTrace());

			}
		}
		
		executor.shutdown();
	}
}
