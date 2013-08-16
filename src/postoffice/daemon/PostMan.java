package postoffice.daemon;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import postoffice.datatypes.Letter;
import postoffice.datatypes.Mailbox;

public class PostMan extends FutureTask<Letter>{

	private Map<String, Mailbox> mailboxes;
	
	public PostMan(Callable<Letter> callable, Map<String, Mailbox> mailboxes) {

		super(callable);
		this.mailboxes = mailboxes;
	}

	@Override
	protected void done(){
		
		try {
			
			Letter letter = this.get();
			
			String recipient = letter.getRecipient();
			
			Mailbox mb = mailboxes.get(recipient);
			
			if(mb != null)
				mb.deliver(letter);
			else { // If the message cannot be delivered, notify the sender.
				
				String sender = letter.getSender();
				
				Letter respLetter = new Letter(PostOffice.OFFICE_ADDRESS, sender, new byte[]{ 1 });
				
				mailboxes.get(sender).deliver(respLetter);
			}
			
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}
}
