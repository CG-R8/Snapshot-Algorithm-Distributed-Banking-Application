import java.util.List;

public class LocalStateSnapshot {
	int snapshot_id;
	int balance;
	String incomingChannelFrom;
	String incomingChannelTO;
	boolean isRecordingStarted;
	List<Integer> messages;

	public String getIncomingChannelTO() {
		return incomingChannelTO;
	}

	public void setIncomingChannelTO(String incomingChannelTO) {
		this.incomingChannelTO = incomingChannelTO;
	}

	public String getIncomingChannelFrom() {
		return incomingChannelFrom;
	}

	public void setIncomingChannelFrom(String incomingChannelFrom) {
		this.incomingChannelFrom = incomingChannelFrom;
	}

	public boolean isRecordingStarted() {
		return isRecordingStarted;
	}

	public void setRecordingStarted(boolean isRecordingStarted) {
		this.isRecordingStarted = isRecordingStarted;
	}

	public int getSnapshot_id() {
		return snapshot_id;
	}

	public void setSnapshot_id(int snapshot_id) {
		this.snapshot_id = snapshot_id;
	}

	public int getBalance() {
		return balance;
	}

	public void setBalance(int balance) {
		this.balance = balance;
	}

	public List<Integer> getMessages() {
		return messages;
	}

	public void setMessages(List<Integer> messages) {
		this.messages = messages;
	}
}
