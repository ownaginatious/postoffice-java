package postoffice.exception.mailbox;

public class ExistentMailboxException extends MailboxException {
	
	private static final long serialVersionUID = -2594368131561775741L;

	public ExistentMailboxException(String message, Exception e){
	
		super(message, e);
	}
	
	public ExistentMailboxException(String message){
		
		super(message);
	}
}
