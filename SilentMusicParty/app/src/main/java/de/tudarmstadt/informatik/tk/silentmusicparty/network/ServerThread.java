package de.tudarmstadt.informatik.tk.silentmusicparty.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import de.tudarmstadt.informatik.tk.silentmusicparty.entities.Person;
import de.tudarmstadt.informatik.tk.silentmusicparty.services.NetworkService;

/**
 * Created by Mohit on 01.03.2017.
 */

public class ServerThread extends Thread {

    private Server server;
    private Socket socket;

    private ObjectInputStream ois;
    private ObjectOutputStream oos;

    public ServerThread(Server server, Socket socket) {
        super();
        this.server = server;
        this.socket = socket;

        try {
            this.ois = new ObjectInputStream(socket.getInputStream());
            this.oos = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        super.run();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message m = (Message) ois.readObject();
                server.handle(m);
            } catch (IOException | ClassNotFoundException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendMessage(Message m) {
        try {
            oos.writeObject(m);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
