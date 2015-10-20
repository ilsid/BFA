package com.ilsid.bfa.common;

//TODO: implement stub
public class BFAClassLoader extends ClassLoader {
	
	private static final BFAClassLoader instance = new BFAClassLoader();
	
	private BFAClassLoader() {
	}
	
	public static ClassLoader getInstance() {
		return instance;
	}

}
