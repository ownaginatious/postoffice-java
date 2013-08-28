package postoffice.datatypes;

public class CommFlags {

	public static enum REQ {

		REQBOX,
		DISCONNECTBOX,
		CREATEBOX,
		REMOVEBOX,
		DISCONNECT,
		SENDLETTER,
		GETMAIL,
		NEXTLETTER,
		SATIATED,
		EMPTYBOX
	}

	public static enum RESP {

		NOAUTH,
		BADCOMMAND,
		ALREADYCONNECTED,
		MAILTIMEOUT,
		BOXEXISTS,
		NONEXISTENTBOX,
		NOBOXCONNECTION,
		BOXINUSE,
		DELIVERYFAILURE,
		SHUTDOWN,
		COMMTIMEOUT,
		REQDATA,
		REQGRANTED,
		NOMAIL,
		INCOMINGMAIL
	}
	
	public static final int END_OF_LINE = 0xA;
	
	private static final REQ[] reqValues = REQ.values();
	private static final RESP[] respValues = RESP.values();
	
	public static RESP getRespByCode(int code){
		
		if(code < 0 || code > (respValues.length - 1))
			return null;
		
		return respValues[code];
	}
	
	public static REQ getReqByCode(int code){
	
		if(code < 0 || code > (reqValues.length - 1))
			return null;
		
		return reqValues[code];
	}
}
