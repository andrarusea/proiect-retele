package client;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {
	private static Scanner scanner = new Scanner(System.in);

	public static void main(String[] args) {
		System.out.print("Username: ");
		
		try(Socket socket = new Socket("localhost", 8000)) {
			new Thread(() -> {
				try {
					while(true) {
						InputStream inputStream = socket.getInputStream();
						DataInputStream dataInputStream = 
								new DataInputStream(inputStream);
						String receivedMessage = dataInputStream.readUTF();
						System.out.println(receivedMessage);
					}
				} catch(Exception e) {
					e.printStackTrace();
				}
			}).start();
			
			while(true) {
				String text = scanner.nextLine();
				
				OutputStream outputStream = socket.getOutputStream();
				ObjectOutputStream objectOutputStream = 
						new ObjectOutputStream(outputStream);
				objectOutputStream.writeObject(text);
			}	

		} catch(Exception e) {
			e.printStackTrace();
		}
		scanner.close();
	}
}
