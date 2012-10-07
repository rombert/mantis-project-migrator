package ro.lmn.mantis.mpm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.lmn.mantis.mpm.internal.Connector;
import ro.lmn.mantis.mpm.internal.ConnectorImpl;
import ro.lmn.mantis.mpm.internal.Handle;

public class Main {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
	
	public static void main(String[] args) throws Exception {
		
		LOGGER.info(Main.class.getName() + " starting up");
		
		// load configuration
		Properties p = new Properties();
		p.load(checkNotNull(Main.class.getClassLoader().getResourceAsStream("mpm.properties"), "No file named mpm.properties found in the classpath"));
		
		// obtain connections
		Connector connector = new ConnectorImpl();
		Handle source = connector.connect(p.getProperty("source.url"), p.getProperty("source.username"), 
				p.getProperty("source.password"), Integer.parseInt(p.getProperty("source.projectId")));
		Handle dest = connector.connect(p.getProperty("destination.url"), p.getProperty("destination.username"), 
				p.getProperty("destination.password"), Integer.parseInt(p.getProperty("destination.projectId")));
		
		// synchronize versions
		dest.synchronizeVersions(source.getVersions());
		
		// synchronize categories
		dest.synchronizeCategories(source.getCategories());
		
		// TODO - synchronize issues
		// - issues will get a comment with original reporter: bla and 'migrated from $OLDURL'
		// - issue notes will be prepended with with original note reporter: bla
		
		LOGGER.info(Main.class.getName() + " completed");
	}

}
