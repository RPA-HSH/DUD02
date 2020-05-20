// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

// This application uses the Azure IoT Hub device SDK for Java
// For samples see: https://github.com/Azure/azure-iot-sdk-java/tree/master/device/iot-device-samples

import com.microsoft.azure.sdk.iot.device.*;
import com.google.gson.Gson;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SimulatedDevice {
    // The device connection string to authenticate the device with your IoT hub.
    // Using the Azure CLI:
    // az iot hub device-identity show-connection-string --hub-name {YourIoTHubName} --device-id MyJavaDevice --output table
    private String connString;
    private String deviceID;

    // Using the MQTT protocol to connect to IoT Hub
    private IotHubClientProtocol protocol = IotHubClientProtocol.MQTT;
    private DeviceClient client;
    private long waitingTime;
    private Output out;

    //Für Ausführung
    private ExecutorService executor;

    // Constructor zum setzen der Parameter
    public SimulatedDevice(String connString, String deviceID, long waitingTime, Output out) {
        this.deviceID = deviceID;
        this.connString = connString;
        this.deviceID = deviceID;
        this.waitingTime = waitingTime;
        this.out = out;
    }

    public void startSimulation() {

        // Connect to the IoT hub.
        try {
            client = new DeviceClient(connString, protocol);
            client.open();
            MessageSender sender = new MessageSender();
            executor = Executors.newFixedThreadPool(1);
            executor.execute(sender);

        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }

    }

    public void stopSimulation() {
        executor.shutdownNow();
        try {
            client.closeNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // Specify the telemetry to send to your IoT hub.
    private class TelemetryDataPoint {
        public String deviceID;
        public double temperature;
        public double humidity;
        public double airPressure;
        public double wind;

        // Serialize object to JSON format.
        public String serialize() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    // Print the acknowledgement received from IoT Hub for the telemetry message sent.
    private class EventCallback implements IotHubEventCallback {
        public void execute(IotHubStatusCode status, Object context) {
            out.print("IoT Hub responded to message with status: " + status.name(), 0);

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
                // Initialize the simulated telemetry.
                double minTemperature = 20;
                double minHumidity = 60;
                Random rand = new Random();

                while (true) {
                    ZoneId z = ZoneId.of("Europe/Berlin");
                    ZonedDateTime now = ZonedDateTime.now(z);
                    ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(z);
                    Duration duration = Duration.between(todayStart, now);
                    double secondsSoFarToday = duration.getSeconds();
                    // Übersetzung in prozentualer Anteil vom Tag
                    double dayPercentage = secondsSoFarToday / (60 * 60 * 24);
                    // für Sinus-Funktion mal PI nehmen (Annahme = PI = 24 Stunden)
                    double dayPercentagePi = Math.PI * dayPercentage;
                    double sinusFactor = Math.sin(dayPercentagePi);
                    System.out.println(sinusFactor);
                    //setup random noise
                    Random random = new Random();

                    // Simulate telemetry.
                    double randomNoise = random.nextInt(30) - 15;
                    double currentTemperature = 20 * sinusFactor + (20 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentHumidity = 50 * sinusFactor + (50 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentAirPressure = 60 * sinusFactor + (60 * (randomNoise / 100));
                    randomNoise = random.nextInt(15) - 15;
                    double currentWind = 35 * sinusFactor + (35 * (randomNoise / 100));

                    TelemetryDataPoint telemetryDataPoint = new TelemetryDataPoint();
                    telemetryDataPoint.temperature = currentTemperature;
                    telemetryDataPoint.humidity = currentHumidity;
                    telemetryDataPoint.airPressure = currentAirPressure;
                    telemetryDataPoint.wind = currentWind;

                    telemetryDataPoint.deviceID = deviceID;

                    // Add the telemetry to the message body as JSON.
                    String msgStr = telemetryDataPoint.serialize();
                    Message msg = new Message(msgStr);

                    out.print("Sending message: " + msgStr, 1);

                    Object lockobj = new Object();

                    // Send the message.
                    EventCallback callback = new EventCallback();
                    client.sendEventAsync(msg, callback, lockobj);

                    synchronized (lockobj) {
                        lockobj.wait();
                    }
                    //Berechnung der Zeit, die gewartet werden soll, in Sekunden
                    Long timeout = 60 / waitingTime;
                    Thread.sleep(timeout * 1000);
                }
            } catch (InterruptedException e) {
                out.print("Finished.", 0);
                out.stop();
            }
        }
    }


}