package postoffice.datatypes;

import java.io.UnsupportedEncodingException;

public class Letter {

	private String sender;
	private String recipient;
	private  byte[] payload = null;
	
	public Letter(String sender, String recipient, byte[] payload){
		
		this.sender = sender;
		this.recipient = recipient;
		this.payload = payload;
	}
	
	public String getSender() {
		
		return sender;
	}

	public String getRecipient() {
	
		return recipient;
	}

	public byte[] getPayloadBytes() {

		return payload;
	}


	public String getPayloadAsString(String encoding) throws UnsupportedEncodingException {

		return new String(this.payload, encoding);
	}
	
	@Override
	public String toString(){

		StringBuffer sb = new StringBuffer();
		
		sb.append("( From : ");
		sb.append(this.sender);
		sb.append(", To : ");
		sb.append(this.recipient);
		sb.append(" [");
		sb.append(this.payload.length);
		sb.append(" bytes ] )");
		
		return sb.toString();
	}
}