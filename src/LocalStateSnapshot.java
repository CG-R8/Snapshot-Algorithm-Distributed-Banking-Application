import java.util.ArrayList;
import java.util.List;

public class LocalStateSnapshot {
	int snapshot_id;
	int balance;
	int markersrecived=0;
	boolean isSnapshotDone = false;	
	List<String> TO = new ArrayList<String>();
	List<String> FROM = new ArrayList<String>();;
	List<Integer> amount = new ArrayList<Integer>();;

	public void addTO(String value)
	{
		this.TO.add(value);
	}
	public void addFROM(String value)
	{
		this.FROM.add(value);
	}
	public void addamount(int value)
	{
		this.amount.add(value);
	}
	public int getMarkersrecived() {
		return markersrecived;
	}
	public void setMarkersrecived(int markersrecived) {
		this.markersrecived = markersrecived;
	}
	public boolean isSnapshotDone() {
		return isSnapshotDone;
	}
	public void setSnapshotDone(boolean isSnapshotDone) {
		this.isSnapshotDone = isSnapshotDone;
	}
	
	public void incrementReceivedMarker_snapshot() {
		this.markersrecived++;
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
}
