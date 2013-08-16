package postoffice.exception.mailbox;

public class NonExistentMailboxException extends MailboxException {
	
	private static final long serialVersionUID = -2594368131561775741L;

	public NonExistentMailboxException(String message, Exception e){
	
		super(message, e);
	}
	
	public NonExistentMailboxException(String message){
		
		super(message);
	}
}
