import java.util.List;

public class LocalStateSnapshot {
	int snapshot_id;
	int balance;

	List<Integer> messages;

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
