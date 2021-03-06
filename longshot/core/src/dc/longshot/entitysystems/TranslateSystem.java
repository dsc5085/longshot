package dc.longshot.entitysystems;

import com.badlogic.gdx.math.Vector2;

import dc.longshot.epf.Entity;
import dc.longshot.epf.EntitySystem;
import dc.longshot.parts.TransformPart;
import dc.longshot.parts.TranslatePart;

public final class TranslateSystem extends EntitySystem {

	@Override
	public final void update(final float delta, final Entity entity) {
		if (entity.hasActive(TranslatePart.class)) { 
			TransformPart transformPart = entity.get(TransformPart.class);
			Vector2 offset = entity.get(TranslatePart.class).getVelocity().scl(delta);
			Vector2 newPosition = transformPart.getPosition().add(offset);
			transformPart.setPosition(newPosition);
		}
	}

}
