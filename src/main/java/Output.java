import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;

public class Output {
    private PrintStream ps;
    private FileWriter writer;
    private String dir;

    public Output(PrintStream ps, String dir) {
        this.dir = dir;
        this.ps = ps;
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        try {
            writer = new FileWriter(dir + "RPI_LOG_" + timeLog + ".txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String text, Integer output) {
        try {
            ps.println(text);
            //create a temporary file
            if (output > 0) {
                writer.write(text);
                writer.write(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
