package ca.afroman.packet.legacy;

import ca.afroman.entity.api.Entity;
import ca.afroman.packet.PacketType;

@Deprecated
public class PacketAddLevelEntity extends Packet
{
	private Entity entity;
	
	/**
	 * Designed to be sent from the <b>server</b> to the <b>client</b>.
	 * <p>
	 * Adds an entity to a ClientLevel.
	 * 
	 * @param level the level to add the entity to.
	 * @param the object type. <b>WARNING: </b> only expecting a TILE or ENTITY.
	 */
	public PacketAddLevelEntity(Entity entity)
	{
		super(PacketType.ADD_LEVEL_ENTITY, true);
		this.entity = entity;
	}
	
	@Override
	public byte[] getData()
	{ // (type, leveltype, assetType, x, y, width, height, hitboxes)
		return (type.ordinal() + "," + id + Packet.SEPARATOR + entity.getLevel().getType().ordinal() + "," + entity.getAssetType() + "," + entity.getX() + "," + entity.getY() + "," + (entity.hasHitbox() ? "," + entity.hitboxesAsSaveable() : "")).getBytes();
	}
}