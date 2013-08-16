package postoffice.exception.mailbox;

public class MailboxInUseException extends MailboxException {
	
	private static final long serialVersionUID = -2594368131561775741L;

	public MailboxInUseException(String message, Exception e){
	
		super(message, e);
	}
	
	public MailboxInUseException(String message){
		
		super(message);
	}
}
