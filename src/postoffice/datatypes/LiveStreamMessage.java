package postoffice.datatypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

public class LiveStreamMessage extends Message {

	private String participantA, participantB;
	
	private UUID liveStreamId;
	
	public static enum MES {
		
		REQUEST,
		REJECT,
		ACCEPT,
		READY,
		FAILURE,
		CLOSING
	}
	
	public LiveStreamMessage(UUID liveStreamId, String participantA, String participantB){

		this.participantA = participantA;
		this.participantB = participantB;
		
		this.liveStreamId = liveStreamId;
	}
	
	@Override
	public void demarshal(byte[] data) {
		
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));
		
		try {
			
			participantA = dis.readUTF();
			participantB = dis.readUTF();
			
			liveStreamId = UUID.fromString(dis.readUTF());
			
		} catch (IOException e) {} // Cannot happen.
	}

	@Override
	public byte[] marshal() {
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		try {
			
			dos.writeUTF(participantA);
			dos.writeUTF(participantB);
			
			dos.writeUTF(liveStreamId.toString());
		
		} catch (IOException e) {} // Cannot happen.
		
		return baos.toByteArray();
	}
}
