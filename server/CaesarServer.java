/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package socket.server;

import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;

public class CaesarServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        System.out.println("=== SERVER DA LUONG DANG KHOI DONG ===");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server dang lang nghe tai cong " + PORT + "...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\n[NEW CONNECTION] Client ket noi tu: " + clientSocket.getRemoteSocketAddress());
                
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread thread = new Thread(handler);
                thread.start(); 
            }
        } catch (IOException e) {
            System.err.println("Loi Server chinh: " + e.getMessage());
        }
    }
}

class ClientHandler implements Runnable {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        int sessionKey = 0; 
        String threadName = Thread.currentThread().getName();
        
        try (
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            while (true) {
                String clientRequest = in.readUTF();
                System.out.println("[" + threadName + "] Nhan lenh: " + clientRequest);

                if (clientRequest.equals("REQ_KEY_EXCHANGE")) {
                    out.writeUTF("READY_FOR_KEY");
                    out.flush();
                    
                    sessionKey = in.readInt();
                    System.out.println("[" + threadName + "] Da luu khoa cho phien nay K = " + sessionKey);
                    
                    out.writeUTF("KEY_ACCEPTED");
                    out.flush();
                } 
                else if (clientRequest.startsWith("ENCRYPTED_DATA:")) {
                    String cipherText = clientRequest.substring("ENCRYPTED_DATA:".length());
                    System.out.println("[" + threadName + "] Ban ma nhan duoc: " + cipherText + " (Su dung khoa K = " + sessionKey + ")");
                    
                    String plainText = decryptCaesar(cipherText, sessionKey);
                    System.out.println("[" + threadName + "] Ban ro sau giai ma: " + plainText);
                    
                    String report = countLetters(plainText);
                    
                    out.writeUTF(report);
                    out.flush();
                    
                    break; 
                } else {
                    out.writeUTF("ERROR: Lenh khong hop le!");
                    out.flush();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("[" + threadName + "] Khach hang ngat ket noi đot ngot.");
        } finally {
            try {
                socket.close();
                System.out.println("[" + threadName + "] Da dong socket va giai phong luong.\n");
            } catch (IOException e) {
                System.err.println("Loi đong socket luong: " + e.getMessage());
            }
        }
    }

    private String decryptCaesar(String text, int key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (Character.isLetter(ch)) {
                char base = Character.isUpperCase(ch) ? 'A' : 'a';
                ch = (char) ((ch - base - key + 26) % 26 + base);
            }
            result.append(ch);
        }
        return result.toString();
    }

    private String countLetters(String text) {
        Map<Character, Integer> countMap = new HashMap<>();
        String lowerText = text.toLowerCase(); 
        
        for (char ch : lowerText.toCharArray()) {
            if (Character.isLetter(ch)) {
                countMap.put(ch, countMap.getOrDefault(ch, 0) + 1);
            }
        }
        
        StringBuilder sb = new StringBuilder("=== KET QUA PHAN TICH TU SERVER ===\n");
        sb.append("Ban ro khoi phuc: \"").append(text).append("\"\n");
        sb.append("Thong ke chu cai:\n");
        if (countMap.isEmpty()) {
            sb.append("(Khong chua chu cai nao de thong ke)\n");
        } else {
            for (Map.Entry<Character, Integer> entry : countMap.entrySet()) {
                sb.append("  • Chu '").append(entry.getKey()).append("': ").append(entry.getValue()).append(" lan\n");
            }
        }
        return sb.toString();
    }
}
