package postoffice.exception.comm;

public class UnknownRequestException extends CommunicationException {
	
	private static final long serialVersionUID = -7664948873488661405L;

	public UnknownRequestException(String message, Exception e){
	
		super(message, e);
	}
	
	public UnknownRequestException(String message){
		super(message);
	}
}
