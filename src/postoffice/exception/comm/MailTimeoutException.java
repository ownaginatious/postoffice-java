package postoffice.exception.comm;

public class MailTimeoutException extends CommunicationException {
	
	private static final long serialVersionUID = 3179935095655580412L;

	public MailTimeoutException(String message, Exception e){
	
		super(message, e);
	}
	
	public MailTimeoutException(String message){
		super(message);
	}
}
