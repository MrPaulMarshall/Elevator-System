package main.java.threads;

import main.java.model.Params;
import main.java.model.ElevatorManager;
import main.java.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paweł Marszał
 * Server side of the connection with FloorClient process
 */
public class FloorConnectionThread extends Thread {

    private final AtomicBoolean serverStillRunning;
    private final int floorNumber;
    private final ElevatorManager manager;

    private final Socket socket;
    private final ObjectOutputStream out;
    private final ObjectInputStream in;

    public FloorConnectionThread(AtomicBoolean serverStillRunning, int floorNumber,
                                 Socket socket, ElevatorManager manager) throws IOException {
        this.serverStillRunning = serverStillRunning;
        this.floorNumber = floorNumber;
        this.manager = manager;

        this.socket = socket;
        this.out = new ObjectOutputStream(socket.getOutputStream());
        this.in = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        while (serverStillRunning.get()) {
            try {
                Message clientMsg = (Message) in.readObject();

                if ("pickup".equals(clientMsg.type)) {
                    if (this.floorNumber == clientMsg.number) {
                        out.writeObject(new Message("result", "You are already on this floor"));
                    }
                    else if (clientMsg.number >= Params.MAX_FLOORS) {
                        out.writeObject(new Message("result",
                                "There is no floor " + clientMsg.number + " in this building"));
                    }
                    else {
                        out.writeObject(new Message("result",
                                "To get to floor " + clientMsg.number + " head to elevator "
                                        + manager.askForElevator(this.floorNumber, clientMsg.number)));
                    }
                } else {
                    throw new IllegalArgumentException("Unrecognized message type: " + clientMsg.type);
                }
            } catch (SocketException ignored) {
                break;
            } catch (ClassNotFoundException | IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        killClient();
        System.out.println("[Connection " + this.floorNumber + "] thread ends");
    }

    public void killClient() {
        try {
            this.out.writeObject(new Message("exit"));
            this.socket.close();
        } catch (IOException ignored) {}
    }
}
