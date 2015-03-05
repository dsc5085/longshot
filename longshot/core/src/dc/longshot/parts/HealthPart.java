package dc.longshot.parts;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import dc.longshot.eventmanagement.EventDelegate;
import dc.longshot.eventmanagement.NoArgsEvent;
import dc.longshot.eventmanagement.NoArgsListener;

@XmlRootElement
public final class HealthPart {

	private final EventDelegate<NoArgsListener> noHealthDelegate = new EventDelegate<NoArgsListener>();

	@XmlElement
	private float maxHealth;
	@XmlElement
	private float health;
	
	public HealthPart() {
	}
	
	public final void addNoHealthListener(final NoArgsListener listener) {
		noHealthDelegate.listen(listener);
	}
	
	public final void reset() {
		health = maxHealth;
	}
	
	public final void decrease(final float value) {
		health -= value;
		if (health <= 0) {
			noHealthDelegate.notify(new NoArgsEvent());
		}
	}
	
}
