import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.*;

public class Main {
    public static Output out;
    public static SimulatedDevice sim;
    public static JTextField txtConnectionString;
    public static JTextField txtDeviceID;
    public static JTextField txtFrequence;
    public static JTextField txtLogDir;

    public static void main(String[] args) {
        // GUI erstellen
        JFrame frame = new JFrame();
        JPanel panel = new JPanel();
        JTextArea ta = new JTextArea(40, 30);

        frame.add(panel);
        frame.add(new JLabel(" Output"));
        txtConnectionString = new JTextField("HostName=DUD02-IoTHUB.azure-devices.net;DeviceId=DUD02-IoTHUB;SharedAccessKey=5wfEP6MZJFmq0NDIorNuh9RQZHsTqoUkov9lC6bFohM=", 40);
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
        panel.add(buttonStart);
        panel.add(buttonStop);
        panel.add(new JScrollPane(ta));

        frame.add(panel);
        frame.setVisible(true);
        frame.setSize(500, 600);

        //Ausgabe in GUI lenken
        TextAreaOutputStream taos = new TextAreaOutputStream(ta, 60);
        PrintStream ps = new PrintStream(taos);


        buttonStart.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //Eigenes Output-Objekt für zentralisiertes Logging erstellen
                out = new Output(ps, txtLogDir.getText());
                sim = new SimulatedDevice(txtConnectionString.getText(),
                        txtDeviceID.getText(),
                        Long.parseLong(txtFrequence.getText()),
                        out);
                sim.startSimulation();
            }
        });

        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sim.stopSimulation();
            }
        });


    }


}

