
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;


public class Main {
    //Klassenattribute
    public static Output out; //Ausgabe in GUI und log-File
    public static SimulatedDevice sim; //Zentrales Objekt für Generierung und Versand der Telemetriedaten
    public static JTextField txtConnectionString;
    public static JTextField txtDeviceID;
    public static JTextField txtFrequence;
    public static JTextField txtLogDir;

    public static void main(String[] args) {
        // Erstellung der zentralen Elemente für die GUI (Frame, Panel, TextArea)
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        JPanel panel2 = new JPanel();
        JTextArea ta = new JTextArea(80, 70);

        //Erstellung der Eingabefelder und Buttons inkl. Label und Vorbelegungen
        frame.add(panel);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 10, 30));
        panel2.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));
        panel.setLayout(new GridLayout(9, 1));
        panel2.setLayout(new FlowLayout());
        txtConnectionString = new JTextField(40);
        txtDeviceID = new JTextField("device", 40);
        txtFrequence = new JTextField("10", 40);
        txtLogDir = new JTextField("C:/temp/", 40);
        JButton buttonStart = new JButton("Start");
        JButton buttonStop = new JButton("Stop");
        panel.add(new Label("Connection String"));
        panel.add(txtConnectionString);
        panel.add(new Label("Device ID"));
        panel.add(txtDeviceID);
        panel.add(new Label("Frequence"));
        panel.add(txtFrequence);
        panel.add(new Label("Log Directory"));
        panel.add(txtLogDir);
        panel2.add(buttonStart);
        panel2.add(buttonStop);
        panel2.add(new JScrollPane(ta));
        panel2.add(new Label(""));
        frame.setLayout(new GridLayout(2, 1));
        frame.add(panel);
        frame.add(panel2);
        frame.setVisible(true);
        frame.setSize(900, 600);

        //Objekte für Ausgabe in GUI erstellen
        TextAreaOutputStream taos = new TextAreaOutputStream(ta, 20);
        PrintStream ps = new PrintStream(taos);

        //Reaktion für Klick auf Start-Button (Eventbasiert)
        buttonStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Eigenes Output-Objekt für zentralisiertes Logging erstellen
                out = new Output(ps, txtLogDir.getText());
                //Simulations-Objekt erstellen
                sim = new SimulatedDevice(txtConnectionString.getText(),
                        txtDeviceID.getText(),
                        Long.parseLong(txtFrequence.getText()),
                        out);
                //Simulation der Werte und Senden an IOT-Hub starten
                sim.startSimulation();
            }
        });

        //Reaktion für Klick auf Stop-Button (Eventbasiert)
        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Simulation stoppen
                sim.stopSimulation();
            }
        });


    }


}

