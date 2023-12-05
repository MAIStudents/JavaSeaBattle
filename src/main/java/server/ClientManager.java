package server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;

public class ClientManager implements Runnable {

    private final Socket socket;

    public ClientManager(Socket socket) throws IOException {
        this.socket = socket;
    }

    public void sendMessage(String message) throws IOException {
        var outputStream = new PrintWriter(socket.getOutputStream());
        outputStream.println(message);
        outputStream.flush();
    }

    @Override
    public void run() {
        try {
            var inputStream = new Scanner(socket.getInputStream());
            while (!socket.isClosed()) {
                if (inputStream.hasNext()) {
                    String messageFromClient = inputStream.nextLine();
                    if (messageFromClient.startsWith("DISCONNECT")) {
                        if (messageFromClient.equals("DISCONNECT_NOTIFIED")) {
                            getOpponent().sendMessage("OPPONENT_DISCONNECTED");
                        }
                        if (equals(Server.clientOne)) {
                            Server.clientOne = null;
                        } else {
                            Server.clientTwo = null;
                        }
                        if (Server.clientOne == null && Server.clientTwo == null) {
                            Server.serverSocket.close();
                        }
                        socket.close();
                        continue;
                    }
                    getOpponent().sendMessage(messageFromClient);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    public ClientManager getOpponent() {
        return Server.getOpponent(this);
    }

    @Override
    public int hashCode() {
        return Objects.hash(socket);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ClientManager clientManager)) {
            return false;
        }
        return clientManager.socket.equals(socket);
    }

}
