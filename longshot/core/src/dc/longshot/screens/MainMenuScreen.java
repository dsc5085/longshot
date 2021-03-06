package dc.longshot.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import dc.longshot.eventing.EventDelegate;
import dc.longshot.eventing.NoArgsEvent;
import dc.longshot.eventing.NoArgsListener;
import dc.longshot.system.Input;
import dc.longshot.ui.UIConstants;
import dc.longshot.ui.UIPack;
import dc.longshot.ui.UIUtils;
import dc.longshot.util.InputUtils;

public final class MainMenuScreen implements Screen {

	private final EventDelegate<NoArgsListener> newGameClickedDelegate = new EventDelegate<NoArgsListener>();
	private final EventDelegate<NoArgsListener> optionsClickedDelegate = new EventDelegate<NoArgsListener>();
	private final EventDelegate<NoArgsListener> highScoresClickedDelegate = new EventDelegate<NoArgsListener>();

	private final UIPack uiPack;
	private final TextureRegion logoRegion;
	private Stage stage;
	
	public MainMenuScreen(final UIPack uiPack, final TextureRegion logoRegion) {
		this.uiPack = uiPack;
		this.logoRegion = logoRegion;
	}
	
	public final void addNewGameClickedListener(final NoArgsListener listener) {
		newGameClickedDelegate.listen(listener);
	}
	
	public final void addOptionsClickedListener(final NoArgsListener listener) {
		optionsClickedDelegate.listen(listener);
	}
	
	public final void addHighScoresClickedListener(final NoArgsListener listener) {
		highScoresClickedDelegate.listen(listener);
	}

	@Override
	public final void render(final float delta) {
		stage.act(delta);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public final void resize(final int width, final int height) {
	    stage.getViewport().update(width, height, true);
	}

	@Override
	public final void show() {
		stage = createStage();
		InputUtils.setCursorVisible(true);
		Input.addProcessor(stage);
	}

	@Override
	public final void hide() {
	}

	@Override
	public final void pause() {
	}

	@Override
	public final void resume() {
	}

	@Override
	public final void dispose() {
		Input.removeProcessor(stage);
		stage.dispose();
	}
	
	private ClickListener buttonClicked(final EventDelegate<NoArgsListener> clickedDelegate) {
		return new ClickListener() {
			@Override
			public final void clicked(final InputEvent event, final float x, final float y) {
				clickedDelegate.notify(new NoArgsEvent());
			}
		};
	}
	
	private ClickListener quitButtonClicked() {
		return new ClickListener() {
			@Override
			public final void clicked(final InputEvent event, final float x, final float y) {
				Gdx.app.exit();
			}
		};
	}
		
	private Stage createStage() {
		Stage stage = new Stage(new ScreenViewport());
		Table mainTable = createMainTable();
		stage.addActor(mainTable);
		return stage;
	}
	
	private Table createMainTable() {
		Table mainTable = uiPack.table();
		mainTable.defaults().spaceBottom(UIConstants.MENU_SPACE_BOTTOM);
		mainTable.setFillParent(true);
		mainTable.add(new Image(logoRegion)).spaceBottom(UIConstants.MENU_SPACE_BOTTOM * 2).row();
		mainTable.add(uiPack.button("New Game", buttonClicked(newGameClickedDelegate))).row();
		mainTable.add(uiPack.button("Options", buttonClicked(optionsClickedDelegate))).row();
		mainTable.add(uiPack.button("High Scores", buttonClicked(highScoresClickedDelegate))).row();
		mainTable.add(uiPack.button("Quit", quitButtonClicked())).row();
		UIUtils.setSameWidth(mainTable, Button.class);
		return mainTable;
	}
	
}
