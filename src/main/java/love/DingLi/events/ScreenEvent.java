package love.DingLi.events;



import com.darkmagician6.eventapi.events.Event;
import net.minecraft.client.gui.GuiScreen;

public class ScreenEvent implements Event {
    private GuiScreen guiScreen;

    public ScreenEvent(GuiScreen guiScreen) {
        this.guiScreen = guiScreen;
    }

    public GuiScreen getGuiScreen() {
        return this.guiScreen;
    }
}
