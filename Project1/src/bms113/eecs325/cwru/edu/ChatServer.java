/**
 * The chat server for EECS325 project 1
 * @author bms113, Bennett Sherman
 */
package bms113.eecs325.cwru.edu;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;

import static bms113.eecs325.cwru.edu.Constants.*;

public class ChatServer {
	
	///////////////////
	// CLASS MEMBERS //
	///////////////////
	
	/**
	 *  I'm in the 325N section and my roster number is 2 (so 46 + 2 + 5000)
	 */
	private static final int SERVER_PORT = 5048;
	
	/**
	 * Links names to chat participants.
	 */
	private Hashtable<String, ChatParticipant> nameToParticipant = new Hashtable<>();
	
	/**
	 * Links participants to their thread.
	 */
	private Hashtable<ChatParticipant, ConnectedClientThread> participantToThread = new Hashtable<>();
	
	/**
	 * The TCP welcoming socket for this server.
	 */
	private ServerSocket welcomeSocket;
	
	
	/////////////////////
	// CLASS FUNCTIONS //
	/////////////////////
	
	/**
	 * Constructor for the Chat Server. It creates the welcome socket.
	 */
	public ChatServer(int port)
	{
		initWelcomeSocket(port);
	}
	
	/**
	 * Starts the server.
	 */
	@SuppressWarnings("static-access")
	public void startServer()
	{
		try 
		{
			System.out.println("Server started; IP Address: " + welcomeSocket.getInetAddress().getLocalHost().getHostAddress() +
					"; Port: " + welcomeSocket.getLocalPort() + "; Hostname: " + welcomeSocket.getInetAddress().getLocalHost().getHostName());
		}
		catch (UnknownHostException e)
		{
			e.printStackTrace();
		}
		
		while (true)
		{
			Socket connectionSocket = createConnectionSocket();
			ChatParticipant newClient = new ChatParticipant(connectionSocket);
			Thread clientThread = new ConnectedClientThread(newClient);
			clientThread.start();
		}
	}
	
	
	/**
	 * Create a DataOutputStream in the direction Server->Client
	 * @param connectionSocket The connection socket that's connected to the client
	 * @return A DataOutputStream that the server can write to in order to send msgs to the client.
	 */
	private DataOutputStream getOutToClientStream(Socket connectionSocket)
	{
		DataOutputStream outStream = null;
		try
		{
			outStream = new DataOutputStream(connectionSocket.getOutputStream());
		}
		catch (IOException err)
		{
			System.err.println("Unable to create a DataOutputStream for port: " + SERVER_PORT + ". Server terminating");
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		} 
		return outStream;
	}
	
	/**
	 * Initializes the ServerSocket member
	 * @param portNum The port to interface with a TCP socket
	 */
	private void initWelcomeSocket(int portNum)
	{
		try
		{
			welcomeSocket = new ServerSocket(SERVER_PORT);
		}
		catch (IOException err)
		{
			System.err.println("Unable to create the welcome socket on port: " + SERVER_PORT + ". Server terminating");
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		} 
	}
	
	/**
	 * @param connectionSocket A connection socket that links the server and client.
	 * @return Get a reader which the server can use to read data from the client.
	 */
	private BufferedReader getReaderForClientToServer(Socket connectionSocket)
	{
		BufferedReader buffRdr = null;
		try
		{
			buffRdr = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
		}
		catch (IOException err)
		{
			System.err.println("Unable to create a BufferedReader for the connection socket on " + SERVER_PORT + ". Server terminating");
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		}
		return buffRdr;
	}
	
	/**
	 * Creates a connection socket for a process connecting to the server.
	 * @return a connection socket that interfaces with the newly-connected client.
	 */
	private Socket createConnectionSocket()
	{
		Socket connectionSocket = null;
		try
		{
			connectionSocket = welcomeSocket.accept();
		}
		catch (IOException err)
		{
			System.err.println("Unable to create the connection socket on port: " + SERVER_PORT + ". Server terminating");
			System.err.println("Error was" + err.getMessage());
			System.exit(-1);
		}
		return connectionSocket;
	}
	
	/**
	 * Close the socket specified by the parameter
	 * @param toClose The socket to close
	 */
	private void closeSocket(Socket toClose)
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
	
	
	///////////////////
	//  INNER CLASS  //
	///////////////////
	
	/**
	 * This private class represents a thread for each client's connection.
	 * It's a private nested class, which means it can access all members and
	 * functions in ChatServer.java, but cannot be accessed from the outside.
	 */
	private class ConnectedClientThread extends Thread
	{
		// The ChatParticipant corresponding to this connection
		private ChatParticipant client;
		private BufferedReader inFromClient;
		private DataOutputStream outToClient;
		
		/**
		 * Constructs a new ConnectedClientThread
		 * @param client The client corresponding to this connection
		 */
		ConnectedClientThread(ChatParticipant client)
		{
			this.client = client;
		}
		
		/**
		 * Initializes the inFromClient and outToClient variables.
		 */
		void initializeStreamAndReader()
		{
			inFromClient = getReaderForClientToServer(client.getSocket());
			outToClient =  getOutToClientStream(client.getSocket());
		}
		
		public void run()
		{
			System.out.println("LOG: New client thread started with IP Address:Port=" + client.getSocket().getRemoteSocketAddress().toString().substring(1));
			
			// Add this client and thread to the participantToThread Hashtable
			participantToThread.put(client, this);
			
			// Initialize inFromClient and outToClient
			initializeStreamAndReader();
			
			// Say hi to the client
			sendMessageToClient("SERVER: Welcome! Please set a name. Type \"C0NTR0L: HELP\" for a list of commands");
			
			// Loop until the peer wants to exit
			boolean shouldContinue = true;
			while(shouldContinue)
			{
				String newMessage = readFromClient();
				System.out.println(client.getSocket().getRemoteSocketAddress().toString().substring(1) + ": " + newMessage);
				
				if (newMessage == null)
				{
					shouldContinue = false;
					continue;
				}
				
				// An exit control message will result in handleControlMessage returning false,
				// which will result in disconnect.
				if (isControlMessage(newMessage))
				{
					shouldContinue = handleControlMessage(newMessage);
					continue;
				}
				// If the client is in listen mode, echo messages.
				// If it's not in listen mode (and therefore is connected 
				// to another client), pass the message to the other client.
				else if (!client.isInListenMode())
				{
						passMessageFromClientToPeer(client.getName() + ": " + newMessage);						
				}
				else
				{
					sendMessageToClient("LISTEN_MODE_ECHO: " + newMessage);
				}
			}
			nameToParticipant.remove(client.getName());
			participantToThread.remove(client);
			closeSocket(client.getSocket());
			System.out.println("LOG: " + client.getSocket().getRemoteSocketAddress().toString().substring(1) + " has left");
		}
		
		/**
		 * Sends a message to the peer associated with this client.
		 * @param msgToSend The message
		 */
		void passMessageFromClientToPeer(String msgToSend)
		{
			// Get the DataOutputStream for the peer
			DataOutputStream peerDataOutStream = participantToThread.get(client.getPeer()).outToClient;
			sendMessageToDataOutputStream(msgToSend, peerDataOutStream);
		}
		
		/**
		 * Determine if the parameter is a control message. A control message
		 * starts with the CONTROL_MESSAGE_SPECIFIER string
		 * @param msgLine The string under consideration
		 * @return true if the message is a control message, false otherwise
		 */
		boolean isControlMessage(String msgLine)
		{
			return msgLine.startsWith(CONTROL_MESSAGE_SPECIFIER);
		}
		
		/**
		 * Performs actions based on control messages
		 * @param controlMsgLine The line containing the control message
		 * @return Whether or not the client should continue
		 */
		boolean handleControlMessage(String controlMsgLine)
		{
			if (controlMsgLine.contains(EXIT_CHAT_CLIENT))
			{
				sendMessageToClient("SERVER: CLOSING CONNECTION. SEE YOU LATER, " + client.getName());
				return false;
			}
			else if (controlMsgLine.contains(SET_USERNAME))
			{
				updateNameControlMsgHandler(controlMsgLine);
			}
			else if (controlMsgLine.contains(SET_PEER_NAME))
			{
				setPeerControlMsgHandler(controlMsgLine);
			}
			else if (controlMsgLine.contains(GET_LIST_OF_CONNECTED_CLIENTS))
			{
				getListOfConnectedClientsControlMsgHandler();
			}
			else if (controlMsgLine.contains(GET_MY_NAME))
			{
				sendMessageToClient(client.getName());
			}
			else
			{
				sendMessageToClient("*** \"" + controlMsgLine + "\" IS NOT A VALID CONTROL MESSAGE***");
			}
			
			return true;
		}
		
		void getListOfConnectedClientsControlMsgHandler()
		{
			String commaSeparatedClientNames = "SERVER: Clients connected to the server: ";
			for (String clientName : nameToParticipant.keySet())
			{
				commaSeparatedClientNames += clientName + ", ";
			}
			// Remove the trailing comma upon sending
			sendMessageToClient(commaSeparatedClientNames.substring(0, commaSeparatedClientNames.length()-2));
		}
		
		//TODO update such that old client is disconnected
		void setPeerControlMsgHandler(String controlMsgLine)
		{
			String newPeerName = controlMsgLine.substring(controlMsgLine.indexOf(SET_PEER_NAME) + SET_PEER_NAME.length());
			
			// If the desired peer doesn't exist and the client is currently connected to someone else
			if (!nameToParticipant.containsKey(newPeerName) && !client.isInListenMode())
			{
				sendMessageToClient("SERVER: The desired client, \"" + newPeerName + "\" is not connected to the server. Try again later." +
						"You are now being disconnected from: " + client.getPeer().getName());
				terminateConnectionBetweenClients();
				return;
			}
			// If the desired peer doesn't exist and the client currently isn't connected to anyone
			else if (!nameToParticipant.containsKey(newPeerName) && client.isInListenMode())
			{
				sendMessageToClient("SERVER: The desired client, \"" + newPeerName + "\" is not connected to the server. Try again later.");
				return;
			}
			// Given the above check, there must be a ChatParticipant corresponding
			// to newPeerName connected to the server.
			ChatParticipant desiredPeer = nameToParticipant.get(newPeerName);

			// If the both the desired peer and the caller are connected to other clients, alert the caller and have the caller disconnect
			// from its current peer.
			if (!desiredPeer.isInListenMode() && !client.isInListenMode())
			{
				sendMessageToClient("SERVER: The desired client, \"" + newPeerName + "\" is chatting with the user " + desiredPeer.getPeer().getName() + ". Try again later." +
										"You are now being disconnected from: " + client.getPeer().getName());
				terminateConnectionBetweenClients();
			}
			// If the desired peer is connected to someone but the caller is not. The caller will not be able to connect to the desired peer.
			else if (!desiredPeer.isInListenMode() && client.isInListenMode())
			{
				sendMessageToClient("SERVER: The desired client, \"" + newPeerName + "\" is chatting with the user " + desiredPeer.getPeer().getName() + ". Try again later.");
			}
			// If the caller is connected to another client, but the other peer is not, the caller will have to end its chat
			// with its current client and then connect to the new desired client.
			else if (desiredPeer.isInListenMode() && !client.isInListenMode())
			{
				sendMessageToClient("SERVER: You are now being disconnected from: " + client.getPeer().getName());
				terminateConnectionBetweenClients();
				connectToOtherClient(desiredPeer);
			}
			// Neither the caller nor the desired peer are currently connected, so connect the two.
			else
			{
				connectToOtherClient(desiredPeer);
			}	
		}
		
		void connectToOtherClient(ChatParticipant newPeer)
		{
			client.setPeer(newPeer);
			client.setIsInListenMode(false);
			sendMessageToClient("SERVER: You are now connected with \"" + newPeer.getName() + "\"");

			newPeer.setPeer(client);
			newPeer.setIsInListenMode(false);
			sendMessageToPeer("SERVER: You are now connected with \"" + client.getName() + "\"");
		}
		
		/**
		 * Called in the event that this thread's client is ending communication
		 * with its current peer.
		 */
		void terminateConnectionBetweenClients()
		{
			// Alert the client's peer of the termination
			sendMessageToPeer("SERVER: User \"" + client.getName() + "\" has exited the chat. You are now in listen mode.");
			
			// Physically break the connection by nulling this client's peer's peer member
			// Also, put the peer into listening mode
			client.getPeer().setIsInListenMode(true);
			client.getPeer().setPeer(null);
			
			// Now, have the calling client disconnect by nulling its peer
			// Also, put the caller into listening mode
			client.setIsInListenMode(true);
			client.setPeer(null);
		}
		
		/**
		 * The handler for the update name control message.
		 * This function reserves and sets the new name if it isn't currently held, and then
		 * un-reserves the client's current name. It then informs the client of the change.
		 * If the name is already in use, tell the client they can't reserve it.
		 * @param controlMsgLine
		 */
		void updateNameControlMsgHandler(String controlMsgLine)
		{
			String newName = controlMsgLine.substring(controlMsgLine.indexOf(SET_USERNAME) + SET_USERNAME.length());
			if (newName.equals(client.getName()))
			{
				sendMessageToClient("SERVER: The username \"" + newName + "\" is your current username.");
			}
			else if (!nameToParticipant.containsKey(newName))
			{
				nameToParticipant.remove(client.getName());
				client.setName(newName);
				nameToParticipant.put(newName, client);
				sendMessageToClient("SERVER: Your username has been set to \"" + newName + "\"");
			}
			else
			{
				sendMessageToClient("SERVER: The username \"" + newName + "\" is already in use. Choose another.");
			}
		}
		
		/**
		 * Send a message to the client directly connected to this socket.
		 * @param msgToSend The message to send
		 */
		void sendMessageToClient(String msgToSend)
		{
			sendMessageToDataOutputStream(msgToSend, outToClient);
		}
		
		void sendMessageToPeer(String msgToSend)
		{
			sendMessageToDataOutputStream(msgToSend, participantToThread.get(client.getPeer()).outToClient);
		}
		
		/**
		 * Send a message to the DataOutputStream parameter.
		 * This is how inter-client communication works. Client A messages the server
		 * intending to message client B, and the server drops a message into Client B's DataOutputStream.
		 * @param msgToSend The message to send
		 * @param stream The stream to write into.
		 */
		void sendMessageToDataOutputStream(String msgToSend, DataOutputStream stream)
		{
			try
			{
				// Append a newline to each of the sent messages
				stream.writeBytes(msgToSend + "\n");
				stream.flush();
			} 
			catch (IOException err) {
				System.err.println("Server encountered an error writing to the client " + client.getName() + "Error was:");
				err.printStackTrace();
				Thread.currentThread().stop();
			}
		}
		
		/**
		 * Read data from the inFromClient BufferedReader. This is a blocking call.
		 * @return
		 */
		String readFromClient()
		{
			String readMsg = "";
			try
			{
				readMsg = inFromClient.readLine();
			}
			catch (IOException err) {
				System.err.println("Server encountered an error reading from the client " + client.getName() + "Error was:");
				err.printStackTrace();
				Thread.currentThread().stop();
			}
			return readMsg;
		}
	}
	
	
	public static void main(String[] args)
	{
		// Start up the chat server.
		ChatServer cs = new ChatServer(SERVER_PORT);
		cs.startServer();
	}
}