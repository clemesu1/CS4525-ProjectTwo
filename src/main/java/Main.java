import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String inputLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\course.csv";
        String outputLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\hfo.csv";
        File in = new File(inputLocation);
        File out = new File(outputLocation);

        createFile(in, out);

        Scanner scan = new Scanner(System.in);
        boolean begin = false;

        /* Main Command Prompt */
        while (true) {
            System.out.print("> ");
            begin = true;
            String userInput = scan.nextLine();
            String[] query = userInput.toLowerCase().split("\\s");
            for (int i = 0; i < query.length; i++) {
                if (query[0].equals("quit")) {
                    scan.close();
                    System.exit(0);
                } else if (query[0].equals("insert")) {
                    insert(query, out);
                    break;
                } else if (query[0].equals("delete")) {
                    delete(query, out);
                    break;
                } else if (query[0].equals("select")) {
                    select(query, out);
                    break;
                } else {
                    System.out.println(query[i]);
                }
            }
            begin = false;
        }
    }

    private static void insert(String[] query, File file) throws FileNotFoundException {
        //RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
    }

    private static void delete(String[] query, File file) {

    }

    private static void select(String[] query, File file) {

    }

    private static void createFile(File in, File out) throws IOException {
        RandomAccessFile inputFile = new RandomAccessFile(in, "r");
        RandomAccessFile outputFile = new RandomAccessFile(out, "rw");
        FileChannel inputChannel = inputFile.getChannel();
        FileChannel outputChannel = outputFile.getChannel();
        ByteBuffer inputBuffer = null;
        ByteBuffer outputBuffer = null;

        // Read attributes of input relation.
        inputBuffer = ByteBuffer.allocate((int) inputChannel.size());
        inputChannel.read(inputBuffer);
        String fileContent = new String(inputBuffer.array(), StandardCharsets.UTF_8);
        inputBuffer.flip();
        inputChannel.close();
        inputFile.close();

        // Place attributes in string array.
        String[] attributes = fileContent.split("\\r?\\n");

        // Get header for attributes from array.
        String[] header = attributes[0].split(",");

        // Add records to list.
        List<String[]> recordList = new ArrayList<>();
        for(int i=1; i<attributes.length; i++) {
            String[] record = attributes[i].split(",");
            recordList.add(record);
        }

        // Create empty array for output.
        String[] output = new String[100];
        Arrays.fill(output, "");

        // Compute hash function on records and save at index.
        for(String[] record : recordList) {
            int hash = hashFunction(record[0]);
            for(int i=0; i<record.length; i++) {
                if(header[i].startsWith("\uFEFF"))
                    header[i] = header[i].substring(1);
                record[i] = header[i] + ": " + record[i];
            }
            output[hash] += String.join(";", record) + ",";
        }

        // Write to output file.
        for(int i=0; i<output.length; i++) {
            String line = Integer.toString(i) + "," + output[i] + "\n";
            outputBuffer = ByteBuffer.wrap(line.getBytes("UTF-8"));
            outputChannel.write(outputBuffer);
        }
        outputChannel.close();
        outputFile.close();
    }

//    private static void createFile(File in, File out) throws IOException {
//        RandomAccessFile randomAccessFile = new RandomAccessFile(in, "r");
//        FileChannel fileChannel = randomAccessFile.getChannel();
//        FileWriter writer = new FileWriter(out);
//
//        // Read column header for the table.
//        randomAccessFile.seek(0);
//        String line = randomAccessFile.readLine();
//        String[] header = line.split(",");
//
//        // Read column values into a list.
//        line = randomAccessFile.readLine();
//        List<String[]> recordList = new ArrayList<>();
//        while(line != null) {
//            String[] record = line.split(",");
//            recordList.add(record);
//            line = randomAccessFile.readLine();
//        }
//
//        randomAccessFile.seek(0);
//
//        // Create empty string array for hash index.
//        String[] output = new String[100];
//        Arrays.fill(output, "");
//
//        // Format and save records to string array.
//        for(String[] record : recordList) {
//            int hash = hashFunction(record[0]);
//            for (int i = 0; i < record.length; i++) {
//                record[i] = header[i] + ": " + record[i];
//            }
//            output[hash] += String.join(";", record) + ",";
//        }
//
//        // Write record data to hash file organization.
//        for (int i=0; i<output.length; i++) {
//            writer.append(Integer.toString(i));
//            writer.append(",");
//            writer.append(output[i]);
//            writer.append("\n");
//        }
//        writer.flush();
//        writer.close();
//        randomAccessFile.close();
//    }

    private static int hashFunction(String key) {
        return Math.abs(key.hashCode() % 100);
    }
}
