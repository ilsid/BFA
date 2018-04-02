package com.ilsid.bfa.main;

/**
 * Entry point. Starts the embedded http server.
 * 
 * @author illia.sydorovych
 *
 */
public class Application {

	public static void main(String[] args) throws Exception {
		HttpServer.start();
		HttpServer.join();
	}

}
