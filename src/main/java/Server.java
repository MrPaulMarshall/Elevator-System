package main.java;

import main.java.model.ElevatorManager;
import main.java.model.ElevatorStatus;
import main.java.model.Message;
import main.java.model.Params;
import main.java.threads.FloorConnectionThread;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Paweł Marszał
 * Main class of this project
 * Process running Server::main registers client processes, communicates with them and calculate state of the system
 * It does so using many different threads, such like Elevators, RegisteringThread and FloorConnections
 */
public class Server {

    private static final AtomicBoolean serverStillRunning = new AtomicBoolean(true);
    private static final AtomicInteger floorCounter = new AtomicInteger(0);
    private static final FloorConnectionThread[] floorConnections = new FloorConnectionThread[Params.MAX_FLOORS];

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // parse limit on the number of floors
        System.out.print("[Server] Insert limit on number of floors: ");
        Params.MAX_FLOORS = scanner.nextInt();
        if (Params.MAX_FLOORS <= 0) {
            throw new IllegalArgumentException("Number of floors must be positive");
        }

        // parse number of elevators in the system
        int numberOfElevators;
        System.out.print("[Server] Insert number of elevators in the building: ");
        numberOfElevators = scanner.nextInt();
        if (numberOfElevators <= 0 || numberOfElevators > Params.MAX_ELEVATORS) {
            throw new IllegalArgumentException("Number of elevators must be between 1 and " + Params.MAX_ELEVATORS);
        }

        // clear scanner's buffer
        if (scanner.hasNextLine()) {System.out.println(scanner.nextLine());}

        // create Issuer
        ElevatorManager manager = new ElevatorManager(numberOfElevators, serverStillRunning);

        // start registering new FloorClient processes
        RegisteringThread registeringThread = new RegisteringThread(manager);
        registeringThread.start();

        // wait for user's input and react to it
        while (serverStillRunning.get()) {
            System.out.print("[Server] ");
            String line = scanner.nextLine();
            String[] tokens = line.split("\\s+");
            // ignore empty line
            if (tokens.length == 0 || tokens[0] == null) {
                System.out.println();
                continue;
            }
            if ("".equals(tokens[0])) continue;

            String cmd = tokens[0];
            switch (cmd) {
                case "exit":
                    serverStillRunning.set(false);

                    // kill registering thread
                    registeringThread.kill();

                    // order clients to exit
                    for (int i = 0; i < floorCounter.get(); i++) {
                        floorConnections[i].killClient();
                    }
                    break;
                case "status":
                    List<ElevatorStatus> status = manager.getStatus();
                    System.out.println("Status of the elevators");
                    for (ElevatorStatus elevatorStatus: status) {
                        System.out.println(elevatorStatus);
                    }
                    break;
                default:
                    System.out.println("Unrecognized command: " + cmd);
            }
        }
    }

    /**
     * Thread that listens for new FloorClient processes and registers them
     * When limit of floors is reached, new clients will be rejected by receiving negative ID
     */
    private static class RegisteringThread extends Thread {

        private ServerSocket serverSocket= null;
        private final ElevatorManager manager;

        private RegisteringThread(ElevatorManager manager) {
            this.manager = manager;
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(Params.serverCommunicationPort);
            } catch (IOException e) {
                serverStillRunning.set(false);
                try {
                    if (serverSocket != null) serverSocket.close();
                } catch (IOException ignored) {}
                return;
            }

            int newID;
            while (serverStillRunning.get()) {
                Socket socket = null;

                newID = floorCounter.intValue();
                try {
                    serverSocket.setSoTimeout(5000);
                    try {
                        socket = serverSocket.accept();
                    } catch (InterruptedIOException | SocketException ignored) {
                        continue;
                    }
                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());

                    // reject redundant clients
                    if (newID >= Params.MAX_FLOORS) {
                        out.writeObject(new Message("ID", -1));
                        try {
                            socket.close();
                        } catch (IOException ignored) {}
                        continue;
                    }

                    out.writeObject(new Message("ID", newID));

                    floorConnections[newID] = new FloorConnectionThread(
                            serverStillRunning, newID, socket, this.manager);
                    floorConnections[newID].start();
                    floorCounter.incrementAndGet();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    try {
                        if (socket != null) socket.close();
                    } catch (IOException ignored) {}
                }
            }

            this.kill();
            System.out.println("[Registering thread] ends");
        }

        public void kill() {
            try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
    }

}
