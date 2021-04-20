package main.java.threads;

import lombok.Getter;
import main.java.model.Params;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Paweł Marszał
 * Thread that represents elevator acting in the system
 */
@Getter
public class Elevator extends Thread {

    private final AtomicBoolean serverStillRunning;
    private final ReentrantLock lock;

    private final int ID;
    private final LinkedList<Integer> floorsQueue = new LinkedList<>();

    private int currentFloor = 0;
    private int nextFloor = 0;
    private boolean isMoving = false;

    public Elevator(int ID, AtomicBoolean serverStillRunning, ReentrantLock lock) {
        this.ID = ID;
        this.serverStillRunning = serverStillRunning;
        this.lock = lock;
    }

    public void addFloorLast(int floor) {
        addFloor(this.floorsQueue.size(), floor);
    }

    public void addFloor(int atIndex, int floor) {
        if (atIndex == 0) nextFloor = floor;
        this.floorsQueue.add(atIndex, floor);
    }

    public boolean willBeVisited(int floor) {
        return this.nextFloor == floor || this.floorsQueue.contains(floor);
    }

    public int totalDistance() {
        int dist = 0, lastFloor = this.currentFloor;

        for (int nextFloor: this.floorsQueue) {
            dist += Math.abs(nextFloor - lastFloor);
            lastFloor = nextFloor;
        }

        return dist;
    }

    @Override
    public void run() {
        try {
            while (serverStillRunning.get()) {

                // wait for available requests from ElevatorIssuer
                boolean requestsAvailable = false;
                do {
                    try {
                        lock.lock();
                        requestsAvailable = !floorsQueue.isEmpty();
                    } finally {
                        lock.unlock();
                    }
                    if (requestsAvailable) break;
                    safeSleep(Params.CHECK_SLEEP_TIME);
                } while (true);

                // start moving to destination currentFloor
                // floorsQueue will not be empty because only this thread can remove elements from it
                int distance = 0;
                try {
                    lock.lock();
                    nextFloor = floorsQueue.pollFirst();
                    distance = Math.abs(nextFloor - currentFloor);
                    isMoving = true;
                } finally {
                    lock.unlock();
                }

                // SLEEP WHILE MOVING
                safeSleep((long) distance * Params.TIME_TO_MOVE_ONE_FLOOR);

                // arrive at destination nextFloor
                try {
                    lock.lock();
                    isMoving = false;
                    currentFloor = nextFloor;
                    if (!floorsQueue.isEmpty()) nextFloor = floorsQueue.peekFirst();
                    System.out.println("\n[Elevator " + this.ID + "] arrived at floor " + this.currentFloor);
                    System.out.println("[Server] ");
                } finally {
                    lock.unlock();
                }

                // WAIT SOME TIME AFTER ARRIVING
                safeSleep(Params.SLEEP_AFTER_ARRIVAL);
            }
        } catch (RuntimeException ignored) {}

        System.out.println("[Elevator " + this.ID + "] thread ends");
    }

    /**
     * @param millis number of milliseconds to sleep
     * @throws RuntimeException breaks the main loop and allow thread to end gracefully
     */
    private void safeSleep(long millis) throws RuntimeException {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {}
        if (!serverStillRunning.get()) throw new RuntimeException("Server ended");
    }
}
