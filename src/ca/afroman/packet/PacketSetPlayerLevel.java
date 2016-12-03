package ca.afroman.packet;

import ca.afroman.game.Role;
import ca.afroman.level.api.LevelType;
import ca.afroman.network.IPConnection;
import ca.afroman.util.ByteUtil;

public class PacketSetPlayerLevel extends BytePacket
{
	private byte[] toSend;
	
	public PacketSetPlayerLevel(Role role, LevelType levelType, IPConnection... connection)
	{
		super(PacketType.SET_PLAYER_LEVEL, true, connection);
		
		byte[] level = ByteUtil.shortAsBytes((byte) levelType.ordinal());
		
		toSend = new byte[] { (byte) role.ordinal(), level[0], level[1] };
	}
	
	@Override
	public byte[] getUniqueData()
	{
		return toSend;
	}
}
