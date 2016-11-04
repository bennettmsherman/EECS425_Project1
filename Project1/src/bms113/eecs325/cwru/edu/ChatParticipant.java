package bms113.eecs325.cwru.edu;

import java.net.Socket;

public class ChatParticipant {
	
	private final Socket socket;
	private ChatParticipant peer = null;
	private String name = "";
	private boolean isInListenMode = true;
	
	ChatParticipant(Socket socket)
	{
		this.socket = socket;
	}
	
	void setName(String name)
	{
		this.name = name;
	}
	
	String getName()
	{
		return name;
	}
	
	void setPeer(ChatParticipant newPeer)
	{
		peer = newPeer;
	}
	
	ChatParticipant getPeer()
	{
		return peer;
	}
	
	Socket getSocket()
	{
		return socket;
	}
	
	boolean isInListenMode()
	{
		return isInListenMode;
	}
	
	void setIsInListenMode(boolean newListenState)
	{
		isInListenMode = newListenState;
	}
}
