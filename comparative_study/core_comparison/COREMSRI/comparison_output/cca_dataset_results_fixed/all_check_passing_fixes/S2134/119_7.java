package cn.nukkit.utils;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ThreadedLogger extends Thread implements Logger {
    @Override
    public abstract void run();
}
