package ca.pixel.game.world;

import java.util.ArrayList;
import java.util.List;

import ca.pixel.game.Game;
import ca.pixel.game.assets.Assets;
import ca.pixel.game.entity.Entity;
import ca.pixel.game.gfx.FlickeringLight;
import ca.pixel.game.gfx.LightMap;
import ca.pixel.game.gfx.PointLight;
import ca.pixel.game.gfx.Texture;
import ca.pixel.game.world.tiles.Tile;

public class Level
{
	private Tile[] tiles;
	private List<Entity> entities;
	public int width;
	public int height;
	public int xOffset = 0;
	public int yOffset = 0;
	private LightMap lightmap = new LightMap(Game.WIDTH, Game.HEIGHT);
	private PointLight playerLight = new FlickeringLight(60, 150, 50, 47, 4);
	private List<PointLight> lights;
	
	public Level(int width, int height)
	{
		tiles = new Tile[width * height];
		
		this.width = height;
		this.height = height;
		
		entities = new ArrayList<Entity>();
		lights = new ArrayList<PointLight>();
		
		// Initializes the level.
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				if (x * y % 10 < 5)// TODO Test generation. Feel free to replace
				{
					tiles[x + (y * width)] = Tile.GRASS;
				}
				else
				{
					tiles[x + (y * width)] = Tile.STONE;
				}
			}
		}
		
		lights.add(playerLight);
		
		lights.add(new PointLight(60, 150, 10));
		
		for (int x = 0; x < 30; x++)
			for (int y = 0; y < 30; y++)
				lights.add(new PointLight((x * 15) + 500, (y * 15) + 500, 20));
				
		lights.add(new PointLight(140, 170, 20));
		lights.add(new PointLight(20, 240, 20));
		lights.add(new PointLight(40, 260, 20));
		lights.add(new PointLight(0, 700, 120));
		
	}
	
	public void setCameraCenterInWorld(int x, int y)
	{
		xOffset = x - (Game.WIDTH / 2);
		yOffset = y - (Game.HEIGHT / 2);
	}
	
	public void render(Texture renderTo)
	{
		// Renders Tiles
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < height; x++)
			{
				switch (getTile(x, y).getMaterial())
				{
					case GRASS:
						renderTo.draw(Assets.grass.getTexture(0), (x * 16) - xOffset, (y * 16) - yOffset);
						break;
					case STONE:
						renderTo.draw(Assets.stone, (x * 16) - xOffset, (y * 16) - yOffset);
						break;
					case VOID:
						break;
				}
			}
		}
		
		for (Entity entity : entities)
		{
			entity.render(renderTo);
		}
		
		// Draws all the lighting over everything else
		lightmap.clear();
		
		for (PointLight light : lights)
		{
			light.renderCentered(lightmap, xOffset, yOffset);
		}
		
		lightmap.patch();
		
		renderTo.draw(lightmap, 0, 0);
	}
	
	public void tick()
	{
		for (Entity entity : entities)
		{
			entity.tick();
		}
		
		for (PointLight light : lights)
		{
			light.tick();
		}
		
		playerLight.setX(Game.instance().player.getX() + 8);
		playerLight.setY(Game.instance().player.getY() + 8);
	}
	
	public int getCameraXOffset()
	{
		return xOffset;
	}
	
	public int getCameraYOffset()
	{
		return yOffset;
	}
	
	public Tile getTile(int x, int y)
	{
		// If off-screen
		if (x < 0 || x > width || y < 0 || y > height)
		{
			return Tile.VOID;
			// return new Tile(Material.VOID, false, false);
		}
		
		return tiles[x + (y * width)];
	}
	
	public void addEntity(Entity entity)
	{
		entities.add(entity);
	}
}
