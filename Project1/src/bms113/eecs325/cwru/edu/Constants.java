/**
 * This final, non-instantiable class is meant to have
 * the constants in it read by both the client and server applications.
 */
package bms113.eecs325.cwru.edu;

final class Constants {
	
	// The fields below are used for detecting and handling control messages
	static final String CONTROL_MESSAGE_SPECIFIER = "C0NTR0L:";
	static final String SET_USERNAME = "SET MY NAME=";
	static final String SET_PEER_NAME = "CONNECT TO PEER WITH NAME=";
	static final String SET_DELIMITER = "SET DELIMETER=";
	static final String EXIT_CHAT_CLIENT = "EXIT";
	static final String GET_LIST_OF_CONNECTED_CLIENTS = "GET CONNECTED CLIENT NAMES";
	static final String GET_MY_NAME = "GET MY NAME";
	// End control message specification
	
	static final String LISTENER_SPECIFIER = "Listener";

	// Don't allow isntantiation
	private Constants()
	{
		
	}
	
}
