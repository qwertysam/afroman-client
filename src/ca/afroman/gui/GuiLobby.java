package ca.afroman.gui;

import java.net.InetAddress;
import java.net.UnknownHostException;

import ca.afroman.assets.AssetType;
import ca.afroman.assets.Assets;
import ca.afroman.assets.SpriteAnimation;
import ca.afroman.assets.Texture;
import ca.afroman.client.ClientGame;
import ca.afroman.client.ExitGameReason;
import ca.afroman.game.Game;
import ca.afroman.game.Role;
import ca.afroman.light.FlickeringLight;
import ca.afroman.light.LightMap;
import ca.afroman.log.ALogType;
import ca.afroman.network.ConnectedPlayer;
import ca.afroman.option.Options;
import ca.afroman.packet.technical.PacketStartServer;
import ca.afroman.resource.Vector2DDouble;

public class GuiLobby extends GuiScreen
{
	private SpriteAnimation player1;
	private int player1X = -300;
	private int player1Y = -300;
	private SpriteAnimation player2;
	private int player2X = -300;
	private int player2Y = -300;
	private LightMap lightmap;
	private FlickeringLight light1;
	private FlickeringLight light2;
	
	private String lanIP;
	private String port;
	
	private GuiTextButton startButton;
	private GuiTextButton stopButton;
	private GuiIconButton optionsButton;
	
	private boolean firstTime;
	
	public GuiLobby()
	{
		super(null);
		
		player1 = Assets.getSpriteAnimation(AssetType.PLAYER_ONE_IDLE_DOWN);
		player2 = Assets.getSpriteAnimation(AssetType.PLAYER_TWO_IDLE_DOWN);
		
		lightmap = new LightMap(ClientGame.WIDTH, ClientGame.HEIGHT, LightMap.DEFAULT_AMBIENT);
		
		light1 = new FlickeringLight(true, new Vector2DDouble(0, 0), 45, 50, 5);
		light2 = new FlickeringLight(true, new Vector2DDouble(0, 0), 45, 50, 5);
		
		startButton = new GuiTextButton(this, 2000, (ClientGame.WIDTH / 2) - 84 - 8, 116, 84, blackFont, "Start Game");
		startButton.setEnabled(ClientGame.instance().isHostingServer());
		addButton(startButton);
		
		optionsButton = new GuiIconButton(this, 2005, 6, ClientGame.HEIGHT - 19, 16, Assets.getStepSpriteAnimation(AssetType.ICON_SETTINGS));
		addButton(optionsButton);
		
		lanIP = "Unknown";
		
		// If it's the host
		if (ClientGame.instance().isHostingServer())
		{
			// Draw a stop server button
			stopButton = new GuiTextButton(this, 2001, (ClientGame.WIDTH / 2) + 8, 116, 84, blackFont, "Stop Server");
			
			// Get the local IP
			try
			{
				InetAddress ip = InetAddress.getLocalHost();
				lanIP = ip.toString();
			}
			catch (UnknownHostException e)
			{
				ClientGame.instance().logger().log(ALogType.CRITICAL, "Could not resolve local host address", e);
			}
			
			port = Options.instance().serverPort;
		}
		// Draw a leave server button
		else
		{
			stopButton = new GuiTextButton(this, 2002, (ClientGame.WIDTH / 2) + 8, 116, 84, blackFont, "Disconnect");
		}
		addButton(stopButton);
		
		// Makes sure that the GUI buttons are initialised no matter what
		firstTime = true;
	}
	
	@Override
	public void drawScreen(Texture renderTo)
	{
		renderTo.draw(player1.getDisplayedTexture(), player1X, player1Y);
		renderTo.draw(player2.getDisplayedTexture(), player2X, player2Y);
		
		light1.setPosition(player1X + 8, player1Y + 8);
		light2.setPosition(player2X + 8, player2Y + 8);
		
		lightmap.clear();
		light1.renderCentered(lightmap);
		light2.renderCentered(lightmap);
		lightmap.patch();
		
		lightmap.render(renderTo, 0, 0);
		
		nobleFont.renderCentered(renderTo, ClientGame.WIDTH / 2, 6, "Connected Players: " + ClientGame.instance().sockets().getConnectedPlayers().size() + "/" + Game.MAX_PLAYERS);
		if (ClientGame.instance().isHostingServer())
		{
			blackFont.renderCentered(renderTo, ClientGame.WIDTH / 2, 20, "LAN: " + lanIP + ":" + port);
		}
		else
		{
			blackFont.renderCentered(renderTo, ClientGame.WIDTH / 2, 20, "(Waiting for host to start server)");
		}
	}
	
	@Override
	public void keyTyped()
	{
		
	}
	
	@Override
	public void pressAction(int buttonID, boolean isLeft)
	{
		
	}
	
	@Override
	public void releaseAction(int buttonID, boolean isLeft)
	{
		if (buttonID == 2000) // Start Game
		{
			ClientGame.instance().sockets().sender().sendPacket(new PacketStartServer(true));
		}
		else if (buttonID == 2001) // Stop Server
		{
			ClientGame.instance().sockets().sender().sendPacket(new PacketStartServer(false));
		}
		else if (buttonID == 2002) // Leave server
		{
			ClientGame.instance().exitFromGame(ExitGameReason.DISCONNECT);
		}
		else if (buttonID == 2005) // Options menu
		{
			ClientGame.instance().setCurrentScreen(new GuiOptionsMenu(this, false));
		}
		else if (buttonID >= 0)
		{
			ClientGame.instance().setCurrentScreen(new GuiChooseRole(this, (short) buttonID));
		}
	}
	
	@Override
	public void tick()
	{
		light1.tick();
		light2.tick();
		
		// Draws all the new buttons if the server list has been updated
		if (ClientGame.instance().hasServerListBeenUpdated() || firstTime)
		{
			firstTime = false;
			
			// Removes all buttons except for that at index 0 (The Start Game Button)
			clearButtons();
			addButton(startButton);
			addButton(stopButton);
			addButton(optionsButton);
			
			player1X = -300;
			player1Y = -300;
			player2X = -300;
			player2Y = -300;
			
			// Draws the player list
			int counter = 0;
			int row = 0;
			for (ConnectedPlayer player : ClientGame.instance().sockets().getConnectedPlayers())
			{
				boolean isEvenNum = (counter & 1) == 0;
				
				if (isEvenNum) row++;
				
				int buttonX = (isEvenNum ? 20 : 148);
				int buttonY = 20 + (18 * row);
				
				// Add each player in the last as a button. Only lets the host of the server edit
				GuiTextButton button = new GuiTextButton(this, player.getID(), buttonX, buttonY, 72, (ClientGame.instance().isHostingServer() ? blackFont : whiteFont), player.getUsername());
				button.setEnabled(ClientGame.instance().isHostingServer());
				this.addButton(button);
				
				// Draws the player sprite beside the name of the user playing as that character
				if (player.getRole() == Role.PLAYER1)
				{
					player1X = buttonX + (isEvenNum ? 80 : -24);
					player1Y = buttonY;
				}
				else if (player.getRole() == Role.PLAYER2)
				{
					player2X = buttonX + (isEvenNum ? 80 : -24);
					player2Y = buttonY;
				}
				
				counter++;
			}
			
			// Only enable the start button if both the player roles are not null
			startButton.setEnabled(ClientGame.instance().isHostingServer() && ClientGame.instance().sockets().getPlayerConnection(Role.PLAYER1) != null && ClientGame.instance().sockets().getPlayerConnection(Role.PLAYER2) != null);
		}
		
		super.tick();
	}
	
	@Override
	public void updateValue(int sliderID, int newValue)
	{
		
	}
}
