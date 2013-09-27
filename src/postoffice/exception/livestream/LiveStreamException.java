package postoffice.exception.livestream;

import postoffice.exception.PostOfficeException;

public class LiveStreamException extends PostOfficeException {
	
	private static final long serialVersionUID = 3126121103116301042L;

	public LiveStreamException(String message, Exception e){
	
		super(message, e);
	}
	
	public LiveStreamException(String message){
		
		super(message);
	}
}
