package dc.longshot.entitysystems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.math.Vector2;

import dc.longshot.epf.Entity;
import dc.longshot.epf.EntityManager;
import dc.longshot.epf.EntitySystem;
import dc.longshot.geometry.PolygonUtils;
import dc.longshot.geometry.VectorUtils;
import dc.longshot.parts.AttachmentPart;
import dc.longshot.parts.TransformPart;
import dc.longshot.parts.TranslatePart;
import dc.longshot.parts.WeaponPart;

public final class ShooterInputSystem implements EntitySystem {
	
	private final EntityManager entityManager;
	
	public ShooterInputSystem(final EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public final void update(final float delta, final Entity entity) {
		if (entity.hasActive(WeaponPart.class, AttachmentPart.class)) {
			if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
				WeaponPart weaponPart = entity.get(WeaponPart.class);
				if (weaponPart.canSpawn()) {
					Entity bullet = weaponPart.createSpawn();
					Entity cannon = entity.get(AttachmentPart.class).getAttachedEntity();
					Vector2 spawnPosition = getMiddleOfCannonMouth(cannon, bullet);
					bullet.get(TransformPart.class).setPosition(spawnPosition);
					Vector2 velocity = VectorUtils.createVectorFromAngle(
							cannon.get(TransformPart.class).getRotation());
					bullet.get(TranslatePart.class).setVelocity(velocity);
					entityManager.add(bullet);
				}
			}
		}
	}
	
	private Vector2 getMiddleOfCannonMouth(final Entity cannon, final Entity spawn) {
		TransformPart cannonTransform = cannon.get(TransformPart.class);
		Vector2 size = cannonTransform.getSize();
		Vector2 spawnPosition = PolygonUtils.toGlobal(size.x, size.y / 2, cannonTransform.getPolygon());
		return PolygonUtils.relativeCenter(spawnPosition, spawn.get(TransformPart.class).getBoundingSize());
	}

}
