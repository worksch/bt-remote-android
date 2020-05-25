package com.worksch.bluetooth.btremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static java.util.Arrays.fill;

public class DirectionActivity extends Activity {
    private String TAG = "DirectionActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Button connectButton;
    private Button disconnectButton;
    private Switch autoReconnectSwitch;
    private TextView statusTextView;
    private TextView voltageView;
    private TextView pingView;

    private Button speedButton;
    private TextView gearText;
    public int speedPwm = 1;

    // 方向控制键
    private ImageButtonListener imageButtonListener;
    private ImageButton leftButton;
    private ImageButton rightButton;
    private ImageButton upButton;
    private ImageButton downButton;
    private Vibrator vibrator;

    private long lastSendTime = 0;
    private ConnectionThread connectionThread;
    private BluetoothDevice targetDevice;
    private SendPackThread sendPackThread;
    private ArrayList<ByteSerializable> byteSerializableArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }
        setContentView(R.layout.activity_direction_control);

        vibrator = (Vibrator)this.getSystemService(this.VIBRATOR_SERVICE);

        connectButton = findViewById(R.id.connect_button);
        disconnectButton = findViewById(R.id.disconnect_button);
        autoReconnectSwitch = findViewById(R.id.auto_reconnect_switch);
        statusTextView = findViewById(R.id.status_text_view);
        voltageView = findViewById(R.id.voltage_view);
        pingView = findViewById(R.id.ping_view);

        speedButton = findViewById(R.id.speed_button);
        gearText = findViewById(R.id.gear_text);

        leftButton = findViewById(R.id.btnleft);
        rightButton = findViewById(R.id.btnright);
        upButton = findViewById(R.id.btnup);
        downButton = findViewById(R.id.btndown);

        imageButtonListener = new ImageButtonListener(this, vibrator);
        leftButton.setOnTouchListener(imageButtonListener);
        rightButton.setOnTouchListener(imageButtonListener);
        upButton.setOnTouchListener(imageButtonListener);
        downButton.setOnTouchListener(imageButtonListener);

        connectButton.setOnClickListener(event -> showDevicesDialog(this::connect));
        disconnectButton.setOnClickListener(event -> disconnect());

        speedButton.setOnClickListener(event -> {
            speedPwm ++;
            if (speedPwm > 5) {
                speedPwm = 1;
            }
            gearText.setText(""+speedPwm);
            imageButtonListener.setSpeedPwm(speedPwm);
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (connectionThread != null) {
            connectionThread.closeConnection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (connectionThread != null) {
            connectionThread.closeConnection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectionThread != null) {
            connectionThread.closeConnection();
        }
    }

    @Override
    public void finish() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("提示");
        dialog.setMessage("是否退出当前程序？");
        dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                connectionThread.closeConnection();
                System.exit(0);
            }
        });
        dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getApplicationContext(), "取消", Toast.LENGTH_LONG).show();
            }
        });
        if (dialog != null) {
            dialog.show();
        }
    }

    /**
     * 蓝牙连接
     */
    private void connect(BluetoothDevice device) {

        if (connectionThread != null) {
            connectionThread.closeConnection();
        }

        try {
            targetDevice = device;
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP);
            connectionThread = new ConnectionThread(socket, this::onBluetoothStatusChange);
            connectionThread.start();

            imageButtonListener.setConnectionThread(connectionThread);

        } catch (IOException e) {
            onBluetoothStatusChange(ConnectionThread.DISCONNECTED, e.getMessage());
        }
    }

    /**
     * 蓝牙断开
     */
    private void disconnect() {
        if (connectionThread != null) {
            connectionThread.closeConnection();
            statusTextView.setText("蓝牙已断开!!!");
        }
    }

    private void showDevicesDialog(Consumer<BluetoothDevice> callback) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "获取蓝牙适配器出错，需要更新此App。", Toast.LENGTH_LONG).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            return;
        }
        Set<BluetoothDevice> devicesSet = bluetoothAdapter.getBondedDevices();
        BluetoothDevice[] devicesArray = devicesSet.toArray(new BluetoothDevice[devicesSet.size()]);
        if (devicesSet.isEmpty()) {
            Toast.makeText(this, "Paired devices not found", Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setItems(Arrays.stream(devicesArray).map(BluetoothDevice::getName)
                .toArray(String[]::new), (event, index)->callback.accept(devicesArray[index]));

        builder.show();
    }
    private boolean shouldAutoReconnect() {
        return autoReconnectSwitch.isChecked();
    }

    private void reconnect() {
        if (targetDevice != null) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException ignore) {

            }
            if (!connectionThread.isConnected()) {
                runOnUiThread(() -> connect(targetDevice));
            }
        }
    }

    private void onBluetoothStatusChange(int status, String message) {
        if (status == ConnectionThread.DISCONNECTED || (!shouldAutoReconnect() && status == ConnectionThread.ERROR_DISCONNECTED)) {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                statusTextView.setText("蓝牙已断开!!!");
            });

            if (connectionThread != null) {
                connectionThread.closeConnection();
            }
        }else if (status == ConnectionThread.ERROR_DISCONNECTED) {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                statusTextView.setText("Disconnected! Trying to reconnect...");
            });
            reconnect();
        }else if (status == ConnectionThread.MESSAGE) {
            runOnUiThread(() -> statusTextView.setText(message));
        }else if (status == ConnectionThread.VOLTAGE) {
            runOnUiThread(() -> voltageView.setText(message));
        } else if (status == ConnectionThread.PING) {
            runOnUiThread(() -> pingView.setText(message));
        }
    }

    private void sendPack() {
        byte[] cache = new byte[200];
        cache[0] |= 0b10000000;
        int off = 1;
        for (ByteSerializable serializable : byteSerializableArrayList) {
            int len = serializable.toBytes(cache, off);
            off += len;
        }

        StringBuffer buffer = new StringBuffer();
        for (ByteSerializable serializable : byteSerializableArrayList) {
            if (serializable instanceof Servo) {
                buffer.append(serializable.getName()).append(": ")
                        .append(((Servo) serializable).getPosition()).append('\n');
            } else if (serializable instanceof Motor) {
                buffer.append(serializable.getName()).append(": ")
                        .append(((Motor) serializable).getDirection().name())
                        .append(" ")
                        .append(((Motor) serializable).getSpeed()).append('\n');
            }
        }
        String message = buffer.toString();

        if (connectionThread.isConnected()) {
            connectionThread.writeBytes(cache, 0, off);
        }
        //runOnUiThread(() -> debugTextView.setText(message));
    }

    public class ConnectionThread extends Thread {

        private BluetoothSocket socket;
        private MainActivity.StatusCallback callback;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean manuallyClosed = false;
        private byte[] buffer = new byte[128];
        int bufferLen = 0;


        public static final int DISCONNECTED = 1;
        public static final int ERROR_DISCONNECTED = 3;
        public static final int MESSAGE = 2;
        public static final int VOLTAGE = 4;
        public static final int PING = 5;


        public ConnectionThread(BluetoothSocket socket, MainActivity.StatusCallback callback) {
            this.socket = socket;
            this.callback = callback;
        }


        int count = 0;
        @Override
        public void run() {
            try {
                socket.connect();
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
                callback.onStatus(MESSAGE, "蓝牙已连接!!!");
                while (true) {
                    try {
                        if (inputStream == null) {
                            callback.onStatus(MESSAGE, "?????" + (++count));
                        } else {
                            while (inputStream.available() > 0) {
                                int inByte = inputStream.read();
                                if (inByte != 0x0A) {
                                    buffer[bufferLen++] = (byte) inByte;
                                } else {
                                    bufferLen = 0;
                                    Log.d(TAG, new String(buffer));
                                    fill(buffer, (byte)'\0');
                                }
                                /*if (bufferLen == 2) {
                                    int lo = buffer[0] & 0b11111;
                                    int hi = buffer[1] & 0b11111;
                                    int vol = lo | hi << 5;
                                    double voltage = vol / 1024d * 5 * 3;
                                    callback.onStatus(VOLTAGE, String.format(Locale.US, "%.2fV", voltage));
                                    callback.onStatus(PING, (System.currentTimeMillis() - lastSendTime) + "ms");
                                }*/
                            }
                            //callback.onStatus(MESSAGE, message);
                        }
                    } catch (IOException e) {
                        //callback.onStatus(ERROR_DISCONNECTED, "Read from input stream failed.");
                        closeConnection();
                        break;
                    }
                }
            } catch (IOException e) {
                callback.onStatus(ERROR_DISCONNECTED, "请重启蓝牙模块!!!");
            }
        }

        public void writeBytes(byte[] bytes, int off, int len) {
            try {
                outputStream.write(bytes, off, len);
                outputStream.flush();
            } catch (IOException e) {
                //callback.onStatus(ERROR_DISCONNECTED, "Write bytes failed.");
                closeConnection();
            }
        }

        private void closeConnection() {
            if (!manuallyClosed) {
                manuallyClosed = true;
                try {
                    if (socket.isConnected()) {
                        socket.close();
                    }
                } catch (IOException e) {
                    callback.onStatus(DISCONNECTED, "Failed to close socket.");
                }
            }
        }

        public boolean isConnected() {
            return socket.isConnected();
        }

    }

    private class SendPackThread extends Thread {
        private boolean isRunning = false;
        @Override
        public void run() {
            isRunning = true;
            while (isRunning) {
                try {
                    Thread.sleep(100);
                    sendPack();
                    lastSendTime = System.currentTimeMillis();
                } catch (InterruptedException ignore) {

                }
            }
        }
        public void setRunning(boolean running) {
            isRunning = running;
        }
    }
}
