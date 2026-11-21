import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

// Given: the check for the homework. You do not change this file.
// Run it with check.bat in this folder  (Mac/Linux: sh check.sh).
// For each way it lets THREADS threads claim at the same moment, ROUNDS times, and counts how many
// were told they won. It also reads the way's source and refuses a way that uses the wrong tool.
public class Check {

    static final int THREADS = 12, ROUNDS = 20;
    static final String[] LOCK_TOOLS = { "synchronized", "Lock", "Semaphore" };
    static final String[] QUEUE_TOOLS = { "Queue", "newSingleThreadExecutor", "Exchanger" };
    static final String[] ATOMIC_TOOLS = { "compareAndSet", "Atomic", "putIfAbsent", "computeIfAbsent", "VarHandle", "compareAndExchange" };

    // One way: its file, how to build it, the tool it has to use and the tools it must not use.
    record Way(int number, String file, Supplier<Claimer> make, String[] needs, String needsText, String[][] forbids) {}

    static final List<Way> WAYS = List.of(
        new Way(1, "Way1.java", Way1::new, LOCK_TOOLS, "a lock (synchronized, a Lock or a Semaphore)", new String[][] {}),
        new Way(2, "Way2.java", Way2::new, QUEUE_TOOLS, "a queue (or a single-thread executor)", new String[][] {}),
        new Way(3, "Way3.java", Way3::new, ATOMIC_TOOLS, "compare-and-set (an Atomic class, putIfAbsent, a VarHandle)",
                new String[][] { LOCK_TOOLS, QUEUE_TOOLS }));

    // One round: THREADS players claim at once. Returns how many were told they won.
    static int round(Supplier<Claimer> make, int n) throws InterruptedException {
        Claimer race = make.get();
        CountDownLatch go = new CountDownLatch(1);
        List<String> winners = Collections.synchronizedList(new ArrayList<>());
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            String who = "player-" + n + "-" + i;
            Thread t = new Thread(() -> {
                try { go.await(); } catch (InterruptedException e) { return; }
                if (race.claim(who)) winners.add(who);
            });
            threads.add(t); t.start();
        }
        go.countDown();
        for (Thread t : threads) t.join();
        if (winners.size() == 1 && !winners.get(0).equals(race.winner()))
            System.out.println("  [FAIL]  round " + n + ": told " + winners.get(0) + " it won, but winner() says " + race.winner());
        return winners.size();
    }

    // The code of one way as text, comments stripped, so a comment cannot count as using a tool.
    static String codeOf(String file) throws Exception {
        return Files.readString(Path.of(file)).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    static boolean usesAny(String code, String[] tools) {
        for (String tool : tools) if (code.contains(tool)) return true;
        return false;
    }

    // What is wrong with the tools of this way, or null when they are right.
    static String toolProblem(Way way, String code) {
        if (!usesAny(code, way.needs())) return "has to use " + way.needsText();
        for (String[] forbidden : way.forbids()) if (usesAny(code, forbidden)) return "must not use a lock or a queue";
        return null;
    }

    // Run one way for ROUNDS rounds. Returns true when every round had exactly one winner.
    static boolean runWay(Way way) throws Exception {
        int bad = 0;
        for (int n = 1; n <= ROUNDS; n++) {
            int told = round(way.make(), n);
            if (told != 1) { bad++; System.out.println("  [FAIL]  way " + way.number() + ", round " + n + ": " + told + " players were told they won"); }
        }
        System.out.println(bad == 0 ? "  [ OK ]  way " + way.number() + ": exactly one winner in all " + ROUNDS + " rounds"
                                    : "  [FAIL]  way " + way.number() + ": " + bad + " of " + ROUNDS + " rounds were wrong");
        return bad == 0;
    }

    public static void main(String[] args) throws Exception {
        int failed = 0;
        for (Way way : WAYS) {
            System.out.println("\n--- way " + way.number() + "  (" + way.file() + ") ---");
            String problem = toolProblem(way, codeOf(way.file()));
            if (problem != null) { System.out.println("  [FAIL]  " + way.file() + " " + problem); failed++; continue; }
            if (!runWay(way)) failed++;
        }
        System.out.println(failed == 0 ? "\n  PASS  all three ways give exactly one winner" : "\n  " + failed + " of 3 ways are not right yet");
        System.exit(failed == 0 ? 0 : 1);
    }
}
