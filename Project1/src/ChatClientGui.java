/**
 * This class is the GUI interface for the ChatClient class.
 * It interfaces with the chat client class through streams.
 * Most of the code is for graphics.
 * @author Bennett Sherman, bms113
 */
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;

// Disable Serial ID warnings for the Action classes
@SuppressWarnings("serial")
public class ChatClientGui {

	///////////////////////////
	// GUI-RELATED MEMBERS	 //
	///////////////////////////
	private JFrame frame;
	private final Action exitMenuItemAction = new ExitMenuItemAction();
	private final Action connectToServerMenuItemAction = new ConnectToServerMenuItemAction();
	private final Action setUsernameCommandAction = new SetUsernameCommandAction();
	private final Action getConnectedUsersAction = new GetConnectUsersAction();
	private final Action connectToNewUserAction = new ConnectToNewUserAction();
	private final Action disconnectFromPeerAction = new DisconnectFromPeerAction();
	private final Action getMyUsernameAction = new GetMyUsernameAction();
	private final Action genericControlMessageAction = new GenericControlMessageAction();
	private final Action disconnectFromServerAction = new DisconnectFromServerAction();
	private final Action setDelimeterAction = new SetDemiliterAction();
	private final Action getPeerNameAction = new GetPeerNameAction();
	private final Action getDelimiterAction = new GetDelimiterAction();
	/**
	 * The text field that the user enters messages into.
	 */
	private JTextField newMessageArea;
	
	/**
	 * When data is written to this list model, it is displayed in the previous message area.
	 */
	private DefaultListModel<String> messageHistoryListModel = new DefaultListModel<>();
	
	/**
	 * The JList in which previous messages are placed
	 */
	private JList<String> messageHistoryJList = new JList<>(messageHistoryListModel);

	
	///////////////////////////
	// CLIENT-RELATED MEMBERS//
	///////////////////////////
	/**
	 * The server port number entered by the user.
	 */
	private int serverPortNum;
	
	/**
	 * The server hostname entered by the user.
	 */
	private String serverHostname;
	
	/**
	 * Each time a new message is to be sent, it is stored
	 * in this field.
	 */
	private String newMessage = "";
	
	/**
	 * Only one client instance can run per GUI. This is the client's thread.
	 */
	private ChatClient chatClientThread;
	
	/**
	 * The GUI writes messages into this pipe, and the underlying client
	 * reads messages from this pipe and sends them to the server.
	 */
	private PipedOutputStream msgsFromGuiToClientStream;
	
	/**
	 * Strings are printed to this writer and then are passed through the
	 * msgsFromGuiClientStream pipe to be read by the client.
	 */
	private PrintWriter msgsFromGuiToClientWriter;
	
	/**
	 * The keycode of the key that serves as the delimiter.
	 */
	private int keyDelimiterValue = 10;
	
	////////////////////
	// FUNCTIONS	 //
	///////////////////
	
	/**
	 * Launch the application.
	 * @param args Command line arguments
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatClientGui window = new ChatClientGui();
					window.connectToServerMenuItemAction.actionPerformed(null);
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Constructor for the GUI. This does a bunch of graphics initialization
	 * in addition to displaying some introductory strings in the previous message
	 * window and the message submission field.
	 */
	public ChatClientGui() {
		initialize();
		displayTextInHistoryWindow("To connect, select Commands->Connect to Server and enter the hostname and IP");
		displayTextInHistoryWindow("To enter a command, select one from the \"Command\" menu");
		displayTextInHistoryWindow("Enter your message/commands in the text field below or select a command from the Command menu.");
	}

	boolean isTextEntryFieldBlank()
	{
		if (newMessageArea.getText().length() == 0)
		{
			return true;
		}
		else
		{
			return false;
		}
	}
	
	/**
	 * Each time a new key is pressed or released while the text box is selected,
	 * this function is executed. It checks to see if the delimiter has ebeen
	 * pressed or of the user is attempting to set a new delimiter. This also
	 * checks to see if the user has entered an exit message
	 */
	private void setupKeyListenerForNewMsgArea()
	{
	    newMessageArea.addKeyListener(new KeyAdapter() { 
	    	@Override
	    	public void keyTyped(KeyEvent arg0) {
	    		// Check if the user wanted to set a new delimiter
	    		if (newMessageArea.getText().equals(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.SET_DELIMITER))
	    		{
	    			textAreaSetNewDelimiterHandler(arg0);
	    		}
	    		// The delimiter was selected - show your message after the buffered messages.
	    		else if (arg0.getKeyChar() == keyDelimiterValue)
	    		{
	    			// If the user specified the exit command
	    			if (newMessageArea.getText().equals(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.EXIT_APP))
	    			{
	    				exitMenuItemAction.actionPerformed(null);
	    			}
	    			// If they just want to send a normal message
	    			else
	    			{
		    			// Send/show the new message
		    			sendMessageFromTextField();

		    			// Wipe out the message area.
		    			newMessageArea.setText(null);
	    			}
	    		}		
	    	}
	      });
	}
	
	/**
	 * Handles setting a new delimiter from the text box field.
	 * @param arg0 A key event for a keypress within the textbox field.
	 */
	private void textAreaSetNewDelimiterHandler(KeyEvent arg0)
	{
		int selection = JOptionPane.showConfirmDialog(frame, "You have selected key with character code " + (int) arg0.getKeyChar() + " as your delimeter." +
				" Do you want to use this as your new delimiter?",
					"Delimiter Confirmation", JOptionPane.YES_NO_OPTION);
		if (selection == JOptionPane.YES_OPTION)
		{
		keyDelimiterValue = (int) arg0.getKeyChar();
		}
		// Wipe out the message area.
		newMessageArea.setText("");
	}
	
	/**
	 * Send a message by reading from the input text field.
	 */
	private void sendMessageFromTextField()
	{
		newMessage = newMessageArea.getText();
		displayTextInHistoryWindow("You: " + newMessage);
		newMessageArea.setText("");
		// Write the message to the stream read by the client application. This is what results
		// in the message being sent to the server.
		if (msgsFromGuiToClientWriter != null && chatClientThread != null && chatClientThread.isAlive())
		{
			msgsFromGuiToClientWriter.println(newMessage);	
		}
		else if (chatClientThread == null || !chatClientThread.isAlive())
		{
			displayTextInHistoryWindow("GUI: You're not connected to a server!");
		}
	}
	
	/**
	 * Initialize the contents of the frame. This is all generated by Window Builder Pro.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("EECS325 Chat Client, Bennett Sherman");
		frame.getContentPane().setBackground(Color.WHITE);
		
		newMessageArea = new JTextField();
	    
		setupKeyListenerForNewMsgArea();
		
		newMessageArea.setToolTipText("Message to be sent");
		newMessageArea.setColumns(10);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setToolTipText("");
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)
				.addComponent(newMessageArea, GroupLayout.DEFAULT_SIZE, 624, Short.MAX_VALUE)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 391, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(newMessageArea, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
		);
		messageHistoryJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		messageHistoryJList.setSelectedIndices(new int[] {-1});
		messageHistoryJList.setVisibleRowCount(12);
		
		scrollPane.setViewportView(messageHistoryJList);
		frame.getContentPane().setLayout(groupLayout);
		frame.setBounds(100, 100, 640, 480);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAction(exitMenuItemAction);
		mnFile.add(mntmExit);
		
		JMenu mnCommands = new JMenu("Commands");
		mnCommands.setBackground(Color.WHITE);
		menuBar.add(mnCommands);
		
		JMenuItem mntmConnectToServer = new JMenuItem("Connect to Server");
		mnCommands.add(mntmConnectToServer);
		mntmConnectToServer.setAction(connectToServerMenuItemAction);
		
		JMenuItem mntmDisconnectFromServer = new JMenuItem("Disconnect From Server");
		mntmDisconnectFromServer.setAction(disconnectFromServerAction);
		mnCommands.add(mntmDisconnectFromServer);
		
		JMenuItem mntmNewMenuItem = new JMenuItem("Specify Name");
		mntmNewMenuItem.setAction(setUsernameCommandAction);
		mnCommands.add(mntmNewMenuItem);
		
		JMenuItem mntmConnectToUser = new JMenuItem("Connect to User");
		mntmConnectToUser.setAction(connectToNewUserAction);
		mnCommands.add(mntmConnectToUser);
		
		JMenuItem mntmDisconnectFromPeer = new JMenuItem("Disconnect from Peer");
		mntmDisconnectFromPeer.setAction(disconnectFromPeerAction);
		mnCommands.add(mntmDisconnectFromPeer);
		
		JMenuItem mntmGetPeersName = new JMenuItem("Get Peer's Name");
		mntmGetPeersName.setAction(getPeerNameAction);
		mnCommands.add(mntmGetPeersName);
		
		JMenuItem mntmGetConnectedUsers = new JMenuItem("Get Connected Users");
		mntmGetConnectedUsers.setAction(getConnectedUsersAction);
		mnCommands.add(mntmGetConnectedUsers);
		
		JMenuItem mntmWhatsMyName = new JMenuItem("What's my name?");
		mntmWhatsMyName.setAction(getMyUsernameAction);
		mnCommands.add(mntmWhatsMyName);
		
		JMenuItem mntmGetDelimiter = new JMenuItem("Get Delimiter");
		mntmGetDelimiter.setAction(getDelimiterAction);
		mnCommands.add(mntmGetDelimiter);
		
		JMenuItem mntmSetDelimiter = new JMenuItem("Set Delimiter");
		mnCommands.add(mntmSetDelimiter);
		mntmSetDelimiter.setAction(setDelimeterAction);
		
		JMenuItem mntmGenericControlMessage = new JMenuItem("Generic Control Message");
		mntmGenericControlMessage.setAction(genericControlMessageAction);
		mnCommands.add(mntmGenericControlMessage);
	}
	
	/**
	 * Initialize the stream and writer that interface with the underlying
	 * ChatClient thread.
	 */
	private void initializeMsgOutputStreamAndWriter()
	{
		msgsFromGuiToClientStream = new PipedOutputStream();
		msgsFromGuiToClientWriter = new PrintWriter(new OutputStreamWriter(msgsFromGuiToClientStream), true);
	}
	
	/**
	 * Creates a new ChatClient, which represents an instance of a client connected
	 * to a ChatServer. Only one instance is allowed per GUI, which is enforced
	 * by this function.
	 */
	private void createClient()
	{
		// Don't allow multiple connections to the server
		if (chatClientThread == null)
		{
			initializeMsgOutputStreamAndWriter();
			chatClientThread = new ChatClient(serverHostname, serverPortNum, this);
			chatClientThread.start();			
		}
		else if (!chatClientThread.isAlive())
		{
			initializeMsgOutputStreamAndWriter();
			chatClientThread = new ChatClient(serverHostname, serverPortNum, this);
			chatClientThread.start();
		}
		else
		{
			displayErrorMsg("Whoops! Multiple connections to the server from one client are not allowed");
		}

	}
	
	/**
	 * Closing the stream and writer is needed to allow for a client to reconnect to the server
	 * if it has already disconnected. In addition, the server monitor thread will
	 * call close the stream to wakeup the user input thread to have it close
	 * in the even that there was an expected disconnect.
	 */
	void closeGuiMsgStreamAndWriter()
	{
		try
		{
			msgsFromGuiToClientStream.close();
			msgsFromGuiToClientWriter.close();
		}
		catch (IOException err)
		{
			displayExceptionMsg("Unable to close the GUI's OutputStream/Writer", err);
		}
	}
	
	/**
	 * Write a new line to the previous messages window. Then, scroll
	 * down to show this new message.
	 * @param msg The message to be displayed.
	 */
	void displayTextInHistoryWindow(String msg)
	{
		// Blank messages don't get added properly, so add a space in the case of a blank msg.
//		if (msg.equals(""))
//		{
//			msg = " ";
//		}
		messageHistoryListModel.addElement(msg);
		
		// Used to scroll the window downwards to show the new message.
		messageHistoryJList.ensureIndexIsVisible(messageHistoryListModel.size()-1); 
	}
	
	/**
	 * Getter for the PipedOutputStream that the GUI writes into.
	 * The client connects to the other side of the stream by retrieving
	 * and connecting to this PipedOutputStream object.
	 * @return The GUI's msgsFromGuiToClientStream PipedOutputStream
	 */
	PipedOutputStream getMessagesFromGuiStream()
	{
		return msgsFromGuiToClientStream;
	}
	
	/**
	 * Display a JOptionPane error message window which shows an error string
	 * in addition to the stack trace of the exception parameter.
	 * @param otherMsg A message to show before the stack trace
	 * @param except An exception whose stack trace is to be displayed.
	 */
	void displayExceptionMsg(String otherMsg, Exception except)
	{
		StringWriter sw = new StringWriter();
		except.printStackTrace(new PrintWriter(sw));
		JOptionPane.showMessageDialog(null, otherMsg + "\n" + "EXCEPTION MESSAGE:\n" + sw.toString(), "ERROR!", JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Show a JOptionPane error message dialog which contains the specified message.
	 * @param errMsg The message to display
	 */
	void displayErrorMsg(String errMsg)
	{
		JOptionPane.showMessageDialog(null, errMsg, "ERROR!", JOptionPane.ERROR_MESSAGE);
	}
	
	////////////////////////////
	// GUI ACTION HANDLERS	 //
	///////////////////////////
	/**
	 * Handles action events for when the "Set Username" command
	 * is selected from the command menu.
	 */
	private class SetUsernameCommandAction extends AbstractAction {
		public SetUsernameCommandAction() {
			putValue(NAME, "Set Username");
			putValue(SHORT_DESCRIPTION, "Click to set username");
		}
		public void actionPerformed(ActionEvent e) {
			String desiredName = JOptionPane.showInputDialog(null, "Enter your desired username", "new name");
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.SET_USERNAME + desiredName);
		}
	}
	
	/**
	 * Handles action events for when the "Get Connected Users" command
	 * is selected from the command menu.
	 */
	private class GetConnectUsersAction extends AbstractAction {
		public GetConnectUsersAction() {
			putValue(NAME, "Get Connected Users");
			putValue(SHORT_DESCRIPTION, "Get the users connected to the server");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.GET_LIST_OF_CONNECTED_CLIENTS);
		}
	}
	
	/**
	 * Handles action events for when the "Connect To Peer" command
	 * is selected from the command menu.
	 */
	private class ConnectToNewUserAction extends AbstractAction {
		public ConnectToNewUserAction() {
			putValue(NAME, "Connect To Peer");
			putValue(SHORT_DESCRIPTION, "Connect to another client");
		}
		public void actionPerformed(ActionEvent e) {
			String desiredPeerName = JOptionPane.showInputDialog(null, "Enter the name of the client you want to talk to", "client name");
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.SET_PEER_NAME + desiredPeerName);
		}
	}
	
	/**
	 * Handles action events for when the "Disconnect From PEer" command
	 * is selected from the command menu.
	 */
	private class DisconnectFromPeerAction extends AbstractAction {
		public DisconnectFromPeerAction() {
			putValue(NAME, "Disconnect From Peer");
			putValue(SHORT_DESCRIPTION, "End the chat with your peer");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.SET_PEER_NAME + ServerClientCommon.LISTENER_SPECIFIER);
		}
	}
	
	/**
	 * Handles action events for when the "What's my Username?" command
	 * is selected from the command menu.
	 */
	private class GetMyUsernameAction extends AbstractAction {
		public GetMyUsernameAction() {
			putValue(NAME, "What's My Username?");
			putValue(SHORT_DESCRIPTION, "Get your username from the server");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.GET_MY_NAME);

		}
	}
	
	/**
	 * Handles action events for when the "Generic Control Message" command
	 * is selected from the command menu.
	 */
	private class GenericControlMessageAction extends AbstractAction {
		public GenericControlMessageAction() {
			putValue(NAME, "Generic Control Message");
			putValue(SHORT_DESCRIPTION, "Put a generic control message specifier in the new message window");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER);
		}
	}
	
	/**
	 * Handles action events for when the "Disconnect From Server" command
	 * is selected from the command menu.
	 */
	private class DisconnectFromServerAction extends AbstractAction {
		public DisconnectFromServerAction() {
			putValue(NAME, "Disconnect From Server");
			putValue(SHORT_DESCRIPTION, "Disconnect from the server");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.DISCONNECT_FROM_SERVER);
		}
	}
	
	/**
	 * Handles action events for when the "Set Delimiter" command
	 * is selected from the command menu.
	 */
	private class SetDemiliterAction extends AbstractAction {
		public SetDemiliterAction() {
			putValue(NAME, "Set Delimiter");
			putValue(SHORT_DESCRIPTION, "Set the message delimiter");
		}
		public void actionPerformed(ActionEvent e) {
			String delimeterMsg = "If OK is clicked, the delimeter set command will be entered into the new message window. To set the delimiter,"
									+ " click on the new message box and select the key you want. A message will confirm your selection.";
						
			int selection = JOptionPane.showConfirmDialog(frame, delimeterMsg, "Do you want to set the delimiter?", JOptionPane.YES_NO_OPTION);
			
			if (selection == JOptionPane.YES_OPTION)
			{
				newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.SET_DELIMITER);		
			}
		}
	}
	
	/**
	 * Handles action events for when the "Get My Peer's Name" command
	 * is selected from the command menu.
	 */
	private class GetPeerNameAction extends AbstractAction {
		public GetPeerNameAction() {
			putValue(NAME, "Get My Peer's Name");
			putValue(SHORT_DESCRIPTION, "Query the server for the name of your peer");
		}
		public void actionPerformed(ActionEvent e) {
			newMessageArea.setText(ServerClientCommon.CONTROL_MESSAGE_SPECIFIER + ServerClientCommon.GET_MY_PEERS_NAME);
		}
	}
	
	/**
	 * Handles action events for when the "Exit" command
	 * is selected from the File menu.
	 */
	private class ExitMenuItemAction extends AbstractAction {
		public ExitMenuItemAction() {
			putValue(NAME, "Exit");
			putValue(SHORT_DESCRIPTION, "Clicking this exits the program");
		}
		public void actionPerformed(ActionEvent e) {
			// Close the socket if it's currently open
			if (chatClientThread != null)
			{
				Socket clientSocket = chatClientThread.getSocket();
				if (clientSocket != null)
				{
					ServerClientCommon.closeSocket(chatClientThread.getSocket(), null);				
				}
			}
			System.exit(0);
		}
	}
	
	/**
	 * Handles action events for when the "Connect To Server" command
	 * is selected from the command menu. This spawns a new ChatClient thread.
	 */
	private class ConnectToServerMenuItemAction extends AbstractAction {
		public ConnectToServerMenuItemAction() {
			putValue(NAME, "Connect To Server");
			putValue(SHORT_DESCRIPTION, "Click this to connect to the server");
		}
		/**
		 * Open window which requests the server port and hostname.
		 */
		public void  actionPerformed(ActionEvent e) {
			JTextField portNumField = new JTextField(""+ ServerClientCommon.DEFAULT_SERVER_PORT, 10);
			JTextField hostField = new JTextField(ServerClientCommon.DEFAULT_SERVER_HOSTNAME, 20);
			JPanel portAndHostPanel = new JPanel();
			portAndHostPanel.add(hostField);
			portAndHostPanel.add(portNumField);
			portAndHostPanel.add(Box.createVerticalStrut(10));
			int result = JOptionPane.showConfirmDialog(null, portAndHostPanel, "Enter the server application's port and hostname", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION)
			{
				// Set the hostname and port and show them in the message window.
				serverHostname = hostField.getText();
				try
				{
					serverPortNum = Integer.parseInt(portNumField.getText());
				}
				catch (NumberFormatException err)
				{
					displayErrorMsg("Error parsing the port number. Specify a valid port in Commands->Connect To Server");
					return;
				}
				if (chatClientThread == null || !chatClientThread.isAlive())
				{
					displayTextInHistoryWindow("GUI: Connecting to hostname, port: " + serverHostname + ":" + serverPortNum);
				}
				// Create the new ChatClient thread
				createClient();
			}
		}
	}
	
	/**
	 * Handles action events for when the "Get Delimiter" command
	 * is selected from the command menu.
	 */
	private class GetDelimiterAction extends AbstractAction {
		public GetDelimiterAction() {
			putValue(NAME, "What's My Delimiter?");
			putValue(SHORT_DESCRIPTION, "Query the server for the delimiter associated with this client");
		}
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(frame, "Your delimeter has the character code " + keyDelimiterValue, "Delimiter Value", JOptionPane.OK_OPTION);
		}
	}
}
