package cn.nukkit.utils;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class ThreadedLogger implements Logger, Runnable {

    @Override
    public abstract void run();
}