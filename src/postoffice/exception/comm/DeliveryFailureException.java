package postoffice.exception.comm;

public class DeliveryFailureException extends CommunicationException {
	
	private static final long serialVersionUID = -8607594523344982010L;

	public DeliveryFailureException(String message, Exception e){
	
		super(message, e);
	}
	
	public DeliveryFailureException(String message){
		super(message);
	}
}
