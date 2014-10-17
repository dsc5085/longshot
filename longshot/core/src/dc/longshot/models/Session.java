package dc.longshot.models;

import dc.longshot.system.ExecutionState;

public final class Session {

	private ExecutionState executionState = ExecutionState.RUNNING;
	private float health = 5;
	
	public final ExecutionState getExecutionState() {
		return executionState;
	}
	
	public final void setExecutionState(ExecutionState executionState) {
		this.executionState = executionState;
	}
	
	public final void toggleExecutionState() {
		switch (executionState) {
		case RUNNING:
			executionState = ExecutionState.PAUSED;
			break;
		case PAUSED:
			executionState = ExecutionState.RUNNING;
			break;
		}
	}
	
	public final float getHealth() {
		return health;
	}
	
	public final void decreaseHealth(final float damage) {
		health -= damage;
	}
	
}
