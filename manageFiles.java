import java.io.*;

public class manageFiles {
    public String readFile(String filePath) throws IOException {
        File file = new File(filePath);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String nextLine;
        do {
            nextLine = bufferedReader.readLine();
            stringBuilder.append(nextLine);
        } while (nextLine != null);
        return stringBuilder.toString();
    }

    public void writeFile(String body, String filePath) {
        File file = new File(filePath);
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file));
            bufferedWriter.write(body);
            bufferedWriter.close();
        } catch (IOException e) {e.printStackTrace();}
    }
}
