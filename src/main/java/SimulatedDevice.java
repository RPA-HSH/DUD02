// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

// This application uses the Azure IoT Hub device SDK for Java
// For samples see: https://github.com/Azure/azure-iot-sdk-java/tree/master/device/iot-device-samples

import com.microsoft.azure.sdk.iot.device.*;
import com.google.gson.Gson;

import java.io.*;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimulatedDevice {
    //Zentrale Steuerungsparameter
    private String connString; //Connection String
    private String deviceID;   //Device ID, die in JSON-Nachricht an den Hub gesendet wird
    private long waitingTime;  //Anzahl Nachrichten pro Minute

    // Nutzung des MQTT Protokoll für Verbindung zum IoT Hub
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    // Client für Verbindung zum IoT Hub
    private DeviceClient client;
    //Ausgabe-Objekt für Ausgabe in GUI und Logfile
    private Output out;
    //Executor Service für Ausführung des Nachrichtenversands als eigener Thread
    private ExecutorService executor;

    public SimulatedDevice(String connString, String deviceID, long waitingTime, Output out) {
        //Zentrale Steuerungsparameter und Ausgabeobjekt setzen
        this.connString = connString;
        this.deviceID = deviceID;
        this.waitingTime = waitingTime;
        this.out = out;
    }

    public void startSimulation() {
        try {
            //Verbindung zum IoT-Hub aufbauen
            client = new DeviceClient(connString, protocol);
            client.open();
            //Senden der Daten an IoT-Hub mittels Executor in eigenem Thread
            MessageSender sender = new MessageSender();
            executor = Executors.newFixedThreadPool(1);
            executor.execute(sender);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public void stopSimulation() {
        //Simulation stoppen, indem der Thread beendet wird
        executor.shutdownNow();
        try {
            //Verbindung zum IoT-Hub schließen
            client.closeNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Klasse für Aufnahme der Telemetrie-Daten
    private class TelemetryDataPoint {
        public String deviceID;
        public double temperature;
        public double humidity;
        public double airPressure;
        public double wind;

        public String serialize() {
            // Instanzattribute in JSON überführen, um Nachricht an IoT-Hub aufzubauen
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    // Rückmeldung zu versendeter Nachricht an den IoT-Hub (Eventbasiert)
    private class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context) {
            //Rückmeldung des IoT-Hub ausgeben
            out.print("IoT Hub responded to message with status: " + status.name(), true);
            if (context != null) {
                synchronized (context) {
                    context.notify();
                }
            }
        }
    }

    private class MessageSender implements Runnable {
        public void run() {
            try {
                // Zufallsgenerator für Randnom Noise initialisieren
                Random random = new Random();

                //Daten solange schicken, bis der Prozess beendet wird
                while (true) {
                    //Vergangene Sekunden seit Mitternacht ermitteln
                    ZoneId z = ZoneId.of("Europe/Berlin");
                    ZonedDateTime now = ZonedDateTime.now(z);
                    ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(z);
                    Duration duration = Duration.between(todayStart, now);
                    double secondsSoFarToday = duration.getSeconds();
                    //Berechnen zu wie viel Prozent der Tag bereits vergangen ist
                    double dayPercentage = secondsSoFarToday / (60 * 60 * 24);
                    //Faktor für weitere Berechnung ermitteln ("sinusFactor")
                    //Grundgedanke: Ein Tag liegt im Intervall 0..Pi
                    //Entsprechend wird Mittags um 12 der höchste Wert (1) zurückgegeben.
                    //Um Mitternacht ist der Wert entsprechend 0.
                    double dayPercentagePi = Math.PI * dayPercentage;
                    double sinusFactor = Math.sin(dayPercentagePi);

                    //Werte für Telemetriedaten Temperatur, Luftfeuchte, Luftdruck und Wind
                    //berechnen. Der jeweilige Wert ermittelt sich aus einer Grundkonstante
                    //multipliziert mit dem sinusFactor. Zu diesem Wert wird ein Randnom Noise
                    //addiert/ subtrahiert. Der Random Noise ergibt sich aus der Grundkonstante
                    //multipliziert mit einem zufälligen Wert aus dem Intervall -0.15..0,15
                    double randomNoise = random.nextInt(30) - 15;
                    double currentTemperature = 20 * sinusFactor + (20 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentHumidity = 50 * sinusFactor + (50 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentAirPressure = 60 * sinusFactor + (60 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentWind = 35 * sinusFactor + (35 * (randomNoise / 100));

                    //Datenobjekt erstellen und Werte zuweisen
                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.deviceID = deviceID;
                    telemetryDataPoint.temperature = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;
                    telemetryDataPoint.airPressure = currentAirPressure;
                    telemetryDataPoint.wind = currentWind;

                    //Message für IoT-Hub aufbauen, indem das Datenobjekt in ein JSON überführt wird
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    //Nachricht an IoT-Hub senden
                    Object lockobj = new Object();
                    EventCallback callback = new EventCallback();
                    client.sendEventAsync(msg, callback, lockobj);

                    //Versand an IoT-Hub protokollieren
                    String timeLog = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(Calendar.getInstance().getTime());
                    out.print(timeLog + "  " + "Sending message: " + msgStr, false);

                    synchronized (lockobj) {
                        lockobj.wait();
                    }
                    //Berechnung der Warte-Zeit
                    Long timeout = 60 / waitingTime;
                    //Warten bis zum nächsten Versandzeitpunkt
                    Thread.sleep(timeout * 1000);
                }
            } catch (InterruptedException e) {
                //Ende der Verarbeitung protokollieren
                out.print("Finished.", true);
                //Log beenden
                out.stop();
            }
        }
    }
}