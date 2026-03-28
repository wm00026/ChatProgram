package server;


import java.util.concurrent.locks.ReentrantLock;

/**
 * Logger
 * 
 * Stub for phase III implementation.
 */
public class Logger {
    
    private final ReentrantLock lock = new ReentrantLock();

    public void log(String roomName, String message) {
        lock.lock();
        try {

        } finally {
            lock.unlock();
        }
    }

    public void close() {
        
    }

}
