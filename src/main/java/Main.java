import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        String fileLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\course.csv";
        File file = new File(fileLocation);
        
        readFile(file);
    }

    private static void readFile(File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();

        // Read column header for the table.
        String line = randomAccessFile.readLine();
        String[] header = line.split(",");

        // Read column values into a list.
        line = randomAccessFile.readLine();
        List<String[]> recordList = new ArrayList<>();
        while(line != null) {
            String[] record = line.split(",");
            recordList.add(record);
            line = randomAccessFile.readLine();
        }

        randomAccessFile.seek(0);

    }
}
