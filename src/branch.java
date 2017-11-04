import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Descriptors.FieldDescriptor;

public class branch {
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	private List<Bank.InitBranch.Branch> branchesList;
	private Thread t;
	private int balance = 0;
	public boolean canTransfer = true;
	private List<Integer> incomingChannelState = new ArrayList<Integer>();
	private ConcurrentHashMap<Integer, LocalStateSnapshot> snapshots = new ConcurrentHashMap<Integer, LocalStateSnapshot>();
	public int current_port;
	public String current_ipAddress;
	public String current_branchName;

	public branch(String branchName, String ipAddress, int port) {
		this.current_branchName = branchName;
		this.current_ipAddress = ipAddress;
		this.current_port = port;
	}

	private void initBranchMethod(Bank.BranchMessage bm) {
		System.out.println("InitBranch Message Received....");
		syncedBalanceUpdate(bm.getInitBranch().getBalance());
		branchesList = bm.getInitBranch().getAllBranchesList();
		for (Bank.InitBranch.Branch branch : branchesList) {
			System.out.println("Branch :" + branch.getName() + " IP :" + branch.getIp() + " Port :" + branch.getPort());
		}
		System.out.println("Current Balance is : " + bm.getInitBranch().getBalance());
		new Thread(new Runnable() {
			@Override
			public void run() {
				transfermoney();
			}
		}).start();
	}

	public void transfermoney() {
		Timer timer = new Timer();
		class transferClass extends TimerTask {
			@Override
			public void run() {
				int transferingMoney = 0;
				Random rand = new Random();
				int randomPercentage = rand.nextInt((5 - 1) + 1) + 1;
				if (balance > 0)
					transferingMoney = balance * randomPercentage / 100;
				else {
					transferingMoney = -1;
					canTransfer = false;
				}
				try {
					if (transferingMoney != -1) {
						syncedBalanceUpdate(-transferingMoney);
						Bank.InitBranch.Branch randomBranch = getRandomBranch();
						Bank.Transfer bt = Bank.Transfer.newBuilder().setMoney(transferingMoney).build();
						Bank.BranchMessage.newBuilder().setTransfer(bt).build();
						// byte[] byteArrayBalanceTransfer =
						// Bank.BranchMessage.newBuilder().setTransfer(bt).build()
						// .toByteArray();
						System.out.println("Balance :" + transferingMoney + " transfering to IP: "
								+ randomBranch.getIp() + " Port:" + randomBranch.getPort());
						Socket socket = new Socket(randomBranch.getIp(), randomBranch.getPort());
						Bank.BranchMessage.newBuilder().setTransfer(bt).build()
								.writeDelimitedTo(socket.getOutputStream());
						// socket.getOutputStream().write(byteArrayBalanceTransfer, 0,
						// byteArrayBalanceTransfer.length);/s
						socket.getOutputStream().close();
						socket.close();
						if (!canTransfer) {
							timer.cancel();
						} else {
							// timer.schedule(new transferClass(), (rand.nextInt(5) * 1000));
							timer.schedule(new transferClass(), (4 * 1000));
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Can not transfer money");
				}
			}
		}
		;
		new transferClass().run();
	}

	public synchronized Bank.InitBranch.Branch getRandomBranch() {
		Random rand = new Random();
		return branchesList.get(rand.nextInt((branchesList.size() - 1) + 1) + 0);
	}

	public synchronized void syncedBalanceUpdate(int money) {
		balance = balance + money;
	}

	private void initSnapshot(int snapshot_id) throws UnknownHostException, IOException, InterruptedException {
		incomingChannelState.clear();
		LocalStateSnapshot localSnapshot = new LocalStateSnapshot();
		localSnapshot.setSnapshot_id(snapshot_id);
		localSnapshot.setBalance(balance);
		localSnapshot.setMessages(this.incomingChannelState);
		snapshots.put(snapshot_id, localSnapshot);

		for (Bank.InitBranch.Branch branch : branchesList) {
			// TODO possible to have different ip same port
			if (branch.getPort() != current_port) {
				Thread.sleep(4000);
				Bank.Marker bMarker = Bank.Marker.newBuilder().setSnapshotId(snapshot_id).build();
				System.out.print("--Sending marker msg SnapshotId :" + snapshot_id);
				System.out.println(" IP: " + branch.getIp() + " Port:" + branch.getPort());
				Socket socket = new Socket(branch.getIp(), branch.getPort());
				Bank.BranchMessage.newBuilder().setMarker(bMarker).build().writeDelimitedTo(socket.getOutputStream());
				socket.getOutputStream().write(current_branchName.getBytes());
				socket.getOutputStream().close();
				socket.close();
			}
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 2) {
			System.err.println("Wrong input....<branchname><Port>");
			System.exit(0);
		}
		try {
			String ipAddress = InetAddress.getLocalHost().getHostAddress();
			String branchName = args[0];
			int port = Integer.valueOf(args[1]);
			branch br = new branch(branchName, ipAddress, port);
			serverSocket = new ServerSocket(port);
			System.out.println("Branch Server started on Port : " + port);
			while (true) {
				clientSocket = serverSocket.accept();
				Bank.BranchMessage bm = Bank.BranchMessage.parseDelimitedFrom(clientSocket.getInputStream());
				if (bm.hasInitBranch()) {
					br.initBranchMethod(bm);
				}
				if (bm.hasTransfer()) {
					System.out.println("Transfer Message Received....Money :" + bm.getTransfer().getMoney());
					br.syncedBalanceUpdate(bm.getTransfer().getMoney());
					System.out.println("Current updated Balance : " + br.balance);
				}
				if (bm.hasInitSnapshot()) {
					System.out.println("Yay...InitSnapshot is here...");
					br.initSnapshot(bm.getInitSnapshot().getSnapshotId());
				}
				if (bm.hasMarker()) {
					System.out.println("*Marker msg recived.....");
					BufferedReader bufferReaderMarker = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					String markerSenderBranchName = bufferReaderMarker.readLine();
					System.out.println("**Marker received from branch : " + markerSenderBranchName + " SnapshotID : "
							+ bm.getMarker().getSnapshotId());
					
					br.processReceivedMarker( bm.getMarker().getSnapshotId(),markerSenderBranchName);
				}
				clientSocket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void processReceivedMarker(int snapshotId, String markerSenderBranchName) throws InterruptedException {

		Thread.sleep(2000);
	}
}
