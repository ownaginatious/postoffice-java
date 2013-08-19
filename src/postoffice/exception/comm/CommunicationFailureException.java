package postoffice.exception.comm;

public class CommunicationFailureException extends CommunicationException {
	
	private static final long serialVersionUID = 5906134903392686415L;

	public CommunicationFailureException(String message, Exception e){
	
		super(message, e);
	}
	
	public CommunicationFailureException(String message){
		super(message);
	}
}
