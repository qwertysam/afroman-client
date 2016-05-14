package ca.afroman.packet;

import ca.afroman.entity.api.Entity;

public class PacketUpdateEntityLocation extends Packet
{
	private Entity entity;
	
	/**
	 * Designed to be sent from the <b>server</b> to the <b>client</b>.
	 * <p>
	 * Sets the location of an Entity.
	 * 
	 * @param the entity. <b>WARNING: </b> only expecting an ENTITY, not a TILE or a PLAYER.
	 */
	public PacketUpdateEntityLocation(Entity entity)
	{
		super(PacketType.SET_ENTITY_LOCATION);
		this.entity = entity;
	}
	
	@Override
	public byte[] getData()
	{ // (type, leveltype, assetType, x, y, width, height, hitboxes)
		return (type.ordinal() + Packet.SEPARATOR + entity.getLevel().getType().ordinal() + "," + entity.getID() + "," + entity.getX() + "," + entity.getY()).getBytes();
	}
}
