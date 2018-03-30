package com.ilsid.bfa.main;

public class Server {

	public static void main(String[] args) throws Exception {
		HttpServer.start();
		HttpServer.join();
	}

}
