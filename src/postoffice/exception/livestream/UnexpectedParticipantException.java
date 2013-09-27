package postoffice.exception.livestream;

public class UnexpectedParticipantException extends LiveStreamException {
	
	private static final long serialVersionUID = 3126121103116301042L;

	public UnexpectedParticipantException(String message, Exception e){
	
		super(message, e);
	}
	
	public UnexpectedParticipantException(String message){
		
		super(message);
	}
}
