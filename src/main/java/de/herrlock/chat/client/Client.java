package de.herrlock.chat.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Properties;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.swing.JOptionPane;

import de.herrlock.chat.util.Constants;
import de.herrlock.chat.util.Messages;
import de.herrlock.chat.util.Messages.Message;
import de.herrlock.chat.util.Messages.Type;

public class Client {
    private static final InetAddress LOCAL;
    private static Properties p = new Properties();
    static {
        try ( InputStream in = new FileInputStream( "./client.properties" ) ) {
            p.load( in );
            LOCAL = InetAddress.getLocalHost();
        } catch ( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }

    public static void main( String... args ) {
        System.out.println( "> client started <" );
        try {
            login();
            try ( ClientServer cs = new ClientServer() ) {
                cs.start();
                inputAndSendMessages();
                cs.close();
            } catch ( IOException ex ) {
                System.err.println( "failed creating the ClientServer" );
                ex.printStackTrace();
            } finally {
                logout();
            }
        } catch ( ConnectException ex ) {
            throw new RuntimeException( ex );
        } finally {
            System.out.println( "> client finished <" );
        }
    }

    private static void inputAndSendMessages() throws ConnectException {
        try ( Scanner in = new Scanner( System.in, "UTF-8" ) ) {
            boolean end = false;
            while ( !end ) {
                String input = in.nextLine();
                if ( input != null && !input.isEmpty() && !input.equalsIgnoreCase( "qqq" ) ) {
                    // @formatter:off
                    Message message = Messages.getMessage(Type.SEND)
                            .setFrom(LOCAL.getHostAddress())
                            .setContent(input);
                    // @formatter:on
                    sendSocket( message );
                } else {
                    end = true;
                }
            }
        }
    }

    public static void sendSocket( Message msg ) throws ConnectException {
        try ( Socket socket = new Socket( p.getProperty( "hostname" ), Constants.SERVER_PORT ) ) {
            try ( JsonWriter writer = Json.createWriter( socket.getOutputStream() ) ) {
                // @formatter:off
                JsonObject sent = Json.createObjectBuilder()
                        .add("messageType", msg.getMessageType().toString())
                        .add("message", msg.getMessage())
                        .add("from", msg.getFrom())
                        .add("to", msg.getTo())
                        .build();
                // @formatter:on
                System.out.println( "sent: " + sent );
                writer.writeObject( sent );

                try ( JsonReader reader = Json.createReader( socket.getInputStream() ) ) {
                    byte success = ( byte ) reader.readObject().getInt( "success" );
                    switch ( success ) {
                        case Constants.RESPONSE_SUCCESS:
                            System.out.println( "Sending successful" );
                            break;
                        case Constants.RESPONSE_ERROR:
                            String msg1 = "Response contains errorcode (" + success + ")";
                            System.out.println( msg1 );
                            throw new RuntimeException( msg1 );
                        default:
                            String msg2 = "Response contains invalid code (" + success + ")";
                            System.out.println( msg2 );
                            throw new RuntimeException( msg2 );
                    }
                }
            }
        } catch ( ConnectException ex ) {
            String message = "Error while connecting. Probably the Server is not running.";
            JOptionPane.showMessageDialog( null, message, "Error", JOptionPane.ERROR_MESSAGE );
            throw ex;
        } catch ( IOException ex ) {
            throw new RuntimeException( ex );
        }
    }

    private static void login() throws ConnectException {
        sendSocket( Messages.getMessage( Type.LOGIN ) );
    }

    private static void logout() throws ConnectException {
        sendSocket( Messages.getMessage( Type.LOGOUT) );
    }

}
