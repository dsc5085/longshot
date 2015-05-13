package dc.longshot.entitysystems;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import dc.longshot.epf.Entity;
import dc.longshot.epf.EntityCache;
import dc.longshot.epf.EntityManager;
import dc.longshot.epf.EntitySystem;
import dc.longshot.parts.GroundExploderPart;
import dc.longshot.parts.TransformPart;

public final class GroundExploderSystem extends EntitySystem {

	private final EntityCache entityCache;
	private final EntityManager entityManager;
	private final Rectangle boundsBox;
	
	public GroundExploderSystem(final EntityCache entityCache, final EntityManager entityManager, 
			final Rectangle boundsBox) {
		this.entityCache = entityCache;
		this.entityManager = entityManager;
		this.boundsBox = boundsBox;
	}
	
	@Override
	public void update(final float delta, final Entity entity) {
		if (entity.hasActive(GroundExploderPart.class)) {
			GroundExploderPart groundExploderPart = entity.get(GroundExploderPart.class);
			float explodeRate = groundExploderPart.getExplodeRate();
			if (MathUtils.randomBoolean(delta / explodeRate)) {
				Entity newEntity = entityCache.create(groundExploderPart.getEntityType());
				float diameter = MathUtils.random(groundExploderPart.getMinSize(), groundExploderPart.getMaxSize());
				float positionX = MathUtils.random(boundsBox.x, boundsBox.x + boundsBox.width);
				float positionY = MathUtils.random(boundsBox.y - diameter / 2, boundsBox.y + diameter);
				Vector2 position = new Vector2(positionX, positionY);
				TransformPart transformPart = newEntity.get(TransformPart.class);
				transformPart.setSize(new Vector2(diameter, diameter));
				transformPart.setCenter(position);
				entityManager.add(newEntity);
			}
		}
	}
	
}