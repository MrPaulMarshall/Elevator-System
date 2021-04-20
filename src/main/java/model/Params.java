package main.java.model;

/**
 * @author Paweł Marszał
 * This class is container for execution parameters of the system
 */
public class Params {
    public static final int serverCommunicationPort = 14141;

    public static final int MAX_ELEVATORS = 16;
    public static int MAX_FLOORS;

    // time (in milliseconds) needed to move 1 floor
    public static final int TIME_TO_MOVE_ONE_FLOOR = 3000;
    // time between consecutive checks if there is assigned target for this elevator
    public static final int CHECK_SLEEP_TIME = 2000;
    // time that elevator must spend still after arriving at given floor
    public static final int SLEEP_AFTER_ARRIVAL = 5000;
}
