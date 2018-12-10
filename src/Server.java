import java.net.*;
import java.io.*;
import java.util.*;
import javafx.application.Platform;

public class Server {

    private final String IPS_FILE = "ips.bin";
    
    private final String SHUTDOWN = "STOP";
    private final String KICK = "KICK";

    private ServerSocket serverSocket;
    private int port;
    private List users;
    private Set registeredIps;
    private ServerGUI gui;
    private Thread listening;

    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Avviare con");
            System.out.println("java Server [porta]");
            return;
        }
        Server server = new Server(Integer.parseInt(args[0]));
        server.start();
    }
    
    public Server(int port) {
        this(port, null);
    }

    public Server(int port, ServerGUI gui) {
        this.gui = gui;
        serverSocket = null;
        this.port = port;
        users = new ArrayList();
        registeredIps = new HashSet();
        loadUsers();
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            listening = new Thread(()->listen());
            listening.setDaemon(true);
            listening.start();
        } catch (IOException exception) {
            System.out.println("Errore creazione socket server");
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                return;
            }
            start();
        }
    }

    private void listen() {
        while (!Thread.interrupted()) {
            try {
                Socket sock = serverSocket.accept();
                String addr = sock.getInetAddress().toString() + ":" + sock.getPort();
                registeredIps.add(addr);
                User newUser = new User(addr, sock);
                users.add(newUser);
                newUser.start();
                broadcast(addr + " connected to server", addr);
            } catch(SocketException exception) {
                break;
            } catch (IOException exception) {
                System.out.println("Errore accettazione connessione");
            }
        }
    }

    private void broadcast(String message) {
        broadcast(message, "");
    }
    
    private void broadcast(String message, String sender) {
        for (int i = 0; i < users.size(); ++i) {
            User user = (User) users.get(i);
            if (user.getAddress().equals(sender)) {
                continue;
            }
            user.send(message);
        }
    }

    private void removeUser(String addr) {
        removeUser(addr, true);
    }
    
    private void removeUser(String addr, boolean disconnect) {
        for (int i = 0; i < users.size(); ++i) {
            User user = (User) users.get(i);
            if (user.getAddress().equals(addr)) {
                if (disconnect)
                    user.disconnect();
                users.remove(i);
                break;
            }
        }
    }

    private void shutdown() {
        close();
        for (int i = 0; i < users.size(); ++i) {
            User u = (User) users.get(i);
            u.disconnect();
            u.interrupt();
        }
        saveUsers();
        listening.interrupt();
        try {
            Thread.sleep(1);
        } catch(InterruptedException exception) {}
        if (gui != null)
            Platform.runLater(()->gui.exit());
    }

    private void kickOut(String address) {
        if (!registeredIps.contains(address))
            return;
        broadcast(address + " cacciato dalla chat");
        removeUser(address);
    }
    
    private String getUsersList() {
        String list = "";
        for(int i=0; i<users.size(); ++i) {
            User user = (User) users.get(i);
            list += (user.getAddress() + System.lineSeparator());
        }
        return list.trim();
    }
    
    public void execute(String command) {
        if (command.equals(SHUTDOWN)) {
            shutdown();
        } else if (command.startsWith(KICK)) {
            String[] params = command.split(" ");
            System.out.println(params[1]);
            kickOut(params[1]);
            if(gui != null)
                Platform.runLater(()->gui.log(params[1] + " cacciato dalla chat"));
            System.out.println(params[1] + " cacciato dalla chat");
        } else {
            broadcast("Server: " + command);
            if (gui != null)
                Platform.runLater(()->gui.log("Server: " + command));
            System.out.println("Server: " + command);
        }
    }

    private void close() {
        try {
            serverSocket.close();
        } catch (IOException exception) {
        }
    }

    private void saveUsers() {
        try (FileOutputStream fout = new FileOutputStream(IPS_FILE);
                ObjectOutputStream out = new ObjectOutputStream(fout)) {
            out.writeObject(registeredIps);
        } catch (FileNotFoundException exception) {
        } catch (IOException exception) {
        }
    }

    private void loadUsers() {
        try (FileInputStream fin = new FileInputStream(IPS_FILE);
                ObjectInputStream in = new ObjectInputStream(fin)) {
            registeredIps = (HashSet) in.readObject();
        } catch (IOException | ClassNotFoundException exception) {
        }
    }

    private final class User extends Thread {

        private String address;
        private Socket socket;
        private ObjectOutputStream out;
        private String name;

        public User(String addr, Socket socket) {
            this(addr, socket, addr);
        }
        
        public User(String addr, Socket socket, String name) {
            this.address = addr;
            this.socket = socket;
            this.name = name;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException exception) {
                disconnect();
            }
            if (gui != null)
                Platform.runLater(()->gui.log(addr + " Connected"));
            System.out.println(addr + " Connected");
            send("Benvenuto sul Server\nVersione 1.0");
            if (users.size() > 0) {
                send("Gli altri utenti online sono:\n"+
                     getUsersList());
            } else {
                send("Sei l'unico utente nella chat");
            }
        }

        public void send(String message) {
            try {
                out.writeObject(message);
            } catch (IOException exception) {
                System.out.println("Impossibile inviare il messaggio");
                disconnect();
            }
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                while (!interrupted()) {
                    String packet = address + ": " + (String) in.readObject();
                    if (gui != null)
                        Platform.runLater(()->gui.log(packet));
                    System.out.println(packet);
                    broadcast(packet, address);
                }
            } catch (IOException exception) { // Client si disconnette
                if (gui != null) {
                    Platform.runLater(()->gui.log("Eccezione nella comunicazione, " + address + " disconnesso"));
                }
                System.out.println("Eccezione nella comunicazione, " + address + " disconnesso");
            } catch (ClassNotFoundException exception) {
                System.out.println("Non convertibile in stringa");
            } finally {
                disconnect();
            }
        }

        public void disconnect() {
            try {
                out.close();
                socket.close();
            } catch (IOException exception) {
            }
            removeUser(address, false); // Rimuovo ma non disconnette
            broadcast(name + " Disconnected");
        }

        public String getAddress() {
            return address;
        }

    }

}
