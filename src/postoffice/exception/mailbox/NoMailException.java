package postoffice.exception.mailbox;

public class NoMailException extends MailboxException {
	
	private static final long serialVersionUID = -5420416375714865874L;

	public NoMailException(String message, Exception e){
	
		super(message, e);
	}
	
	public NoMailException(String message){
		
		super(message);
	}
}
