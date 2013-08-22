package postoffice.datatypes;

public abstract class Message {

	private	String sender;
	
	public void initialize(String sender, byte[] data){
		
		this.sender = sender;
		demarshal(data);
	}

	public final String getSender(){
		
		return sender;
	}
	
	public abstract void demarshal(byte[] data);
	
	public abstract byte[] marshal();
}
