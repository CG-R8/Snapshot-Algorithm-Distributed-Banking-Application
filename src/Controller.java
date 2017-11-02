import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class Controller {
	public static void main(String[] args) throws UnknownHostException, IOException {
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
				System.out.println(line);
				String[] sarr = line.split(" ");
				System.out.println(sarr.length);
				String bBranchName = sarr[0];
				String bIpAddress = sarr[1];
				int bPort = Integer.parseInt(sarr[2]);
				Bank.InitBranch.Branch.Builder ibb;
				ibb = Bank.InitBranch.Branch.newBuilder().setIp(bIpAddress).setName(bBranchName).setPort(bPort);
				// branchesBuildList.add(ibb.build());
				ib.addAllBranches(ibb.build());
			}
			// ib.addAllAllBranches(branchesBuildList);
			ib.setBalance(Integer.parseInt(args[0]) / ib.getAllBranchesCount());
			Bank.InitBranch bm = ib.build();
			byte[] byteArray = Bank.BranchMessage.newBuilder().setInitBranch(bm).build().toByteArray();
			List<Bank.InitBranch.Branch> branchesList = bm.getAllBranchesList();
			// System.out.println("branches Size "+bm.getAllBranchesList().size());
			for (Bank.InitBranch.Branch branch : ib.getAllBranchesList()) {
				System.out.println("IP: " + branch.getIp() + " Port:" + branch.getPort());
				Socket socket = new Socket(branch.getIp(), branch.getPort());
				socket.getOutputStream().write(byteArray, 0, byteArray.length);
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
	}
}
