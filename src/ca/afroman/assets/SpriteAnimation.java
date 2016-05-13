package ca.afroman.assets;

import ca.afroman.interfaces.ITickable;

public class SpriteAnimation extends Asset implements ITickable
{
	/** Holds the textures for each frame. */
	private Texture[] textures;
	private int currentFrameIndex = 0;
	private boolean pingPong;
	private boolean goingUp = true;
	private int ticksPerFrame;
	private int tickCounter = 0;
	
	public SpriteAnimation(int ticksPerFrame, Texture... frames)
	{
		this(false, ticksPerFrame, frames);
	}
	
	public SpriteAnimation(boolean pingPong, int ticksPerFrame, Texture... frames)
	{
		this.pingPong = pingPong;
		textures = frames;
		this.ticksPerFrame = ticksPerFrame;
	}
	
	/**
	 * Progresses this to the next frame.
	 */
	@Override
	public void tick()
	{
		if (ticksPerFrame != 0)
		{
			tickCounter++;
			
			// If it's supposed to progress based on tpf
			if (tickCounter >= ticksPerFrame)
			{
				tickCounter = 0;
				
				if (goingUp)
				{
					currentFrameIndex++;
				}
				else
				{
					currentFrameIndex--;
				}
				
				// If it's going over the limit, loop back to frame 1, or ping pong
				if (currentFrameIndex > frameCount() - 1)
				{
					if (pingPong)
					{
						// Makes animation play the other way.
						goingUp = !goingUp;
						// Puts back to the next frame
						currentFrameIndex -= 2;
					}
					else
					{
						currentFrameIndex = 0;
					}
				}
				
				if (currentFrameIndex < 0)
				{
					if (pingPong)
					{
						// Makes animation play the other way.
						goingUp = !goingUp;
						// Puts back to the next frame
						currentFrameIndex += 2;
					}
				}
			}
		}
	}
	
	public Texture getCurrentFrame()
	{
		return textures[currentFrameIndex];
	}
	
	public int frameCount()
	{
		return textures.length;
	}
	
	public void setFrame(int frame)
	{
		currentFrameIndex = frame;
	}
	
	@Override
	public void render(Texture renderTo, int x, int y)
	{
		renderTo.draw(getCurrentFrame(), x, y);
	}
	
	@Override
	public Asset clone()
	{
		Texture[] newTextures = new Texture[textures.length];
		
		for (int i = 0; i < textures.length; i++)
		{
			newTextures[i] = textures[i].clone();
		}
		
		return new SpriteAnimation(pingPong, ticksPerFrame, newTextures);
	}
}
