package dc.longshot.game;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSprite;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import dc.longshot.epf.Entity;
import dc.longshot.geometry.PolygonFactory;
import dc.longshot.geometry.PolygonUtils;
import dc.longshot.geometry.UnitConvert;
import dc.longshot.geometry.VectorUtils;
import dc.longshot.graphics.RegionFactory;
import dc.longshot.parts.ColorChangePart;
import dc.longshot.parts.DrawablePart;
import dc.longshot.parts.SpeedPart;
import dc.longshot.parts.TimedDeathPart;
import dc.longshot.parts.TransformPart;
import dc.longshot.parts.TranslatePart;

public final class Fragmenter {
	
	private final int fragWidth;
	private final int fragHeight;
	private final float fragSpeedMultiplier;
	
	public Fragmenter(final int fragWidth, final int fragHeight, final float fragSpeedMultiplier) {
		this.fragWidth = fragWidth;
		this.fragHeight = fragHeight;
		this.fragSpeedMultiplier = fragSpeedMultiplier;
	}
	
	public final List<Entity> createFrags(final PolygonRegion region, final Polygon parentPolygon, final float z, 
			final float fadeTime) {
		List<PolygonSprite> fragSprites = createFragSprites(region, fragWidth, fragHeight);
		return createFrags(region.getRegion().getRegionWidth(), region.getRegion().getRegionHeight(), parentPolygon, 
				fragSprites, z, fadeTime);
	}
	
	private final List<Entity> createFrags(final int regionWidth, final int regionHeight, final Polygon parentPolygon, 
			final List<PolygonSprite> fragSprites, final float z, final float fadeTime) {
		List<Entity> frags = new ArrayList<Entity>();
		Vector2 scale = UnitConvert.worldToPixel(PolygonUtils.size(parentPolygon))
				.scl(1.0f / regionWidth, 1.0f / regionHeight);
		for (PolygonSprite fragSprite : fragSprites) {
			Entity frag = createFrag(fragSprite, parentPolygon, scale, z, fadeTime);
			frags.add(frag);
		}
		return frags;
	}
	
	private final Entity createFrag(final PolygonSprite fragSprite, final Polygon parentPolygon, final Vector2 scale, 
			final float z, final float fadeTime) {
		Entity entity = new Entity();
		Polygon fragPolygon = new Polygon();
		fragPolygon.setRotation(parentPolygon.getRotation());
		Vector2 fragSize = UnitConvert.pixelToWorld(fragSprite.getWidth(), fragSprite.getHeight()).scl(scale);
		fragPolygon.setVertices(PolygonFactory.createRectangleVertices(fragSize.x, fragSize.y));
		Vector2 worldPosition = UnitConvert.pixelToWorld(fragSprite.getX(), fragSprite.getY()).scl(scale);
		Vector2 globalPosition = PolygonUtils.toGlobal(worldPosition.x, worldPosition.y, parentPolygon);
		entity.attach(new TransformPart(fragPolygon, new Vector3(globalPosition.x, globalPosition.y, z)));
		PolygonSprite sprite = new PolygonSprite(fragSprite);
		entity.attach(new DrawablePart(sprite));
		Vector2 velocity = calculateVelocity(parentPolygon, fragPolygon);
		entity.attach(new SpeedPart(velocity.len()));
		TranslatePart translatePart = new TranslatePart();
		entity.attach(translatePart);
		EntityUtils.setDirection(entity, velocity);
		entity.attach(new TimedDeathPart(fadeTime));
		entity.attach(new ColorChangePart(fadeTime, Color.WHITE.cpy(), Color.CLEAR.cpy()));
		return entity;
	}
	
	private final Vector2 calculateVelocity(final Polygon parentPolygon, final Polygon childPolygon) {
		Vector2 offset = VectorUtils.offset(getFragOrigin(parentPolygon), PolygonUtils.center(childPolygon));
		return VectorUtils.lengthened(offset, offset.len() * fragSpeedMultiplier);
	}
	
	private final List<PolygonSprite> createFragSprites(final PolygonRegion polygonRegion, final int fragWidth, 
			final int fragHeight) {
		List<PolygonSprite> fragSprites = new ArrayList<PolygonSprite>();
		TextureRegion textureRegion = polygonRegion.getRegion();
		for (int x = 0; x < textureRegion.getRegionWidth(); x += fragWidth) {
			for (int y = 0; y < textureRegion.getRegionHeight(); y += fragHeight) {
				int width = Math.min(fragWidth, textureRegion.getRegionWidth() - x);
				int height = Math.min(fragHeight, textureRegion.getRegionHeight() - y);
				TextureRegion fragRegion = new TextureRegion(polygonRegion.getRegion(), x, y, width, height);
				PolygonSprite fragSprite = new PolygonSprite(RegionFactory.createPolygonRegion(fragRegion));
				fragSprite.setOrigin(0, 0);
				fragSprite.setPosition(x, textureRegion.getRegionHeight() - y - height);
				fragSprites.add(fragSprite);
			}
		}
		return fragSprites;
	}
	
	private final Vector2 getFragOrigin(final Polygon polygon) {
		return PolygonUtils.center(polygon);
	}
	
}
