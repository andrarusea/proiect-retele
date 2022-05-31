package server;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

public class Server extends Thread {
	private static List<Socket> clients = new Vector<>();
	private Socket currentClient;

	private static Map<String, List<ArrayList<Character>>> clientsMap = new HashMap<>();

	public static List<List<Character>> tablaJoc;

	private static List<Character> capAvioane = new ArrayList<Character>(Arrays.asList('A', 'B', 'C'));
	private static List<Character> corpAvioane = new ArrayList<Character>(Arrays.asList('1', '2', '3'));

	private static Map<String, Integer> punctajJucatori = new HashMap<>();

	private static Map<String, Socket> nameSocketMap = new HashMap<>();

	public Server(Socket currentClient) {
		this.currentClient = currentClient;
	}

	private String receive() throws Exception {
		InputStream inputStream = currentClient.getInputStream();
		ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
		String message = (String) objectInputStream.readObject();
		return message;
	}

	private void send(String message) throws Exception {
		OutputStream outputStream = currentClient.getOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
		dataOutputStream.writeUTF(message);
	}

	private void send(String message, Socket client) throws Exception {
		OutputStream outputStream = client.getOutputStream();
		DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
		dataOutputStream.writeUTF(message);
	}

	private void setareMatrice(Integer linie, Integer coloana, String nume, Character valoareTabla) {
		clientsMap.get(nume).get(linie).set(coloana, tablaJoc.get(linie).get(coloana));

		if (capAvioane.contains(valoareTabla)) {
			if (valoareTabla == 'A')
				descoperireAvion('1', valoareTabla, nume);
			else if (valoareTabla == 'B')
				descoperireAvion('2', valoareTabla, nume);
			else
				descoperireAvion('3', valoareTabla, nume);
		}
	}

	private void descoperireAvion(Character cifra, Character valoareTabla, String nume) {
		for (var i = 0; i < 10; i++) {
			for (var j = 0; j < 10; j++) {
				if (tablaJoc.get(i).get(j) == cifra) {
					clientsMap.get(nume).get(i).set(j, cifra);
				}
			}
		}
		punctajJucatori.put(nume, punctajJucatori.get(nume) + 1);
	}

	private void sendMatrice(String nume) throws Exception {
		send("   01 02 03 04 05 06 07 08 09 10");
		var contor = 1;
		for (var linie : clientsMap.get(nume)) {
			var outputString = contor == 10 ? contor + " " + linie.toString() : "0" + contor + " " + linie.toString();
			send(outputString);
			contor++;
		}
	}

	private void sendMatrice(String nume, Socket socket) throws Exception {
		send("   01 02 03 04 05 06 07 08 09 10", socket);
		var contor = 1;
		for (var linie : clientsMap.get(nume)) {
			var outputString = contor == 10 ? contor + " " + linie.toString() : "0" + contor + " " + linie.toString();
			send(outputString, socket);
			contor++;
		}
	}

	@Override
	public void run() {
		String userName = null;
		try {
			userName = receive();
			addJucator(userName);

			while (true) {

				Boolean aCastigat = false;
				while (!aCastigat) {
					sendMatrice(userName);
					send("Introduceti coordonatele la care vreti sa loviti - exemplu:linie,coloana");
					String coordonate = receive();
					Integer linie = Integer.parseInt(coordonate.split(",")[0]);
					Integer coloana = Integer.parseInt(coordonate.split(",")[1]);

					if (linie > 10 || coloana > 10 || linie < 1 || coloana < 1) {
						send("Coordonate in afara limitelor!");
					} else {
						Character valoareTabla = Server.tablaJoc.get(linie - 1).get(coloana - 1);
						if (capAvioane.contains(valoareTabla)) {
							send("X");
						} else if (corpAvioane.contains(valoareTabla)) {
							send("1");
						} else {
							send("0");
						}
						setareMatrice(linie - 1, coloana - 1, userName, valoareTabla);
					}
					if (punctajJucatori.get(userName) == 3) {
						send("Ai castigat!");
						aCastigat = true;
						for (var client : clients) {
							if (client != currentClient) {
								send("A castigat " + userName, client);
							}
						}
					}
				}
				chooseConfig();
				for (var client : clientsMap.keySet()) {
					initializeJucator(client);
				}
				if (nameSocketMap.size() > 1) {
					for (var client : nameSocketMap.keySet()) {
						if (nameSocketMap.get(client) != currentClient) {
							sendMatrice(client, nameSocketMap.get(client)); //
							send("Introduceti coordonatele la care vreti sa loviti - exemplu:linie,coloana",
									nameSocketMap.get(client));
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			clients.remove(currentClient);
			nameSocketMap.remove(userName);
		}
	}

	private void addJucator(String userName) throws Exception {
		if (clientsMap.containsKey(userName)) {
			send("Exista deja un jucator cu acest nume!");
			throw new Exception("Failed connection!");
		} else {
			initializeJucator(userName);
			nameSocketMap.put(userName, currentClient);
		}
	}

	private void initializeJucator(String userName) {
		List<ArrayList<Character>> tablaDefault = new ArrayList<ArrayList<Character>>();
		for (var i = 0; i < 10; i++) {
			var line = new ArrayList<Character>();
			for (var j = 0; j < 10; j++) {
				line.add('X');
			}
			tablaDefault.add(line);
		}
		clientsMap.put(userName, tablaDefault);
		punctajJucatori.put(userName, 0);
	}

	public static List<List<Character>> readConfigFile(String configFile) {
		BufferedReader br;
		List<List<Character>> config = new ArrayList<List<Character>>();
		try {
			br = new BufferedReader(new FileReader(configFile));
			String st;
			int line = 0;

			while ((st = br.readLine()) != null) {
				config.add(Arrays.asList(st.split("")).stream().map(str -> str.charAt(0)).toList());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return config;
	}

	public static void main(String[] args) {
		int port = 8000;

		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Server started on port " + port);
			chooseConfig();

			while (true) {
				Socket clientSocket = serverSocket.accept();
				clients.add(clientSocket);
				Server serverInstance = new Server(clientSocket);
				serverInstance.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void chooseConfig() {
		String configFile = "config" + new Random().nextInt(1, 6) + ".txt";
		System.out.println(configFile);
		Server.tablaJoc = readConfigFile(configFile);
	}

}
