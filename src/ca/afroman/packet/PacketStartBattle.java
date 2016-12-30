package ca.afroman.packet;

import java.nio.ByteBuffer;

import ca.afroman.network.IPConnection;
import ca.afroman.util.ByteUtil;

public class PacketStartBattle extends BytePacket
{
	private static final int BUFFER_SIZE = 2 + ByteUtil.INT_BYTE_COUNT;
	
	public PacketStartBattle(int entityFighting, boolean p1, boolean p2, IPConnection... connection)
	{
		super(PacketType.START_BATTLE, true, connection);
		
		ByteBuffer buf = ByteBuffer.allocate(BUFFER_SIZE).put(typeOrd());
		
		buf.put((byte) ((p1 ? 1 : 0) + (p2 ? 2 : 0)));
		buf.putInt(entityFighting);
		System.out.println("P(" + p1 + ", " + p2 + ")");
		
		
		content = buf.array();
	}
}