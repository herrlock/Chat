package de.herrlock.chat.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.swing.JOptionPane;

import de.herrlock.chat.util.Constants;
import de.herrlock.chat.util.Messages.Type;

class ClientServer extends Thread implements Closeable {
    private final ServerSocket s = new ServerSocket(Constants.CLIENT_PORT);

    public ClientServer() throws IOException {
        super("ClientServer");
    }

    @Override
    public void run() {
        boolean end = false;
        while (!end) {
            try {
                Socket socket = this.s.accept();
                new SocketHandler(socket).run();
            }
            catch (SocketException ex) {
                end = true;
            }
            catch (RuntimeException ex) {
                end = true;
                throw ex;
            }
            catch (IOException ex) {
                end = true;
                throw new RuntimeException(ex);
            }
        }
    }

    public void printMessage(JsonObject object) {
        String msg = getMessage(object);
        System.out.println(msg);
        JOptionPane.showMessageDialog(null, msg);
    }

    private static String getMessage(JsonObject object) {
        String from = object.getString("from");
        String message = object.getString("message");
        return "[ > " + from + "] " + message;
    }

    @Override
    public void close() throws IOException {
        if (this.s != null && !this.s.isClosed())
            this.s.close();
    }

    class SocketHandler {
        private final Socket socket;
        private JsonObject json;

        public SocketHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.json = Json.createReader(this.socket.getInputStream()).readObject();
        }

        public void run() throws IOException {
            if (Type.determineType(this.json.getString("messageType")) == Type.SEND) {
                ClientServer.this.printMessage(this.json);
                // @formatter:off
                JsonObject response = Json.createObjectBuilder()
                        .add("success", Constants.RESPONSE_SUCCESS)
                        .build();
                // @formatter:on
                Json.createWriter(this.socket.getOutputStream()).writeObject(response);
            }
            else {
                throw new RuntimeException("wrong messageType in message");
            }
        }
    }
}
