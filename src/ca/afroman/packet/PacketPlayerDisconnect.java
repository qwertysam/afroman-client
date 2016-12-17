package ca.afroman.packet;

import ca.afroman.network.IPConnection;

public class PacketPlayerDisconnect extends BytePacket
{
	public PacketPlayerDisconnect(IPConnection... connection)
	{
		super(PacketType.PLAYER_DISCONNECT, false, connection);
		
		content = new byte[] { typeOrd() };
	}
}
