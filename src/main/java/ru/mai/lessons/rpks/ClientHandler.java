package ru.mai.lessons.rpks;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class ClientHandler {
    private Socket socket;
    private BufferedReader in;
    private BufferedWriter out;

    private String addr;
    private int port;

    private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    ClientController clientController;
    private volatile boolean running = true;

    private ReadMsg readMsg;
    private WriteMsg writeMsg;


    public ClientHandler(String locale, int port, ClientController clientController) {
        this.addr = locale;
        this.port = port;
        this.clientController = clientController;
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket fall");
        }
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            readMsg = new ReadMsg();
            writeMsg = new WriteMsg();
            readMsg.start();
            writeMsg.start();

        } catch (Exception e) {
            ClientHandler.this.downService();
        }
    }



    public void downService() {
        try {
            running = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            readMsg.interrupt();
            writeMsg.interrupt();

            System.out.println("Client disconnected");
        } catch (IOException ignored) {}
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            String str;
            try {
//                while (running && (str = in.readLine()) != null) {
//                    if (str.equals("exit")) {
//                        out.write("exit" + "\n");
//                        out.flush();
//                        ClientHandler.this.downService();
//                        break;
//                    }
//                    clientController.receiveMessage(str);
//                }
//                ClientHandler.this.downService();

                while (true) {
                    if (in.ready()) {
                        str = in.readLine();
                        if (str == null || str.equals("to") || str.equals("exit")) {
                            if (str == null) {
                                System.out.println("null");
                            } else if (str.equals("to")) {
                                System.out.println("have send to");
                            } else {
                                System.out.println("exit");
                            }
                            ClientHandler.this.downService();
                            break;
                        }

                        clientController.receiveMessage(str);
                    }
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public void addMessage(String message) {
        try {
            messageQueue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public class WriteMsg extends Thread {
        @Override
        public void run() {
            while (running) {
                try {
                    String message = messageQueue.take();
                    if (message.equals("exit")) {
                        out.write("exit" + "\n");
                        out.flush();
                        ClientHandler.this.downService();
                        break;
                    } else {
                        out.write(message + "\n");
                    }
                    out.flush();
                } catch (IOException | InterruptedException e) {
                    ClientHandler.this.downService();
                    break;
                }
            }

            ClientHandler.this.downService();

//            while (true) {
//
//                try{
//                    String message = messageQueue.take();////////////////////////////////////////////////тут может быть null
//
//                    if (message.isEmpty()) {
//                        continue;
//                    }
//
//                    if(message.equals("exit")){
//                        out.write("exit" + "\n");
//                        ClientHandler.this.downService();
//                        break;
//                    } else {
//                        out.write(message + "\n");
//                    }
//                    out.flush();
//                } catch (IOException | InterruptedException e) {
//                    ClientHandler.this.downService();
//                    break;
//                }
//            }
        }
    }
}


