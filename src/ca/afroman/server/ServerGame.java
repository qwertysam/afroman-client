package ca.afroman.server;

import java.nio.ByteBuffer;

import ca.afroman.client.ExitGameReason;
import ca.afroman.entity.PlayerEntity;
import ca.afroman.game.Game;
import ca.afroman.game.Role;
import ca.afroman.game.SocketManager;
import ca.afroman.inventory.ItemType;
import ca.afroman.level.api.Level;
import ca.afroman.level.api.LevelType;
import ca.afroman.log.ALogType;
import ca.afroman.network.ConnectedPlayer;
import ca.afroman.network.IPConnectedPlayer;
import ca.afroman.network.IPConnection;
import ca.afroman.network.IncomingPacketWrapper;
import ca.afroman.packet.BytePacket;
import ca.afroman.packet.PacketDenyJoin;
import ca.afroman.packet.PacketLoadLevels;
import ca.afroman.packet.PacketPingServerClient;
import ca.afroman.packet.PacketPlayerMoveServerClient;
import ca.afroman.packet.PacketReturnToLobby;
import ca.afroman.packet.PacketSetPlayerLocationServerClient;
import ca.afroman.packet.PacketType;
import ca.afroman.resource.ModulusCounter;
import ca.afroman.resource.Vector2DDouble;
import ca.afroman.util.ByteUtil;
import ca.afroman.util.CommandUtil;
import ca.afroman.util.VersionUtil;

public class ServerGame extends Game
{
	private static ServerGame game = null;
	
	public static ServerGame instance()
	{
		return game;
	}
	
	private static ThreadGroup newDefaultThreadGroupInstance()
	{
		return new ThreadGroup("Server");
	}
	
	private String password;
	private boolean stopServer = false;
	private boolean isCommandLine;
	
	private boolean waitingForPlayersToLoad = false;;
	
	private ModulusCounter updatePing;
	
	private boolean isReturnedToLobby = false;
	
	public ServerGame(boolean commandLine, String ip, String password, String port)
	{
		super(true, newDefaultThreadGroupInstance(), "Game", 60);
		
		if (game == null) game = this;
		
		isCommandLine = commandLine;
		
		if (commandLine && ConsoleListener.instance() == null)
		{
			System.getProperties().list(System.out);
			
			// Starts the console listener
			new ConsoleListener(isServerSide()).startThis();
		}
		
		this.password = password;
		
		updatePing = new ModulusCounter((int) ticksPerSecond * 2);
		
		int valPort = SocketManager.validatedPort(port);
		boolean started = startSocket(ip, valPort);
		
		if (started)
		{
			logger().log(ALogType.DEBUG, "Server connected on " + sockets().getServerConnection().asReadable());
			super.startThis();
		}
		else
		{
			logger().log(ALogType.WARNING, "Failed to connected server on " + ip + ":" + valPort + ", aborting startup");
		}
	}
	
	public String getPassword()
	{
		return password;
	}
	
	public boolean isCommandLine()
	{
		return isCommandLine;
	}
	
	/**
	 * Reads a packet's data and acts accordingly.
	 * 
	 * @param data the of the packet to parse
	 * @param connection the connection that the packet is being sent from
	 */
	@Override
	public void parsePacket(IncomingPacketWrapper inPack)
	{
		try
		{
			BytePacket packet = inPack.getPacket();
			PacketType type = packet.getType();
			
			// Finds if this packet was send by a connected player and.or the host
			IPConnectedPlayer sender = sockets().getPlayerConnection(inPack.getIPAddress(), inPack.getPort());
			boolean sentByConnected = sender != null;
			boolean sentByHost = (sentByConnected ? (sender.getID() == 0) : false);
			
			// if (sender != null && sender.getConnection().getTCPSocket() != null)
			// {
			// System.out.println("------------");
			//
			// System.out.println("CON-UPD: " + sender.getConnection().getIPAddress().getHostAddress() + ":" + sender.getConnection().getPort());
			// System.out.println("CON-TCP: " + sender.getConnection().getTCPSocket().getSocket().getInetAddress().getHostAddress() + ":" + sender.getConnection().getTCPSocket().getSocket().getPort());
			//
			// System.out.println("------------");
			// for (ConnectedPlayer c : sockets().getConnectedPlayers())
			// {
			// Socket con = ((IPConnectedPlayer) c).getConnection().getTCPSocket().getSocket();
			//
			// System.out.println("UDP: " + ((IPConnectedPlayer) c).getConnection().getIPAddress().getHostAddress() + ":" + ((IPConnectedPlayer) c).getConnection().getPort());
			// System.out.println("TCP: " + con.getInetAddress().getHostAddress() + ":" + con.getPort());
			// }
			// System.out.println("------------");
			// }
			
			if (sentByConnected && type != null)
			{
				switch (type)
				{
					default:
					case INVALID:
					{
						logger().log(ALogType.CRITICAL, "INVALID PACKET: " + sender.getConnection().asReadable());
						
						sockets().removeConnection(sender);
						
						if (sender.getRole() == Role.PLAYER1 || sender.getRole() == Role.PLAYER2)
						{
							logger().log(ALogType.CRITICAL, "Player is vital role (" + sender.getRole() + "), pausing game");
							setIsInGame(false, ExitGameReason.VITAL_PLAYER_CONNECTION_LOST);
						}
						else
						{
							logger().log(ALogType.DEBUG, "Player is not a vital role, continuing with gameplay");
						}
					}
						break;
					case PLAYER_DISCONNECT:
					{
						logger().log(ALogType.DEBUG, "Disconnecting: " + sender.getConnection().asReadable());
						
						sockets().removeConnection(sender);
						
						if (sender.getRole() == Role.PLAYER1 || sender.getRole() == Role.PLAYER2)
						{
							logger().log(ALogType.CRITICAL, "Player is vital role (" + sender.getRole() + "), pausing game");
							setIsInGame(false, ExitGameReason.VITAL_PLAYER_DISCONNECT);
						}
						else
						{
							logger().log(ALogType.DEBUG, "Player is not a vital role, continuing with gameplay");
						}
					}
						break;
					case TEST_PING:
						if (sender.isPendingPingUpdate())
						{
							sender.updatePing(System.currentTimeMillis());
							// System.out.println("Ping[" + sender.getConnection().asReadable() + "]: " + sender.getPing());
						}
						else
						{
							logger().log(ALogType.WARNING, "Received ping response from a client that isn't pending a ping update: " + sender.getConnection().asReadable());
						}
						break;
					case LOAD_LEVELS:
					{
						boolean sendingLevels = packet.getContent().get() == 1;
						
						if (sendingLevels) // TODO Use as a request for level to be sent?
						{
							
						}
						else // Finished loading levels
						{
							sender.setIsLoadingLevels(false);
							
							logger().log(ALogType.DEBUG, "Finished loading levels: " + sender.getConnection().asReadable());
							
							waitingForPlayersToLoad = false;
							for (ConnectedPlayer con : sockets().getConnectedPlayers())
							{
								if (con.isLoadingLevels())
								{
									waitingForPlayersToLoad = true;
									break;
								}
							}
							
							if (!waitingForPlayersToLoad)
							{
								logger().log(ALogType.DEBUG, "Finished loading levels for all players");
								sockets().sender().sendPacketToAllClients(new PacketLoadLevels(false));
							}
						}
					}
						break;
					case SETROLE:
					{
						if (sentByHost)
						{
							ByteBuffer buf = packet.getContent();
							
							ConnectedPlayer player = sockets().getPlayerConnection(buf.getShort());
							
							// The player who is currently holding that role
							Role newRole = Role.fromOrdinal(buf.get());
							ConnectedPlayer currentPlayerWithRole = sockets().getPlayerConnection(newRole);
							
							if (currentPlayerWithRole != null)
							{
								currentPlayerWithRole.setRole(Role.SPECTATOR);
							}
							
							// As long as their role isn't trying to be set to the same thing, give their previous role to the next spectator
							if (player.getRole() != newRole)
							{
								switch (player.getRole())
								{
									default:
									case SPECTATOR:
										player.setRole(newRole);
										break;
									case PLAYER1:
										// If the player already had a critical role, set it to the next spectator
										ConnectedPlayer newForRole1 = sockets().getPlayerConnection(Role.SPECTATOR);
										if (newForRole1 != null) newForRole1.setRole(Role.PLAYER1);
										break;
									case PLAYER2:
										ConnectedPlayer newForRole2 = sockets().getPlayerConnection(Role.SPECTATOR);
										if (newForRole2 != null) newForRole2.setRole(Role.PLAYER2);
										break;
								}
								
								player.setRole(newRole);
								sockets().updateClientsPlayerList();
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "A non-host user was trying to change the roles: " + sender.getConnection().asReadable());
						}
					}
						break;
					case START_SERVER:
						if (sentByHost)
						{
							byte x = packet.getContent().get();
							
							// 1 if true, 0 if false
							setIsInGame(x == 1);
						}
						else
						{
							logger().log(ALogType.WARNING, "A non-host user was trying to start the server: " + sender.getConnection().asReadable());
						}
						break;
					case PLAYER_MOVE:
					{
						Role role = sender.getRole();
						if (role != Role.SPECTATOR)
						{
							PlayerEntity player = getPlayer(role);
							if (player != null)
							{
								byte x = packet.getContent().get();
								byte y = packet.getContent().get();
								
								player.autoMove(x, y);
								
								sockets().sender().sendPacketToAllClients(new PacketPlayerMoveServerClient(role, x, y), sender.getConnection());
							}
							else
							{
								logger().log(ALogType.WARNING, "No player with role " + role + " to move: " + sender.getConnection().asReadable());
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "A spectator is sending movement packets: " + sender.getConnection().asReadable());
						}
					}
						break;
					case SET_PLAYER_POSITION:
					{
						Role role = sender.getRole();
						if (role != Role.SPECTATOR)
						{
							PlayerEntity player = getPlayer(role);
							if (player != null)
							{
								double x = packet.getContent().getDouble();
								double y = packet.getContent().getDouble();
								
								Vector2DDouble pos = new Vector2DDouble(x, y);
								
								// If the player is within a range, allow the change, else set the player's position
								if (!player.getPosition().isDistanceGreaterThan(pos, 10D))
								{
									player.setPosition(pos, false);
									
									sockets().sender().sendPacketToAllClients(new PacketSetPlayerLocationServerClient(role, pos, true), sender.getConnection());
								}
								else // Else force it back
								{
									sockets().sender().sendPacketToAllClients(new PacketSetPlayerLocationServerClient(role, player.getPosition(), true));
								}
							}
							else
							{
								logger().log(ALogType.WARNING, "No player with role " + role + " to set the position of: " + sender.getConnection().asReadable());
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "A spectator is sending set player position packets: " + sender.getConnection().asReadable());
						}
					}
						break;
					case PLAYER_DROP_ITEM:
					{
						PlayerEntity pe = getPlayer(sender.getRole());
						
						if (pe != null)
						{
							ByteBuffer buf = packet.getContent();
							
							byte itemOrd = buf.get();
							ItemType item = ItemType.fromOrdinal(itemOrd);
							
							if (item != null)
							{
								Vector2DDouble pos = new Vector2DDouble(buf.getDouble(), buf.getDouble());
								
								// If the distance between the server's position for the player and the position that the client
								// is telling the server is greater than 10, ignore the client because they're being dinguses
								if (pe.getPosition().isDistanceGreaterThan(pos, 10D))
								{
									pos = pe.getPosition();
								}
								
								pe.getInventory().removeItem(item, pos, true);
							}
							else
							{
								logger().log(ALogType.WARNING, "No ItemType with ordinal " + itemOrd);
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "No PlayerEntity with role " + sender.getRole());
						}
					}
						break;
					case PLAYER_INTERACT:
					{
						PlayerEntity pe = getPlayer(sender.getRole());
						
						if (pe != null)
						{
							if (pe.getLevel() != null)
							{
								Level level = pe.getLevel();
								
								Vector2DDouble newPos = new Vector2DDouble(packet.getContent().getDouble(), packet.getContent().getDouble());
								
								// If the distance between the server's position for the player and the position that the client
								// is telling the server is greater than 10, ignore the client because they're being dinguses
								if (pe.getPosition().isDistanceGreaterThan(newPos, 10D))
								{
									level.tryInteract(pe);
								}
								else
								{
									// Else, move the player to where they were when they interacted by the client's point of view,
									// then test for interaction, then move back
									Vector2DDouble oldPos = pe.getPosition().clone();
									pe.setPosition(newPos, false);
									level.tryInteract(pe);
									pe.setPosition(oldPos, false);
								}
							}
							else
							{
								logger().log(ALogType.WARNING, "Player is not in a level");
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "No PlayerEntity with role " + sender.getRole());
						}
					}
						break;
					case COMMAND:
					{
						ByteBuffer buf = packet.getContent();
						
						ConsoleCommand command = ConsoleCommand.fromOrdinal(buf.getInt());
						String[] params = new String[buf.get()];
						
						for (int i = 0; i < params.length; i++)
						{
							params[i] = new String(ByteUtil.extractBytes(buf, Byte.MAX_VALUE, Byte.MIN_VALUE, Byte.MAX_VALUE, Byte.MIN_VALUE));
						}
						
						CommandUtil.issueCommand(command, params, sentByHost);
					}
						break;
				}
			}
			else if (type == PacketType.REQUEST_CONNECTION)
			{
				IPConnection connection = new IPConnection(inPack.getIPAddress(), inPack.getPort(), null);
				
				int version = packet.getContent().getInt();
				
				// Checks that the client's game version is not above or below this version
				if (version > VersionUtil.SERVER_TEST_VERSION)
				{
					PacketDenyJoin passPacket = new PacketDenyJoin(DenyJoinReason.OLD_SERVER, connection);
					sockets().sender().sendPacket(passPacket);
					return;
				}
				
				if (version < VersionUtil.SERVER_TEST_VERSION)
				{
					PacketDenyJoin passPacket = new PacketDenyJoin(DenyJoinReason.OLD_CLIENT, connection);
					sockets().sender().sendPacket(passPacket);
					return;
				}
				
				// Checks if there's space for the user on the server
				if (sockets().getConnectedPlayers().size() >= Game.MAX_PLAYERS)
				{
					PacketDenyJoin passPacket = new PacketDenyJoin(DenyJoinReason.FULL_SERVER, connection);
					sockets().sender().sendPacket(passPacket);
					return;
				}
				
				// Extrapolates the provided username
				byte[] nameBytes = ByteUtil.extractBytes(packet.getContent(), Byte.MIN_VALUE, Byte.MAX_VALUE);
				String name = new String(nameBytes).trim();
				
				// Checks that there's no duplicated usernames
				if (sockets().getPlayerConnection(name) != null)
				{
					PacketDenyJoin passPacket = new PacketDenyJoin(DenyJoinReason.DUPLICATE_USERNAME, connection);
					sockets().sender().sendPacket(passPacket);
					return;
				}
				
				// If there's a password
				if (!password.equals(""))
				{
					// Extrapolates the provided password
					byte[] passBytes = ByteUtil.extractBytes(packet.getContent(), Byte.MIN_VALUE, Byte.MAX_VALUE);
					String pass = new String(passBytes).trim();
					
					// If got the correct password, allow the player to join
					if (pass.equals(password))
					{
						sockets().addConnection(connection, name);
					}
					// If got the wrong password, let the client know
					else
					{
						PacketDenyJoin passPacket = new PacketDenyJoin(DenyJoinReason.NEED_PASSWORD, connection);
						sockets().sender().sendPacket(passPacket);
					}
				}
				else // Allow the player to join
				{
					sockets().addConnection(connection, name);
				}
			}
		}
		catch (
		
		Exception e)
		{
			logger().log(ALogType.IMPORTANT, "Exception upon packet parsing", e);
		}
	}
	
	@Override
	public void render()
	{
		// Is never used because this is server side
	}
	
	@Override
	public void setIsInGame(boolean isInGame)
	{
		setIsInGame(isInGame, null);
	}
	
	private void setIsInGame(boolean isInGame, ExitGameReason reason)
	{
		super.setIsInGame(isInGame);
		
		if (reason != null && !isInGame) // Pause game
		{
			// TODO pause game
			isReturnedToLobby = true;
			
			sockets().sender().sendPacketToAllClients(new PacketReturnToLobby(reason));
		}
		else if (!isInGame) // Stop game
		{
			stopThis();
		}
		else if (isReturnedToLobby) // Resume game
		{
			// TODO Resume game
			isReturnedToLobby = false;
		}
		else if (isInGame) // Start Game
		{
			sockets().sender().sendPacketToAllClients(new PacketLoadLevels(true));
			
			loadLevels();
			
			players.clear();
			players.add(new PlayerEntity(true, Role.PLAYER1, new Vector2DDouble(80.0, 80.0)));
			players.add(new PlayerEntity(true, Role.PLAYER2, new Vector2DDouble(112.0, 80.0)));
			
			for (int i = 0; i < players.size(); i++)
			{
				PlayerEntity player = players.get(i);
				player.addToLevel(getLevel(LevelType.MAIN));// TODO make the save files specify this
				player.setPosition(player.getPosition());
			}
		}
	}
	
	@Override
	public void stopThis()
	{
		stopServer = true;
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		// Does this so that when a packet is sent telling the server to stop, it will not cause a concurrentmodificationexception
		if (stopServer)
		{
			game = null;
			
			super.stopThis();
		}
		
		if (updatePing.isAtInterval())
		{
			int p1Ping = PacketPingServerClient.NONE;
			int p2Ping = PacketPingServerClient.NONE;
			
			for (ConnectedPlayer p : sockets().getConnectedPlayers())
			{
				if (p.getRole() == Role.PLAYER1)
				{
					p1Ping = p.getPing();
				}
				else if (p.getRole() == Role.PLAYER2)
				{
					p2Ping = p.getPing();
				}
			}
			
			for (ConnectedPlayer p : sockets().getConnectedPlayers())
			{
				p.setPingTestTime(System.currentTimeMillis());
				
				PacketPingServerClient pingPacket = new PacketPingServerClient(p.getPing(), p1Ping, p2Ping, ((IPConnectedPlayer) p).getConnection());
				sockets().sender().sendPacket(pingPacket);
			}
		}
		
		if (isInGame() && !waitingForPlayersToLoad)
		{
			if (getLevels() != null)
			{
				for (Level level : getLevels())
				{
					level.tick();
				}
			}
		}
	}
}
