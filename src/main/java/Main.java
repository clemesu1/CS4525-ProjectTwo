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
    public static void main(String[] args) throws Exception {
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
            String userInput = scan.nextLine();
            String[] query = userInput.split("\\s");
            for (int i = 0; i < query.length; i++) {
                if (query[0].toLowerCase().equals("quit")) {
                    scan.close();
                    System.exit(0);
                } else if (query[0].toLowerCase().equals("insert")) {
                    insert(userInput, hfo);
                    break;
                } else if (query[0].toLowerCase().equals("delete")) {
                    delete(userInput, hfo);
                    break;
                } else if (query[0].toLowerCase().equals("select")) {
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
    private static void insert(String query, File file) throws Exception {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.position(0);
            // Parse input query.
            String[] header = null;
            String[] record = null;
            Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(query);
            for (int i = 0; m.find(); i++) {
                if (i == 0) {
                    header = m.group(1).split(",");
                } else {
                    record = m.group(1).split(",");
                }
            }
            // Compute hash function for primary key.
            int hash = hashFunction(record[0]);

            // Put string in correct format to write.
            for (int i = 0; i < record.length; i++) {
                if (header[i].startsWith("\uFEFF"))
                    header[i] = header[i].substring(1);
                record[i] = header[i] + ": " + record[i];
            }
            String tuple = String.join(";", record) + ",";

            // Read data from file.
            fileChannel.read(byteBuffer);
            String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);

            // Place hash file organization in string array.
            String[] hfo = fileContent.split("\\r?\\n");

            // Insert record to hash file organization.
            for (int i = 0; i < hfo.length; i++) {
                int index = Integer.parseInt(hfo[i].substring(0, hfo[i].indexOf(",")));
                if (index == hash) {
                    hfo[index] += tuple;
                }
            }

            System.out.println("INSERTED RECORD INTO DATABASE");

            // Write to output file.
            fileChannel.truncate(0);
            fileChannel.position(0);
            for(String line : hfo) {
                line += "\n";
                byteBuffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
                fileChannel.write(byteBuffer);
            }


            fileChannel.close();

        } catch (StringIndexOutOfBoundsException e) {
            return;
        }
    }

    // delete from table where credits=3
    private static void delete(String query, File file) throws Exception {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());

            // Read HFO from file.
            fileChannel.position(0);
            fileChannel.read(byteBuffer);
            byteBuffer.flip();
            String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);
            String[] hfo = fileContent.split("\\r?\\n");

            int fileSize = hfo.length;

            // Get condition for delete from query.
            String condition = query.substring(query.toLowerCase().indexOf("where") + 6);

            // Get condition values.
            String[] conditionValues = condition.split("=");
            String column = conditionValues[0];
            String value = conditionValues[1];

            // Format condition for search in relation.
            String searchAttribute = String.format("%s: %s", column, value);

            // Delete records that meet condition.
            for (int i=0; i<hfo.length; i++) {
                String[] rows = hfo[i].split(",");
                for (int j=1; j<rows.length; j++) {
                    String[] attributes = rows[j].split(";");
                    for (String attribute : attributes) {
                        if (attribute.equals(searchAttribute)) {
                            rows[j] = "";
                            break;
                        }
                    }
                }
                hfo[i] = String.join(",", rows);
            }

            System.out.println("DELETED ROWS WHERE " + condition);

            // Write to file
            fileChannel.truncate(0);
            fileChannel.position(0);
            for(String line : hfo) {
                line += "\n";
                byteBuffer = ByteBuffer.wrap(line.getBytes(StandardCharsets.UTF_8));
                fileChannel.write(byteBuffer);
            }

            fileChannel.close();
        }
    }

    // SELECT * FROM table_name;
    // SELECT column1, column2 FROM table_name;
    private static void select(String query, File file) {
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            FileChannel fileChannel = randomAccessFile.getChannel();
            ByteBuffer byteBuffer = ByteBuffer.allocate((int) fileChannel.size());
            fileChannel.position(0);

            // Determine select operation type.
            String[] tokens = query.split("\\s");
            String columns = ((tokens[1].equals("*")) ? "*" : query.substring(tokens[0].length() + 1, query.toLowerCase().indexOf("from") - 1));

            // Read file from disk.
            fileChannel.read(byteBuffer);
            String fileContent = new String(byteBuffer.array(), StandardCharsets.UTF_8);

            // Place file into string array.
            String[] hashFileOrganization = fileContent.split("\\r?\\n");

            List<String[]> attributeList = new ArrayList<>();
            for (int i=0; i<hashFileOrganization.length; i++) {
                String[] row = hashFileOrganization[i].split(",");
                for (int j=1; j<row.length; j++) {
                    String[] attribute = row[j].split(";");
                    if (!columns.equals("*")) {
                        String selectedAttributes = "";
                        for (int k=0; k<columns.split(", ").length; k++) {
                            for (int col=0; col<attribute.length; col++) {
                                if (attribute[col].startsWith(columns.split(", ")[k])) {
                                    selectedAttributes += attribute[col] + ";";
                                }
                            }
                        }
                        attributeList.add(selectedAttributes.split(";"));
                    } else {
                        attributeList.add(attribute);
                    }
                }
            }

            int numOfRows = attributeList.size();
            int numOfCols = attributeList.get(0).length;
            String[][] relationData = new String[numOfCols][numOfRows];
            String[] header = new String[numOfCols];
            int[] headerSizes = new int[numOfCols];

            for (int i=0; i<numOfCols; i++) {
                String[] attribute = null;
                for (int j=0; j<numOfRows; j++) {
                    attribute = attributeList.get(j)[i].split(":");
                    relationData[i][j] = attribute[1];
                }
                header[i] = attribute[0];
                headerSizes[i] = getLongestStringSize(relationData[i]);
            }

            printTable(relationData, header, headerSizes);

            fileChannel.close();

        } catch (Exception e) {
            return;
        }
    }

    private static void printTable(String[][] relationData, String[] header, int[] headerSizes) {
        // Output to command prompt.
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

        for (int i=0; i<relationData[0].length; i++) {
            String rowFormat = "|";
            for (int j=0; j<relationData.length; j++) {
                rowFormat += String.format(" %-" + Math.max((headerSizes[j]), header[j].length()) + "s |", relationData[j][i]);
            }
            System.out.println(rowFormat);
        }

        for(int i=0; i<headerLength; i++) {
            System.out.print("-");
        }
        System.out.println();
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

    private static int getLongestStringSize(String[] array) {
        int maxLength = 0;
        for (String s : array) {
            if (s.length() > maxLength) {
                maxLength = s.length();
            }
        }
        return maxLength;
    }

}
