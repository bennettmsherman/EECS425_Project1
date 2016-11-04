/**
 * This class is the chat client executable, excluding GUI,
 * for the EECS325 chat client project. I am implementing
 * the EECS425 extra credit.
 * @author Bennett Sherman, bms113
 */
package bms113.eecs325.cwru.edu;

import static bms113.eecs325.cwru.edu.ServerClientCommon.*;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ChatClient {
	
	///////////////////
	// CLASS MEMBERS //
	///////////////////c
	
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
	 * Constructor that provides the server socket's IP address
	 * and port number
	 * @param serverHostname Server's hostname
	 * @param portNumber Server socket's port number
	 */
	public ChatClient(String serverHostname, int portNumber)
	{
		this.portNumber = portNumber;
		this.serverHostname = serverHostname;
	}
	
	/**
	 * Starts execution of the client.
	 */
	public void startClient()
	{
		connectToSocket(serverHostname, portNumber);
		Thread userInThread = new ConnectedClientThread(ThreadPurpose.USER_INPUT);
		Thread serverInThread = new ConnectedClientThread(ThreadPurpose.SERVER_MONITOR);
		userInThread.start();
		serverInThread.start();
	}
	
	/**
	 * Updates this class's socket member
	 * @param hostname The hostname of the server to connect to
	 * @param portNumber The port to connect to
	 */
	private void connectToSocket(String hostname, int portNumber)
	{
			try
			{
				socket = new Socket(hostname, portNumber);
			} catch (UnknownHostException e) {
				System.err.println("Unable to reach " + hostname + ":" + portNumber);
				System.exit(-1);
			} catch (IOException e) {
				System.err.println("Unable to reach " + hostname + ":" + portNumber);
				System.exit(-1);
			}
	}
	
	private class ConnectedClientThread extends Thread
	{
		private ThreadPurpose threadPurpose;
		private BufferedReader inFromServer = null;
		private DataOutputStream outToServer = null;
		private Scanner stdInReader = null;
		
		private ConnectedClientThread(ThreadPurpose threadPurpose)
		{
			this.threadPurpose = threadPurpose;
		}
		
		public void run()
		{
			initializeStreamAndReader();
			boolean shouldContinue = true;
			// User input monitoring mode
			if (threadPurpose == ThreadPurpose.USER_INPUT)
			{
				while (shouldContinue && stdInReader.hasNext())
				{
					String currentLine = stdInReader.nextLine();
					if (currentLine.startsWith(CONTROL_MESSAGE_SPECIFIER) && currentLine.contains(EXIT_CHAT_CLIENT))
					{
						shouldContinue = false;
						continue;
					}
					sendMessageToServer(currentLine);
				}
			}
			// Monitor messages from the server, threadPurpose == SERVER_MONITOR
			else
			{
				while (shouldContinue)
				{
					String newMsgFromSocket = readFromSocket(inFromServer);
					System.out.println(newMsgFromSocket);
				}
			}
		}
		
		/**
		 * Initializes the inFromServer, outToServer, and stdInReader variables
		 * depending on the value of threadPurpose.
		 */
		void initializeStreamAndReader()
		{
			if (threadPurpose == ThreadPurpose.USER_INPUT)
			{
				outToServer =  getSocketDataOutputStream(socket);
				stdInReader = new Scanner(System.in);
			}
			else
			{
				inFromServer = getSocketBufferedReader(socket);
			}
		}
		
		void sendMessageToServer(String msgToSend)
		{
			sendMessageToDataOutputStream(msgToSend, outToServer);
		}
	}
	
	public static void main(String[] args)
	{
		ChatClient cc = new ChatClient("localhost", SERVER_PORT);
		cc.startClient();
	}
}
