package server;

public class RunningThreadCounter
{
    private volatile int numOfRunningThreads = 0;

    public synchronized void increment() {
    	numOfRunningThreads++;
    }

    public synchronized void decrement() {
    	numOfRunningThreads--;
    }

    public int getNumberOfRunningThreads() {
        return numOfRunningThreads;
    }
}
