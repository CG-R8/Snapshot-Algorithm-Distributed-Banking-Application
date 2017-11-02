import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

public class branch implements Runnable {
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	private static List<Bank.InitBranch.Branch> branchesList;
	private static int currentBranchBalance;
	private Thread t;

	public branch(Socket clientSocket2) {
		this.clientSocket = clientSocket2;
	}

	public void run() {
		{
		}
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}
	public void transfermoney() {
//		String randomBranchIp = branchesList.get(new Random().nextInt(bound));
//		Socket socket = new Socket(branch.getIp(), branch.getPort());
//		socket.getOutputStream().write(byteArray, 0, byteArray.length);
//		socket.close();
	}
	public static void main(String[] args) throws IOException {
		if (args.length < 2) {
			System.err.println("Wrong input....<branchname><Port>");
			System.exit(0);
		}
		try {
			String ipAddress = InetAddress.getLocalHost().getHostAddress();
			String branchName = args[0];
			int port = Integer.valueOf(args[1]);
			serverSocket = new ServerSocket(port);
			clientSocket = serverSocket.accept();
			System.out.println("Socket accepted....Port :"+port);
			Bank.BranchMessage bm = Bank.BranchMessage.parseFrom(clientSocket.getInputStream());
			if (bm.hasInitBranch()) {
				System.out.println("InitBranch Message Received....");
				currentBranchBalance = bm.getInitBranch().getBalance();
				 branchesList = bm.getInitBranch().getAllBranchesList();
				for (Bank.InitBranch.Branch branch : branchesList) {
					System.out.println(
							"Branch :" + branch.getName() + " IP :" + branch.getIp() + " Port :" + branch.getPort());
				}
			}
			if(bm.hasTransfer()) {
				System.out.println("Transfer Message Received....");

			}
			clientSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
