package com.ilsid.bfa.persistence;

import java.util.Map;

public interface DatabaseServerManager {
	
	public Map<String, String> getConfig();
	
	public void startServer() throws Exception;
	
	public void stopServer();

}
