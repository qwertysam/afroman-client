package ca.afroman.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import ca.afroman.network.IPConnection;
import ca.afroman.packet.Packet;
import ca.afroman.thread.DynamicTickThread;

public class ServerSocketPacketPusher extends DynamicTickThread
{
	private HashMap<IPConnection, List<Packet>> sendingPackets; // The packets that are still trying to be sent.
	
	/**
	 * Constantly sends required packets to any client until they confirm that they've received them.
	 */
	public ServerSocketPacketPusher()
	{
		super(2);
		
		sendingPackets = new HashMap<IPConnection, List<Packet>>();
	}
	
	@Override
	public void tick()
	{
		// For each connection
		for (Entry<IPConnection, List<Packet>> entry : getPacketQueue().entrySet())
		{
			synchronized (this)
			{
				// For each packet queued to send to the connection
				for (Packet pack : entry.getValue())
				{
					ServerGame.instance().socket().sendPacket(pack, entry.getKey());
				}
			}
		}
	}
	
	@Override
	public void onPause()
	{
		
	}
	
	@Override
	public void onUnpause()
	{
		
	}
	
	@Override
	public void onStop()
	{
		sendingPackets.clear();
	}
	
	public List<Packet> getPacketsSendingTo(IPConnection connection)
	{
		for (Entry<IPConnection, List<Packet>> entry : getPacketQueue().entrySet())
		{
			if (entry.getKey().equals(connection)) return entry.getValue();
		}
		
		return null;
	}
	
	public void addConnection(IPConnection connection)
	{
		getPacketQueue().put(connection, new ArrayList<Packet>());
	}
	
	public void removeConnection(IPConnection connection)
	{
		getPacketQueue().remove(connection);
	}
	
	/**
	 * @param connection the connection that this was sending the packet to
	 * @param id the ID of the packet being sent
	 */
	public void removePacketFromQueue(IPConnection connection, int id)
	{
		Packet toRemove = null;
		
		List<Packet> sent = getPacketsSendingTo(connection);
		
		if (sent != null)
		{
			// Find the packet that the server is saying it recieved.
			for (Packet pack : sent)
			{
				if (pack.getID() == id)
				{
					toRemove = pack;
					break;
				}
			}
			
			// Remove that packet from the queue
			if (toRemove != null)
			{
				sent.remove(toRemove);
			}
		}
		else
		{
			System.out.println("[SERVER] [PUSHER] Cannot find the connection to remove the packet from.");
		}
	}
	
	public void addPacketSendingTo(IPConnection connection, Packet packet)
	{
		List<Packet> packs = getPacketsSendingTo(connection);
		
		// Don't add it if it's just looping through and trying to add it again
		if (!packs.contains(packet))
		{
			packs.add(packet);
		}
	}
	
	public synchronized HashMap<IPConnection, List<Packet>> getPacketQueue()
	{
		return sendingPackets;
	}
}
