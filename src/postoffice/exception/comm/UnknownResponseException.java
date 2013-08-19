package postoffice.exception.comm;

public class UnknownResponseException extends CommunicationException {
	
	private static final long serialVersionUID = -7664948873488661405L;

	public UnknownResponseException(String message, Exception e){
	
		super(message, e);
	}
	
	public UnknownResponseException(String message){
		super(message);
	}
}
