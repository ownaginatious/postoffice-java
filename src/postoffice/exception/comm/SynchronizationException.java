package postoffice.exception.comm;

public class SynchronizationException extends CommunicationException {
	
	private static final long serialVersionUID = 6620715950094438357L;

	public SynchronizationException(String message, Exception e){
	
		super(message, e);
	}
	
	public SynchronizationException(String message){
		super(message);
	}
}
