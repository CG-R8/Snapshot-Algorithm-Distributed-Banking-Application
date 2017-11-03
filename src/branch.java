import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class branch {
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	private List<Bank.InitBranch.Branch> branchesList;
	private Thread t;
	private int balance = 0;
	public boolean canTransfer = true;
	private List<Integer> incomingChannelState = new ArrayList<Integer>();
	private ConcurrentHashMap<Integer, LocalStateSnapshot> snapshots = new ConcurrentHashMap<Integer, LocalStateSnapshot>();

	public branch(String branchName, String ipAddress, int port) {
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
						byte[] byteArrayBalanceTransfer = Bank.BranchMessage.newBuilder().setTransfer(bt).build().toByteArray();
						System.out.println("Balance :" + transferingMoney + " transfering to IP: "
								+ randomBranch.getIp() + " Port:" + randomBranch.getPort());
						Socket socket = new Socket(randomBranch.getIp(), randomBranch.getPort());
						socket.getOutputStream().write(byteArrayBalanceTransfer, 0, byteArrayBalanceTransfer.length);
						socket.getInputStream().close();
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

	public static void main(String[] args) throws IOException {
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
			System.out.println("Branch Server started on Port : "+port);
			while (true) {
				clientSocket = serverSocket.accept();
				
				//System.out.println("New client connection done : " + server.getRemoteSocketAddress());
				String connected_client = clientSocket.getRemoteSocketAddress().toString();
				String client_Address = clientSocket.getInetAddress().toString();
				int client_Port = clientSocket.getPort();
				System.out.println("[[" + client_Address + "][" + client_Port + "]]");

				
				
				
				System.out.println("Socket accepted....Port :" + serverSocket.getInetAddress()+" Port "+serverSocket.getLocalPort());
				Bank.BranchMessage bm = Bank.BranchMessage.parseFrom(clientSocket.getInputStream());
				if (bm.hasInitBranch()) {
					br.initBranchMethod(bm);
				}
				if (bm.hasTransfer()) {
					System.out.println("Transfer Message Received....Money :"+bm.getTransfer().getMoney());
					br.syncedBalanceUpdate(bm.getTransfer().getMoney());
					System.out.println("Current updated Balance : " + br.balance);
				}
				if(bm.hasInitSnapshot()) {
					System.out.println("Yay...InitSnapshot is here...");
					br.initSnapshot(bm.getInitSnapshot().getSnapshotId());
				}
				clientSocket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	private void initSnapshot(int snapshot_id) {
			incomingChannelState.clear();
			LocalStateSnapshot localSnapshot = new LocalStateSnapshot();
			localSnapshot.setSnapshot_id(snapshot_id);
			localSnapshot.setBalance(balance);
			localSnapshot.setMessages(this.incomingChannelState);
			snapshots.put(snapshot_id, localSnapshot);
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
}
