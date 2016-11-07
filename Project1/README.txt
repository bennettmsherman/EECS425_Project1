Bennett Sherman, bms113
EECS325 Project 1, Fall 2016

Hello! First off, I did the extra credit/425 project and therefore utilized a client GUI (which should actually make your testing significantly easier).
Also note that in the "doc" folder of my submission there are javadocs for each class, each function, and each member variable. This might help you
sift through the code.

This document is organized into several sections:
1.) Code Organization
2.) How to use
3.) Command syntax
4.) Assumptions/Other


1.) Code Organization
- I have a total of 6 separate .java files that make up this assignment.
	1.) ChatClient.java - This file contains all of the NON GUI code for the client.
		In order to comply with the 425 assignment requirements, this client is in fact multi-threaded.
		When it is executed, two threads are spawned off of the main thread: 1.) a thread which reads user
		input from the console/GUI and sends messages to the server and 2.) a thread which reads data
		sent from the server to the client. The actual threads are "ConnectionClientThreads" which is
		a private subclass within ChatClient.java. All of the GUI-client functionality is handled through
		hooks inside the ChatClient.java class.
	2.) ChatClientGui.java - This file is what you will use as the client. It is a GUI which allows
		for user input as well as shows messages from the server (and therefore from a chat partner).
		Thanks Window Builder Pro! The ChatClientGui has a main "message history" window, which shows
		old messages that the client has typed (starting with "You:"), administrative messages read from
		the server (starting with "SRV: ") and messages from a connected client (starting with "<Peer name>: ").
		There is also text box at the bottom of the window that you will use to enter messages and commands
		(more about that later). Messages entered into this box are sent by pressing the "Enter" key while
		the box is selected, or by configuring the delimiter key to another value. Note that in order to prevent
		"interleaving," no new messages, be it from the server or the client's peer, will be shown if the message
		entry box is non-blank.
		***When you first open the client if you didn't enter the server information into the initial prompt,
		you need to connect to the server by pressing the "Commands" menu, then "Connect To Server."
		In the popup, you can enter the IP and hostname, but I have autoconfigured them, so
		you should just need to click OK (on clicking OK, a ChatClient thread is instantiated and started).
		Once this step is done, you can either use the commands specified in section 3, command syntax, 
		to talk to the server and communicate with the client, or you can use the select commands from the
		"Command" menu dropdown. All of the supported commands are in this menu.
	3.) ChatParticipant.java - For each thread of the server that handles communication with a client,
		each client is uniquely identified by a ChatParticipant object. A ChatParticipant contains
		the socket that the server uses to connect to the client, the client's peer (if connected to one),
		its name and its listen mode state.
	4.) chatd.java - Although the real server class is in ChatServer.java, as per the spec, you will
		be executing my server using "chatd port <num>." That's exactly what chatd.java handles; you
		execute chatd by specifying the port number and it will create a new ChatServer instance and
		start the server.
	5.) ServerClientCommon.java - This class contains constants and static members that are common
		to both the server and client. It is final and cannot be instantiated (private constructor).
		Common functionality includes interacting with sockets, streams, and readers, as well as
		error message handling. All of the commands supported by the server are stored in this file
		as static final Strings.
	6.) ChatServer.java - This class contains the implementation for my chat server. It is obviously
		multithreaded; for each client, a new ConnectedClientThread, a private inner class of ChatServer,
		is executed. This inner class handles all communication with the client it corresponds to.
		The server keeps track of the clients and names via two hashtable members, which link
		names to ChatParticipant instances and ChatParticipants to ConnectedClientThreads. All
		accesses to common data, which includes the nameToParticipant HashTable, the participantToThread
		HashTable, and the both the peer and listener mode members of each ChatParticipant synchronized
		using a ReentrantLock. Note that the GUI/Client doesn't store much state data;
		name, and peer of a client are all preserved on the server.
		
2.) How to use
	1.) First thing's first - start up the server. To do this, first compile chatd ("javac chatd.java") and then execute it
		with "java chatd port 50048." Both commands must be run from within the directory containing the source.
	2.) Execute at least one client - compile ChatClientGui.java ("javac ChatClientGui.java"), then run the client GUI
		with "java ChatClientGui". The client should open up.
	3.) With the client GUI open, connect to the server. The first prompt will allow you to do this. If you click cancel or the
		connection fails, you can connect by clicking "Commands" then "Connect To Server". The eecslinab1 server and my assigned
		port are preloaded. Click OK to initiate the connection. Make sure that you don't have any text in the message-entry box
		or you won't see the confirmation message (in order to prevent interleaving. Note that the default delimiter is the Enter key.
	4.) Once the connection completes, you'll see several messages:
		"Connecting to hostname, port: eecslinab1.engineering.cwru.edu:50048"
		"SVR: Welcome from 129.22.156.193/eecslinab1.ENGINEERING.CWRU.Edu:50048"
		"SVR: You've been given the default name: DefaultName_0" <- the default name varies with the number of users.
		All users are given a default name in the form of DefaultName_<a number determined primarily from the current
		number of users>.
		Note that all messages starting with "SVR: " are administrative messages from the server
	5.) At this point, the client is in listen mode. Any messages sent to the server that aren't control messages
		will be echoed back to the client, with the preceding username "LISTENER_MODE_ECHO:". You'll see that the
		messages that you submitted start with "You:"
	6.) From here, you can use any of the commands described below in the syntax section OR, the FAR EASIER approach
		is to click different commands from the commands menu. Any command from this menu will autogenerate the
		proper control message and enter it into the message field. Just click your delimiting key to send the
		message once it's ready to go. Note that control messages need to be their own message, so when selecting
		a control message from the menu, your current message is deleted. Also, click "OK" in the command message
		boxes, not Enter, as that conflicts with the delimiter.
	7)  Once you've connected to a peer ("Connect To Peer" option and enter their name), you can chat with them
		(provided, of course, that they're online and not chatting with someone else).
		Their username will be shown to the left of each received message. You can click "Disconnect from Peer"
		to enter listener mode, or you can click "Disconnect from Server" to disconnect from both the peer
		and server. Other commands are described below.
	8.) The delimiter: The default delimiter is the Enter key. The delimiting key can be changed
		by selecting "Set Delimiter" from the command menu, and will be described below in the command section.
		When the delimiter is pressed, the client's message will be sent to the server.
		
3.) Command syntax
	0.) General: All commands MUST start with the "C0NTR0L:". Any messages that start with "C0NTR0L:" will be interpreted
		as commands. If the message following "C0NTR0L:" is not one of the commands specified below, the server will
		inform the client that the message is invalid.
		-Furthermore, for any of the commands specified below, ONLY one command is allowed per message. For example,
		 the client cannot both set their name and connect to a peer with one message; two separate messages are required.
		-The message syntax is "C0NTR0L:<Command>=<Input>", where the equals sign and <Input> are only required for
		 setter commands. There is no space between "C0NTR0L:" and the command. For setter commands, all text
		 following the equals is considered to be part of the input for that command.
		-Each command submission to the server results in an acknowledgement/feedback of some sort.
		-Note that each of these commands is documented and specified in ServerClientCommon.java
		-Lastly, as mentioned above, each of these commands can be pre-entered by using the commands
		 menu in the GUI with the exception of "What's my Delimiter?"
		 -Command messages are parsed by ChatServer.handleControlMessage()
	COMMANDS:
	1.) Setting your username
		- Command: "SET MY NAME=<New Name>"
		- Ex:"C0NTR0L:SET MY NAME=BEN"
		- The server will respond and let you know if the name is already in use. If it is not in use,
		  it will tell you that the you now have the specified name. All cases can be found in
		  ChatServer.updateNameControlMsgHandler(). Invalid/reserved usernames are specified in 
		  ServerClientCommon.RESERVED_NAMES. Whitespace-only or blank names are not permitted and
		  the leading and trailing whitespace of a requsted name are not included in the name. By this,
		  I mean that " BEN " == "BEN".
	2.) Connect to a peer
		- Command: "CONNECT TO PEER WITH NAME=<Peer name>"
		- Special syntax: If <Peer name> is "Listener", you will be disconnected from your current peer if you
		  currently are connected to a peer
		- Ex:"C0NTR0L:CONNECT TO PEER WITH NAME=BEN"
		- This will connect you to "BEN" if he is online and available. If he is either offline or not available
		  and you are currently chatting with someone else, your chat session with your current peer will end. If
		  "Listener" is the name, your chat will also end (provided you are chatting with someone else). In the
		  event that you are chatting with someone but the desired peer is free, you will be disconnected with
		  your current peer and will connect to the new peer. The nominal case is that neither you nor your
		  peer are chatting with anyone else and you two will be connected. All cases can be found in
		  ClientServer.setPeerControlMsgHandler().
	3.) Set delimiter
		- Command: "SET DELIMITER=<New delimiter>"
		- This is a special command for the GUI only. When "C0NTR0L:SET DELIMITER=" is entered
		  into the text-entry box, provided no other text is currently in it, the first key
		  pressed after the = sign is your new delimiter. A box will pop up to confirm this.
		  A delimiter is a particular key on your keyboard (not a sequence of keys/a string, as we
		  discussed) which signifies that the message is to be sent. By default, it's Enter, and
		  any key can be set to be used instead. The easiest way to use this is with the item
		  in the Command menu.
	4.) Disconnect from server
		- Command: "DISCONNECT FROM SERVER"
		- Ex: "C0NTROL:DISCONNECT FROM SERVER"
		- Upon receipt of this message, the server will start to shut down its thread corresponding
		  to the calling client. When this happens, the server will cleanup any resources associated
		  with this client, send the client a goodbye message, and log that the client has left.
		  "Freeing resources" entails making the client's name usable again. Note that in normal
		  operation, the server is the first to close the socket between the client and server.
	5.) Get a list of clients currently connected to the server
		- Command: "GET CONNECTED CLIENT NAMES"
		- Ex: "C0NTR0L: GET CONNECTED CLIENT NAMES"
		- The server will send to the client a comma separated list of all of the names of the clients
		  attached to the server.
	6.) Get what the server considers to be your name
		- Command: "GET MY NAME"
		- Ex: "C0NTR0L:GET MY NAME"
		- The server will send to the client the name that it has stored for that client.
	7.) Get the name of the person you're chatting with
		- Command: "GET MY PEER'S NAME"
		- Ex: "C0NTR0L:GET MY PEER'S NAME"
		- The server will return the name of the client that it thinks the caller is
		  connected to. If the client has no other connections, the server will tell
		  the client that it is in listener mode.
	8.) GUI only - What's my delimiter?
		- Selecting the "What's my delmimter" option from the command menu will
		  result in a pop up telling you the keycode for the delimiter.
	
4.) Assumptions/Other
	
	
	
	
	
	
	
	
	
	
	
		