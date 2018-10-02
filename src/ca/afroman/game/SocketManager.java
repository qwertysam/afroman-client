package ca.afroman.game;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PortUnreachableException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ca.afroman.client.ClientGame;
import ca.afroman.gui.GuiClickNotification;
import ca.afroman.gui.GuiJoinServer;
import ca.afroman.gui.GuiMainMenu;
import ca.afroman.interfaces.IDynamicRunning;
import ca.afroman.log.ALogType;
import ca.afroman.log.ALogger;
import ca.afroman.network.ConnectedPlayer;
import ca.afroman.network.IPConnectedPlayer;
import ca.afroman.network.IPConnection;
import ca.afroman.network.IncomingPacketWrapper;
import ca.afroman.network.TCPSocket;
import ca.afroman.option.Options;
import ca.afroman.packet.BytePacket;
import ca.afroman.packet.PacketType;
import ca.afroman.packet.technical.PacketAssignClientID;
import ca.afroman.packet.technical.PacketPingClientServer;
import ca.afroman.packet.technical.PacketServerClientStartTCP;
import ca.afroman.packet.technical.PacketUpdatePlayerList;
import ca.afroman.resource.ServerClientObject;
import ca.afroman.server.ServerGame;
import ca.afroman.thread.DynamicThread;
import ca.afroman.util.IPUtil;

public class SocketManager extends DynamicThread
{
	/**
	 * Returns a usable port. If the provided one is eligible then it will return it, otherwise it will return the default port.
	 * 
	 * @param port
	 * @return
	 */
	public static int validatedPort(int port)
	{
		return (!(port < 0 || port > 0xFFFF)) ? port : Game.DEFAULT_PORT;
	}
	
	/**
	 * Returns a usable port. If the provided one is eligible then it will return it, otherwise it will return the default port.
	 * 
	 * @param port
	 * @return
	 */
	public static int validatedPort(String port)
	{
		if (port.length() > 0)
		{
			try
			{
				return validatedPort(Integer.parseInt(port));
			}
			catch (NumberFormatException e)
			{
				e.printStackTrace();
			}
		}
		
		return Game.DEFAULT_PORT;
	}
	
	private ThreadGroup threadGroup = null;
	
	private Game game;
	
	private List<ConnectedPlayer> playerList;
	private IPConnection serverConnection;
	
	private ServerSocketChannel server;
	private DatagramChannel datagram;
	private Selector selector;
		
	private boolean isStopping;
	
	public SocketManager(Game game)
	{
		super(game.isServerSide(), game.getThread().getThreadGroup(), "SocketManager");
		
		this.game = game;
		
		playerList = new ArrayList<ConnectedPlayer>();
		
		serverConnection = new IPConnection(null, -1, null);
				
		try
		{
			selector = Selector.open();
		}
		catch (IOException ioe)
		{
			game.logger().log(ALogType.CRITICAL, "Instance cannot open selector!", ioe);
		}
	}
	
	/**
	 * Sets up a IPConnectedPlayer for a new connection. Makes the player join the server.
	 * 
	 * @param connection the connection to set up for
	 * @param username the desired username
	 */
	public void addConnection(IPConnection connection, String username)
	{
		if (isServerSide())
		{
			// Gives player a default role based on what critical roles are still required
			Role role = (getPlayerConnection(Role.PLAYER1) == null ? Role.PLAYER1 : (getPlayerConnection(Role.PLAYER2) == null ? Role.PLAYER2 : Role.SPECTATOR));
			
			short id = (short) ConnectedPlayer.getIDCounter().getNext();
			
			IPConnectedPlayer newConnection = new IPConnectedPlayer(connection, id, role, username);
			playerList.add(newConnection);
			
			try
			{
				sendPacket(new PacketServerClientStartTCP(newConnection.getConnection()));
				Socket clientTCP = welcomeSocket().accept();
				TCPSocket tcp = new TCPSocket(clientTCP, isServerSide());
				newConnection.getConnection().setTCPSocket(tcp);
				
				synchronized (tcpSockets)
				{
					TCPReceiver rec = new TCPReceiver(isServerSide(), this, tcp);
					tcpSockets.add(rec);
					rec.startThis();
				}
			}
			catch (IOException e)
			{
				ServerGame.instance().logger().log(ALogType.WARNING, "Failed to accept connection from the welcome socket", e);
			}
		}
		else
		{
			ClientGame.instance().logger().log(ALogType.WARNING, "Client is trying to use addConnection() method in SocketManager");
		}
	}
	
	private void sendPacket(BytePacket BytePacket)
	{
		// copy what PacketSender does here
	}

	public List<ConnectedPlayer> getConnectedPlayers()
	{
		return playerList;
	}
	
	public Game getGame()
	{
		return game;
	}
	
	public IPConnectedPlayer getPlayerConnection(InetAddress address, int port)
	{
		if (isServerSide())
		{
			for (ConnectedPlayer player : playerList)
			{
				if (player instanceof IPConnectedPlayer)
				{
					IPConnectedPlayer ipPlayer = (IPConnectedPlayer) player;
					
					if (ipPlayer.getConnection() != null)
					{
						// If the IP and port equal those that were specified, return the player
						if (IPUtil.equals(address, port, ipPlayer.getConnection())) return ipPlayer;
					}
				}
			}
		}
		else
		{
			ClientGame.instance().logger().log(ALogType.WARNING, "Client is trying to use getPlayerConnection(InetAddress, int) method in SocketManager");
		}
		
		return null;
	}
	
	public ConnectedPlayer getPlayerConnection(int id)
	{
		for (ConnectedPlayer player : getConnectedPlayers())
		{
			if (player.getID() == id) return player;
		}
		
		return null;
	}
	
	public ConnectedPlayer getPlayerConnection(Role role)
	{
		for (ConnectedPlayer player : getConnectedPlayers())
		{
			if (player.getRole() == role) return player;
		}
		
		return null;
	}
	
	public ConnectedPlayer getPlayerConnection(String name)
	{
		for (ConnectedPlayer player : getConnectedPlayers())
		{
			if (player.getUsername().equals(name)) return player;
		}
		
		return null;
	}
	
	public IPConnection getServerConnection()
	{
		return serverConnection;
	}
	
	public boolean hasActiveServerConnection()
	{
		return getServerConnection() != null;
	}
	
	public void initServerTCPConnection()
	{
		if (!isServerSide())
		{
			try
			{
				SocketChannel client = SocketChannel.open(getServerConnection().getAsInet());
				getServerConnection().setSocket(client);
				
				/*
				Socket clientSocket = new Socket(getServerConnection().getIPAddress(), getServerConnection().getPort());
				TCPSocket sock = new TCPSocket(clientSocket, isServerSide());
				getServerConnection().setTCPSocket(sock);
				
				TCPReceiver thread = new TCPReceiver(isServerSide(), this, sock);
				synchronized (tcpSockets)
				{
					tcpSockets.add(thread);
				}
				thread.startThis();
				*/
			}
			catch (UnknownHostException e)
			{
				game.logger().log(ALogType.WARNING, "Unkonwn host while setting up client TCP connection", e);
			}
			catch (IOException e)
			{
				game.logger().log(ALogType.WARNING, "IOException while setting up client TCP connection", e);
			}
		}
		else
		{
			ServerGame.instance().logger().log(ALogType.WARNING, "Server is trying to use initServerTCPConnection() method in SocketManager");
		}
	}
	
	public boolean isStopping()
	{
		return isStopping;
	}
	
	public void removeConnection(IPConnectedPlayer connection)
	{
		if (isServerSide())
		{
			/*
			TCPSocket tcpSock = connection.getConnection().getTCPSocket();
			synchronized (tcpSockets)
			{
				int index = -1;
				
				for (int i = 0; i < tcpSockets.size(); i++)
				{
					TCPReceiver rec = tcpSockets.get(i);
					if (rec.getTCPSocket() == tcpSock)
					{
						index = i;
						break;
					}
				}
				
				if (index != -1)
				{
					TCPReceiver t = tcpSockets.remove(index);
					t.stopThis();
				}
			}
			
			tcpSock.stopThis();
			*/
			
			playerList.remove(connection);
			
			ConnectedPlayer.getIDCounter().reset();
			// shifts everyone's ID
			for (ConnectedPlayer player : getConnectedPlayers())
			{
				if (player instanceof IPConnectedPlayer)
				{
					IPConnectedPlayer cPlayer = (IPConnectedPlayer) player;
					
					cPlayer.setID((short) ConnectedPlayer.getIDCounter().getNext());
					
					sendPacket(new PacketAssignClientID(cPlayer.getID(), cPlayer.getConnection()));
				}
				else
				{
					ServerGame.instance().logger().log(ALogType.CRITICAL, "There shouldn't be a non-IPConnectedPlayer in the server's SocketManager");
				}
			}
			
			updateClientsPlayerList();
		}
		else
		{
			ClientGame.instance().logger().log(ALogType.WARNING, "Client is trying to use removeConnection() method in SocketManager");
		}
	}
	
	public boolean setServerConnection(String serverIpAddress, int port)
	{
		port = validatedPort(port);
		
		serverConnection.setPort(port);
		
		if (serverIpAddress == null)
		{
			serverConnection.setIPAddress(null);
			return false;
		}
		
		InetAddress ip = null;
		
		try
		{
			ip = InetAddress.getByName(serverIpAddress);
		}
		catch (UnknownHostException e)
		{
			game.logger().log(ALogType.CRITICAL, "Couldn't resolve hostname", e);
			
			if (!isServerSide())
			{
				ClientGame.instance().setCurrentScreen(new GuiJoinServer(new GuiMainMenu()));
				new GuiClickNotification(ClientGame.instance().getCurrentScreen(), -1, "UNKNOWN", "HOST");
			}
			return false;
		}
		
		serverConnection.setIPAddress(ip);
		
		if (isServerSide())
		{
			Options.instance().serverPort = "" + port;
			
			try
			{
				server = ServerSocketChannel.open();
				// server.socket().setSoTimeout(15000);// TODO make gui to display that it's waiting?
				server.configureBlocking(false);
				server.bind(new InetSocketAddress(serverConnection.getIPAddress(), serverConnection.getPort()));
				server.register(selector, SelectionKey.OP_ACCEPT);
			}
			catch (IOException e)
			{
				logger().log(ALogType.CRITICAL, "Failed to create server ServerSocket", e);
			}
			
			try
			{
				datagram = DatagramChannel.open();
				datagram.socket().bind(serverConnection.getAsInet());
			}
			catch (IOException ioe)
			{
				logger().log(ALogType.CRITICAL, "Failed to create server DatagramSocket", ioe);
				return false;
			}
		}
		else
		{
			Options.instance().clientPort = "" + port;
			
			try
			{
				datagram = DatagramChannel.open();
				datagram.connect(serverConnection.getAsInet());
			}
			catch (IOException ioe)
			{
				logger().log(ALogType.CRITICAL, "Failed to create client DatagramSocket", ioe);
				return false;
			}
		}
		
		return true;
	}
	
	public DatagramChannel socket()
	{
		return datagram;
	}
	
	@Override
	public void stopThis()
	{
		isStopping = true;
		
		try
		{
			if (server != null) server.close();
			
			if (datagram != null) datagram.close();
			
			if (serverConnection != null && serverConnection.getSocket() != null) serverConnection.getSocket().close();
		}
		catch (IOException ioe)
		{
			logger().log(ALogType.CRITICAL, "Cannot stop SocketManager", ioe);
		}
		
		playerList.clear();
	}
	
	public ThreadGroup threadGroupInstance()
	{
		if (threadGroup == null)
		{
			threadGroup = new ThreadGroup(Game.instance(isServerSide()).getThread().getThreadGroup(), "Socket");
		}
		return threadGroup;
	}
	
	/**
	 * Updates the player list for all the connected clients.
	 * <p>
	 * For server use.
	 */
	public void updateClientsPlayerList()
	{
		if (isServerSide())
		{
			sendPacketToAllClients(new PacketUpdatePlayerList(playerList));
		}
		else
		{
			ClientGame.instance().logger().log(ALogType.WARNING, "Client is trying to use updateClientsPlayerList() method in SocketManager");
		}
	}
	
	/**
	 * For client use.
	 * 
	 * @param players
	 */
	public void updateConnectedPlayers(List<ConnectedPlayer> players)
	{
		if (!isServerSide())
		{
			playerList = players;
			ClientGame.instance().setRole(getPlayerConnection(ClientGame.instance().getID()).getRole());
		}
		else
		{
			ServerGame.instance().logger().log(ALogType.WARNING, "Server is trying to use updateConnectedPlayers() method in SocketManager");
		}
	}
	
	public ServerSocketChannel server()
	{
		return server;
	}

	@Override
	public void onRun()
	{
		keyCheck();
	}

	private void keyCheck()
	{
		selector.select();
		
		Set<SelectionKey> selectedKeys = selector.selectedKeys();
		Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

		while (keyIterator.hasNext()) {
			SelectionKey key = keyIterator.next();

			if (key.isAcceptable()) {
				//accept(key);
			} else if (key.isConnectable()) {
				//establish(key);
			} else if (key.isWritable()) {
				write(key);
			} else if (key.isReadable()) {
				read(key);
			}
			keyIterator.remove();
		}
	}
	
	/**
	 * Sends a byte array of data to the server.
	 */
	private void sendData(byte[] data, InetAddress address, int port)
	{
		if (address != null)
		{
			DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
			
			try {
				datagram.register(selector, SelectionKey.OP_WRITE, packet);
			} 
			catch (ClosedChannelException cce)
			{
				logger().log(ALogType.WARNING, "Data cannot be sent along closed channel!", cce);
			}
		}
	}
	
	public void sendPacket(BytePacket packet, IPConnection... exceptedConnections)
	{
		if (!isServerSide()) // Pend the server connection
		{
			packet.setConnections(ClientGame.instance().sockets().getServerConnection());
		}
		
		if (packet.mustSend()) // Use TCP
		{
			for (IPConnection con : packet.getConnections())
			{
				if (con != null && con.getSocket() != null)
				{
					try
					{
						con.getSocket().register(selector, SelectionKey.OP_WRITE, packet.getData());
					}
					catch (ClosedChannelException cce)
					{
						logger().log(ALogType.WARNING, "Data cannot be sent along closed channel!", cce);
					}
				}
			}
		}
		else // Use UDP
		{
			for (IPConnection con : packet.getConnections())
			{
				if (con != null && con.getIPAddress() != null)
				{
					sendData(packet.getData(), con.getIPAddress(), con.getPort());
					if (ALogger.tracePackets) logger().log(ALogType.DEBUG, "[" + con.asReadable() + "] " + packet.getType());
				}
			}
		}
	}
	
	public void sendPacketToAllClients(BytePacket packet, IPConnection... exceptedConnections)
	{
		if (isServerSide()) // Pend all connected players
		{
			// ArrayList<IPConnection> cons = new ArrayList<IPConnection>();
			
			List<ConnectedPlayer> players = getConnectedPlayers();
			
			IPConnection[] cons = new IPConnection[players.size()];
			
			for (int i = 0; i < cons.length; i++)
			{
				// just assume that they're all connected players to speed up process
				IPConnection con = ((IPConnectedPlayer) players.get(i)).getConnection();
				
				boolean isAllowed = true;
				for (IPConnection exc : exceptedConnections)
				{
					if (exc == con)
					{
						isAllowed = false;
						break;
					}
				}
				
				if (isAllowed) cons[i] = con;
			}
			
			packet.setConnections(cons);
		}
		else
		{
			logger().log(ALogType.DEBUG, "Server is using the method sendPacketToAllClients() in PacketSender. The client should be using sendPacket(). There isn't a problem at the moment, but it is bad practice and could cause future problems.");
		}
		
		sendPacket(packet);
	}
	
	private void write(SelectionKey key)
	{
		try
		{
			SelectableChannel abstractClient = key.channel();
			
			if (abstractClient instanceof SocketChannel)
			{
				SocketChannel client = (SocketChannel) abstractClient;
				ByteBuffer buffer = ByteBuffer.allocate(ClientGame.RECEIVE_PACKET_BUFFER_LIMIT);
				buffer.clear();
				buffer.put((byte[]) key.attachment());
				buffer.flip();
				
				while (buffer.hasRemaining())
				{
					client.write(buffer);
				}
				
				client.register(selector, SelectionKey.OP_READ, null);
			}
			else if (abstractClient instanceof DatagramChannel)
			{
				DatagramChannel client = (DatagramChannel) abstractClient;
				client.socket().send((DatagramPacket) key.attachment());
				
				/*
				ByteBuffer buffer = ByteBuffer.allocate(ClientGame.RECEIVE_PACKET_BUFFER_LIMIT);
				DatagramPacket packet = (DatagramPacket) key.attachment();
				buffer.clear();
				buffer.put(packet.getData());
				buffer.flip();
				
				while (buffer.hasRemaining())
				{
					client.write(buffer);
				}
				*/
								
				client.register(selector, SelectionKey.OP_READ, null);
			}
		}
		catch (IOException e)
		{
			if (isRunning) logger().log(ALogType.CRITICAL, "I/O error while sending", e);
			
			if (!isStopping()) e.printStackTrace();
		}
	}
	
	private void read(SelectionKey key)
	{
		try
		{
			SelectableChannel abstractClient =  key.channel();
			
			if (abstractClient instanceof SocketChannel) // TODO: find similarities between reads and simplify code
			{
				SocketChannel client = (SocketChannel) abstractClient;
				ByteBuffer buffer = ByteBuffer.allocate(ClientGame.RECEIVE_PACKET_BUFFER_LIMIT);
				int bytesRead = client.read(buffer);
				
				while (bytesRead > 0) {
					bytesRead = client.read(buffer);
				}
				
				InetSocketAddress remoteAddress = (InetSocketAddress) client.getRemoteAddress();
				if (bytesRead == -1) {
					getGame().logger().log(ALogType.WARNING, "Connection closed by: " + IPUtil.asReadable(remoteAddress));
					client.close();
				}
				
				BytePacket packet = new BytePacket(buffer.array());
				InetAddress address = remoteAddress.getAddress();
				int port = remoteAddress.getPort();
				
				if (ALogger.tracePackets) logger().log(ALogType.DEBUG, "[" + IPUtil.asReadable(address, port) + "] " + packet.getType());
				
				getGame().addPacketToParse(new IncomingPacketWrapper(packet, address, port));
			}
			else if (abstractClient instanceof DatagramChannel)
			{
				DatagramChannel client = (DatagramChannel) abstractClient;
				ByteBuffer buffer = ByteBuffer.allocate(ClientGame.RECEIVE_PACKET_BUFFER_LIMIT);
				int bytesRead = client.read(buffer);
				
				while (bytesRead > 0) {
					bytesRead = client.read(buffer);
				}
				
				InetSocketAddress remoteAddress = (InetSocketAddress) client.getRemoteAddress();
				if (bytesRead == -1) {
					game.logger().log(ALogType.WARNING, "Connection closed by: " + IPUtil.asReadable(remoteAddress));
					client.close();
				}
				
				BytePacket packet = new BytePacket(buffer.array());
				InetAddress address = remoteAddress.getAddress();
				int port = remoteAddress.getPort();
				
				if (ALogger.tracePackets) logger().log(ALogType.DEBUG, "[" + IPUtil.asReadable(address, port) + "] " + packet.getType());
				
				// Faster ping times because it doesn't have to go through the client's tick system
				if (packet.getType() == PacketType.TEST_PING)
				{
					if (isServerSide())
					{
						IPConnectedPlayer sender = getPlayerConnection(address, port);
						
						if (sender != null)
						{
							if (sender.isPendingPingUpdate())
							{
								sender.updatePing(System.currentTimeMillis());
							}
							else
							{
								logger().log(ALogType.WARNING, "Received ping response from a client that isn't pending a ping update: " + sender.getConnection().asReadable());
							}
						}
						else
						{
							logger().log(ALogType.WARNING, "Received ping response from a client isn't connected [" + IPUtil.asReadable(address, port) + "]");
						}
					}
					else
					{
						sendPacket(new PacketPingClientServer());
						getGame().addPacketToParse(new IncomingPacketWrapper(packet, address, port));
					}
				}
				else
				{
					getGame().addPacketToParse(new IncomingPacketWrapper(packet, address, port));
				}
			}
			
		}
		catch (PortUnreachableException e)
		{
			logger().log(ALogType.CRITICAL, "Port is unreachable: " + socket().socket().getPort(), e);
			if (!isServerSide())
			{
				ClientGame.instance().setCurrentScreen(new GuiJoinServer(new GuiMainMenu()));
				new GuiClickNotification(ClientGame.instance().getCurrentScreen(), -1, "PORT", "UNREACHABLE");
			}
		}
		catch (SocketException e)
		{
			// TODO this is invisible
			if (!isStopping()) e.printStackTrace();
		}
		catch (IOException e)
		{
			if (isRunning) logger().log(ALogType.CRITICAL, "I/O error while receiving", e);
			
			if (!isStopping()) e.printStackTrace();
		}
	}
}
