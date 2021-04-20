package main.java.model;

import lombok.Getter;
import lombok.NonNull;
import main.java.threads.Elevator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Class that decides where to send elevators
 */
public class ElevatorManager {

    @Getter
    private final ReentrantLock lock = new ReentrantLock();
    private final List<Elevator> elevators = new ArrayList<>();

    public ElevatorManager(int numberOfElevators, AtomicBoolean serverStillRunning) {
        for (int i = 0; i < numberOfElevators; i++) {
            elevators.add(i, new Elevator(i, serverStillRunning, this.lock));
        }

        elevators.forEach(Elevator::start);
    }

    public int askForElevator(int from, int to) {
        int chosenElevator = -1;
        try {
            this.lock.lock();
            chosenElevator = chooseElevator(from, to);
        } finally {
            this.lock.unlock();
        }

        return chosenElevator;
    }

    public List<ElevatorStatus> getStatus() {
        List<ElevatorStatus> status = new LinkedList<>();
        try {
            lock.lock();
            for (var elevator : elevators) {
                status.add(new ElevatorStatus(elevator.getID(), elevator.getCurrentFloor(), elevator.getNextFloor(), elevator.isMoving()));
            }
        } finally {
            lock.unlock();
        }
        return status;
    }

    // ---------------

    private Elevator findElevatorWithLowestCost(@NonNull List<Elevator> list) {
        if (list.isEmpty()) throw new IllegalArgumentException("Empty list cannot have smallest element");
        int lowestCost = Integer.MAX_VALUE;
        Elevator result = null;
        for (Elevator elevator: list) {
            int cost = elevator.totalDistance();
            if (cost < lowestCost) {
                lowestCost = cost;
                result = elevator;
            }
            if (cost == 0) break;
        }
        return result;
    }

    private Elevator findElevatorClosestToFloor(@NonNull List<Elevator> list, int floor) {
        if (list.isEmpty()) throw new IllegalArgumentException("Empty list");
        int lowestDistance = Integer.MAX_VALUE;
        Elevator result = null;
        for (Elevator elevator: list) {
            int dist = Math.abs(elevator.getCurrentFloor() - floor);
            if (dist < lowestDistance) {
                lowestDistance = dist;
                result = elevator;
            }
            if (dist == 0) break;
        }
        return result;
    }

    private int findInsertionWithLowestCost(int floorToInsert, @NonNull Elevator elevator, Integer floorRequiredBefore) {
        //if (floorRequiredBefore != null && !elevator.willBeVisited(floorRequiredBefore)) return -1;
        int minimalAdditionalCost = Integer.MAX_VALUE;
        int insertionIndex = -1;
        int lastFloor = elevator.getCurrentFloor();

        int i = 0;
        for (int newFloor: elevator.getFloorsQueue()) {
            if (floorRequiredBefore != null) {
                if (newFloor == floorRequiredBefore)
                    floorRequiredBefore = null;
                else
                    continue;
            }

            int additionalCost;

            // target floor above last and next floor
            if (floorToInsert > Math.max(lastFloor, newFloor)) {
                additionalCost = 2 * Math.abs(floorToInsert - Math.max(lastFloor, newFloor));
            }
            // target floor below last and next floor
            else if (floorToInsert < Math.min(lastFloor, newFloor)) {
                additionalCost = 2 * Math.abs(floorToInsert - Math.min(lastFloor, newFloor));
            }
            // target floor between the two - cost is exactly zero
            else {
                additionalCost = 0;
            }

            // if better position has been found
            if (additionalCost < minimalAdditionalCost) {
                minimalAdditionalCost = additionalCost;
                insertionIndex = i;
            }
            // cost cannot get lower, so end the loop
            if (minimalAdditionalCost == 0) break;

            lastFloor = newFloor;
            i++;
        }

        // if there was no position found or the end of the list is just optimal
        if (insertionIndex == -1 || Math.abs(floorToInsert - lastFloor) < minimalAdditionalCost) {
            insertionIndex = i;
        }

        return insertionIndex;
    }


    private int chooseElevator(int from, int to) {
        Elevator chosenElevator;

        // get elevators that are currently staying on floor 'from'
        List<Elevator> elevatorsThatVisitDepartureFloor = this.elevators.stream()
                .filter(e -> !e.isMoving())
                .filter(e -> e.getCurrentFloor() == from)
                .collect(Collectors.toList());

        // if there are elevators that are or will be on floor 'from', pick one from them
        if (!elevatorsThatVisitDepartureFloor.isEmpty()) {
            // check for elevator that is planning to ride to floor 'to'
            chosenElevator = elevatorsThatVisitDepartureFloor.stream()
                    .filter(e -> e.willBeVisited(to)).findFirst().orElse(null);
            if (chosenElevator != null)
                return chosenElevator.getID();

            chosenElevator = findElevatorWithLowestCost(elevatorsThatVisitDepartureFloor);
            chosenElevator.addFloor(findInsertionWithLowestCost(to, chosenElevator, null), to);
            return chosenElevator.getID();
        }

        // if not, take those that are coming to floor 'from'
        elevatorsThatVisitDepartureFloor = this.elevators.stream()
                .filter(e -> e.willBeVisited(from)).collect(Collectors.toList());
        if (!elevatorsThatVisitDepartureFloor.isEmpty()) {
            chosenElevator = findElevatorWithLowestCost(elevatorsThatVisitDepartureFloor);
            chosenElevator.addFloor(findInsertionWithLowestCost(to, chosenElevator, null), to);
            return chosenElevator.getID();
        }

        // if not, take elevator without requested floors
        List<Elevator> inactiveElevators = this.elevators.stream()
                .filter(e -> !e.isMoving() && e.getFloorsQueue().isEmpty()).collect(Collectors.toList());
        if (!inactiveElevators.isEmpty()) {
            chosenElevator = findElevatorClosestToFloor(inactiveElevators, from);
            chosenElevator.addFloorLast(from);
            chosenElevator.addFloorLast(to);
            return chosenElevator.getID();
        }

        // if not, take elevator with lowest cost
        chosenElevator = findElevatorWithLowestCost(this.elevators);
        chosenElevator.addFloorLast(from);
        chosenElevator.addFloorLast(to);
        return chosenElevator.getID();
    }
}
