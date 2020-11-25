import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    public static void main(String[] args) throws IOException {
        String inputLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\course.csv";
        String outputLocation = "C:\\Users\\coliw\\OneDrive\\Documents\\University Year 4\\CS 4525\\Project 2\\hashFileOrganization.csv";
        File in = new File(inputLocation);
        File out = new File(outputLocation);

        if(Files.notExists(Paths.get(outputLocation)))
            createFile(in, out);

        File hfo = new File(outputLocation);

        Scanner scan = new Scanner(System.in);
        boolean begin = false;

        /* Main Command Prompt */
        while (true) {
            System.out.print("> ");
            begin = true;
            String userInput = scan.nextLine().toLowerCase();
            String[] query = userInput.split("\\s");
            for (int i = 0; i < query.length; i++) {
                if (query[0].equals("quit")) {
                    scan.close();
                    System.exit(0);
                } else if (query[0].equals("insert")) {
                    insert(userInput, hfo);
                    break;
                } else if (query[0].equals("delete")) {
                    delete(userInput, hfo);
                    break;
                } else if (query[0].equals("select")) {
                    select(userInput, hfo);
                    break;
                } else {
                    System.out.println(query[i]);
                }
            }
            begin = false;
        }
    }

    // insert into (course_id, title, dept_name, credits) values (CS-452, Database Systems, Comp. Sci, 4);
    private static void insert(String query, File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());

        // Parse input query.
        String[] header = null;
        String[] record = null;
        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(query);
        for(int i=0; m.find(); i++) {
            if (i==0) {
                header = m.group(1).split(",");
            } else {
                record = m.group(1).split(",");
            }
        }
        // Compute hash function for primary key.
        int hash = hashFunction(record[0]);

        // Put string in correct format to write.
        for(int i=0; i<record.length; i++) {
            if(header[i].startsWith("\uFEFF"))
                header[i] = header[i].substring(1);
            record[i] = header[i] + ": " + record[i];
        }
        String tuple = String.join(";", record) + ",";

        // Read data from file.
        fileChannel.read(byteBuffer);
        String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        byteBuffer.flip();

        // Place hash file organization in string array.
        String[] hfo = fileContent.split("\\r?\\n");

        // Insert record to hash file organization.
        for(int i=0; i<hfo.length; i++) {
            int index = Integer.parseInt(hfo[i].substring(0, hfo[i].indexOf(",")));
            if(index == hash) {
                hfo[index] += tuple;
            }
        }

        System.out.println("INSERTED COLUMN INTO DATABASE");

        // Write to output file.
        fileChannel.position(0);
        for(int i=0; i<hfo.length; i++) {
            String line = hfo[i] + "\n";
            byteBuffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
            fileChannel.write(byteBuffer);
        }

        fileChannel.close();
        randomAccessFile.close();
    }

    private static void delete(String query, File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());

        // Get delete condition from query string.
        String condition = query.substring(query.indexOf("where") + 6, query.length());

        // Separate attribute header and value from condition.
        String[] attributes = condition.split("=");
        attributes[1] = attributes[1].toUpperCase();

        // Format string for search.
        condition = attributes[0] + ": " + attributes[1];

        // Read data from file.
        fileChannel.read(byteBuffer);
        String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        byteBuffer.flip();

        // Place hash file organization in string array.
        String[] hfo = fileContent.split("\\r?\\n");

        // Calculate hash value on attribute
        int hash = hashFunction(attributes[1]);

        String[] rows = hfo[hash].split(",");
        for(int i=0; i<rows.length; i++) {
            if (rows[i].startsWith(condition)) {
                // Delete row.
                rows[i] = "";
                break;
            }
        }

        System.out.println("DELETED " + attributes[1] + " FROM DATABASE");

        // Update value in HFO.
        hfo[hash] = String.join(",", rows);

        // Write to output file.
        fileChannel.position(0);
        for(int i=0; i<hfo.length; i++) {
            String line = hfo[i] + "\n";
            byteBuffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
            fileChannel.write(byteBuffer);
        }

        fileChannel.close();
        randomAccessFile.close();
    }

    // SELECT * FROM table_name;
    // SELECT column1, column2 FROM table_name;
    private static void select(String query, File file) throws IOException {
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        FileChannel fileChannel = randomAccessFile.getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());

        String[] queryTokens = query.split("\\s");

        if (queryTokens[1].equals("*")) {
            // Select * statement
            selectAll(fileChannel);
        } else {
            // Select specified columns
            int beginIndex = queryTokens[0].length() + 1;
            int endIndex = query.indexOf("from") - 1;
            String columns = query.substring(beginIndex, endIndex);
            selectColumns(columns, fileChannel);
        }
    }

    // SELECT * FROM table_name;
    private static void selectAll(FileChannel fileChannel) throws IOException {
        // Read table from file.
        // Format correctly.
        // Print to the command prompt.

        ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
        fileChannel.read(byteBuffer);

        // Read data from file.
        fileChannel.read(byteBuffer);
        String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);
        byteBuffer.flip();

        String[] fileOrganization = fileContent.split("\\r?\\n");

        // Create list of table row values.
        List<String> rowList = new ArrayList<>();
        for (int i=0; i<fileOrganization.length; i++) {
            String[] file = fileOrganization[i].split(",");
            for (int j = 1; j < file.length; j++) {
                rowList.add(file[j]);
            }
        }

        // Split rows into attribute list
        List<String[]> attributeList = new ArrayList<>();
        for(String row : rowList) {
            String[] attribute = row.split(";");
            attributeList.add(attribute);
        }

        int attributeLength = attributeList.get(0).length;

        String[][] tableData = new String[attributeLength][rowList.size()];
        String[] header = new String[attributeLength];
        int[] headerSizes = new int[attributeLength];

        for (int i=0; i<attributeLength; i++) {
            String[] value = null;
            for (int j=0; j<attributeList.size(); j++) {
                value = attributeList.get(j)[i].split(":");
                tableData[i][j] = value[1];
            }
            header[i] = value[0];
            headerSizes[i] = getLongestStringSize(tableData[i]);
        }

        String attributeHeader = "#";

        for (int i=0; i<headerSizes.length; i++) {
            int headerSize = Math.max((headerSizes[i]), header[i].length());
            attributeHeader += String.format(" %-" +headerSize+"s #", header[i]);
        }

        int headerLength = attributeHeader.length();
        for(int i=0; i<headerLength; i++) {
            System.out.print("#");
        }
        System.out.println("\n" + attributeHeader);
        for(int i=0; i<headerLength; i++) {
            System.out.print("#");
        }
        System.out.println();



        for (int i=0; i<tableData[0].length; i++) {
            String rowFormat = "|";
            for (int j=0; j<tableData.length; j++) {
                rowFormat += String.format(" %-" + Math.max((headerSizes[j]), header[j].length()) + "s |", tableData[j][i]);
            }
            System.out.println(rowFormat);
        }

        for(int i=0; i<headerLength; i++) {
            System.out.print("-");
        }
        System.out.println();

    }

    // SELECT column1, column2 FROM table_name;
    private static void selectColumns(String columns, FileChannel fileChannel) {
        String[] attributes = columns.split(", ");

    }

    private static int getLongestStringSize(String[] array) {
        int maxLength = 0;
        String longestString = null;
        for (String s : array) {
            if (s.length() > maxLength) {
                maxLength = s.length();
            }
        }
        return maxLength;
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

        // Place relation in string array.
        String[] relation = fileContent.split("\\r?\\n");

        // Get header for attributes from array.
        String[] header = relation[0].split(",");

        // Add records to list.
        List<String[]> recordList = new ArrayList<>();
        for(int i=1; i<relation.length; i++) {
            String[] record = relation[i].split(",");
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
            outputBuffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
            outputChannel.write(outputBuffer);
        }
        outputChannel.close();
        outputFile.close();
    }

    private static int hashFunction(String key) {
        return Math.abs(key.hashCode() % 100);
    }
}
