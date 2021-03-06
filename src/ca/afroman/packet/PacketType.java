package ca.afroman.packet;

import ca.afroman.level.api.BuildMode;

public enum PacketType
{
	INVALID,
	
	REQUEST_CONNECTION,
	TEST_PING,
	COMMAND,
	DENY_JOIN,
	START_TCP,
	ASSIGN_CLIENTID,
	UPDATE_PLAYERLIST,
	SETROLE,
	PLAYER_DISCONNECT,
	START_SERVER,
	RETURN_TO_LOBBY,
	
	LOAD_LEVELS,
	
	ACTIVATE_TRIGGER,
	
	START_BATTLE,
	BATTLE_UPDATE_TURN_ROLE,
	BATTLE_UPDATE_TURN_ORD,
	BATTLE_EXECUTE_ID,
	BATTLE_EXECUTE_ID_HEALTH,
	BATTLE_SELECT_ENTITY,
	BATTLE_ADD_PLAYER,
	
	SET_ENTITY_POSITION,
	SET_ENTITY_LEVEL,
	SET_PLAYER_POSITION,
	SET_PLAYER_LEVEL,
	
	ENTITY_MOVE,
	
	PLAYER_MOVE,
	PLAYER_INTERACT,
	PLAYER_PICKUP_ITEM,
	PLAYER_DROP_ITEM;
	
	public static PacketType fromOrdinal(int ordinal)
	{
		if (ordinal < 0 || ordinal > values().length - 1) return null;
		
		return values()[ordinal];
	}
	
	/**
	 * Gets the enum value of this prior to this value.
	 * <p>
	 * If no value is found before this value, the value at index
	 * <i>n - 1</i> will be returned, where <i>n</i> is the total
	 * number of values for this enumerator.
	 * 
	 * @return the next item on the list of this enumerator.
	 */
	public PacketType getLast()
	{
		int newOrdinal = ordinal() - 1;
		
		if (newOrdinal < 0)
		{
			return fromOrdinal(BuildMode.values().length - 1);
		}
		else
		{
			return fromOrdinal(newOrdinal);
		}
	}
	
	/**
	 * Gets the enum value of this past this value.
	 * <p>
	 * If no value is found past this value, the value at
	 * index 0 will be returned.
	 * 
	 * @return the next item on the list of this enumerator.
	 */
	public PacketType getNext()
	{
		int newOrdinal = ordinal() + 1;
		
		if (newOrdinal > values().length - 1)
		{
			return fromOrdinal(0);
		}
		else
		{
			return fromOrdinal(newOrdinal);
		}
	}
}
