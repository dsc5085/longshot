package dc.longshot.screens;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import dc.longshot.collision.CollisionManager;
import dc.longshot.entitysystems.AIShooterSystem;
import dc.longshot.entitysystems.BounceSystem;
import dc.longshot.entitysystems.BoundPositionSystem;
import dc.longshot.entitysystems.CityDamageSystem;
import dc.longshot.entitysystems.CollisionDamageSystem;
import dc.longshot.entitysystems.EmitSystem;
import dc.longshot.entitysystems.InputMovementSystem;
import dc.longshot.entitysystems.NoHealthSystem;
import dc.longshot.entitysystems.OutOfBoundsRemoveSystem;
import dc.longshot.entitysystems.RotateToCursorSystem;
import dc.longshot.entitysystems.TimedDeathSystem;
import dc.longshot.epf.Entity;
import dc.longshot.epf.EntityAddedEvent;
import dc.longshot.epf.EntityAddedListener;
import dc.longshot.epf.EntityManager;
import dc.longshot.epf.EntityRemovedEvent;
import dc.longshot.epf.EntityRemovedListener;
import dc.longshot.epf.EntitySystem;
import dc.longshot.eventmanagement.EventManager;
import dc.longshot.game.BackdropManager;
import dc.longshot.game.DecorationProfile;
import dc.longshot.game.EntityFactory;
import dc.longshot.game.GameInputProcessor;
import dc.longshot.game.LevelController;
import dc.longshot.game.Skins;
import dc.longshot.geometry.Bound;
import dc.longshot.geometry.PolygonUtils;
import dc.longshot.geometry.ScreenUnitConversion;
import dc.longshot.geometry.VectorUtils;
import dc.longshot.graphics.SpriteCache;
import dc.longshot.models.GameSession;
import dc.longshot.models.Level;
import dc.longshot.models.LevelSession;
import dc.longshot.models.SpriteKey;
import dc.longshot.parts.AttachmentPart;
import dc.longshot.parts.BoundsDiePart;
import dc.longshot.parts.DrawablePart;
import dc.longshot.parts.ExplodeOnSpawnPart;
import dc.longshot.parts.HealthPart;
import dc.longshot.parts.ScorePart;
import dc.longshot.parts.SpawnOnDeathPart;
import dc.longshot.parts.TransformPart;
import dc.longshot.parts.TranslatePart;
import dc.longshot.parts.WeaponPart;
import dc.longshot.system.ExecutionState;
import dc.longshot.system.Input;
import dc.longshot.system.ScreenManager;
import dc.longshot.ui.UIUtils;
import dc.longshot.ui.controls.EscapeMenu;
import dc.longshot.ui.controls.ScoreEntryDialog;
import dc.longshot.util.ColorUtils;
import dc.longshot.util.XmlUtils;

public final class LevelScreen implements Screen {
	
	private static final Color MIDNIGHT_BLUE = ColorUtils.toGdxColor(0, 12, 36);
	
	private final ScreenManager screenManager;
	private final SpriteCache<SpriteKey> spriteCache;
	private Screen mainMenuScreen;
	private Camera camera;
	private Vector2 defaultScreenSize;
	private final SpriteBatch spriteBatch;
	private final GameSession gameSession;
	private final float speedMultiplier = 1f;

	private Stage stage;
	private Table worldTable;
	private Label healthLabel;
	private Label scoreLabel;
	
	private EventManager eventManager;
	private EntityManager entityManager;
	private EntityFactory entityFactory;
	private CollisionManager collisionManager;
	private BackdropManager backdropManager;
	private LevelController levelController;
	private List<EntitySystem> entitySystems;
	private InputProcessor gameInputProcessor;

	private final Texture cursorTexture;
	
	private LevelSession levelSession; 
	private int score;
	
	private Level level;
	private Entity shooter;
	
	public LevelScreen(final ScreenManager screenManager, final SpriteCache<SpriteKey> spriteCache, 
			final SpriteBatch spriteBatch, GameSession gameSession) {
		this.screenManager = screenManager;
		this.spriteCache = spriteCache;
		this.spriteBatch = spriteBatch;
		this.gameSession = gameSession;
		cursorTexture = spriteCache.getTexture(SpriteKey.CROSSHAIRS);
	}
	
	public final void setMainMenuScreen(Screen mainMenuScreen) {
		this.mainMenuScreen = mainMenuScreen;
	}

	// TODO: temp
	boolean justDied = false;
	
	@Override
	public final void render(final float delta) {
		handleInput();
		stage.act(delta);
		entityManager.update();
		camera.update();
		boundCursor();
		updateUI();
		
		if (levelSession.getHealth() <= 0) {
			if (!justDied) {
				justDied = true;
				if (gameSession.canAddHighScore(score)) {
					ScoreEntryDialog scoreEntryDialogFactory = new ScoreEntryDialog(Skins.defaultSkin, 
							Skins.ocrFont, stage, screenManager, this, mainMenuScreen, gameSession, score);
					scoreEntryDialogFactory.showDialog();
				}
			}
		}
		
		if (levelController.isComplete()) {
			// TODO: win case
			Gdx.app.exit();
		}
		
		if (levelSession.getExecutionState() == ExecutionState.RUNNING) {
			updateWorld(delta * speedMultiplier);
		}
		
		draw();
	}

	@Override
	public final void resize(final int width, final int height) {
	    stage.getViewport().update(width, height, true);
	}

	@Override
	public final void show() {
		entityFactory = new EntityFactory(spriteCache);
		defaultScreenSize = new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		eventManager = new EventManager();
		entityManager = new EntityManager(eventManager);
		collisionManager = new CollisionManager(eventManager);
		stage = new Stage();
		levelSession = new LevelSession();
		score = 0;
		InputStream levelInputStream = Gdx.files.internal("levels/level1.xml").read();
		level = XmlUtils.unmarshal(levelInputStream, new Class[] { Level.class });
		levelController = new LevelController(entityManager, entityFactory, level);
		
		setupCamera();
		setupBackdropManager();
		addInputProcessors();
		setupStage();
		createSystems();
		createInitialEntities();
		listenToGameEvents();
	}

	@Override
	public final void hide() {
		Input.removeProcessor(gameInputProcessor);
		Input.removeProcessor(stage);
		stage.dispose();
	}

	@Override
	public final void pause() {
		levelSession.setExecutionState(ExecutionState.PAUSED);
	}

	@Override
	public final void resume() {
		levelSession.setExecutionState(ExecutionState.RUNNING);
	}

	@Override
	public final void dispose() {
	}
	
	private EntityAddedListener handleEntityAdded() {
		return new EntityAddedListener() {
			@Override
			public void created(final Entity entity) {
				if (entity.hasActive(ExplodeOnSpawnPart.class, TransformPart.class)) {
					ExplodeOnSpawnPart explodeOnSpawnPart = entity.get(ExplodeOnSpawnPart.class);
					for (Entity other : entityManager.getAll()) {
						if (other != entity && other.hasActive(HealthPart.class, TransformPart.class)) {
							Vector2 entityCenter = entity.get(TransformPart.class).getCenter();
							Vector2 otherCenter = other.get(TransformPart.class).getCenter();
							float distance = otherCenter.cpy().sub(entityCenter).len(); 
							if (distance <= explodeOnSpawnPart.getRadius()) {
								other.get(HealthPart.class).decrease(explodeOnSpawnPart.getDamage());
							}
						}
					}
				}
			}
		};
	}
	
	private EntityRemovedListener handleEntityRemoved() {
		return new EntityRemovedListener() {
			@Override
			public void removed(final Entity entity) {
				// Spawn on death of the entity
				if (entity.hasActive(SpawnOnDeathPart.class)) {
					Entity spawn = entity.get(SpawnOnDeathPart.class).createSpawn();
					entityManager.add(spawn);
				}
				
				// If killed, increase score
				if (entity.hasActive(ScorePart.class)) {
					if (!Bound.isOutOfBounds(entity.get(TransformPart.class).getBoundingBox(), level.getBoundsBox(), 
							entity.get(BoundsDiePart.class).getBounds())) {
						score += entity.get(ScorePart.class).getScore();
					}
				}
				
				if (entity.hasActive(AttachmentPart.class)) {
					Entity child = entity.get(AttachmentPart.class).getChild();
					entityManager.remove(child);
				}
			}
		};
	}
	
	private void setupCamera() {
		Rectangle levelBoundsBox = level.getBoundsBox();
		camera = new OrthographicCamera(levelBoundsBox.width * ScreenUnitConversion.PIXELS_PER_UNIT, 
				levelBoundsBox.height * ScreenUnitConversion.PIXELS_PER_UNIT);
		camera.position.set(levelBoundsBox.x + camera.viewportWidth / 2, 
				levelBoundsBox.y + camera.viewportHeight / 2, 0);
	}
	
	private void setupBackdropManager() {
		List<DecorationProfile> decorationProfiles = new ArrayList<DecorationProfile>();
		
		TextureRegion starTextureRegion = new TextureRegion(spriteCache.getTexture(SpriteKey.STAR));
		DecorationProfile starProfile = new DecorationProfile(level.getBoundsBox(), true, 1, 0.5f, 0.02f, 0.1f, 
				starTextureRegion);
		decorationProfiles.add(starProfile);
		
		TextureRegion cloudTextureRegion = new TextureRegion(spriteCache.getTexture(SpriteKey.CLOUD));
		Rectangle cloudBoundsBox = level.getBoundsBox();
		// TODO: clean this calculation up
		float offsetY = cloudBoundsBox.height / 2;
		cloudBoundsBox.setY(cloudBoundsBox.y + offsetY);
		cloudBoundsBox.setHeight(cloudBoundsBox.height - offsetY);
		DecorationProfile cloudProfile = new DecorationProfile(cloudBoundsBox, false, 4, 0.75f, 3, 6, 1f, 2, 
				cloudTextureRegion);
		decorationProfiles.add(cloudProfile);
		
		backdropManager = new BackdropManager(Bound.LEFT, decorationProfiles);
	}
	
	private void addInputProcessors() {
		EscapeMenu escapeMenu = new EscapeMenu(Skins.defaultSkin, Skins.ocrFont, stage, screenManager, levelSession, 
				this, mainMenuScreen);
		gameInputProcessor = new GameInputProcessor(escapeMenu);
		Input.addProcessor(stage);
		Input.addProcessor(gameInputProcessor);
	}
	
	private void listenToGameEvents() {
		eventManager.listen(EntityAddedEvent.class, handleEntityAdded());
		eventManager.listen(EntityRemovedEvent.class, handleEntityRemoved());
	}
	
	private void setupStage() {
		Skin skin = Skins.defaultSkin;
		LabelStyle labelStyle = Skins.ocrStyle;
		worldTable = createWorldTable(skin);
		Table statusTable = createStatusTable(skin, labelStyle);
		Table mainTable = createMainTable(skin, worldTable, statusTable);
		stage.addActor(mainTable);
	}
	
	private Table createWorldTable(Skin skin) {
		Table worldTable = new Table(skin);
		return worldTable;
	}
	
	private Table createStatusTable(Skin skin, LabelStyle labelStyle) {
		Table statusTable = new Table(skin);
		healthLabel = new Label("", labelStyle);
		statusTable.add(healthLabel).expandX().left();
		statusTable.row();
		scoreLabel = new Label("", labelStyle);
		statusTable.add(scoreLabel).left();
		return statusTable;
	}
	
	private Table createMainTable(Skin skin, Table worldTable, Table statusTable) {
		Table mainTable = new Table(skin).top().left();
		mainTable.setFillParent(true);
		mainTable.add(worldTable).expand().fill();
		mainTable.row();
		mainTable.add(statusTable).expandX().fillX();
		mainTable.row();
		return mainTable;
	}
	
	private void createSystems() {
		entitySystems = new ArrayList<EntitySystem>();
		entitySystems.add(new BounceSystem(level.getBoundsBox()));
		entitySystems.add(new BoundPositionSystem(level.getBoundsBox()));
		entitySystems.add(new CollisionDamageSystem(collisionManager));
		entitySystems.add(new CityDamageSystem(level.getBoundsBox(), levelSession));
		entitySystems.add(new EmitSystem(entityManager));
		entitySystems.add(new AIShooterSystem(entityManager));
		entitySystems.add(new InputMovementSystem());
		entitySystems.add(new RotateToCursorSystem(camera, worldTable, defaultScreenSize));
		entitySystems.add(new NoHealthSystem(entityManager));
		entitySystems.add(new OutOfBoundsRemoveSystem(level.getBoundsBox(), entityManager));
		entitySystems.add(new TimedDeathSystem(entityManager));
	}
	
	private void createInitialEntities() {
		Rectangle boundsBox = level.getBoundsBox();
		Entity ground = entityFactory.createBaseEntity(new Vector3(boundsBox.width, 0.1f, boundsBox.width), 
				new Vector2(boundsBox.x, boundsBox.y), SpriteKey.GREEN);
		entityManager.add(ground);
		Vector3 shooterSize = new Vector3(2, 1.5f, 1);
		TransformPart groundTransform = ground.get(TransformPart.class);
		Entity shooterCannon = entityFactory.createShooterCannon();
		entityManager.add(shooterCannon);
		float shooterX = VectorUtils.relativeMiddle(boundsBox.width / 2, shooterSize.x);
		Vector2 shooterPosition = new Vector2(shooterX, PolygonUtils.top(groundTransform.getBoundingBox()));
		shooter = entityFactory.createShooter(shooterSize, shooterPosition, shooterCannon);
		entityManager.add(shooter);
	}
	
	private void boundCursor() {
		if (Gdx.input.getX() > Gdx.graphics.getWidth()) {
			Gdx.input.setCursorPosition(Gdx.graphics.getWidth(), Gdx.input.getY());
		}
		if (Gdx.input.getX() < 0) {
			Gdx.input.setCursorPosition(0, Gdx.input.getY());
		}
		if (Gdx.input.getY() > Gdx.graphics.getHeight()) {
			Gdx.input.setCursorPosition(Gdx.input.getX(), Gdx.graphics.getHeight());
		}
		if (Gdx.input.getY() < 0) {
			Gdx.input.setCursorPosition(Gdx.input.getX(), 0);
		}
	}
	
	private void updateUI() {
		healthLabel.setText("HEALTH: " + levelSession.getHealth());
		scoreLabel.setText("SCORE: " + score);
	}
	
	private void handleInput() {
		if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
			if (shooter.hasActive(WeaponPart.class, AttachmentPart.class)) {
				WeaponPart weaponPart = shooter.get(WeaponPart.class);
				if (weaponPart.canSpawn()) {
					Entity bullet = weaponPart.createSpawn();
					Vector2 spawnPosition = getMiddleOfCannonMouth(bullet);
					bullet.get(TransformPart.class).setPosition(spawnPosition);
					Entity shooterCannon = shooter.get(AttachmentPart.class).getChild();
					Vector2 velocity = VectorUtils.createVectorFromAngle(shooterCannon.get(TransformPart.class).getRotation());
					bullet.get(TranslatePart.class).setVelocity(velocity);
					entityManager.add(bullet);
				}
			}
		}
	}
	
	private void updateWorld(final float delta) {
		collisionManager.checkCollisions(entityManager.getAll());
		backdropManager.update(delta);
		levelController.update(delta);
		updateEntities(delta);
	}
	
	private void updateEntities(final float delta) {
		for (Entity entity : entityManager.getAll()) {
			entity.update(delta);
			for (EntitySystem entitySystem : entitySystems) {
				entitySystem.update(delta, entity);
			}
		}
	}
	
	private void draw() {
		setWorldViewport();
		drawWorld();
		setUIViewPort();
		stage.draw();
		drawCursor();
	}
	
	private void setWorldViewport() {
		Rectangle worldTableRect = UIUtils.boundingBox(worldTable, defaultScreenSize);
		Gdx.gl.glViewport((int)worldTableRect.x, (int)worldTableRect.y, (int)worldTableRect.getWidth(), 
				(int)worldTableRect.getHeight());
	}
	
	private void drawWorld() {
		Gdx.gl.glClearColor(MIDNIGHT_BLUE.r, MIDNIGHT_BLUE.g, MIDNIGHT_BLUE.b, MIDNIGHT_BLUE.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		spriteBatch.setProjectionMatrix(camera.combined);
		spriteBatch.begin();
		backdropManager.draw(spriteBatch);
		List<Entity> entities = entityManager.getAll();
		Collections.sort(entities, new ZComparator());
		for (Entity entity : entities) {
			if (entity.hasActive(DrawablePart.class)) {
				DrawablePart drawablePart = entity.get(DrawablePart.class);
				drawablePart.getSprite().draw(spriteBatch);
			}
		}
		spriteBatch.end();
	}
	
	private void setUIViewPort() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	private void drawCursor() {
		spriteBatch.setProjectionMatrix(getUIMatrix());
		spriteBatch.begin();
		spriteBatch.draw(cursorTexture, Gdx.input.getX() - cursorTexture.getWidth() / 2, 
				Gdx.graphics.getHeight() - Gdx.input.getY() - cursorTexture.getHeight() / 2);
		spriteBatch.end();
	}
	
	private Matrix4 getUIMatrix() {
		Matrix4 uiMatrix = camera.combined.cpy();
		uiMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		return uiMatrix;
	}
	
	private Vector2 getMiddleOfCannonMouth(final Entity spawn) {
		Entity shooterCannon = shooter.get(AttachmentPart.class).getChild();
		TransformPart cannonTransform = shooterCannon.get(TransformPart.class);
		List<Vector2> vertices = cannonTransform.getTransformedVertices();
		TransformPart spawnTransform = spawn.get(TransformPart.class);
		Vector2 spawnPosition = VectorUtils.relativeEdgeMiddle(vertices.get(1), vertices.get(2), 
				spawnTransform.getSize().y);
		return spawnPosition;
	}
	
	private class ZComparator implements Comparator<Entity> {
		
	    @Override
	    public final int compare(final Entity e1, final Entity e2) {
	    	if (e1.hasActive(DrawablePart.class) && e2.hasActive(DrawablePart.class)) {
	    		return Float.compare(e1.get(DrawablePart.class).getZ(), e2.get(DrawablePart.class).getZ());
	    	}
	    	else {
	    		return 0;
	    	}
	    }
	    
	}

}