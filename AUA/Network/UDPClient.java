import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UDPClient {
    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 9999;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket()) {
            InetAddress serverAddress = InetAddress.getByName(SERVER_ADDRESS);

            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter your nickname: ");
            String nickname = scanner.nextLine();
            sendMessage(clientSocket, serverAddress, "REGISTER " + nickname);

            Thread listenerThread = new Thread(() -> listenForMessages(clientSocket));
            listenerThread.start();

            while (true) {
                String command = scanner.nextLine();
                if (command.equals("QUIT")) {
                    sendMessage(clientSocket, serverAddress, "QUIT");
                    break;
                } else if (command.equals("LIST")) {
                    sendMessage(clientSocket, serverAddress, "LIST");
                } else if (command.startsWith("SENDTO")) {
                    System.out.print("Enter recipient nickname: ");
                    String recipientNick = scanner.nextLine();
                    System.out.print("Enter your message: ");
                    String message = scanner.nextLine();
                    sendMessage(clientSocket, serverAddress, "SENDTO " + recipientNick + " " + message);
                } else if (command.equals("BROADCAST")) {
                    System.out.print("Enter your message: ");
                    String message = scanner.nextLine();
                    sendMessage(clientSocket, serverAddress, "BROADCAST " + message);
                }
            }

            scanner.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(DatagramSocket socket, InetAddress address, String message) {
        try {
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listenForMessages(DatagramSocket socket) {
        try {
            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Message from server: " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

