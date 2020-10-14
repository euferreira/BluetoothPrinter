package com.br.bluetoothprinter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    OutputStream outputStream;
    InputStream inputStream;
    Thread thread;

    byte[] readBuffer;
    int readBufferPosition;
    volatile boolean stopWorker;

    Button btnConnect;
    Button btnDisconnect;
    Button btnPrinter;
    EditText txtText;
    TextView lblAttach;
    TextView lblPrinterName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create object of controls
        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnPrinter = findViewById(R.id.btnPrinter);
        txtText = findViewById(R.id.txtText);
        lblAttach = findViewById(R.id.lblAttachPrinter);
        lblPrinterName = findViewById(R.id.lblPrinterName);

        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnPrinter.setOnClickListener(this);
    }

    void findBluetoothDevice() {
        try {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                lblPrinterName.setText("Não encontrou impressora bluetooth");
            }
            if (bluetoothAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, 0);
            }

            Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
            if (pairedDevice.size() > 0) {
                for (BluetoothDevice pairedDev : pairedDevice) {

                    String nomeImpressora = pairedDev.getName();
                    //nome da impressora do cara
                    if (pairedDev.getName().equals("BTP_F09F1A")) {
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer attached: " + pairedDev.getName());
                        break;
                    } else if (pairedDev.getName().equals(nomeImpressora)) {
                        bluetoothDevice = pairedDev;
                        lblPrinterName.setText("Bluetooth Printer attached: " + nomeImpressora);
                        break;
                    }
                }
            }
            lblPrinterName.setText("Bluetooth Printer Attached");


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //OpenBluetoothPrinter
    void openBluetoothPrinter() throws IOException {
        try {
            //standar uuid from string
            UUID uuidString = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuidString);
            bluetoothSocket.connect();

            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();

            beginListenData();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    void beginListenData() {
        try {
            final Handler handler = new Handler();
            final byte delimater = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];

            thread = new Thread(new Runnable() {
                @Override
                public void run() {

                    while (!Thread.currentThread().isInterrupted() && stopWorker) {

                        try {
                            int byteAvailable = inputStream.available();
                            if (byteAvailable > 0) {
                                byte[] packetByte = new byte[byteAvailable];
                                inputStream.read(packetByte);

                                for (int i = 0; i < byteAvailable; i++) {
                                    byte b = packetByte[i];
                                    if (b == delimater) {
                                        byte[] encondedByte = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encondedByte, 0, encondedByte.length);
                                        final String data = new String(encondedByte, "US-ASCII");
                                        readBufferPosition = 0;
                                        handler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                lblPrinterName.setText(data);
                                            }
                                        });
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }

                            }


                        } catch (Exception e) {
                            e.printStackTrace();
                            stopWorker = true;
                        }

                    }

                }
            });
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //mandando imprimir
    void printData() throws IOException {

        try {
            String msg = txtText.getText().toString();

            String textoImpressao = "\n\n\nImpressao de Teste\n\n\n\n\n\n";


            msg += "\n";
            outputStream.write(textoImpressao.getBytes());
            lblPrinterName.setText("Printing text...");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //disconnect printer
    void disconnectPrinter() throws IOException {
        try {
            stopWorker = true;
            outputStream.close();
            inputStream.close();
            bluetoothSocket.close();
            lblPrinterName.setText("Printer disconnect");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btnPrinter) {
            try {
                printData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.btnConnect) {
            try {
                findBluetoothDevice();
                openBluetoothPrinter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (v.getId() == R.id.btnDisconnect) {
            try {
                disconnectPrinter();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
