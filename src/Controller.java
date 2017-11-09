import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Controller {
	private static List<Bank.InitBranch.Branch> branchesList;
	private static List<String> inputBranch = new ArrayList<String>();

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
				inputBranch.add(bBranchName);
				String bIpAddress = sarr[1];
				int bPort = Integer.parseInt(sarr[2]);
				Bank.InitBranch.Branch.Builder ibb;
				ibb = Bank.InitBranch.Branch.newBuilder().setIp(bIpAddress).setName(bBranchName).setPort(bPort);
				ib.addAllBranches(ibb.build());
			}
			ib.setBalance(Integer.parseInt(args[0]) / ib.getAllBranchesCount());
			Bank.InitBranch bm = ib.build();
			Bank.BranchMessage branchMessage = Bank.BranchMessage.newBuilder().setInitBranch(bm).build();
			branchesList = ib.getAllBranchesList();
			for (Bank.InitBranch.Branch branch : branchesList) {
				System.out.println("IP: " + branch.getIp() + " Port:" + branch.getPort());
				Socket socket = new Socket(branch.getIp(), branch.getPort());
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
		for (int i = 1; i < 4; i++) {
			initSnapshot(i);
			Thread.sleep(5000);
			retrieveSnapshotValues(i);
		}
		Thread.sleep(1000);
	}

	private static void retrieveSnapshotValues(int snapshotID)
			throws UnknownHostException, IOException, InterruptedException {
		int temp_total = 0;
		int index_of_branchList = 0;
		for (index_of_branchList = 0; index_of_branchList < branchesList.size(); index_of_branchList++) {
			Bank.InitBranch.Branch branch = branchesList.get(index_of_branchList);
			Socket socket = new Socket(branch.getIp(), branch.getPort());
			Bank.RetrieveSnapshot bRetrive = Bank.RetrieveSnapshot.newBuilder().setSnapshotId(snapshotID).build();
			Bank.BranchMessage branchMessage = Bank.BranchMessage.newBuilder().setRetrieveSnapshot(bRetrive).build();
			branchMessage.writeDelimitedTo(socket.getOutputStream());
			Bank.BranchMessage bm = Bank.BranchMessage.parseDelimitedFrom(socket.getInputStream());
			socket.close();
//			if (bm == null) {
//				Thread.sleep(1000);
//				System.out.println("Got null value");
//				index_of_branchList--;
//			} else 
				if (bm.hasReturnSnapshot()) {
				System.out.print(
						"\n" + branch.getName() + " : " + bm.getReturnSnapshot().getLocalSnapshot().getBalance());
//				System.out.println(bm.getReturnSnapshot().getLocalSnapshot().getChannelStateList());
				temp_total = temp_total + bm.getReturnSnapshot().getLocalSnapshot().getBalance();
				List<Integer> channelLocalList = bm.getReturnSnapshot().getLocalSnapshot().getChannelStateList();
				for (int index = 0; index < channelLocalList.size(); index++) {
					System.out.print("\t" + inputBranch.get(index) + "-->" + branch.getName() + " "
							+ channelLocalList.get(index) + " ");
					temp_total = temp_total + channelLocalList.get(index);
				}
			}
		}
		System.out.println("\nTotal : " + temp_total);
	}

	private static void initSnapshot(int snapShotNum) throws UnknownHostException, IOException {
		int snapshotID = snapShotNum;
		Bank.InitBranch.Branch randomBranchInit = getRandomBranch();
		Bank.InitSnapshot bInitSnapshot = Bank.InitSnapshot.newBuilder().setSnapshotId(snapshotID).build();
		System.out.println("Initiating snapshot ID :" + snapshotID + " transfering to IP: " + randomBranchInit.getIp()
				+ " Port:" + randomBranchInit.getPort());
		Socket socket = new Socket(randomBranchInit.getIp(), randomBranchInit.getPort());
		Bank.BranchMessage.newBuilder().setInitSnapshot(bInitSnapshot).build()
				.writeDelimitedTo(socket.getOutputStream());
		socket.getInputStream().close();
		socket.close();
	}

	public static Bank.InitBranch.Branch getRandomBranch() {
		Random rand = new Random();
		return branchesList.get(rand.nextInt((branchesList.size() - 1) + 1) + 0);
	}
}
