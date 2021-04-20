package main.java.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author Paweł Marszał
 * This class represents status of specific elevator
 */
@AllArgsConstructor
@Getter
public class ElevatorStatus {

    private final int ID;
    private final int currentFloor;
    private final int nextFloor;
    private final boolean isMoving;

    @Override
    public String toString() {
        String information = "[Elevator " + ID + "] ";
        if (isMoving) {
            information += "is moving from floor " + currentFloor + " to floor " + nextFloor;
        }
        else {
            information += "is currently at floor " + currentFloor;
            if (nextFloor != currentFloor) {
                information += " and later moves to floor " + nextFloor;
            }
        }
        return information;
    }
}
