/**
 * This class is the chat client executable, excluding GUI,
 * for the EECS325 chat client project. I am implementing
 * the EECS425 extra credit.
 * @author Bennett Sherman, bms113
 */

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ChatClient extends Thread{
	
	///////////////////
	// CLASS MEMBERS //
	///////////////////
	
	/**
	 * The port number of the server's socket
	 */
	private int portNumber;
	
	/**
	 * The hostname of the server
	 */
	private String serverHostname;
	
	/**
	 * A TCP socket used to connect to the server.
	 */
	private Socket socket;

	/**
	 * This will be used to specify thread type. Since we'll have one thread
	 * monitoring messages and the other monitoring user IO, this enum
	 * will be used to specify which is which.
	 */
	private enum ThreadPurpose { USER_INPUT, SERVER_MONITOR };
	
	/**
	 * User input is read from this reader, regardless of it's from
	 * the GUI or command line. 
	 */
	private BufferedReader userInputReader;
	
	/**
	 * The GUI used by this client. Null if in command line interface mode.
	 */
	private ChatClientGui assocGui;
		
	///////////////////
	// FUNCTIONS	 //
	///////////////////
	
	/**
	 * Constructor that provides the server socket's IP address
	 * and port number. It also is used to specify the GUI used by this client.
	 * @param serverHostname Server's hostname
	 * @param portNumber Server socket's port number
	 * @param associatedGui The GUI used by this client.
	 */
	public ChatClient(String serverHostname, int portNumber, ChatClientGui associatedGui)
	{
		this.portNumber = portNumber;
		this.serverHostname = serverHostname;
		this.assocGui = associatedGui;
		
		connectToAndGetReaderForPipedOutputStream(associatedGui.getMessagesFromGuiStream());
	}
	
	/**
	 * Constructor that provides the server socket's IP address
	 * and port number. This constructor is used for terminal mode.
	 * @param serverHostname Server's hostname
	 * @param portNumber Server socket's port number
	 */
	public ChatClient(String serverHostname, int portNumber)
	{
		this.portNumber = portNumber;
		this.serverHostname = serverHostname;
		this.assocGui = null;
	}
	
	/**
	 * This function creates a PipedInputStream and connects it to the PipedOutputStream
	 * parameter. It then uses this input stream to initialize this classes' userInputReader
	 * member. This function is called during construction and is used to link messages
	 * typed by the user in the GUI (pushing data into the PipedOutputStream) to the lower level
	 * of the client (reading out of the PipedOutputStream).
	 * @param stream The GUI's side of the PipedOutputStream
	 */
	private void connectToAndGetReaderForPipedOutputStream(PipedOutputStream stream)
	{
		PipedInputStream inStream = new PipedInputStream();
		// If this operation fails, the client MUST close since it won't be
		// able to read data from the GUI
		try
		{
			inStream.connect(stream);
		} 
		catch (IOException err)
		{
			assocGui.displayExceptionMsg("Error linking client and GUI. Program closing.", err);
			System.exit(-1);
		}
		this.userInputReader = new BufferedReader(new InputStreamReader(inStream));
	}
	
	/**
	 * Starts execution of the client. Doing so will spawn two threads:
	 * 1.) A thread to monitor user input
	 * 2.) A thread to monitor data from the server and output it to the user
	 */
	public void run()
	{
		// If the server is unreachable, kill this thread.
		boolean socketConnectResult = connectToSocket(serverHostname, portNumber);
		if (socketConnectResult == false)
		{
			return;
		}
		
		Thread userInThread = new ConnectedClientThread(ThreadPurpose.USER_INPUT, this);
		Thread serverMonitorThread = new ConnectedClientThread(ThreadPurpose.SERVER_MONITOR, this);
		
		// Start both threads
		userInThread.start();
		serverMonitorThread.start();
		
		// Wait for both threads to terminate before closing the socket
		try 
		{
			userInThread.join();
			serverMonitorThread.join();
		}
		catch (InterruptedException err)
		{
			displayExceptionMessage("Error waiting for the termination of the user input and server monitoring threads", err);
		}
		
		// Close the socket
		ServerClientCommon.closeSocket(socket, this);
	}
	
	/**
	 * Updates this class's socket member, which includes connecting to the server.
	 * @param hostname The hostname of the server to connect to
	 * @param portNumber The port to connect to
	 * @return true if successful, false otherwise
	 */
	private boolean connectToSocket(String hostname, int portNumber)
	{
		boolean success = false;
		try
		{
			socket = new Socket(hostname, portNumber);
			success = true;
		}
		catch (UnknownHostException e)
		{
			displayErrorMessage("Unable to determine the IP address of the hostname: " + serverHostname + 
								"\nEnter a valid hostname");
		}
		catch (IOException e) {
			displayErrorMessage("Unable to reach " + hostname + ":" + portNumber +
								"\nVerify that the hostname and port are valid");
		}
		return success;
	}
	
	/**
	 * Displays an exception to the GUI or console depending on which
	 * the client is utilizing.
	 * @param msg Extra information to display
	 * @param err An exception whose stack trace will be printed.
	 */
	void displayExceptionMessage(String msg, Exception err)
	{
		if (assocGui == null)
		{
			ServerClientCommon.printExceptionMsgToConsole(msg, err);
		}
		else
		{
			assocGui.displayExceptionMsg(msg, err);
		}
	}
	
	/**
	 * Displays an error message to the GUI or console depending
	 * on which the client is utilizing.
	 * @param msg The message to display.
	 */
	void displayErrorMessage(String msg)
	{
		if (assocGui == null)
		{
			System.err.println(msg);
		}
		else
		{
			assocGui.displayErrorMsg(msg);
		}
	}
	
	/**
	 * If the user is using the GUI, post messages to the message history window.
	 * Otherwise, write to the console's stdout.
	 * @param msg The message to display
	 */
	private void displayMessage(String msg)
	{
		if (assocGui != null)
		{
			assocGui.displayTextInHistoryWindow(msg);
		}
		else
		{
			System.out.println(msg);
		}
	}
	
	/**
	 * @return The socket that this client uses to connect to the server
	 */
	Socket getSocket()
	{
		return socket;
	}
	
	///////////////////
	// THREAD CLASS  //
	///////////////////
	/**
	 * This class represents a thread of execution for the chat client.
	 * During normal operation, two threads are running:
	 * 1.) A thread to monitor user input
	 * 2.) A thread to receive data from the server and display it to the user
	 */
	private class ConnectedClientThread extends Thread
	{
		///////////////////
		// MEMBERS		 //
		///////////////////
		/**
		 * This specifies what this thread is for, either user input or server monitoring
		 */
		private ThreadPurpose threadPurpose;
		
		/**
		 * Data from the server is read through this BufferedReader.
		 */
		private BufferedReader inFromServer = null;
		
		/**
		 * Data is sent to the server through this DataOutputStream.
		 */
		private DataOutputStream outToServer = null;
		
		/**
		 * The ChatClient that created this thread.
		 */
		private ChatClient parent;
		
		///////////////////
		// FUNCTIONS	 //
		///////////////////
		/**
		 * Constructor for this thread.
		 * @param threadPurpose What this thread is intended to do
		 * @param parent The creator of this thread
		 * @see the ChatClient.ThreadPurpose
		 */
		private ConnectedClientThread(ThreadPurpose threadPurpose, ChatClient parent)
		{
			this.parent = parent;
			this.threadPurpose = threadPurpose;
		}
		
		/**
		 * The initial execution point for a ConnectedClientThread.
		 * An if statement determines what this thread is intended
		 * to do, which is either monitor user input or read from the server.
		 */
		public void run()
		{
			initializeStreamAndReader();
			// User input monitoring mode
			if (threadPurpose == ThreadPurpose.USER_INPUT)
			{
				executeUserInputMonitoringActions();
			}
			// Monitor messages from the server, threadPurpose == SERVER_MONITOR
			else
			{
				executeServerMonitoringActions();
			}
		}
		
		/**
		 * When the thread is in user input monitoring mode, this function
		 * executed. It handles reading from the GUI/console as well
		 * as sending data to the server.
		 */
		private void executeUserInputMonitoringActions()
		{
			boolean shouldContinue = true;
			while (shouldContinue)
			{
				String currentLine = readLineFromUser();
				if (currentLine == null)
				{
					shouldContinue = false;
				}
				// If the user wants to exit, stop parsing user input
				// The serverMonitor thread will handle ending the client's session when
				// the server disconnects
				else if (currentLine.startsWith(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER) && currentLine.contains(ServerClientCommon.DISCONNECT_FROM_SERVER))
				{
					// Tell the server the client wants to leave.
					sendMessageToServer(currentLine);
					
					// Stop waiting for user input after this loop run
					shouldContinue = false;		
				}
				else
				{
					sendMessageToServer(currentLine);
				}
			}
		}
		
		/**
		 * When the thread is in server monitoring mode, this
		 * function is executed. It handles reading data from
		 * the server.
		 */
		private void executeServerMonitoringActions()
		{
			boolean shouldContinue = true;
			while (shouldContinue)
			{
				String newMsgFromSocket = ServerClientCommon.readFromSocket(inFromServer, parent);
				// A null message indicates that the connection was broken
				if (newMsgFromSocket == null)
				{
					displayMessage("The connection to the server has broken. Chat ended");
					shouldContinue = false;
					// If the server breaks the connection to the client without the client having requested it,
					// the user input thread is unaware of the fact that the connection has dropped.
					// Close the stream that the client is reading to wake it up and have it exit.
					if (assocGui != null)
					{
						assocGui.closeGuiMsgStreamAndWriter();
					}
					continue;
				}
				displayMessage(newMsgFromSocket);
			}
		}
		
		/**
		 * This function takes in data from the user through the userInputReader
		 * member variable. It blocks when waiting for input.
		 * Good thing this client is mulththreaded!
		 * @return The string read from the user
		 */
		private String readLineFromUser()
		{
			String lineRead = null;
			try
			{
				// Attempt to read data from the user
				lineRead = userInputReader.readLine();
			}
			catch (IOException err)
			{
				displayExceptionMessage("Error reading data entered from the user!" , err);
			}
			return lineRead;
		}
		
		/**
		 * Initializes the inFromServer, outToServer, and userInputReader variables
		 * depending on this thread's intended functionality.
		 * 1.) If this thread is reading user input, outToServer is initialized with a stream used to send
		 * 	   data to the server. In addition, it will initialize the source of user input,
		 * 	   be it the GUI or command line depending on which executable the client is using.
		 * 2.) If this thread is reading from the server, inFromServer is initialized with a BufferedReader
		 * 	   that is used to retrieve data from the server.
		 */
		void initializeStreamAndReader()
		{
			if (threadPurpose == ThreadPurpose.USER_INPUT)
			{
				outToServer =  ServerClientCommon.getSocketDataOutputStream(socket, parent);
				if (userInputReader == null)
				{
					userInputReader = new BufferedReader(new InputStreamReader(System.in));
				}
			}
			else
			{
				inFromServer = ServerClientCommon.getSocketBufferedReader(socket, parent);
			}
		}
		
		/**
		 * A simple wrapper to send data to the server. Writing to a DataOutputStream
		 * is common between the client and server (albeit with different streams),
		 * so the common sendMessageToDataOutputStream is called with the desired
		 * message and DataOutputStream (in this case, the stream to the server).
		 * @param msgToSend The message to send to the server.
		 */
		void sendMessageToServer(String msgToSend)
		{
			ServerClientCommon.sendMessageToDataOutputStream(msgToSend, outToServer, parent);
		}
	}
	
	/**
	 * If the user desires to run this client in command line mode, call the executable
	 * with the hostname as parameter 0 and the port as parameter 1.
	 * @param args Command line arguments
	 */
	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.err.println("You must specifiy the hostname as the first parameter and the port as the second.");
			System.exit(-1);
		}
		String hostname = args[0];
		int portNum = Integer.parseInt(args[1]);
		
		Thread cc = new ChatClient(hostname, portNum);
		cc.run();
	}
}
