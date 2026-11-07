import javax.microedition.lcdui.Graphics;

abstract class App {
    abstract void paint(Graphics g, Hub h);
    abstract void key(int k, Hub h);
    void enter(Hub h) {}
    void leave(Hub h) {}
    void pause(Hub h) {}
    void resume(Hub h) {}
    boolean tick(Hub h, long now) { return false; }
    int tickMs() { return 0; }
    boolean wall() { return true; }
    boolean live() { return false; }
    boolean repeat() { return false; }
}
