package postoffice.daemon;

import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import postoffice.datatypes.Letter;
import postoffice.datatypes.LiveStreamMessage;
import postoffice.datatypes.Message;
import postoffice.exception.livestream.UnexpectedParticipantException;
import postoffice.exception.mailbox.NonExistentMailboxException;

public class ServerRoutedLiveStreamHandler {

	private String[] participants;
	private Socket[] sockets = new Socket[2];
	
	private Set<String> awaiting = new HashSet<String>();
	
	// Initialize with a randomly generated UUID.
	private UUID liveStreamId = UUID.randomUUID();
	
	public UUID getIdentifier() {
		
		return liveStreamId;
	}
	
	public ServerRoutedLiveStreamHandler(String requester, String partner, PostOffice po) throws NonExistentMailboxException {
		
		if(!po.mailboxExists(partner))
			throw new NonExistentMailboxException("Requested livestream partner '" + partner + "' does not exist.");
		
		participants = new String[]{ requester, partner };
		
		awaiting.add(requester);
		awaiting.add(partner);
			
		Message lsm = new LiveStreamMessage(liveStreamId, participants[0], participants[1]);

		// Announce the live stream to the participants.
		for(String p : participants)
			po.sendLetter(new Letter(PostOffice.OFFICE_ADDRESS, p, lsm.marshal()));
	}

	public boolean joinLiveStream(String id, Socket s) throws UnexpectedParticipantException {
	
		if(awaiting.contains(id)){
			
			awaiting.remove(id);
			
			sockets[awaiting.size() - 1] = s;
		}
		else
			throw new UnexpectedParticipantException("This livestream was not intended to include the user '" + id + "'.");
		
		return awaiting.size() > 0;
	}
}
