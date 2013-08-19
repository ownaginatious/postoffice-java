package postoffice.exception.mailbox;

public class MailboxDisconnectedException extends MailboxException {
	
	private static final long serialVersionUID = -4730739140840723363L;

	public MailboxDisconnectedException(String message, Exception e){
	
		super(message, e);
	}
	
	public MailboxDisconnectedException(String message){
		
		super(message);
	}
}
