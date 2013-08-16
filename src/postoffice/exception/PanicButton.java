package postoffice.exception;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

public class PanicButton {

	private static Logger logger = Logger.getLogger(PanicButton.class);
	
	private static Set<Killable> killableThreads = new HashSet<Killable>();
	
	public static void addStoppable(Killable s){
		
		killableThreads.add(s);
	}
	
	public static void killEverybody(String id){
		
		logger.error("Thread identified by '" + id + "' hit the panic button.");
		
		for(Killable k : killableThreads)
			k.kill();
	}
	
	public static void killEverybody(String id, Exception e){
		
		logger.error("Thread identified by '" 
				+ id + "' hit the panic button because of the following: " 
				+ e.getStackTrace());
		
		for(Killable k : killableThreads)
			k.kill();
	}
}
