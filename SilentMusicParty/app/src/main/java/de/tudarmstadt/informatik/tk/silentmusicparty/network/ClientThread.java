package de.tudarmstadt.informatik.tk.silentmusicparty.network;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

/**
 * Created by Mohit on 01.03.2017.
 */

public class ClientThread extends Thread {

    private Client client;
    private Socket socket;

    private ObjectInputStream ois;

    public ClientThread(Client client, Socket socket) {
        this.client = client;
        this.socket = socket;
    }

    @Override
    public void run() {
        super.run();
        try {
            ois = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Message m = (Message)ois.readObject();
                client.handle(m);
            } catch (IOException | ClassNotFoundException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
