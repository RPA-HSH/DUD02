import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Output {
    private PrintStream ps;
    private FileWriter writer;
    private String dir;

    public Output(PrintStream ps, String dir) {
        //Instanzattribute setzen
        this.dir = dir;
        this.ps = ps;
        //Timestamp für Erstellung der Log-Datei generieren
        String timeLog = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        try {
            //Objekt für Ausgabe in Textfile erstellen
            writer = new FileWriter(dir + "RPI_LOG_" + timeLog + ".txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void print(String text, Boolean onlyInGUI) {
        try {
            //Ausgabe des Texts in GUI
            ps.println(text);
            //Ausgabe in Log-File, falls explizit gefordert
            if (onlyInGUI == false) {
                writer.write(text);
                writer.write(System.getProperty("line.separator"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            //Ausgabe in Log-Datei schreiben und Filezugriff beenden
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
