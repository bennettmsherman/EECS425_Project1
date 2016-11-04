/**
 * This class has a variey of static methods that are
 * usable by both the client and server.
 * @author Bennett Sherman
 */

package bms113.eecs325.cwru.edu;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

abstract class ServerClientCommon {
	
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
	
	/**
	 *  I'm in the 325N section and my roster number is 2 (so 46 + 2 + 5000)
	 */
	static final int SERVER_PORT = 5048;

	/**
	 * The combination of a private constructor and abstract class
	 * means that this class can not be instantiated.
	 */
	private ServerClientCommon() {}
	
	/**
	 * Close the socket specified by the parameter
	 * @param toClose The socket to close
	 */
	static void closeSocket(Socket toClose)
	{
		assert !toClose.isClosed();
		try
		{
			toClose.close();
		}
		catch (IOException err)
		{
			System.err.println("Error closing socket.");
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		}
	}
	
	/**
	 * @param connectionSocket A connection socket that links the server and client.
	 * @return Get a buffered reader based on a connectionSocket's input stream
	 */
	static BufferedReader getSocketBufferedReader(Socket connectionSocket)
	{
		BufferedReader buffRdr = null;
		try
		{
			buffRdr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		}
		catch (IOException err)
		{
			System.err.println("Unable to create a BufferedReader for the socket on " + SERVER_PORT);
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		}
		return buffRdr;
	}
	
	/**
	 * Get a DataOutputStream associated with the parameter's outputStream
	 * @param connectionSocket A connected connection socket
	 * @return A DataOutputStream that the server can be written into
	 */
	static DataOutputStream getSocketDataOutputStream(Socket connectionSocket)
	{
		DataOutputStream outStream = null;
		try
		{
			outStream = new DataOutputStream(connectionSocket.getOutputStream());
		}
		catch (IOException err)
		{
			System.err.println("Unable to create a DataOutputStream for port: " + SERVER_PORT);
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		} 
		return outStream;
	}
	
	/**
	 * Read data from the socket's BufferedReader. This is a blocking call.
	 * @return
	 */
	static String readFromSocket(BufferedReader reader)
	{
		String readMsg = "";
		try
		{
			readMsg = reader.readLine();
		}
		catch (IOException err) {
			System.err.println("Server encountered an error reading from a socket. Error was:");
			err.printStackTrace();
			Thread.currentThread().stop();
		}
		return readMsg;
	}
	
	/**
	 * Send a message to the DataOutputStream parameter.
	 * This is how inter-client communication works. Client A messages the server
	 * intending to message client B, and the server drops a message into Client B's DataOutputStream.
	 * @param msgToSend The message to send
	 * @param stream The stream to write into.
	 */
	static void sendMessageToDataOutputStream(String msgToSend, DataOutputStream stream)
	{
		try
		{
			// Append a newline to each of the sent messages
			stream.writeBytes(msgToSend + "\n");
			stream.flush();
		} 
		catch (IOException err) {
			System.err.println("Server encountered an error writing to DataOutputStream. Error was:");
			err.printStackTrace();
			Thread.currentThread().stop();
		}
	}
	
}
