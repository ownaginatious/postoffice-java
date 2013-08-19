package postoffice.exception.comm;

import postoffice.exception.PostOfficeException;

public class CommunicationException extends PostOfficeException {
	
	private static final long serialVersionUID = 5906134903392686415L;

	public CommunicationException(String message, Exception e){
	
		super(message, e);
	}
	
	public CommunicationException(String message){
		super(message);
	}
}
