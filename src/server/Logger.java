package server;

import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Logger class to handle logging messages for each chat room. 
 * It ensures thread safety when writing to log files and manages log file creation and access.
 */
public class Logger {
    
    private final ReentrantLock lock = new ReentrantLock();

    private final ConcurrentHashMap<String, PrintWriter> writers = new ConcurrentHashMap<>();
    private static final String LOG_DIR = "logs";


    public void log(String roomName, String message) {
        lock.lock();
        try {
            PrintWriter writer = writers.computeIfAbsent(roomName, name -> {
                try {
                  File dir = new File(LOG_DIR);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileWriter fw = new FileWriter(LOG_DIR + "/" + name + ".log", true);
                return new PrintWriter(fw, true);  
                } catch (IOException e) {
                    System.err.println("Could not open log file for room: " + name);
                    return null;
                }
            });

            if (writer != null) {
                writer.println(message);
            }
        } finally {
            lock.unlock();
        }
    }

    public void close() {
        writers.values().forEach(writer -> {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        });
    }

}
