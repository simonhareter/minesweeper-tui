import java.io.IOException;

public class TerminalUtils {

    public static void enableRawMode() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty raw </dev/tty"});
            p.waitFor();
        } catch (InterruptedException e) {
            System.err.println("Thread got interrupted");
        } catch (IOException e) {
            System.err.println("Error while running stty raw mode");
        }
    }

    public static void disableRawMode() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "stty cooked </dev/tty"});
            p.waitFor();
        } catch (InterruptedException e) {
            System.err.println("Thread got interrupted");
        } catch (IOException e) {
            System.err.println("Error while running stty cooked mode");
        }
    }

}
