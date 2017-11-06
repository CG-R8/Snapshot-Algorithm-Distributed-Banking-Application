import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Controller {
	private static List<Bank.InitBranch.Branch> branchesList;

	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		int count = args.length;
		if (count != 2) {
			System.err.println("Wrong number inputs....Format <initial Amount> <Filename>");
			System.exit(0);
		}
		String branchFileName = "";
		try {
			branchFileName = args[1];
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		try {
			FileProcessor fp = new FileProcessor(branchFileName);
			String line;
			boolean isFileEmpty = true;
			Bank.InitBranch.Builder ib = Bank.InitBranch.newBuilder();
			while ((line = fp.readLine()) != null && line.trim().length() > 0) {
				isFileEmpty = false;
				String[] sarr = line.split(" ");
				String bBranchName = sarr[0];
				String bIpAddress = sarr[1];
				int bPort = Integer.parseInt(sarr[2]);
				Bank.InitBranch.Branch.Builder ibb;
				ibb = Bank.InitBranch.Branch.newBuilder().setIp(bIpAddress).setName(bBranchName).setPort(bPort);
				// branchesBuildList.add(ibb.build());
				ib.addAllBranches(ibb.build());
			}
			ib.setBalance(Integer.parseInt(args[0]) / ib.getAllBranchesCount());
			Bank.InitBranch bm = ib.build();
			Bank.BranchMessage branchMessage = Bank.BranchMessage.newBuilder().setInitBranch(bm).build();
			branchesList = ib.getAllBranchesList();
			for (Bank.InitBranch.Branch branch : branchesList) {
				System.out.println("IP: " + branch.getIp() + " Port:" + branch.getPort());
				Socket socket = new Socket(branch.getIp(), branch.getPort());
				// socket.getOutputStream().write(byteArray, 0, byteArray.length);
				branchMessage.writeDelimitedTo(socket.getOutputStream());
				socket.close();
			}
			if (isFileEmpty) {
				System.err.println("Input file is empty or starting with invalid charactres. \n Exiting Process\n");
				isFileEmpty = true;
				System.exit(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread.sleep(4000);
		// TimeUnit.SECONDS.sleep(10000);
		// TODO make this automatic afterward
		int snapshotID = 1;
		// snapshotID = Integer.parseInt(args[2]);
		System.out.println("Starting snapshot initiation");
		Bank.InitBranch.Branch randomBranchInit = getRandomBranch();
		Bank.InitSnapshot bInitSnapshot = Bank.InitSnapshot.newBuilder().setSnapshotId(snapshotID).build();
		// byte[] byteArrayInitSnapshot =
		// Bank.BranchMessage.newBuilder().setInitSnapshot(bInitSnapshot).build().toByteArray();
		System.out.println("Initiating snapshot ID :" + snapshotID + " transfering to IP: " + randomBranchInit.getIp()
				+ " Port:" + randomBranchInit.getPort());
		Socket socket = new Socket(randomBranchInit.getIp(), randomBranchInit.getPort());
		Bank.BranchMessage.newBuilder().setInitSnapshot(bInitSnapshot).build()
				.writeDelimitedTo(socket.getOutputStream());
		// socket.getOutputStream().write(byteArrayInitSnapshot, 0,
		// byteArrayInitSnapshot.length);
		socket.getInputStream().close();
		socket.close();
		
		
		
		
		Thread.sleep(20000);
		
		
		System.out.println("Starting 2nd snapshot initiation");
		snapshotID=2;
		randomBranchInit = getRandomBranch();
		bInitSnapshot = Bank.InitSnapshot.newBuilder().setSnapshotId(snapshotID).build();
		// byte[] byteArrayInitSnapshot =
		// Bank.BranchMessage.newBuilder().setInitSnapshot(bInitSnapshot).build().toByteArray();
		System.out.println("Initiating snapshot ID :" + snapshotID + " transfering to IP: " + randomBranchInit.getIp()
				+ " Port:" + randomBranchInit.getPort());
		socket = new Socket(randomBranchInit.getIp(), randomBranchInit.getPort());
		Bank.BranchMessage.newBuilder().setInitSnapshot(bInitSnapshot).build()
				.writeDelimitedTo(socket.getOutputStream());
		// socket.getOutputStream().write(byteArrayInitSnapshot, 0,
		// byteArrayInitSnapshot.length);
		socket.getInputStream().close();
		socket.close();
		// send the retrival msg
		/*
		 * RetrieveSnapshot the controller sends retrieveSnapshot messages to all
		 * branches to collect snapshots. This mes- sage will contain the snapshot_id
		 * that uniquely identifies a snapshot. A receiving branch should its recorded
		 * local and channel states and return them to the caller (i.e., the controller)
		 * by sending a returnSnap- shot message (next).a
		 */
		for (Bank.InitBranch.Branch branch : branchesList) {
			System.out.println("Sending retrival msg to IP: " + branch.getIp() + " Port:" + branch.getPort());
			socket = new Socket(branch.getIp(), branch.getPort());
			Bank.RetrieveSnapshot bRetrive = Bank.RetrieveSnapshot.newBuilder().setSnapshotId(snapshotID).build();
			Bank.BranchMessage branchMessage = Bank.BranchMessage.newBuilder().setRetrieveSnapshot(bRetrive).build();
			branchMessage.writeDelimitedTo(socket.getOutputStream());
			socket.close();
		}
	}

	public static Bank.InitBranch.Branch getRandomBranch() {
		Random rand = new Random();
		return branchesList.get(rand.nextInt((branchesList.size() - 1) + 1) + 0);
	}
}
