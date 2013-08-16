package postoffice.exception.mailbox;

public class UnauthorizedActionException extends MailboxException {
	
	private static final long serialVersionUID = -2594368131561775741L;

	public UnauthorizedActionException(String message, Exception e){
	
		super(message, e);
	}
	
	public UnauthorizedActionException(String message){
		
		super(message);
	}
}
