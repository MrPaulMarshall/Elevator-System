package main.java;

import main.java.model.Params;
import main.java.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Paweł Marszał
 * Client process that represents device on given floor designed to pickup elevators
 */
public class FloorClient {

    private static final AtomicBoolean processStillRunning = new AtomicBoolean(true);
    private static int ID;

    public static void main(String[] args) {
        Socket socket = null;
        ObjectOutputStream out = null;
        try {
            // connect to unidirectional connection (server -> channel) and obtain ID
            socket = new Socket("localhost", Params.serverCommunicationPort);
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            try {
                Message idMsg = (Message) in.readObject();
                if (!"ID".equals(idMsg.type)) {
                    throw new IllegalStateException("First message from server must contain \"ID\"");
                }
                if (idMsg.number < 0) {
                    throw new IllegalStateException("Server didn't accept this client; assigned ID: " + idMsg.number);
                }
                ID = idMsg.number;
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Unrecognizable message from server");
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
            System.out.println("Program exits");
            System.exit(1);
        }

        Listener listener = new Listener(socket);
        listener.start();

        Scanner scanner = new Scanner(System.in);

        while (processStillRunning.get()) {
            printPrompt();
            String line = scanner.nextLine();
            String[] tokens = line.split("\\s+");
            if (tokens.length == 0 || tokens[0] == null || "".equals(tokens[0])) continue;

            try {
                if ("pickup".equals(tokens[0])) {
                    if (tokens.length != 2) throw new IllegalArgumentException("Command \"pickup\" gets 1 argument");
                    int newFloor = Integer.parseInt(tokens[1]);
                    if (newFloor < 0) {
                        throw new IllegalArgumentException("Floor ID cannot be negative");
                    }

                    out.writeObject(new Message("pickup", newFloor));
                } else {
                    throw new IllegalArgumentException("Unrecognized command: " + tokens[0]);
                }
            } catch (IOException | IllegalArgumentException e) {
                e.printStackTrace();
            }
        }

        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    private static void printPrompt() {
        System.out.print("[Floor " + ID + "] ");
    }

    /**
     * Secondary thread that listens to messages from server
     *  - "exit" causes FloorClient to stop and shutdown
     *  - "result" contain answer for user's request
     */
    private static class Listener extends Thread {

        private final Socket socket;

        public Listener(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

                while (processStillRunning.get()) {
                    try {
                        Message msg = (Message) in.readObject();
                        if (msg == null || msg.type == null)
                            throw new IllegalArgumentException("Received monolog message is empty");

                        if ("exit".equals(msg.type)) {
                            System.out.println("\n[Server] exit");
                            processStillRunning.set(false);
                            try {
                                socket.close();
                            } catch (IOException ignored) {}
                            System.exit(0);
                        }
                        else if ("result".equals(msg.type)) {
                            System.out.println("\n[Server] " + msg.text);
                        }
                        else {
                            throw new IllegalArgumentException("Unrecognized message type: " + msg.type);
                        }
                    } catch (ClassNotFoundException | IllegalArgumentException e) {
                        e.printStackTrace();
                        System.out.println();
                        printPrompt();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                processStillRunning.set(false);
            }

            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}
