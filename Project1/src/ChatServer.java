/**
 * The chat server for EECS325 project 1.
 * This is intended to be executed by running
 * the chatd executable with a desired port number
 * @author bms113, Bennett Sherman
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer {
	
	///////////////////
	// CLASS MEMBERS //
	///////////////////
	
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
	
	/**
	 * The port number that this server is operating from.
	 */
	private int serverPortNumber;
	
	/**
	 * This server's IP address
	 */
	private String serverIpAddr;
	
	/**
	 * This server's hostname
	 */
	private String serverHostname;
	
	/**
	 * Used to make sure that neither nameToParticipant and/or participantToThread
	 * is modified and read or modified and modified by multiple threads at once.
	 * Also makes sure that multiple threads can't change aspects of ChatParticipants
	 * at once. It's reentrant to prevent *oops, deadlock* issues.
	 */
	private ReentrantLock criticalServerDataLock = new ReentrantLock();
	
	/////////////////////
	// CLASS FUNCTIONS //
	/////////////////////
	
	/**
	 * Constructor for the Chat Server.
	 * @param port The port number of this server
	 */
	public ChatServer(int port)
	{
		this.serverPortNumber = port;
	}
	
	/**
	 * Starts the server. Its first step is to initialize the welcome socket and determine
	 * the host's IP and hostname.
	 */
	public void startServer()
	{		
		initWelcomeSocket();
		
		setServerIpAndHostname();
		
		System.out.println("Server started; IP Address: " + serverIpAddr + "; Port: " + serverPortNumber + "; Hostname: " + serverHostname);
		
		// The server will run endlessly
		while (true)
		{
			// Create a connection socket for the next client to connect
			// This will block until a new client wants to connect
			Socket connectionSocket = createConnectionSocket();
			
			// Create a new ChatParticipant object to identify the new client
			ChatParticipant newClient = new ChatParticipant(connectionSocket);
			
			// Give the client a default name and store the name/client combo
			// in nameToParitipant
			assignClientDefaultName(newClient);
		
			// Create a new thread to handle communication with then new client
			Thread clientThread = new ConnectedClientThread(newClient);
			
			// Here we go!
			clientThread.start();
		}
	}
	
	/**
	 * Generate a name that is not current in use by any other client.
	 * It will have the form "DefaultName_<integer>" where the integer
	 * is the current size of the nameToParticipant hash table.
	 * If that name is already reserved, increment the integer until
	 * a free name combo is found.
	 * The nameToParticipant HashTable will then be updated to account for the new name.
	 * @param client The ChatParticipant who will have a name assigned
	 */
	private void assignClientDefaultName(ChatParticipant client)
	{
		criticalServerDataLock.lock();
		try
		{
			int nameNumber = nameToParticipant.size();
			
			// Iterate until we find a valid name number
			while (nameToParticipant.containsKey("DefaultName_" + nameNumber))
			{
				++nameNumber;
			}
			
			String newName = "DefaultName_" + nameNumber;
			
			// Update their corresponding ChatParticipant with the new name
			client.setName(newName);
			
			// Update the nameToParticipant table to have an entry for the new name
			// that corresponds to this client's ChatParticipant
			nameToParticipant.put(newName, client);
		}
		finally
		{
			criticalServerDataLock.unlock();
		}
	}
	
	/**
	 * Set the server IP and hostname member variables.
	 */
	@SuppressWarnings("static-access")
	private void setServerIpAndHostname()
	{
		try 
		{
			 serverIpAddr = welcomeSocket.getInetAddress().getLocalHost().getHostAddress();
			 serverHostname = welcomeSocket.getInetAddress().getLocalHost().getHostName();
		}
		catch (UnknownHostException err)
		{
			
			String otherMsg = "Error determining this host's IP and hostname. This is definitely not a good sign, but the"
					+ "server will continue execution";
			ServerClientCommon.printExceptionMsgToConsole(otherMsg, err);
		}
	}
	
	/**
	 * Initializes the ServerSocket member, and therefore creates
	 * a port for this server to have clients connect to.
	 * The port number is specified by the serverPortNumber member
	 */
	private void initWelcomeSocket()
	{
		try
		{
			welcomeSocket = new ServerSocket(serverPortNumber);
		}
		catch (IOException err)
		{
			String otherMsg = "Unable to create the welcome socket on port: " + serverPortNumber + ". This is a critical failure, and the"
					+ " server will now exit. Try a different port number.";
			ServerClientCommon.printExceptionMsgToConsole(otherMsg, err);
			System.exit(-1);
		} 
	}
	
	/**
	 * Creates a connection socket for a client trying to connect to to the server.
	 * This function will block the server until a client tries to connect.
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
			String otherMsg = "Error in creating a connection socket for an incoming client.";
			ServerClientCommon.printExceptionMsgToConsole(otherMsg, err);
		}
		return connectionSocket;
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
		///////////////////
		//  MEMBERS      //
		///////////////////
		
		/**
		 * The ChatParticipant that identifies this connection.
		 */
		private ChatParticipant client;
		
		/**
		 * A BufferedReader which the client writes into and this process (the server)
		 * reads from.
		 */
		private BufferedReader inFromClient;
		
		/**
		 * A DataOutputStream which this process (the server) writes into
		 * in order to send the client data.
		 */
		private DataOutputStream outToClient;
		
		///////////////////
		// FUNCTIONS     //
		///////////////////
		
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
			inFromClient = ServerClientCommon.getSocketBufferedReader(client.getSocket(), null);
			outToClient =  ServerClientCommon.getSocketDataOutputStream(client.getSocket(), null);
		}
		
		/**
		 * This function is executed when the thread is started. It handles talking to the client,
		 * which includes reading from it and writing to it.
		 */
		public void run()
		{
			// Has the form <IP>:<Port>
			String clientIpAndSocket = client.getSocket().getRemoteSocketAddress().toString().substring(1);
			
			clientConnectionInitialization(clientIpAndSocket);
			
			// Loop until the client wants to exit
			boolean shouldContinue = true;
			while(shouldContinue)
			{
				// Block on a new message from the client
				String newMessage = ServerClientCommon.readFromSocket(inFromClient, null);

				// If the message is null, cease the connection to this client.
				if (newMessage == null)
				{
					shouldContinue = false;
					continue;
				}
				
				// Now that we're sure the message isn't null, have the server log it
				System.out.println("SVR LOG :" + clientIpAndSocket + "(" + client.getName() + "): " + newMessage);
				
				// An exit control message will result in handleControlMessage() returning false,
				// which will result in disconnect.
				if (isControlMessage(newMessage))
				{
					shouldContinue = handleControlMessage(newMessage);
					continue;
				}
				// If the client is in listen mode, echo received messages.
				// If it's not in listen mode (and therefore is connected 
				// to another client), pass the message to the other client.
				else if (!client.isInListenMode())
				{
					sendMessageToThisClientsPeer(newMessage, false);						
				}
				else
				{
					echoMessageToClient(newMessage);
				}
			}
			
			clientDisconnectProcess(clientIpAndSocket);
		}
		
		/**
		 * Handles run()'s actions post-loop. Specifically, this terminates the connection to the client
		 * and its peer (if necessary), removes this client's data from the hash tables, closes the socket
		 * to the client, and logs that the client has left
		 * @param clientIpAndSocket The IP and Port of the client's socket in the form IP:Socket
		 */
		private void clientDisconnectProcess(String clientIpAndSocket)
		{
			// If the client was in the middle of chatting when they decided to leave,
			// let their partner know
			if (!client.isInListenMode())
			{
				terminateConnectionBetweenThisClientAndItsPeer();
			}
			
			criticalServerDataLock.lock();
			try
			{
				// At this point, the server is disconnecting from the client.
				// Remove its name/instance combination from the nameToParticipant hashtable
				nameToParticipant.remove(client.getName());
				
				// Remove the client and its thread from the participantToThread hash table
				participantToThread.remove(client);
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
			
			// Close the socket that connects the server and client
			// The server will always be the one to initiate a close
			// (provided the client didn't exit abnormally)
			ServerClientCommon.closeSocket(client.getSocket(), null);
			
			// Log that the client and server are disconnected
			System.out.println("SVR LOG: " + clientIpAndSocket + "(" + client.getName() + ") has left");
		}
		
		
		/**
		 * When run() is called, this function is called to setup the client
		 * connection. It logs a a client has been connected, adds this ChatParticipant
		 * and this thread to the participantToThread HashTable, and initializes 
		 * inFromClient and outToClient.
		 * @param clientIpAndSocket The client's IP and port in the form IP:Port
		 */
		private void clientConnectionInitialization(String clientIpAndSocket)
		{		
			// Print to the server's console indicating that a new client has connected
			System.out.println("SVR LOG: New client thread started with IP Address:Port=" + clientIpAndSocket);
			
			// Lock table access
			criticalServerDataLock.lock();
			try
			{
				// Add this client and thread to the participantToThread Hashtable
				participantToThread.put(client, this);	
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
			
			// Initialize inFromClient and outToClient
			initializeStreamAndReader();
			
			// Introduce yourself to the client
			sendMessageToClient("SVR: Welcome from " + serverIpAddr + "/" + serverHostname + ":" + serverPortNumber);
			sendMessageToClient("SVR: You've been given the default name: " + client.getName());
			
		}
		
		/**
		 * Forwards a message from this client to its peer
		 * @param msgToSend The message
		 * @param isServerMsg true if the message is a server administrative msg. False if it's a passed client message.
		 * 		  Server messages already have "SVR" prepended. Furthermore, server messages are only one line.
		 */
		void sendMessageToThisClientsPeer(String msgToSend, boolean isServerMsg)
		{
			// Get the DataOutputStream for the peer
			DataOutputStream peerDataOutStream = participantToThread.get(client.getPeer()).outToClient;

			// Pass msgToSend to the peer (write to its DataOutputStream)
			String output = isServerMsg ? msgToSend : client.getName() + ": " + msgToSend;
			ServerClientCommon.sendMessageToDataOutputStream(output, peerDataOutStream, null);	
		}
		
		/**
		 * Determine if the parameter is a control message. A control message
		 * starts with the CONTROL_MESSAGE_SPECIFIER string ("C0NTR0L:")
		 * @param msgLine The string in question
		 * @return true if the message is a control message, false otherwise
		 */
		boolean isControlMessage(String msgLine)
		{
			return msgLine.startsWith(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER);
		}
		
		/**
		 * Performs actions based on control messages
		 * @param controlMsgLine The line containing the control message
		 * @return Whether or not the client should continue; true = continue
		 * 		   interacting with the client, false = close connection
		 */
		boolean handleControlMessage(String controlMsgLine)
		{
			// When the client wants to end the connection to the server
			if (controlMsgLine.contains(ServerClientCommon.DISCONNECT_FROM_SERVER))
			{
				sendMessageToClient("SVR: CLOSING CONNECTION. SEE YOU LATER, " + client.getName());
				return false;
			}
			// When the client wants to set their username
			else if (controlMsgLine.contains(ServerClientCommon.SET_USERNAME))
			{
				updateNameControlMsgHandler(controlMsgLine);
			}
			// When the client wants to connect to another client
			else if (controlMsgLine.contains(ServerClientCommon.SET_PEER_NAME))
			{
				setPeerControlMsgHandler(controlMsgLine);
			}
			// When the client wants the server to send it the list of connected client names
			else if (controlMsgLine.contains(ServerClientCommon.GET_LIST_OF_CONNECTED_CLIENTS))
			{
				getListOfConnectedClientsControlMsgHandler();
			}
			// When the client wants to know its own name
			else if (controlMsgLine.contains(ServerClientCommon.GET_MY_NAME))
			{
				sendMessageToClient("SVR: Your name is: \"" + client.getName() + "\"");
			}
			// When the client wants to know the name of its peer
			else if (controlMsgLine.contains(ServerClientCommon.GET_MY_PEERS_NAME))
			{
				getMyPeersNameControlMsgHandler();
			}
			// If none of the control messages above match the message that the client
			// passed (that started with "C0NTR0L:"), tell them that the message was invalid.
			else
			{
				sendMessageToClient("SVR: \"" + controlMsgLine + "\" is not a valid control message");
			}
			
			// Return true, indicating that the client and server are to stay connected
			return true;
		}
		
		/**
		 * When the user requests their peer's name with a
		 * GET_MY_PEERS_NAME message, send them the desired info.
		 */
		private void getMyPeersNameControlMsgHandler()
		{
			criticalServerDataLock.lock();
			try
			{
				if (client.isInListenMode())
				{
					sendMessageToClient("SVR: Your are not connected to another user; your peer's name is: " + ServerClientCommon.LISTENER_SPECIFIER);
				}
				else
				{
					sendMessageToClient("SVR: Your peer's name is: " + client.getPeer().getName());
				}
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
		}
		
		/**
		 * Generate a comma separated list of names of clients connected to this server.
		 * Then, send that list to the requesting client.
		 */
		void getListOfConnectedClientsControlMsgHandler()
		{
			String commaSeparatedClientNames = "SVR: Clients connected to the server: ";
			// Don't synchronize this; it's not really a big deal if one client's
			// name isn't sent because of a concurrent read/write. With many clients,
			// locking on this iteration would hurt performance.
			for (String clientName : nameToParticipant.keySet())
			{
				commaSeparatedClientNames += clientName + ", ";
			}
			// Remove the trailing comma upon sending
			sendMessageToClient(commaSeparatedClientNames.substring(0, commaSeparatedClientNames.length()-2));
		}
		
		/**
		 * This handles setting a new peer for the client.
		 * There are quite a number of possible scenarios which can occur
		 * when the client wants to connect to another client,
		 * which are documented in the source below.
		 * @param controlMsgLine The raw control message containing the new peer's name (ex: "C0NTR0L:CONNECT TO PEER WITH NAME=BEN")
		 */
		void setPeerControlMsgHandler(String controlMsgLine)
		{
			// Synchronize this entire thread to prevent issues with multiple threads
			// trying to connect to the same client.
			criticalServerDataLock.lock();
			try
			{
				// Parse the control message for the new peer's name. The name is every character following the "=" sign in the control message
				String newPeerName = controlMsgLine.substring(controlMsgLine.indexOf(ServerClientCommon.SET_PEER_NAME) + ServerClientCommon.SET_PEER_NAME.length());
				
				// If the client wants to change their peer to their current peer, tell them that they're already connected
				if (!client.isInListenMode() && newPeerName.equals(client.getPeer().getName()))
				{
					sendMessageToClient("SVR: You're already chatting with \"" + newPeerName + "\".");
					return;
				}
				// If the client wants to disconnect from their current partner and become a listener given that they're
				// currently connected to another client.
				else if (newPeerName.equals(ServerClientCommon.LISTENER_SPECIFIER) && !client.isInListenMode())
				{
					terminateConnectionBetweenThisClientAndItsPeer();
					sendMessageToClient("SVR: Disconnected with \"" + client.getName() + "\". You are now in listen mode.");
					return;
				}
				// If the client wants to disconnect from their current partner and become a listener given that they're NOT
				// currently connected to another client.
				else if(newPeerName.equals(ServerClientCommon.LISTENER_SPECIFIER) && client.isInListenMode())
				{
					sendMessageToClient("SVR: You are now in listen mode.");
					return;
				}
				
				// If the desired peer isn't currently connected to the server AND the calling client
				// currently is chatting with another user.
				// In this case, tell the client that they can't connect to the desired user
				// and then disconnect them from their current chat.
				if (!nameToParticipant.containsKey(newPeerName) && !client.isInListenMode())
				{
					sendMessageToClient("SVR: The desired client, \"" + newPeerName + "\" is not connected to the server. Try again later." +
							"You are now being disconnected from \"" + client.getPeer().getName() + "\"");
					terminateConnectionBetweenThisClientAndItsPeer();
					return;
				}
				// If the desired peer doesn't exist and the client currently isn't connected to anyone
				else if (!nameToParticipant.containsKey(newPeerName) && client.isInListenMode())
				{
					sendMessageToClient("SVR: The desired client, \"" + newPeerName + "\" is not connected to the server. Try again later.");
					return;
				}
				
				// Given the above checks, there must be a ChatParticipant corresponding
				// to newPeerName connected to the server.
				ChatParticipant desiredPeer = nameToParticipant.get(newPeerName);

				// If the both the desired peer and the caller are connected to other clients, alert the caller and have the caller disconnect
				// from its current peer.
				if (!desiredPeer.isInListenMode() && !client.isInListenMode())
				{
					sendMessageToClient("SVR: The desired client, \"" + newPeerName + "\" is chatting with the user \"" +
											desiredPeer.getPeer().getName() + "\". Try again later." +
											"You are now being disconnected from: \"" + client.getPeer().getName() + "\"");
					terminateConnectionBetweenThisClientAndItsPeer();
				}
				// If the desired peer is connected to someone but the caller is not. The caller will not be able to connect to the desired peer.
				else if (!desiredPeer.isInListenMode() && client.isInListenMode())
				{
					sendMessageToClient("SVR: The desired client, \"" + newPeerName + "\" is chatting with the user \"" + desiredPeer.getPeer().getName() + "\". Try again later.");
				}
				// If the caller is connected to another client, but the desired peer is not, the caller will have to end its chat
				// with its current client and then connect to the new desired client.
				else if (desiredPeer.isInListenMode() && !client.isInListenMode())
				{
					sendMessageToClient("SVR: You are now being disconnected from: \"" + client.getPeer().getName() + "\"");
					terminateConnectionBetweenThisClientAndItsPeer();
					connectToOtherClient(desiredPeer);
				}
				// Neither the caller nor the desired peer are currently connected, so connect the two.
				else
				{
					connectToOtherClient(desiredPeer);
				}	
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
		}
		
		/**
		 * Connects this client to the client specified by the parameter
		 * @param newPeer The client to connect the caller to
		 */
		void connectToOtherClient(ChatParticipant newPeer)
		{
			criticalServerDataLock.lock();
			try
			{
				// Update this client's references to the new peer
				// and inform them that they have been connected
				client.setPeer(newPeer);
				client.setIsInListenMode(false);
				sendMessageToClient("SVR: You are now connected with \"" + newPeer.getName() + "\"");

				// Update the new peer's references to the client
				// and inform them that they have been connected
				newPeer.setPeer(client);
				newPeer.setIsInListenMode(false);
				sendMessageToThisClientsPeer("SVR: You are now connected with \"" + client.getName() + "\"", true);
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
		}
		
		/**
		 * Called in the event that this thread's client is ending communication
		 * with its current peer.
		 */
		void terminateConnectionBetweenThisClientAndItsPeer()
		{
			criticalServerDataLock.lock();
			try
			{
				// Alert the client's peer of the termination
				sendMessageToThisClientsPeer("SVR: User \"" + client.getName() + "\" has exited the chat. You are now in listen mode.", true);
				
				// Physically break the connection by nulling this client's peer's peer member
				// Also, put the peer into listening mode
				client.getPeer().setIsInListenMode(true);
				client.getPeer().setPeer(null);
				
				// Now, have the calling client disconnect by nulling its peer
				// Also, put the caller into listening mode
				client.setIsInListenMode(true);
				client.setPeer(null);
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
		}
		
		/**
		 * The handler for the SET_USERNAME ("SET MY NAME=") control message.
		 * This function reserves and sets the new name if it isn't currently held, and then
		 * un-reserves the client's current name. It then informs the client of the change.
		 * If the name is already in use, tell the client they can't reserve it.
		 * Note that if this client is talking to another client, the connection
		 * will not be broken, but rather the peer will be informed of the change.
		 * Whitespace only names are not allowed, and whitespace of a name is removed
		 * from the beginning and end of the string. In otherwords " Ben " = "Ben"
		 * @param controlMsgLine The raw input message which contains the control strings
		 */
		void updateNameControlMsgHandler(String controlMsgLine)
		{
			// Parse the control message for the new name. The name is every character following the "=" sign in the control message
			// Use .trim() to remove leading or trailing whitespace
			String newName = controlMsgLine.substring(controlMsgLine.indexOf(ServerClientCommon.SET_USERNAME) + ServerClientCommon.SET_USERNAME.length()).trim();
			
			// If the client is trying to set its new name to its current name, inform them.
			if (newName.equals(client.getName()))
			{
				sendMessageToClient("SVR: The username \"" + newName + "\" is your current username.");
				return;
			}
			// If their desired name is reserved
			if (Arrays.asList(ServerClientCommon.RESERVED_NAMES).contains(newName))
			{
				sendMessageToClient("SVR: The username \"" + newName + "\" is reserved. Pick another");
				return;
			}
			// If the desired name is all whitespace, tell the client that it's a no-go
			if (newName.equals(""))
			{
				sendMessageToClient("SVR: Whitespace-only usernames are not permitted. Pick another");
				return;
			}
			
			criticalServerDataLock.lock();
			try
			{
				// If the desired name isn't currently reserved, then allow the client to reserve it.
				if (!nameToParticipant.containsKey(newName))
				{
					// Remove their current name and ChatParticipant from the namesToParticipant hash table
					nameToParticipant.remove(client.getName());
					
					// Update their corresponding ChatParticipant with the new name
					client.setName(newName);
					
					// Update the nameToParticipant table to have an entry for the new name
					// that corresponds to this client's ChatParticipant
					nameToParticipant.put(newName, client);
					
					// Tell them that the new name has been set
					sendMessageToClient("SVR: Your username has been set to \"" + newName + "\"");
					
					// If the client is chatting with someone else, let them know of the name change,
					// but don't disconnect from them
					if (!client.isInListenMode())
					{
						sendMessageToThisClientsPeer("SVR: Your peer has changed their name to: \"" + client.getName() + "\".", true);
					}
				}
				// If the name is current in use by someone else, tell the client they can't change take it.
				else
				{
					sendMessageToClient("SVR: The username \"" + newName + "\" is already in use. Choose another.");
				}
			}
			finally
			{
				criticalServerDataLock.unlock();
			}
		}
		
		/**
		 * Send a message to the client directly whom this thread corresponds to.
		 * @param msgToSend The message to send
		 */
		void sendMessageToClient(String msgToSend)
		{
			ServerClientCommon.sendMessageToDataOutputStream(msgToSend, outToClient, null);
		}

		/**
		 * This echos a message from the client back to the client.
		 * It is called when the client sends non-control messages to the server
		 * as a listener
		 * @param msgToSend The message to send
		 */
		void echoMessageToClient(String msgToSend)
		{
			ServerClientCommon.sendMessageToDataOutputStream("LISTENER_MODE_ECHO: " + msgToSend, outToClient, null);	
		}
	}
}