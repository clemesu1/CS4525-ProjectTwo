import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        String fileLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\course.csv";
        String outputLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\hfo.csv";
        File in = new File(fileLocation);
        File out = new File(outputLocation);

        readFile(in, out);

        Scanner scan = new Scanner(System.in);
        boolean begin = false;

        /* Main Command Prompt */
        while (true) {
            if (!begin) {
                System.out.print("> ");
                begin = true;
            }
            String userInput = scan.nextLine();
            String[] query = userInput.toLowerCase().split("\\s");
            for (int i = 0; i < query.length; i++) {
                if (query[0].equals("quit")) {
                    scan.close();
                    System.exit(0);
                } else if (query[0].equals("insert")) {
                    insert(query);
                } else if (query[0].equals("delete")) {
                    delete(query);
                } else if (query[0].equals("select")) {
                    select(query);
                    query = new String[0];
                } else {
                    System.out.println(query[i]);
                }
            }
            begin = false;
        }


    }

    private static void delete(String[] query) {

    }

    private static void insert(String[] query) {

    }

    private static void select(String[] query) {

    }

    private static void readFile(File in, File out) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(in, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();
        FileWriter writer = new FileWriter(out);

        // Read column header for the table.
        randomAccessFile.seek(3);
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

        // Create empty string array for hash index.
        String[] output = new String[100];
        Arrays.fill(output, "");

        // Format and save records to string array.
        for(String[] record : recordList) {
            int hash = hashFunction(record[0]);
            for (int i = 0; i < record.length; i++)
                record[i] = header[i] + ": " + record[i];
            output[hash] += String.join(";", record) + ",";
        }

        // Write record data to hash file organization.
        for (int i=0; i<output.length; i++) {
            writer.append(Integer.toString(i));
            writer.append(",");
            writer.append(output[i]);
            writer.append("\n");
        }
        writer.flush();
        
    }

    private static int hashFunction(String key) {
        return Math.abs(key.hashCode() % 100);
    }
}
