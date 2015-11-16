package com.ilsid.bfa.persistence;

/**
 * Listens the class updates in the repository.
 * 
 * @author illia.sydorovych
 *
 */
public interface ClassUpdateListener {

	/**
	 * The method is triggered when the class with the specific name is updated in the repository.
	 * 
	 * @param className
	 *            the name of the class that has been updated
	 */
	void onClassUpdate(String className);

}
