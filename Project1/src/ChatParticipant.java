/**
 * This class is the representation of a client
 * connected to the ChatServer
 * @author Bennett Sherman, bms113
 */
import java.net.Socket;

public class ChatParticipant {
	
	///////////////////
	// CLASS MEMBERS //
	///////////////////
	/**
	 * The socket the server is connected to the client through.
	 */
	private final Socket socket;
	
	/**
	 * The peer of this client. This is the other
	 * client that this one is talking with, if not null.
	 */
	private ChatParticipant peer = null;
	
	/**
	 * The name of this client.
	 */
	private String name = "";
	
	/**
	 * If this client is a listener or not.
	 */
	private boolean isInListenMode = true;
	
	/////////////////////
	// CLASS FUNCTIONS //
	/////////////////////
	/**
	 * Constructor.
	 * @param socket The socket that the server is connected to the client through.
	 */
	ChatParticipant(Socket socket)
	{
		this.socket = socket;
	}
	
	/**
	 * @param name The name to give to this client
	 */
	void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * @return The name of this client
	 */
	String getName()
	{
		return name;
	}
	
	/**
	 * @param newPeer The client that this one has been connected to
	 */
	void setPeer(ChatParticipant newPeer)
	{
		peer = newPeer;
	}
	
	/**
	 * @return The client that this one is connected to
	 */
	ChatParticipant getPeer()
	{
		return peer;
	}
	
	/**
	 * @return The socket that the server contacts this client through
	 */
	Socket getSocket()
	{
		return socket;
	}
	
	/**
	 * @return true if the client is in listen mode, false otherwise
	 */
	boolean isInListenMode()
	{
		return isInListenMode;
	}
	
	/**
	 * @param newListenState true to put this client into listen mode, false to make it not a listener.
	 */
	void setIsInListenMode(boolean newListenState)
	{
		isInListenMode = newListenState;
	}
}
