package cn.nukkit.utils;

import cn.nukkit.Server;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

public class Watchdog extends Thread {

    private final Server server;
    private final long time;
    public boolean running;
    private boolean responding = true;

    public Watchdog(Server server, long time) {
        this.server = server;
        this.time = time;
        this.running = true;
        this.setName("Watchdog");
    }

    public void kill() {
        running = false;
        synchronized (this) {
            this.notifyAll(); // This is calling on this, the Watchdog thread object, which is OK since it's not a Thread instance waiting for thread state, it's an object monitor.
        }
    }

    @Override
    public void run() {
        while (this.running) {
            long current = server.getNextTick();
            if (current != 0) {
                long diff = System.currentTimeMillis() - current;
                if (!responding && diff > time * 2) {
                    System.exit(1); // Kill the server if it gets stuck on shutdown
                }
                if (server.isRunning() && diff > time) {
                    if (responding) {
                        MainLogger logger = this.server.getLogger();
                        logger.emergency("--------- Server stopped responding --------- (" + Math.round(diff / 1000d) + "s)");
                        logger.emergency("Please report this to Nukkit:");
                        logger.emergency(" - https://github.com/NukkitX/Nukkit/issues/new");
                        logger.emergency("---------------- Main thread ----------------");

                        dumpThread(ManagementFactory.getThreadMXBean().getThreadInfo(this.server.getPrimaryThread().getId(), Integer.MAX_VALUE), logger);

                        logger.emergency("---------------- All threads ----------------");
                        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                        for (int i = 0; i < threads.length; i++) {
                            if (i != 0) logger.emergency("------------------------------");
                            dumpThread(threads[i], logger);
                        }
                        logger.emergency("---------------------------------------------");
                        responding = false;
                        this.server.forceShutdown();
                    }
                } else {
                    responding = true;
                }
            }
            try {
                synchronized (this) {
                    this.wait(Math.max(time / 4, 1000));
                }
            } catch (InterruptedException ignore) {}
        }
    }

    private static void dumpThread(ThreadInfo thread, Logger logger) {
        logger.emergency("Current Thread: " + thread.getThreadName());
        logger.emergency("\tPID: " + thread.getThreadId() + " | Suspended: " + thread.isSuspended() + " | Native: " + thread.isInNative() + " | State: " + thread.getThreadState());
        // Monitors
        if (thread.getLockedMonitors().length != 0) {
            logger.emergency("\tThread is waiting on monitor(s):");
            for (MonitorInfo monitor : thread.getLockedMonitors()) {
                logger.emergency("\t\tLocked on:" + monitor.getLockedStackFrame());
            }
        }

        logger.emergency("\tStack:");
        for (StackTraceElement stack : thread.getStackTrace()) {
            logger.emergency("\t\t" + stack);
        }
    }
}
```

**Explanation:**  
The SonarQube warning is about calling `wait()`, `notify()`, or `notifyAll()` on `Thread` instances, which is dangerous because it pertains to the thread's internal JVM state rather than normal object monitors.

In the initial buggy code, the `notifyAll()` and `wait()` calls are on `this` which is a `Thread` subclass instance (`Watchdog extends Thread`). This triggers the warning because you're calling them on a Thread instance, not on a separate lock object.

**Fix:**  
Replace the monitor object for `wait()` and `notifyAll()` calls from `this` (the `Watchdog` thread instance) to another dedicated final lock object. This will avoid calling wait/notify on a `Thread` instance.

Here is the modification needed:

Add a private final Object lock:

private final Object lock = new Object();
```

Replace the synchronized blocks on `this` with synchronized blocks on `lock`, and call `lock.wait(...)` and `lock.notifyAll()` instead of on `this`.

Here is the corrected code with minimal changes applied:

package cn.nukkit.utils;

import cn.nukkit.Server;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;

public class Watchdog extends Thread {

    private final Server server;
    private final long time;
    public boolean running;
    private boolean responding = true;
    private final Object lock = new Object();

    public Watchdog(Server server, long time) {
        this.server = server;
        this.time = time;
        this.running = true;
        this.setName("Watchdog");
    }

    public void kill() {
        running = false;
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Override
    public void run() {
        while (this.running) {
            long current = server.getNextTick();
            if (current != 0) {
                long diff = System.currentTimeMillis() - current;
                if (!responding && diff > time * 2) {
                    System.exit(1); // Kill the server if it gets stuck on shutdown
                }
                if (server.isRunning() && diff > time) {
                    if (responding) {
                        MainLogger logger = this.server.getLogger();
                        logger.emergency("--------- Server stopped responding --------- (" + Math.round(diff / 1000d) + "s)");
                        logger.emergency("Please report this to Nukkit:");
                        logger.emergency(" - https://github.com/NukkitX/Nukkit/issues/new");
                        logger.emergency("---------------- Main thread ----------------");

                        dumpThread(ManagementFactory.getThreadMXBean().getThreadInfo(this.server.getPrimaryThread().getId(), Integer.MAX_VALUE), logger);

                        logger.emergency("---------------- All threads ----------------");
                        ThreadInfo[] threads = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true);
                        for (int i = 0; i < threads.length; i++) {
                            if (i != 0) logger.emergency("------------------------------");
                            dumpThread(threads[i], logger);
                        }
                        logger.emergency("---------------------------------------------");
                        responding = false;
                        this.server.forceShutdown();
                    }
                } else {
                    responding = true;
                }
            }
            try {
                synchronized (lock) {
                    lock.wait(Math.max(time / 4, 1000));
                }
            } catch (InterruptedException ignore) {}
        }
    }

    private static void dumpThread(ThreadInfo thread, Logger logger) {
        logger.emergency("Current Thread: " + thread.getThreadName());
        logger.emergency("\tPID: " + thread.getThreadId() + " | Suspended: " + thread.isSuspended() + " | Native: " + thread.isInNative() + " | State: " + thread.getThreadState());
        // Monitors
        if (thread.getLockedMonitors().length != 0) {
            logger.emergency("\tThread is waiting on monitor(s):");
            for (MonitorInfo monitor : thread.getLockedMonitors()) {
                logger.emergency("\t\tLocked on:" + monitor.getLockedStackFrame());
            }
        }

        logger.emergency("\tStack:");
        for (StackTraceElement stack : thread.getStackTrace()) {
            logger.emergency("\t\t" + stack);
        }
    }
}
```

This properly avoids calling `wait()` and `notifyAll()` on a `Thread` instance.