import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.locks.ReentrantLock;

public class branch {
	private static ServerSocket serverSocket = null;
	private static Socket clientSocket = null;
	private List<Bank.InitBranch.Branch> branchesList;
	private int balance = 0;
	public boolean canTransfer = true;
	private List<Integer> incomingChannelState = new ArrayList<Integer>();
	private ConcurrentHashMap<Integer, LocalStateSnapshot> snapshots = new ConcurrentHashMap<Integer, LocalStateSnapshot>();
	public ConcurrentSkipListMap<String, incommingChannelData> incommingChannel = new ConcurrentSkipListMap<String, incommingChannelData>();
	public int current_port;
	public String current_ipAddress;
	public String current_branchName;
	public int markersReceived = 0;
	public int MarkersSent = 0;
	private final ReentrantLock marker_transfer_lock = new ReentrantLock();

	public branch(String branchName, String ipAddress, int port) {
		this.current_branchName = branchName;
		this.current_ipAddress = ipAddress;
		this.current_port = port;
	}

	private void initBranchMethod(Bank.BranchMessage bm) throws InterruptedException {
		System.out.println("InitBranch Message Received....");
		syncedBalanceUpdate(bm.getInitBranch().getBalance());
		branchesList = bm.getInitBranch().getAllBranchesList();
		for (Bank.InitBranch.Branch branch : branchesList) {
			// if (!branch.getName().equals(current_branchName))
			initIncomingChannels(current_branchName, branch);
		}
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
				marker_transfer_lock.lock();
				{
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
							Socket socket = new Socket(randomBranch.getIp(), randomBranch.getPort());
							Bank.BranchMessage.newBuilder().setTransfer(bt).build()
									.writeDelimitedTo(socket.getOutputStream());
							socket.getOutputStream().write(current_branchName.getBytes());
							System.out.println(current_branchName + " >>>[-" + transferingMoney + "]>>> "
									+ randomBranch.getName());
							socket.getOutputStream().close();
							socket.close();
							if (!canTransfer) {
								timer.cancel();
							} else {
								timer.schedule(new transferClass(), (rand.nextInt(5) * 1000));
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("Can not transfer money");
					}
				}
				marker_transfer_lock.unlock();
			}
		}
		;
		new transferClass().run();
	}

	public synchronized Bank.InitBranch.Branch getRandomBranch() {
		Random rand = new Random();
		while (true) {
			Bank.InitBranch.Branch branch = branchesList.get(rand.nextInt((branchesList.size() - 1) + 1) + 0);
			if (!(branch.getName().equals(current_branchName)))
				return branch;
		}
	}

	public synchronized void syncedBalanceUpdate(int money) {
		balance = balance + money;
	}

	private synchronized void setMarkersReceived(int value) {
		this.markersReceived = value;
	}

	private synchronized void setMarkersSent(int value) {
		this.MarkersSent = value;
	}

	private synchronized void incrementMarkersReceived() {
		this.markersReceived++;
	}

	private synchronized void incrementMarkersSent() {
		this.MarkersSent++;
	}

	private void initSnapshot(int snapshot_id) throws UnknownHostException, IOException, InterruptedException {
		for (Bank.InitBranch.Branch branch : branchesList) {
			// if (!branch.getName().equals(current_branchName))
			{
				this.incommingChannel.get(branch.getName()).setIncommingChannelData_snapshot_id(snapshot_id);
			}
		}
		marker_transfer_lock.lock();
		incomingChannelState.clear();
		LocalStateSnapshot localSnapshot = new LocalStateSnapshot();
		localSnapshot.setSnapshot_id(snapshot_id);
		localSnapshot.setBalance(balance);
		snapshots.put(snapshot_id, localSnapshot);
		setMarkersReceived(0);
		setMarkersSent(0);
		//
		for (Bank.InitBranch.Branch branch : branchesList) {
			// start the incoming traffic recording On mm inititator
			if ((!branch.getName().equals(current_branchName))) {
				this.incommingChannel.get(branch.getName()).setRecordingStarted(true);
				this.incommingChannel.get(branch.getName()).setIncommingChannelData_snapshot_id(snapshot_id);
				System.out.println("@Recording started C " + branch.getName() + "--->" + current_branchName);
			}
		}
		for (Bank.InitBranch.Branch branch : branchesList) {
			if (!branch.getName().equals(current_branchName)) {
//				Thread.sleep(1000);
				Bank.Marker bMarker = Bank.Marker.newBuilder().setSnapshotId(snapshot_id).build();
				System.out.print(this.current_branchName + "------ " + snapshot_id + " -------> :" + branch.getName());
				Socket socket = new Socket(branch.getIp(), branch.getPort());
				Bank.BranchMessage.newBuilder().setMarker(bMarker).build().writeDelimitedTo(socket.getOutputStream());
				socket.getOutputStream().write(current_branchName.getBytes());
				incrementMarkersSent();
				socket.getOutputStream().close();
				socket.close();
			}
		}
		marker_transfer_lock.unlock();
	}

	private void initIncomingChannels(String current_branchName2, Bank.InitBranch.Branch branch)
			throws InterruptedException {
		incommingChannelData incommingChannel = new incommingChannelData();
		//
		incommingChannel.setIncomingChannelFrom(branch.getName());
		incommingChannel.setIncomingChannelTO(current_branchName2);
		incommingChannel.setRecordingStarted(false);
		incommingChannel.setIncommingChannelData_Amount(0);
		incommingChannel.setIncommingChannelData_snapshot_id(0);
		//
		this.incommingChannel.put(branch.getName(), incommingChannel);
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
					BufferedReader bufferReaderMarker = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					String moneySender = bufferReaderMarker.readLine();
					System.out.println(
							br.current_branchName + " <<<[+" + bm.getTransfer().getMoney() + "]<<< " + moneySender);
					br.receiveMoney(bm.getTransfer().getMoney(), moneySender);
				}
				if (bm.hasInitSnapshot()) {
					System.out.println("Yay...InitSnapshot is here...");
					br.initSnapshot(bm.getInitSnapshot().getSnapshotId());
				}
				if (bm.hasMarker()) {
					BufferedReader bufferReaderMarker = new BufferedReader(
							new InputStreamReader(clientSocket.getInputStream()));
					String markerSenderBranchName = bufferReaderMarker.readLine();
					System.out.println("**Marker received from branch : " + markerSenderBranchName + " SnapshotID : "
							+ bm.getMarker().getSnapshotId());
					br.processReceivedMarker(bm.getMarker().getSnapshotId(), markerSenderBranchName);
				}
				if (bm.hasRetrieveSnapshot()) {
					System.out.println("Retrievals request is here.......");
					br.retrieveSnapshot(bm.getRetrieveSnapshot().getSnapshotId(), clientSocket);
				}
				clientSocket.close();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/*
	 * receiving branch should its recorded local and channel states and return them
	 * to the caller (i.e., the controller) by sending a returnSnap- shot message
	 * (next). ReturnSnapshot a branch returns the controller its captured local
	 * snapshot in this message. This message should include the snapshot_id,
	 * captured local state, as well as all incoming channel states.
	 */
	private void retrieveSnapshot(int snapshotId, Socket clientSocket2) throws IOException {
		if (snapshots.get(snapshotId) != null)
			if (snapshots.get(snapshotId).isSnapshotDone) {
				Bank.ReturnSnapshot.LocalSnapshot.Builder blocal = Bank.ReturnSnapshot.LocalSnapshot.newBuilder();
				blocal.setBalance(snapshots.get(snapshotId).balance);
				blocal.setSnapshotId(snapshotId);
				blocal.addAllChannelState(snapshots.get(snapshotId).amount);
				Bank.ReturnSnapshot bReturn = Bank.ReturnSnapshot.newBuilder().setLocalSnapshot(blocal).build();
				Bank.BranchMessage bm = Bank.BranchMessage.newBuilder().setReturnSnapshot(bReturn).build();
				bm.writeDelimitedTo(clientSocket2.getOutputStream());
			}
	}

	public synchronized void receiveMoney(int money, String moneySender) {
		syncedBalanceUpdate(money);
		// let the incoming channel know about money.
		// TODO MUST : add check for snapshot id.
		try {
			if (!incommingChannel.isEmpty())
				// System.out.println("moneySender " + moneySender);
				if (incommingChannel.get(moneySender).isRecordingStarted()) {
					int oldAmount = incommingChannel.get(moneySender).getIncommingChannelData_Amount();
					System.out.println(" Incomming Channels " + moneySender + ":" + current_branchName + " Amount old "
							+ oldAmount + " now :" + money + " total :" + (oldAmount + money));
					incommingChannel.get(moneySender).setIncommingChannelData_Amount(money + oldAmount);
					incommingChannel.get(moneySender).setIncomingChannelFrom(moneySender);
					incommingChannel.get(moneySender).setIncomingChannelTO(current_branchName);
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void processReceivedMarker(int snapshotId, String markerSenderBranchName)
			throws InterruptedException, UnknownHostException, IOException {
//		Thread.sleep(1);
		marker_transfer_lock.lock();
		incrementMarkersReceived();
		if (!snapshots.containsKey(snapshotId)) {
			// This is the first marker
			// System.out.println("First marker received from " + markerSenderBranchName);
			/*
			 * 1.receiving branch records its own local state (balance)
			 * 
			 * records the state of the incoming channel from the sender to itself as empty,
			 * 
			 * immediately starts recording on other incoming channelsS
			 * 
			 * sends out Marker messages to all of its outgoing channels (except itself).
			 */
			LocalStateSnapshot localSnapshot = new LocalStateSnapshot();
			incommingChannelData incommingChannel_obj = new incommingChannelData();
			localSnapshot.setSnapshot_id(snapshotId);
			localSnapshot.setBalance(this.balance);
			localSnapshot.incrementReceivedMarker_snapshot();
			//
			incommingChannel_obj.setIncommingChannelData_snapshot_id(snapshotId);
			incommingChannel_obj.setIncomingChannelFrom(markerSenderBranchName);
			incommingChannel_obj.setIncomingChannelTO(current_branchName);
			// * records the state of the incoming channel from the sender to itself as
			// empty,
			System.out.println("Stopping recording of " + markerSenderBranchName + " --->" + current_branchName);
			incommingChannel_obj.setRecordingStarted(false);
			incommingChannel_obj.setIncommingChannelData_snapshot_id(snapshotId);
			//
			// * immediately starts recording on other incoming channels **for that snapshot
			// id
			for (Bank.InitBranch.Branch branch : branchesList) {
				if ((branch.getPort() != current_port) && (!branch.getName().equals(markerSenderBranchName))) {
					this.incommingChannel.get(branch.getName()).setRecordingStarted(true);
					System.out.println("@Recording started C " + branch.getName() + "--->" + current_branchName);
				}
			}
			//
			// * sends out Marker messages to all of its outgoing channels (except itself).
			//
			this.snapshots.put(snapshotId, localSnapshot);
			this.incommingChannel.put(markerSenderBranchName, incommingChannel_obj);
			for (Bank.InitBranch.Branch branch : branchesList) {
				if (!branch.getName().equals(current_branchName)) {
//					Thread.sleep(1000);
					Bank.Marker bMarker = Bank.Marker.newBuilder().setSnapshotId(snapshotId).build();
					System.out.print(this.current_branchName + " ---- " + snapshotId + " -----> :" + branch.getName());
					Socket socket = new Socket(branch.getIp(), branch.getPort());
					Bank.BranchMessage.newBuilder().setMarker(bMarker).build()
							.writeDelimitedTo(socket.getOutputStream());
					socket.getOutputStream().write(current_branchName.getBytes());
					// TODO setMarkersSent(this.numberOfMarkersSent+1);
					incrementMarkersSent();
					socket.getOutputStream().close();
					socket.close();
				}
			}
		} else {
		}
		// Marker is receiving at second or more times
		/*
		 * the receiving branch records the state of the incoming channel as the
		 * sequence of money transfers that arrived between when it recorded its local
		 * state and when it received the Marker.
		 */
		{
			// this is the mm initiators 2nd marker.
			this.incommingChannel.get(markerSenderBranchName).setRecordingStarted(false);
			this.incommingChannel.get(markerSenderBranchName).setIncommingChannelData_snapshot_id(snapshotId);
			;
			this.snapshots.get(snapshotId).incrementReceivedMarker_snapshot();
			System.out.println("Stopping recording of " + markerSenderBranchName + " --->" + current_branchName);
		}
		if (markersReceived == branchesList.size() - 1) {
			print_StoreResult(snapshotId);
		}
		marker_transfer_lock.unlock();
	}

	private synchronized void print_StoreResult(Object snapshotId) throws InterruptedException {
		System.out.println("========================================================");
		System.out.println("================Snapshot Done here======================");
		System.out.println("Balance :" + snapshots.get(snapshotId).getBalance() + "Markers count "
				+ snapshots.get(snapshotId).markersrecived);
		System.out.println("Incoming channel");
		incommingChannel.forEach((key, value) -> System.out.println(key + ":" + value.getIncommingChannelData_Amount()
				+ ":" + value.isRecordingStarted + " : " + value.getIncommingChannelData_snapshot_id()));
		snapshots.get(snapshotId).setSnapshotDone(true);
		for (Entry<String, incommingChannelData> entry : incommingChannel.entrySet()) {
			incommingChannelData value = entry.getValue();
			snapshots.get(snapshotId).addFROM(value.getIncomingChannelFrom());
			snapshots.get(snapshotId).addTO(value.getIncomingChannelTO());
			snapshots.get(snapshotId).addamount(value.getIncommingChannelData_Amount());
		}
		setMarkersReceived(0);
		setMarkersSent(0);
		for (Bank.InitBranch.Branch branch : branchesList) {
			initIncomingChannels(current_branchName, branch);
		}
		System.out.println("========================================================");
	}
	
	
}
