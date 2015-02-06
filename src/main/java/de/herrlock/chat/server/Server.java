package de.herrlock.chat.server;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.HashSet;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import de.herrlock.chat.util.Constants;
import de.herrlock.chat.util.Messages.Type;

public class Server implements Closeable {

    private final ServerSocket s = new ServerSocket( Constants.SERVER_PORT );

    public static void main( String... args ) throws IOException {
        try ( Server server = new Server() ) {
            System.out.println( "Server started - " + server );
            server.run();
            System.out.println( "Server finished - " + server );
        }
    }

    private Collection<InetAddress> clients = new HashSet<>();

    private Server() throws IOException {
        // nothing to initialize
    }

    public void run() {
        try {
            while ( true ) {
                Socket socket = this.s.accept();
                new SocketHandler( socket ).start();
            }
        } catch ( RuntimeException ex ) {
            ex.printStackTrace();
            throw ex;
        } catch ( IOException ex ) {
            ex.printStackTrace();
            throw new RuntimeException( ex );
        }
    }

    protected synchronized boolean addClient( InetAddress ip ) {
        return this.clients.add( ip );
    }

    protected synchronized boolean removeClient( InetAddress ip ) {
        return this.clients.remove( ip );
    }

    @Override
    public void close() throws IOException {
        if ( !this.s.isClosed() )
            this.s.close();
    }

    @Override
    public String toString() {
        return this.s.toString();
    }

    volatile int handlerIndex = 0;

    class SocketHandler extends Thread {

        private Socket socketFromSender;

        private InetAddress client;
        private JsonObject json;

        public SocketHandler( Socket socket ) throws IOException {
            super( String.valueOf( Server.this.handlerIndex++ ) );
            this.socketFromSender = socket;
            this.client = this.socketFromSender.getInetAddress();
            this.json = Json.createReader( this.socketFromSender.getInputStream() ).readObject();
        }

        @Override
        public void run() {
            try {
                boolean success = processSocket();
                // @formatter:off
                JsonObject response = Json.createObjectBuilder()
                        .add("success", success ? Constants.RESPONSE_SUCCESS : Constants.RESPONSE_ERROR)
                        .build();
                // @formatter:on
                Json.createWriter( this.socketFromSender.getOutputStream() ).writeObject( response );
            } catch ( IOException ex ) {
                throw new RuntimeException( ex );
            }
        }

        private boolean processSocket() {
            switch ( Type.determineType( this.json.getString( "messageType" ) ) ) {
                case LOGIN:
                    return login();
                case LOGOUT:
                    return logout();
                case SEND:
                    return processMessage();
                default:
                    return false;
            }
        }

        private boolean processMessage() {
            String from = this.json.getString( "from" );
            String to = this.json.getString( "to" );
            String message = this.json.getString( "message" );

            System.out.println( "  [" + from + " > " + to + "]" );
            System.out.println( "  message: " + message );

            try ( Socket socketToReceiver = new Socket( to, Constants.CLIENT_PORT ) ) {
                try ( JsonWriter writer = Json.createWriter( socketToReceiver.getOutputStream() ) ) {
                    writer.writeObject( this.json );
                    try ( JsonReader reader = Json.createReader( socketToReceiver.getInputStream() ) ) {
                        int success = reader.readObject().getInt( "success" );
                        switch ( success ) {
                            case Constants.RESPONSE_SUCCESS:
                                return true;
                            case Constants.RESPONSE_ERROR:
                                return false;
                            default:
                                throw new RuntimeException( "wrong responseCode: " + success );
                        }
                    }
                }
            } catch ( IOException ex ) {
                System.err.println( ex );
                return false;
            }
        }

        private boolean login() {
            boolean success = Server.this.addClient( this.client );
            loginoutTrace( success, "Login" );
            return success;
        }

        private boolean logout() {
            boolean success = Server.this.removeClient( this.client );
            loginoutTrace( success, "Logout" );
            return success;
        }

        private void loginoutTrace( boolean success, String x ) {
            String msg = success ? "Successful {0} from {1}" : "{0} failed ( {1} )";
            System.out.println( MessageFormat.format( msg, x, this.client.getHostAddress() ) );
        }

    }
}
