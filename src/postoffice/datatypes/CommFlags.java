package postoffice.datatypes;

public class CommFlags {

	public static enum REQ {

		REQBOX,
		RETBOX,
		CREATEBOX,
		REMOVEBOX,
		SENDLETTER,
		GETMAIL,
		NEXTLETTER,
		SATIATED,
		EMPTYBOX,
		DISCONNECT,
		LIVESTREAM
	}

	public static enum RESP {

		REQGRANTED,
		REQDATA,
		NOAUTH,
		BADCOMMAND,
		ALREADYCONN,
		MAILTIMEOUT,
		BOXEXISTS,
		NONEXISTBOX,
		NOBOXCONN,
		BOXINUSE,
		DELFAIL,
		SHUTDOWN,
		COMMTIMEOUT,
		INMAIL,
		UNSUPPORTED
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
