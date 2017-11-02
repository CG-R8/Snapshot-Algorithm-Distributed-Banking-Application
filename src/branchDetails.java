
public class branchDetails {
	String ipAddress;
	String branchName;
	int port;
	
	public branchDetails(String branchNameIn,String ipIN,String portIN) {
		this.branchName = branchNameIn;
		this.ipAddress=ipIN;
		this.port=Integer.parseInt(portIN);
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
	
	
}
