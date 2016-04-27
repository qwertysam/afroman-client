package ca.afroman.gui;

import java.awt.Rectangle;

import ca.afroman.Game;
import ca.afroman.assets.Assets;
import ca.afroman.assets.Texture;
import ca.afroman.input.InputType;

public class GuiButton extends InputType
{
	private GuiScreen screen;
	
	protected Rectangle hitbox;
	
	private int id;
	private boolean canHold = false;
	
	private int state = 0;
	private Texture[] textures;
	
	public GuiButton(GuiScreen screen, int id, int x, int y)
	{
		this(screen, Assets.getTexture(Assets.BUTTON_NORMAL), Assets.getTexture(Assets.BUTTON_HOVER), Assets.getTexture(Assets.BUTTON_PRESSED), id, x, y);
	}
	
	public GuiButton(GuiScreen screen, Texture normal, Texture hover, Texture pressed, int id, int x, int y)
	{
		hitbox = new Rectangle(x, y, normal.getWidth(), normal.getHeight());
		
		textures = new Texture[3];
		textures[0] = normal;
		textures[1] = hover;
		textures[2] = pressed;
		
		this.screen = screen;
		this.id = id;
	}
	
	public void tick()
	{
		int mouseX = Game.instance().input.getMouseX();
		int mouseY = Game.instance().input.getMouseY();
		
		if (hitbox.contains(mouseX, mouseY))
		{
			if (Game.instance().input.mouseLeft.isPressed())
			{
				state = 2; // Down
				
				this.setPressed(true);
			}
			else
			{
				state = 1; // Hovering
				
				this.setPressed(false);
			}
		}
		else
		{
			state = 0; // Not on the button at all
			
			this.setPressed(false);
		}
		
		if (this.canHold() && this.isPressed())
		{
			onPressed();
		}
		else if (this.isPressedFiltered())
		{
			onPressed();
		}
		else if (this.isReleasedFiltered())
		{
			onRelease();
		}
	}
	
	public boolean canHold()
	{
		return canHold;
	}
	
	public void setCanHold(boolean canHold)
	{
		this.canHold = canHold;
	}
	
	protected void onPressed()
	{
		screen.pressAction(id);
	}
	
	protected void onRelease()
	{
		screen.releaseAction(id);
	}
	
	public Texture getTexture()
	{
		return textures[state];
	}
	
	public void render(Texture drawTo)
	{
		drawTo.draw(getTexture(), hitbox.x, hitbox.y);
	}
}
