package postoffice.exception.mailbox;

public class ExistingMailboxConnectionException extends MailboxException {
	
	private static final long serialVersionUID = -2594368131561775741L;

	public ExistingMailboxConnectionException(String message, Exception e){
	
		super(message, e);
	}
	
	public ExistingMailboxConnectionException(String message){
		
		super(message);
	}
}
