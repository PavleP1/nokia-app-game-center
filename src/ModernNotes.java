import javax.microedition.midlet.MIDlet;
import javax.microedition.lcdui.Display;

public class ModernNotes extends MIDlet {
    private Hub hub;

    public void startApp() {
        if (hub == null) {
            Cfg.load();
            hub = new Hub(this);
            hub.start();
        }
        Display.getDisplay(this).setCurrent(hub);
        hub.onResume();
    }
    public void pauseApp() { if (hub != null) hub.stop(false); }
    public void destroyApp(boolean unconditional) { if (hub != null) hub.stop(true); }
    void exit() { destroyApp(true); notifyDestroyed(); }
    String prop(String k) { return getAppProperty(k); }
}
