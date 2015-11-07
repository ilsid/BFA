package com.ilsid.bfa.common;

import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * Performs the logging configuration.
 * 
 * @author illia.sydorovych
 * 
 */
public class LoggingConfigurator {

	/**
	 * Configures log4j system using the specified configuration file. The configuration file can have either
	 * <i>.xml</i> or <i>.properties</i> format. If the passed file path is <code>null</code> or the file extension is
	 * different from <i>.xml</i> or <i>.properties</i> then no action is performed.
	 * 
	 * @param logConfigFile
	 *            the path to the configuration file
	 */
	public static void configureLog4j(String logConfigFile) {
		if (logConfigFile != null) {
			if (logConfigFile.endsWith(".xml")) {
				DOMConfigurator.configure(logConfigFile);
			} else if (logConfigFile.endsWith(".properties")) {
				PropertyConfigurator.configure(logConfigFile);
			}
		}
	}

}