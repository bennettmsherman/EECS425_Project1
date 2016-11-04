package bms113.eecs325.cwru.edu;

import java.awt.EventQueue;
import static bms113.eecs325.cwru.edu.ServerClientCommon.*;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class ChatClientGui {

	// GUI Members
	private JFrame frame;
	private JTextField newMessageArea;
	private final Action closeConnectionMenuItemAction = new ConnectionCloseMenuItemAction();
	private final Action exitMenuItemAction = new ExitMenuItemAction();
	private final Action connectToServerMenuItemAction = new ConnectToServerMenuItemAction();
	private final Action sendButtonPressAction = new SendButtonPressAction();
	private DefaultListModel<String> messageHistoryListModel = new DefaultListModel<>();
	private JList<String> messageHistoryJList = new JList<>(messageHistoryListModel);
	private String defaultHostname = "eecslinab1.case.edu";
	private int defaultPortNumber = SERVER_PORT;

	// Chat client members
	private int portNumber;
	private String hostname;
	private String newMessage = "";
	
	// Redirect system in
	InputStream stdInReplacer = new ByteArrayInputStream(newMessage.getBytes());
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatClientGui window = new ChatClientGui();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public ChatClientGui() {
		initialize();
		System.setIn(stdInReplacer);
		messageHistoryListModel.addElement("To connect, select File->Connect to Server and enter the hostname and IP");
		messageHistoryListModel.addElement("For an explanation on server commands, see Help->Commands");
		newMessageArea.setText("Enter your message/commands here.");
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("EECS325 Chat Client, Bennett Sherman");
		frame.setResizable(false);
		frame.getContentPane().setBackground(Color.WHITE);
		
		newMessageArea = new JTextField();
		newMessageArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER)
				{
					sendButtonPressAction.actionPerformed(null);
				}
			}
		});
		newMessageArea.setToolTipText("Message to be sent");
		newMessageArea.setColumns(10);
		
		JButton sendButton = new JButton("Send");
		sendButton.setAction(sendButtonPressAction);
		sendButton.setToolTipText("Press to send message");
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setToolTipText("");
		GroupLayout groupLayout = new GroupLayout(frame.getContentPane());
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(Alignment.LEADING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(newMessageArea)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addComponent(sendButton)
					.addContainerGap())
				.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(Alignment.TRAILING)
				.addGroup(groupLayout.createSequentialGroup()
					.addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(groupLayout.createParallelGroup(Alignment.BASELINE)
						.addComponent(newMessageArea, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(sendButton)))
		);
		messageHistoryJList.setSelectedIndices(new int[] {-1});
		messageHistoryJList.setVisibleRowCount(10);
		
		scrollPane.setViewportView(messageHistoryJList);
		frame.getContentPane().setLayout(groupLayout);
		frame.setBounds(100, 100, 450, 300);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmConnectToServer = new JMenuItem("Connect to Server");
		mntmConnectToServer.setAction(connectToServerMenuItemAction);
		mnFile.add(mntmConnectToServer);
		
		JMenuItem mntmCloseConnection = new JMenuItem("Close Connection");
		mntmCloseConnection.setAction(closeConnectionMenuItemAction);
		mnFile.add(mntmCloseConnection);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.setAction(exitMenuItemAction);
		mnFile.add(mntmExit);
		
		JMenu mnHelp = new JMenu("Help");
		menuBar.add(mnHelp);
		
		JMenuItem mntmCommands = new JMenuItem("Commands");
		mnHelp.add(mntmCommands);
	}
	
	
	private class ConnectionCloseMenuItemAction extends AbstractAction {
		public ConnectionCloseMenuItemAction() {
			putValue(NAME, "Close Connection");
			putValue(SHORT_DESCRIPTION, "Close the connection to the client");
		}
		public void actionPerformed(ActionEvent e) {
			
		}
	}
	private class ExitMenuItemAction extends AbstractAction {
		public ExitMenuItemAction() {
			putValue(NAME, "Exit");
			putValue(SHORT_DESCRIPTION, "Clicking this exits the program");
		}
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	}
	private class ConnectToServerMenuItemAction extends AbstractAction {
		public ConnectToServerMenuItemAction() {
			putValue(NAME, "Connect to Server");
			putValue(SHORT_DESCRIPTION, "Click this to connect to the server");
		}
		public void actionPerformed(ActionEvent e) {
			JTextField portNumField = new JTextField(""+defaultPortNumber, 10);
			JTextField hostField = new JTextField(defaultHostname, 20);
			JPanel portAndHostPanel = new JPanel();
			portAndHostPanel.add(hostField);
			portAndHostPanel.add(portNumField);
			portAndHostPanel.add(Box.createVerticalStrut(10));
			int result = JOptionPane.showConfirmDialog(null, portAndHostPanel, "Enter the server application's port and hostname", JOptionPane.OK_CANCEL_OPTION);
			if (result == JOptionPane.OK_OPTION)
			{
				hostname = hostField.getText();
				messageHistoryListModel.addElement("Entered hostname: " + hostname);
				portNumber = Integer.parseInt(portNumField.getText());
				messageHistoryListModel.addElement("Entered port: " + portNumber);
			}
			
			ChatClient cc = new ChatClient(hostname, portNumber);
			cc.startClient();
		}
	}
	private class SendButtonPressAction extends AbstractAction {
		public SendButtonPressAction() {
			putValue(NAME, "Send");
			putValue(SHORT_DESCRIPTION, "Send the message");
		}
		public void actionPerformed(ActionEvent e) {
			newMessage = newMessageArea.getText();
			messageHistoryListModel.addElement(newMessage);
			newMessageArea.setText("");
			messageHistoryJList.ensureIndexIsVisible(messageHistoryListModel.size()-1);
		}
	}
}
