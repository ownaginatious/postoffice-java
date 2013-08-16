package postoffice.exception.mailbox;

import postoffice.exception.PostOfficeException;

public class MailboxException extends PostOfficeException {

	private static final long serialVersionUID = -3203428976712292848L;
	
	public MailboxException(String message, Exception e){
	
		super(message, e);
	}
	
	public MailboxException(String message){
		
		super(message);
	}
}
