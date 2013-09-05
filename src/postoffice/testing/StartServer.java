package postoffice.testing;

import java.io.IOException;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import postoffice.daemon.PostOffice;

public class StartServer {

	public static void main(String[] args) throws IOException {
		
		Logger logger = Logger.getLogger("postoffice");
		
		ConsoleAppender console = new ConsoleAppender();
		
		// Configuration
		String pattern = "[%-5p] %t->%c{1} - %m%n";
		console.setLayout(new PatternLayout(pattern));
		console.setThreshold(Level.DEBUG);
		console.activateOptions();
		
		logger.addAppender(console);

		PostOffice.createPostOffice();
	}
}
