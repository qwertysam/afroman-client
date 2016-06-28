package ca.afroman.legacy.packet;

import ca.afroman.packet.BytePacket;

public abstract class Packet
{
	/** The pattern that separates the PacketType ordinal from a the content. */
	public static final String SEPARATOR = ":;";
	
	protected int id;
	protected PacketType type;
	
	public Packet(PacketType type, boolean mustSend)
	{
		this.type = type;
		id = mustSend ? BytePacket.getIDCounter().getNext() : -1;
	}
	
	/**
	 * Gets the data from this Packet in a sendable form.
	 * 
	 * @return the data
	 */
	public abstract byte[] getData();
	
	/**
	 * Gets the packet's content as a String without the initial
	 * separator that separates the PacketType ordinal in the data.
	 * 
	 * @param data the raw data
	 * @return the data as a String
	 */
	public static String readContent(byte[] data)
	{
		String message = "";
		try
		{
			message = new String(data).trim();
			String[] split = message.split(SEPARATOR);
			message = "";
			
			for (int i = 1; i < split.length; i++)
			{
				if (split[i] != null)
				{
					message += split[i];
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
		return message;
	}
	
	public boolean mustSend()
	{
		return id != -1;
	}
	
	/**
	 * Gets the packet's PacketType from the raw data.
	 * 
	 * @param data the raw data
	 * @return the PacketType
	 */
	public static PacketType readType(byte[] data)
	{
		int ordinal = 0;
		
		try
		{
			String message = new String(data).trim();
			String[] split = message.split(SEPARATOR);
			ordinal = Integer.parseInt(split[0].split(",")[0]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return PacketType.INVALID;
		}
		
		if (ordinal >= 0 && ordinal < PacketType.values().length)
		{
			return PacketType.fromOrdinal(ordinal);
		}
		else
		{
			return PacketType.INVALID;
		}
	}
	
	/**
	 * Gets the packet's id from the raw data.
	 * 
	 * @param data the raw data
	 * @return the id
	 */
	public static int readID(byte[] data)
	{
		try
		{
			String message = new String(data).trim();
			String[] split = message.split(SEPARATOR);
			return Integer.parseInt(split[0].split(",")[1]);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	/**
	 * @return this player's ID.
	 */
	public int getID()
	{
		return id;
	}
	
	public PacketType getType()
	{
		return type;
	}
}