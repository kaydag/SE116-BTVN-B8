// Given. What every way offers: many threads call claim() at the same moment, one is told true.
// You do not change this file.
public interface Claimer {

    // A busy server: the gap between reading and writing. Keep the call -- deleting it hides the bug.
    int PAUSE_MS = 20;

    // True for the one thread that is first, false for everybody else.
    boolean claim(String who);

    // Who is first, or null when nobody is.
    String winner();

    // Sleep PAUSE_MS: stands in for a busy server.
    static void pause() {
        try { Thread.sleep(PAUSE_MS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
