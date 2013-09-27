package postoffice.exception.livestream;

public class NonExistentLiveStreamException extends LiveStreamException {
	
	private static final long serialVersionUID = 3126121103116301042L;

	public NonExistentLiveStreamException(String message, Exception e){
	
		super(message, e);
	}
	
	public NonExistentLiveStreamException(String message){
		
		super(message);
	}
}
