import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import javax.sound.midi.Receiver;

public class BluetoothMain {

    public static void main(String[] args) {
        log("Local Bluetooth device...\n");

        LocalDevice local = null;

        try {
            local = LocalDevice.getLocalDevice();
        } catch (BluetoothStateException e2) {

        }

        log("address: " + local.getBluetoothAddress());
        log("name: " + local.getFriendlyName());

        Runnable runnable = new ServerRunnable();
        Thread thread = new Thread(runnable);
        thread.start();
    }

    private static void log(String msg) {
        System.out.println("[" + (new Date()) + "]");
    }
}

class ServerRunnable implements Runnable {
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:" + uuid + ";name=SPP Server";

    private StreamConnectionNotifier mStreamConnectionNotifier = null;
    private StreamConnection mStreamConnction = null;
    private int count = 0;

    @Override
    public void run() {
        try {
            mStreamConnectionNotifier = (StreamConnectionNotifier) Connector.open(CONNECTION_URL_FOR_SPP);
            log("Opened connection successful.");
        } catch (IOException e) {
            log("Could not open connection: " + e.getMessage());
            return;
        }
        log("Server is now running.");

        while(true) {
            log("Wait for client requests...");

            try {
                mStreamConnction = mStreamConnectionNotifier.acceptAndOpen();
            } catch (IOException e1) {
                log("Could not open connection: " + e1.getMessage());
            }
            count++;
            log("현재 접속 중인 클라이언트 수: " + count);

            new Receiver(mStreamConnction).start();
        }
    }

    class Receiver extends Thread {
        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private String mRemoteDeviceString = null;
        private StreamConnection mStreamConnection = null;

        Receiver(StreamConnection streamConnection) {
            mStreamConnction = streamConnection;

            try {
                mInputStream = mStreamConnction.openInputStream();
                mOutputStream = mStreamConnction.openOutputStream();

                log("Open streams...");
            } catch (IOException e) {
                log("Couldn't open Stream: " + e.getMessage());

                Thread.currentThread().interrupt();
                return;
            }

            try {
                RemoteDevice remoteDevice = RemoteDevice.getRemoteDevice(mStreamConnction);
                mRemoteDeviceString = remoteDevice.getBluetoothAddress();

                log("Remote device");
                log("address: " + mRemoteDeviceString);
            } catch (IOException e1) {
                log("Found device, but couldn't connect to it: " + e1.getMessage());
                return;
            }
            log("Client is connected...");
        }

        @Override
        public void run() {
            try {
                Reader mReader = new BufferedReader(new InputStreamReader(mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));

                boolean isDisconnected = false;

                Sender("에코 서버에 접속하셨습니다.");
                Sender("보내신 문자를 에코해드립니다.");

                while (true) {
                    log("Ready");

                    StringBuilder stringBuilder = new StringBuilder();
                    int c = 0;

                    while ('\n' != (char) (c = mReader.read())) {
                        if (c == -1) {
                            log("Client has been disconnected");

                            count--;

                            log("현재 접속 중인 클라이언트 수: " + count);

                            isDisconnected = true;
                            Thread.currentThread().interrupt();

                            break;
                        }
                        stringBuilder.append((char) c);
                    }

                    if (isDisconnected) {
                        break;
                    }

                    String recvMessage = stringBuilder.toString();
                    log(mRemoteDeviceString + ": " + recvMessage);

                    Sender(recvMessage);
                }
            } catch (IOException e) {
                log("Receiver closed" + e.getMessage());
            }
        }

        void Sender(String msg) {
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    mOutputStream, Charset.forName(StandardCharsets.UTF_8.name()))));

            printWriter.write(msg + "\n");
            printWriter.flush();

            log("Me: " + msg);
        }
    }

    private static void log(String msg) {
        System.out.println("[" + (new Date()) + "] " + msg);
    }
}

