import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileProcessor {
	public FileReader file;
	public BufferedReader breader;

	public FileProcessor(String path) {
		try {
			//file = new FileReader("studentCoursesBackup/" + path);
			file = new FileReader(path);
			breader = new BufferedReader(file);
		} catch (Exception e) {
			System.err.println("File not found...");
			e.printStackTrace();
			System.exit(0);
		}
	}

	/**
	 * This method read one line at a time return it
	 * 
	 * @return oneLine
	 */
	public String readLine() {
		try {
			String oneLine = breader.readLine();
			if (oneLine != null)
				return oneLine.trim();
		} catch (IOException e) {
			System.out.println("Problem while reading file");
			e.printStackTrace();
			System.exit(0);
		}
		return "";
	}
}
