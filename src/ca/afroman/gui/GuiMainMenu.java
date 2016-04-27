package ca.afroman.gui;

import java.awt.Color;

import javax.swing.JOptionPane;

import ca.afroman.Game;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Font;
import ca.afroman.assets.SpriteAnimation;
import ca.afroman.assets.Texture;
import ca.afroman.gfx.FlickeringLight;
import ca.afroman.gfx.LightMap;
import ca.afroman.server.GameServer;

public class GuiMainMenu extends GuiScreen
{
	private Font font;
	private SpriteAnimation afroMan;
	private SpriteAnimation player2;
	private LightMap lightmap;
	private FlickeringLight light;
	
	public GuiMainMenu(Game game)
	{
		super(game, null);
	}
	
	@Override
	public void init()
	{
		font = Assets.getFont(Assets.FONT_NORMAL);
		afroMan = Assets.getSpriteAnimation(Assets.PLAYER_ONE_IDLE_DOWN);
		player2 = Assets.getSpriteAnimation(Assets.PLAYER_TWO_IDLE_DOWN);
		
		lightmap = new LightMap(Game.WIDTH, Game.HEIGHT, new Color(0F, 0F, 0F, 0.3F));
		light = new FlickeringLight(Game.WIDTH / 2, 38, 60, 62, 5);
		
		buttons.add(new GuiTextButton(this, 1, (Game.WIDTH / 2) - (72 / 2), 60, font, "Join Game"));
		buttons.add(new GuiTextButton(this, 2, (Game.WIDTH / 2) - (72 / 2), 90, font, "Host Game"));
	}
	
	@Override
	public void drawScreen(Texture renderTo)
	{
		lightmap.clear();
		light.renderCentered(lightmap);
		lightmap.patch();
		
		renderTo.draw(lightmap, 0, 0);
		
		font.renderCentered(renderTo, Game.WIDTH / 2, 15, "Cancer: The Adventures of Afro Man");
		renderTo.draw(afroMan.getCurrentFrame(), (Game.WIDTH / 2) - 20, 30);
		renderTo.draw(player2.getCurrentFrame(), (Game.WIDTH / 2) + 4, 30);
	}
	
	@Override
	public void tick()
	{
		super.tick();
		
		light.tick();
		afroMan.tick();
		player2.tick();
	}
	
	@Override
	public void pressAction(int buttonID)
	{
		
	}
	
	@Override
	public void releaseAction(int buttonID)
	{
		String ip = "localhost";
		String pass = "";
		
		switch (buttonID)
		{
			case 2: // Host Server
				pass = JOptionPane.showInputDialog("Please create a password (Leave blank for no password)");
				
				game.socketServer = new GameServer(pass);
				game.socketServer.start();
				game.isHosting = true;
				
			case 1: // Join Server
				game.setCurrentScreen(new GuiJoinServer(game, this));
				
				/*game.socketClient = new GameClient();
				game.socketClient.start();
				
				if (!game.isHostingServer()) // If it's not hosting get IP. If it is hosting, assume localhost
				{
					ip = JOptionPane.showInputDialog("What is the server's IP");
				}
				
				game.socketClient.setServerIP(ip);
				game.socketClient.sendPacket(new PacketRequestConnection(pass));
				break;*/
		}
	}
	
}
