import org.junit.Test;
import static org.junit.Assert.*;
import io.github.beidouxiaonan.carconnect.startup.StartupState;

public class StartupStateTest {
    @Test public void firstLaunchAndFailedBootNeverAutoRetry() {
        for (String state : new String[] {null, "first", "launching", "failed", "unknown"})
            assertFalse(StartupState.shouldAutoStart(state, false, true));
    }
    @Test public void onlyHealthyPreviousLaunchCanAutoStart() {
        assertTrue(StartupState.shouldAutoStart("ready", false, true));
        assertFalse(StartupState.shouldAutoStart("ready", true, true));
        assertFalse(StartupState.shouldAutoStart("ready", false, false));
    }
}
