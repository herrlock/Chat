package de.herrlock.chat.util;

public class Messages {
    public static Message getMessage(Type t) {
        switch (t) {
            case Login:
                return new Login();
            case Logout:
                return new Logout();
            case Send:
                return new Send();
            default:
                throw new IllegalArgumentException(t.toString());
        }
    }

    public static abstract class Message {
        protected String from = "", to = "", message = "";

        public abstract Type getMessageType();

        public String getFrom() {
            return this.from;
        }

        public String getMessage() {
            return this.message;
        }

        public String getTo() {
            return this.to;
        }

        public Message setFrom(String from) {
            this.from = from;
            return this;
        }

        public Message setContent(@SuppressWarnings("unused") String msg) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "[" + this.getTo() + " > " + this.getTo() + "] " + this.getMessage();
        }
    }

    public static enum Type {
        Login, Send, Logout;

        public static Type determineType(String name) {
            for (Type t : Type.values()) {
                if (t.toString().equals(name)) {
                    return t;
                }
            }
            return null;
        }
    }

    static class Login extends Message {
        public Login() {
            this.message = "Hello";
        }

        @Override
        public Type getMessageType() {
            return Type.Login;
        }
    }

    static class Logout extends Message {
        public Logout() {
            this.message = "Bye";
        }

        @Override
        public Type getMessageType() {
            return Type.Logout;
        }
    }

    static class Send extends Message {
        @Override
        public Type getMessageType() {
            return Type.Send;
        }

        @Override
        public Message setContent(String content) {
            String[] split = content.split(" ", 2);
            String ip = split[0];
            if (ip.matches("\\d{3}\\.\\d{3}\\.\\d{3}\\.\\d{3}")) {
                this.to = ip;
            }
            else if (ip.matches("\\d{3}")) {
                this.to = "192.168.2." + ip;
            }
            else {
                throw new RuntimeException(ip);
            }
            this.message = split[1];
            return this;
        }
    }
}
