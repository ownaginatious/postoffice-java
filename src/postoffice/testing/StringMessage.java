package postoffice.testing;

import java.io.UnsupportedEncodingException;

import postoffice.datatypes.Message;

public class StringMessage extends Message {

	private String stringData;
	
	@Override
	public void demarshal(byte[] data) {
		
		try {
			
			stringData = new String(data, "UTF-8");
			
		} catch (UnsupportedEncodingException e) {} // This will not ever happen.
	}

	@Override
	public byte[] marshal() {
		
		try {
			return stringData.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {} // This will not ever happen.
		
		return null;
	}
	
	public String getMessage(){
		
		return stringData;
	}
	
	public void setMessage(String message){
		
		this.stringData = message;
	}

}
