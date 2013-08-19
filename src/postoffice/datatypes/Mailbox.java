package postoffice.datatypes;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import postoffice.exception.mailbox.MailboxInUseException;
import postoffice.exception.mailbox.UnauthorizedActionException;

public class Mailbox {

	private Lock checkoutLock = new ReentrantLock();

	private Thread lockHolder = null;

	private String owner;
	private byte[] passwordHash;

	private LinkedBlockingQueue<Letter> inbox = new LinkedBlockingQueue<Letter>();

	public Mailbox(String owner, byte[] passwordHash){

		this.owner = owner;
		this.passwordHash = passwordHash;
	}

	public synchronized Letter popMessage(){
	
		return inbox.poll();
	}
	
	public synchronized Letter popMessage(long timeout, TimeUnit tu){
		
		try {
			return inbox.poll(timeout, tu);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	public int getQueueSize(){
	
		return inbox.size();
	}
	
	public String getOwner(){
		
		return this.owner;
	}
	
	/**
	 * Puts the message in the message queue of the mailbox to be picked up later.
	 * 
	 * @param letter The letter to put in the message queue.
	 */
	public synchronized void deliver(Letter letter){

		inbox.add(letter);
	}

	/**
	 * Clears all messages from the mailbox.
	 * 
	 * @throws UnauthorizedActionException 
	 */
	public synchronized void empty() throws UnauthorizedActionException{

		if(this.holdingMailbox())
			inbox.clear();
		else
			throw new UnauthorizedActionException("Attempted to clear the mailbox '"
					+ this.owner + "' not presently held by the calling thread.");
	}

	public void checkout(byte[] passwordHash) throws UnauthorizedActionException, MailboxInUseException {

		if(!this.passwordHash.equals(passwordHash))
			throw new UnauthorizedActionException("Bad credentials to mailbox '" + this.owner + "'.");

		if(checkoutLock.tryLock())
			lockHolder = Thread.currentThread();
		else
			throw new MailboxInUseException("The mailbox '" + this.owner + "' is currently in use.");
	}

	/**
	 * Attempts to return or release a mailbox currently possessed by the calling thread.
	 * 
	 * @throws UnauthorizedActionException Thrown if the calling thread has not checked out the mailbox.
	 */
	public void release() throws UnauthorizedActionException {

		try {

			checkoutLock.unlock();
			lockHolder = null;
		}
		catch(IllegalMonitorStateException e){
			throw new UnauthorizedActionException("Attempted to release the mailbox '"
					+ this.owner +  "' not presently held by the calling thread.");
		}
	}

	/**
	 * Returns whether the calling thread is the one who has presently checkout this mailbox.
	 * 
	 * @return True is the calling thread has checked out the box, false otherwise.
	 */
	public boolean holdingMailbox(){

		return Thread.currentThread().equals(lockHolder);
	}

	/**
	 * Returns whether the mailbox is currently checked out.
	 * 
	 * @return True if the checked out, false otherwise.
	 */
	public boolean isCheckedOut(){

		return lockHolder != null;
	}
}
