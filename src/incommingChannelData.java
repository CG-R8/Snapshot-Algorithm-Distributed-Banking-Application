import java.util.List;

public class incommingChannelData {
	
	int incommingChannelData_snapshot_id;
	int incommingChannelData_Amount;
	String incomingChannelFrom;
	String incomingChannelTO;
	boolean isRecordingStarted;
	List<Integer> incommingChannelData_amounts_LIST;
	public int getIncommingChannelData_snapshot_id() {
		return incommingChannelData_snapshot_id;
	}
	public void setIncommingChannelData_snapshot_id(int incommingChannelData_snapshot_id) {
		this.incommingChannelData_snapshot_id = incommingChannelData_snapshot_id;
	}
	public int getIncommingChannelData_Amount() {
		return incommingChannelData_Amount;
	}
	public void setIncommingChannelData_Amount(int incommingChannelData_Amount) {
		this.incommingChannelData_Amount = incommingChannelData_Amount;
	}
	public String getIncomingChannelFrom() {
		return incomingChannelFrom;
	}
	public void setIncomingChannelFrom(String incomingChannelFrom) {
		this.incomingChannelFrom = incomingChannelFrom;
	}
	public String getIncomingChannelTO() {
		return incomingChannelTO;
	}
	public void setIncomingChannelTO(String incomingChannelTO) {
		this.incomingChannelTO = incomingChannelTO;
	}
	public boolean isRecordingStarted() {
		return isRecordingStarted;
	}
	public void setRecordingStarted(boolean isRecordingStarted) {
		this.isRecordingStarted = isRecordingStarted;
	}
	public List<Integer> getIncommingChannelData_amounts_LIST() {
		return incommingChannelData_amounts_LIST;
	}
	public void setIncommingChannelData_amounts_LIST(List<Integer> incommingChannelData_amounts_LIST) {
		this.incommingChannelData_amounts_LIST = incommingChannelData_amounts_LIST;
	}
	
	
}
