import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UDPServer {
    private static final int PORT = 9999;
    private static Map<String, ClientInfo> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);
                String message = new String(receivePacket.getData(), 0, receivePacket.getLength());

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientKey = clientAddress.toString() + ":" + clientPort;

                Thread clientHandler = new Thread(() -> handleClient(serverSocket, receivePacket, message, clientKey));
                clientHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(DatagramSocket serverSocket, DatagramPacket receivePacket, String message, String clientKey) {
        try {
            String[] parts = message.split(" ", 3);
            String command = parts[0];
            InetAddress clientAddress = receivePacket.getAddress();
            int clientPort = receivePacket.getPort();

            switch (command) {
                case "REGISTER":
                    String nickname = parts[1];
                    clients.put(clientKey, new ClientInfo(nickname, clientAddress, clientPort));
                    System.out.println(nickname + " registered.");
                    break;
                case "LIST":
                    String userList = String.join(", ", clients.values().stream().map(ClientInfo::getNickname).toArray(String[]::new));
                    byte[] listBytes = userList.getBytes();
                    DatagramPacket listPacket = new DatagramPacket(listBytes, listBytes.length, clientAddress, clientPort);
                    serverSocket.send(listPacket);
                    break;
                case "SENDTO":
                    String targetNick = parts[1];
                    String privateMessage = parts[2];
                    clients.values().stream()
                        .filter(client -> client.getNickname().equals(targetNick))
                        .findFirst()
                        .ifPresent(targetClient -> {
                            try {
                                byte[] messageBytes = privateMessage.getBytes();
                                DatagramPacket messagePacket = new DatagramPacket(messageBytes, messageBytes.length, targetClient.getAddress(), targetClient.getPort());
                                serverSocket.send(messagePacket);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    break;
                case "BROADCAST":
                    String broadcastMessage = parts[1];
                    byte[] broadcastBytes = broadcastMessage.getBytes();
                    for (ClientInfo client : clients.values()) {
                        if (!clientKey.equals(client.getKey())) {
                            DatagramPacket broadcastPacket = new DatagramPacket(broadcastBytes, broadcastBytes.length, client.getAddress(), client.getPort());
                            serverSocket.send(broadcastPacket);
                        }
                    }
                    break;
                case "QUIT":
                    ClientInfo clientInfo = clients.remove(clientKey);
                    if (clientInfo != null) {
                        System.out.println("Client disconnected: " + clientInfo.getNickname());
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClientInfo {
    private final String nickname;
    private final InetAddress address;
    private final int port;

    public ClientInfo(String nickname, InetAddress address, int port) {
        this.nickname = nickname;
        this.address = address;
        this.port = port;
    }

    public String getNickname() {
        return nickname;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getKey() {
        return address.toString() + ":" + port;
    }
}

