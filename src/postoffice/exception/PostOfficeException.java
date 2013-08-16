package postoffice.exception;

public class PostOfficeException extends Exception {

	private static final long serialVersionUID = -2933505721409351352L;

	public PostOfficeException(String message, Exception e){
	
		super(message, e);
	}
	
	public PostOfficeException(String message){
		super(message);
	}
}
