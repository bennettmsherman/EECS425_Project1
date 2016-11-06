/**
 * This class is used to start the server as per the requirements.
 * To start the server, execute "java chatd port <port num>"
 */
public class chatd {

	public static void main(String[] args) {
		// args[0] is the string "port"
		// args[1] is the port number
		
		if (args.length != 2)
		{
			System.err.println("Invalid arguments specified. The program can only be started with \"java chatd port <port num, 50048 for me>\"");
			System.exit(-1);
		}
		
		// Create a ChatServer instance with the port number in arg[1]
		ChatServer chatServer = new ChatServer(Integer.parseInt(args[1]));
		
		// Start the server
		chatServer.startServer();
	}
}
