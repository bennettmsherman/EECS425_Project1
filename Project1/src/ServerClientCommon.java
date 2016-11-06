/**
 * This class has a variety of methods and constants that are
 * usable by both the client and server.
 * @author Bennett Sherman
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

abstract class ServerClientCommon {
	
	///////////////////
	// CONSTANTS	 //
	///////////////////
	
	/**
	 * This must be placed at the beginning of a client message in order to specify a control command.
	 */
	static final String CONTROL_MESSAGE_SPECIFIER = "C0NTR0L:";
	
	/**
	 * The following group of strings represents constants which serve as control messages.
	 * "C0NTR0L:" must precede each of these strings. Furthermore, for messages with an "=",
	 * it means that data is to be entered immediately following the equals sign. Trying two
	 * commands in one message is undefined.
	 */
	/**
	 * Specify the name of the client.
	 */
	static final String SET_USERNAME = "SET MY NAME=";
	
	/**
	 * Specify the name of the peer that a client wants to connect to.
	 * Following this command, the server will attempt to connect this
	 * client to the desired peer. Specify "Listener" to disconnect with
	 * the current client if currently chatting with someone.
	 */
	static final String SET_PEER_NAME = "CONNECT TO PEER WITH NAME=";

	/**
	 * Used to specify the delimiter for the client. The delimiter breaks messages
	 * into multiple lines. "" means no delimiter.
	 */
	static final String SET_DELIMITER = "SET DELIMETER=";
	
	/**
	 * Have the server inform the client of its delimiter
	 */
	static final String GET_DELIMETER = "GET DELIMETER";
	
	/**
	 * This disconnects the client from
	 * the server. The peer (if one exists at the time of calling) will be notified
	 * of the disconnection.
	 */
	static final String DISCONNECT_FROM_SERVER = "DISCONNECT FROM SERVER";
	
	/**
	 * The server will return a comma separated list of clients currently attached
	 * to this server, by name.
	 */
	static final String GET_LIST_OF_CONNECTED_CLIENTS = "GET CONNECTED CLIENT NAMES";
	
	/**
	 * The server will return what it thinks this client's name is.
	 */
	static final String GET_MY_NAME = "GET MY NAME";
	
	/**
	 * Get the name of the client that the client is chatting with
	 */
	static final String GET_MY_PEERS_NAME = "GET MY PEER'S NAME";
	
	/**
	 * As noted above, when the command "SET MY NAME=" has this
	 * string as the parameter, the client will disconnect from
	 * the current chat and enter listening mode.
	 */
	static final String LISTENER_SPECIFIER = "Listener";
	
	/**
	 *  I'm in the 325N section and my roster number is 2 (so 46 + 2)
	 *  This is the default port number for both the server and client.
	 */
	static final int DEFAULT_SERVER_PORT = 50048;
	
	/**
	 * This program is expected to be run on eecslinab1.engineering.cwru.edu,
	 * so use this hostname as the default.
	 */
	static final String DEFAULT_SERVER_HOSTNAME = "eecslinab1.engineering.cwru.edu";
	
	/**
	 * Names that a client isn't allowed to take
	 */
	static final String[] RESERVED_NAMES = {LISTENER_SPECIFIER, "SVR", "SVR LOG", CONTROL_MESSAGE_SPECIFIER, "You", "LISTENER_MODE_ECHO", "GUI"};

	
	///////////////////
	// FUNCTIONS	 //
	///////////////////
	/**
	 * The combination of a private constructor and abstract class
	 * means that this class can not be instantiated.
	 */
	private ServerClientCommon() {}
	
	/**
	 * Close the socket specified by the parameter.
	 * @param socketToClose The socket to close
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 */
	static void closeSocket(Socket socketToClose, ChatClient client)
	{
		if (socketToClose == null)
		{
			return;
		}
		
		try
		{
			socketToClose.close();
		}
		catch (IOException err)
		{
			String otherMsg = "Error closing the socket with port number" + socketToClose.getLocalPort();
			displayExceptionMessageForClientOrServer(otherMsg, err, client);
		}
	}
	
	/**
	 * @param connectionSocket The socket to generate a BufferedReader from
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 * @return A BufferedReader associated with the specified socket.
	 */
	static BufferedReader getSocketBufferedReader(Socket connectionSocket, ChatClient client)
	{
		BufferedReader buffRdr = null;
		try
		{
			buffRdr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		}
		catch (IOException err)
		{
			String otherMsg = "Unable to create a BufferedReader for the socket on " + DEFAULT_SERVER_PORT + ". This application will exit.";
			displayExceptionMessageForClientOrServer(otherMsg, err, client);
			System.exit(-1);
		}
		return buffRdr;
	}
	
	/**
	 * @param connectionSocket The socket whose DataOutputStream is desired.
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 * @return A DataOutputStream associated with the socket parameter
	 */
	static DataOutputStream getSocketDataOutputStream(Socket connectionSocket, ChatClient client)
	{
		DataOutputStream outStream = null;
		try
		{
			outStream = new DataOutputStream(connectionSocket.getOutputStream());
		}
		catch (IOException err)
		{
			String otherMsg = "Unable to create a DataOutputStream for the specified socket. This application will exit.";
			displayExceptionMessageForClientOrServer(otherMsg, err, client);
			System.exit(-1);
		} 
		return outStream;
	}
	
	/**
	 * Retrieve a line from the specified BufferedReader. readLine()
	 * is a blocking call.
	 * @param reader The buffered reader to read from
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 * @return A string containing the current line of the parameter. If null, it means
	 * 		   that the connection was terminated and that the caller should execute
	 * 		   it's connection-break code.
	 */
	static String readFromSocket(BufferedReader reader, ChatClient client)
	{
		String readMsg = "";
		try
		{
			readMsg = reader.readLine();
		}
		catch (IOException err)
		{
//			String otherMsg = "Error encountered when reading from a socket. This application will exit.";
//			displayExceptionMessageForClientOrServer(otherMsg, err, client);
			readMsg = null;
		}
		return readMsg;
	}
	
	/**
	 * Send a message to the DataOutputStream parameter.
	 * This is how inter-socket communication works. Client A messages the server through the DataOutputStream
	 * that the server reads, intending to message client B, and the server drops a message into Client B's DataOutputStream.
	 * @param msgToSend The message to send
	 * @param stream The stream to write into.
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 */
	static void sendMessageToDataOutputStream(String msgToSend, DataOutputStream stream, ChatClient client)
	{
		try
		{
			// Append a newline to each of the sent messages
			stream.writeBytes(msgToSend + "\n");
			stream.flush();
		} 
		catch (IOException err)
		{
			String otherMsg = "Error encountered when writing to the DataOutputStream";
			displayExceptionMessageForClientOrServer(otherMsg, err, client);
			err.printStackTrace();
		}
	}
	
	/**
	 * This function is used to display a stack trace from either the client or server.
	 * @param otherMessage A string to print before the stack trace
	 * @param exc The exception whose stack trace will be printed
	 * @param client Null if the server is calling. Otherwise, the client param's
	 * 		  exception message handler is called.
	 */
	static void displayExceptionMessageForClientOrServer(String otherMessage, Exception exc, ChatClient client)
	{
		// A server is the caller
		if (client == null)
		{
			printExceptionMsgToConsole(otherMessage, exc);
		}
		else // A client is the caller
		{
			client.displayExceptionMessage(otherMessage, exc);
		}
	}
	
	/**
	 * Print an exception message and stack trace to the console.
	 * @param otherMsg A string to print before the stack trace
	 * @param except The exception whose stack trace will be printed.
	 */
	static void printExceptionMsgToConsole(String otherMsg, Exception except)
	{
		StringWriter sw = new StringWriter();
		except.printStackTrace(new PrintWriter(sw));
		System.err.println(otherMsg + "\n" + "EXCEPTION MESSAGE:\n" + sw.toString());
	}
}
