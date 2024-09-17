package love.DingLi.events;

import com.darkmagician6.eventapi.events.Event;

public class Render3DEvent implements Event {

    private final float renderPartialTicks;

    public Render3DEvent(float renderPartialTicks) {
        this.renderPartialTicks = renderPartialTicks;
    }

    public float getRenderPartialTicks() {
        return renderPartialTicks;
    }
}
