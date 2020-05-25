package rccar.btremote;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.xw.repo.BubbleSeekBar;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivityb";

    private static final int REQUEST_ENABLE_BT = 1;

    private static final UUID SPP = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private float servoSpeed = 180;
    private TextView statusTextView;
    private TextView debugTextView;
    private Button connectButton;
    private Button disconnectButton;
    private Switch autoReconnectSwitch;
    private Switch sameSpeedSwitch;
    private Switch congdongSwitch;
    private JoystickView joystickView;
    private ConnectionThread connectionThread;
    private BluetoothDevice targetDevice;
    private SendPackThread sendPackThread;
    private TextView voltageView;
    private TextView pingView;

    private Button positionA;
    private Button positionB;
    private Button positionC;
    private Button positionTightInitial;
    private Button positionRetrieveButton;
    private Button positionSlope;
    private Button positionCurve;

    /*private Button grabSquareTopButton;
    private Button grabSquareBottomButton;
    private Button grabCircleMiddleButton;
    private Button grabCircleBottomButton;
    private Button grabHexagonButton;*/
    private long lastSendTime = 0;

    private Button grabObjectButton;
    private Button releaseObjectButton;
    private Button oneClickButton;
    private Button swapModeButton;

    private Spinner itemSelectSpinner;
    private EditText angleEdit;
    private Button angleApplyButton;
    private Servo selectedServo;

    private BubbleSeekBar servo1SeekBar;
    private BubbleSeekBar servo2SeekBar;
    private BubbleSeekBar servo3SeekBar;
    private BubbleSeekBar leftSpeedSeekBar;
    private BubbleSeekBar rightSpeedSeekBar;

    private Servo servo1;
    private Servo servo2;
    private Servo servo3;
    private Servo servo4;
    private Motor motorLeft = new Motor("MotorLeft");
    private Motor motorRight = new Motor("MotorRight");
    private ArrayList<ByteSerializable> byteSerializableArrayList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusTextView = findViewById(R.id.status_text_view);
        connectButton = findViewById(R.id.connect_button);
        disconnectButton = findViewById(R.id.disconnect_button);
        autoReconnectSwitch = findViewById(R.id.auto_reconnect_switch);
        sameSpeedSwitch = findViewById(R.id.same_speed_switch);
        congdongSwitch = findViewById(R.id.congdong);
        joystickView = findViewById(R.id.joystick);
        debugTextView = findViewById(R.id.debug_info);
        positionA = findViewById(R.id.position_a);
        positionB = findViewById(R.id.position_b);
        positionC = findViewById(R.id.position_c);
        positionTightInitial = findViewById(R.id.tight_initial);
        positionRetrieveButton = findViewById(R.id.position_retrieve);
        swapModeButton = findViewById(R.id.swap_button);

        /*grabSquareTopButton = findViewById(R.id.grab_square_top);
        grabSquareBottomButton = findViewById(R.id.grab_square);
        grabCircleMiddleButton = findViewById(R.id.grab_circle_middle);
        grabCircleBottomButton = findViewById(R.id.grab_circle);
        grabHexagonButton = findViewById(R.id.grab_hexagon);*/
        grabObjectButton = findViewById(R.id.grab_object);
        releaseObjectButton = findViewById(R.id.release_object);

        oneClickButton = findViewById(R.id.one_click_up_stair);

        itemSelectSpinner = findViewById(R.id.item_select);
        angleEdit = findViewById(R.id.angle_text);
        angleApplyButton = findViewById(R.id.angle_apply);
        voltageView = findViewById(R.id.voltage_view);
        pingView = findViewById(R.id.ping_view);
        positionSlope = findViewById(R.id.position_slope);
        positionCurve = findViewById(R.id.position_curve);

        swapModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, DirectionActivity.class);
                startActivity(intent);
            }
        });

        connectButton.setOnClickListener(event -> showDevicesDialog(this::connect));
        disconnectButton.setOnClickListener(event -> disconnect());
        joystickView.setOnMoveListener(this::onMove);
        positionA.setOnClickListener(event -> setPositionInitial());
        positionB.setOnClickListener(event -> safeSmoothSetServoPositions(24, 24, 30));
        positionC.setOnClickListener(event -> safeSmoothSetServoPositions(40, 40, 30));
        positionTightInitial.setOnClickListener(event -> safeSmoothSetServoPositions(82, 163, 175));
        positionSlope.setOnClickListener(event -> safeSmoothSetServoPositions(30, 55, 118));
        positionCurve.setOnClickListener(event -> safeSmoothSetServoPositions(53, 163, 175));
        oneClickButton.setOnClickListener(event -> safeSmoothUpStair());

        positionRetrieveButton.setOnClickListener(event -> smoothSetServoPositions(78, 163, 115));

        servo1SeekBar = findViewById(R.id.servo1_seek_bar);
        servo2SeekBar = findViewById(R.id.servo2_seek_bar);
        servo3SeekBar = findViewById(R.id.servo3_seek_bar);
        leftSpeedSeekBar = findViewById(R.id.left_speed_seek_bar);
        rightSpeedSeekBar = findViewById(R.id.right_speed_seek_bar);

        // 80, 165

        // square 165, 125
        // circle 160, 130
        // hexagon 135

        /*grabSquareBottomButton.setOnClickListener(event -> servo4.setPosition(125));
        grabSquareTopButton.setOnClickListener(event -> servo4.setPosition(165));
        grabCircleBottomButton.setOnClickListener(event -> servo4.setPosition(130));
        grabCircleMiddleButton.setOnClickListener(event -> servo4.setPosition(160));
        grabHexagonButton.setOnClickListener(event -> servo4.setPosition(135));*/
        grabObjectButton.setOnClickListener(event -> servo4.setPosition(170));
        releaseObjectButton.setOnClickListener(event -> servo4.setPosition(90));

        angleApplyButton.setOnClickListener(event -> {
            if (selectedServo != null) {
                try {
                    int value = Integer.parseInt(String.valueOf(angleEdit.getText()));
                    if (selectedServo.isLegalPosition(value)) {
                        selectedServo.setPosition(value);
                    }
                } catch (NumberFormatException ignore) {
                    angleEdit.setError("应为整数");
                }
            }
        });
        angleEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if (selectedServo != null) {
                    try {
                        int value = Integer.parseInt(String.valueOf(angleEdit.getText()));
                        if (!selectedServo.isLegalPosition(value)) {
                            angleEdit.setError("范围: [" + selectedServo.getAngleRangeMin() + ", " + selectedServo.getAngleRangeMax() + "]");
                        }
                    } catch (NumberFormatException ignore) {
                        angleEdit.setError("应为整数");
                    }
                }
            }
        });

        leftSpeedSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                if (sameSpeedSwitch.isChecked() && rightSpeedSeekBar.getProgress() != progress) {
                    rightSpeedSeekBar.setProgress(progress);
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        rightSpeedSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                if (sameSpeedSwitch.isChecked() && leftSpeedSeekBar.getProgress() != progress) {
                    leftSpeedSeekBar.setProgress(progress);
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        sameSpeedSwitch.setOnCheckedChangeListener((event, checked) -> {
            if (checked) {
                rightSpeedSeekBar.setProgress(leftSpeedSeekBar.getProgress());
            }
        });

        initCar();
    }


    private void showDevicesDialog(Consumer<BluetoothDevice> callback) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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

    private void onBluetoothStatusChange(int status, String message) {
        if (status == ConnectionThread.DISCONNECTED || (!shouldAutoReconnect() && status == ConnectionThread.ERROR_DISCONNECTED)) {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                statusTextView.setText("Disconnected!");
            });

            if (connectionThread != null) {
                connectionThread.closeConnection();
            }
        } else if (status == ConnectionThread.ERROR_DISCONNECTED) {
            runOnUiThread(() -> {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                statusTextView.setText("Disconnected! Trying to reconnect...");
            });

            reconnect();
        } else if (status == ConnectionThread.MESSAGE) {
            runOnUiThread(() -> statusTextView.setText(message));
        } else if (status == ConnectionThread.VOLTAGE) {
            runOnUiThread(() -> voltageView.setText(message));
        } else if (status == ConnectionThread.PING) {
            runOnUiThread(() -> pingView.setText(message));
        }
    }

    private void disconnect() {
        if (connectionThread != null) {
            connectionThread.closeConnection();
            statusTextView.setText("Disconnected!");
        }
    }

    private void connect(BluetoothDevice device) {

        if (connectionThread != null) {
            connectionThread.closeConnection();
        }

        try {
            targetDevice = device;
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(SPP);
            connectionThread = new ConnectionThread(socket, this::onBluetoothStatusChange);
            connectionThread.start();

            /*if (sendPackThread != null) {
                sendPackThread.setRunning(false);
            }
            sendPackThread = new SendPackThread();
            sendPackThread.start();*/

        } catch (IOException e) {
            onBluetoothStatusChange(ConnectionThread.DISCONNECTED, e.getMessage());
        }


    }

    private void setPositionInitial() {
        setServoPositions(82, 163, 175, 90);
    }

    private void setServoPositions(int position1, int position2, int position3, int position4) {
        servo1.setPosition(position1);
        servo2.setPosition(position2);
        servo3.setPosition(position3);
        servo4.setPosition(position4);
    }

    private static float linearInsert(float a1, float a2, float p) {
        return (p * (a2 - a1)) + a1;
    }

    // 1 up, 2 down  : 1, 2
    // 1 down, 2 up  : 2, 1
    // 1 up, 2 up    : 1, 2
    // 1 down, 2 down: 2, 1

    private void safeSmoothSetServoPositions(int position1, int position2, int position3) {
        boolean up1 = servo1.getPosition() < position1;
        boolean up2 = servo2.getPosition() < position2;

        if (up1) {
            smoothSetServoPositions(position1, servo2.getPosition(), position3);
            smoothSetServoPositions(servo1.getPosition(), position2, position3);
        } else {
            smoothSetServoPositions(servo1.getPosition(), position2, position3);
            smoothSetServoPositions(position1, servo2.getPosition(), position3);
        }

    }

    private UpStairThread upStairThread;

    private class UpStairThread extends Thread {

        @Override
        public void run() {
            try {
                if (isInterrupted()) return;
                safeSmoothSetServoPositions(40, 112, 81);
                if (isInterrupted()) return;
                Thread.sleep(200);
                if (isInterrupted()) return;
                servo4.setPosition(170);
                if (isInterrupted()) return;
                Thread.sleep(1000);
                if (isInterrupted()) return;
                safeSmoothSetServoPositions(33, 41, 109);
                if (isInterrupted()) return;
                runForward(1000);
                if (isInterrupted()) return;
                safeSmoothSetServoPositions(78, 123, 113);
                if (isInterrupted()) return;
                runForward(1000);
                if (isInterrupted()) return;
                safeSmoothSetServoPositions(78, 163, 179);
                if (isInterrupted()) return;
            } catch (InterruptedException e) {
                return;
            }

        }
    }
    private void safeSmoothUpStair() {
        // 伸出至抓取位置
        if (upStairThread == null || !upStairThread.isAlive()) {
            upStairThread = new UpStairThread();
            upStairThread.start();
        } else {
            upStairThread.interrupt();
        }
    }

    private void runForward(long millis) {
        try {
            motorLeft.setSpeed(255);
            motorRight.setSpeed(255);
            Thread.sleep(millis);
            motorLeft.setSpeed(0);
            motorRight.setSpeed(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void smoothSetServoPositions(int position1, int position2, int position3) {
        int delta1 = Math.abs(servo1.getPosition() - position1);
        int delta2 = Math.abs(servo2.getPosition() - position2);
        int delta3 = Math.abs(servo3.getPosition() - position3);
        int servo1BeginPosition = servo1.getPosition();
        int servo2BeginPosition = servo2.getPosition();
        int servo3BeginPosition = servo3.getPosition();
        int maxDelta = Math.max(delta1, Math.max(delta2, delta3));
        float timeConsumption = (maxDelta / servoSpeed * 1000);
        int step = 30;
        for (int i = 0; i < timeConsumption; i += step) {
            try {
                Thread.sleep(step);
            } catch (InterruptedException ignore) {
                break;
            }
            servo1.setPosition((int)linearInsert(servo1BeginPosition, position1, i / timeConsumption));
            servo2.setPosition((int)linearInsert(servo2BeginPosition, position2, i / timeConsumption));
            servo3.setPosition((int)linearInsert(servo3BeginPosition, position3, i / timeConsumption));
        }

        servo1.setPosition(position1);
        servo2.setPosition(position2);
        servo3.setPosition(position3);
    }

    private void initCar() {
        servo1 = new Servo("Servo1", 270, 0, 82);
        servo2 = new Servo("Servo2", 270, 0, 163);
        servo3 = new Servo("Servo3", 180, 0, 175);
        servo4 = new Servo("Servo4", 180, 90, 170);

        byteSerializableArrayList.add(servo1);
        byteSerializableArrayList.add(servo2);
        byteSerializableArrayList.add(servo3);
        byteSerializableArrayList.add(servo4);
        byteSerializableArrayList.add(motorLeft);
        byteSerializableArrayList.add(motorRight);
        motorLeft.setDirection(Motor.Direction.FORWARD);
        motorRight.setDirection(Motor.Direction.REVERSE);

        final Servo[] servoList = {servo1, servo2, servo3, servo4};
        final String[] servoNameList = {servo1.getName(), servo2.getName(), servo3.getName(), servo4.getName()};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, servoNameList);
        itemSelectSpinner.setAdapter(adapter);
        itemSelectSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedServo = servoList[position];
                angleEdit.setText(String.valueOf(selectedServo.getPosition()));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedServo = null;
                angleEdit.setText("");
            }
        });
        setPositionInitial();


        servo1SeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                if (progress != servo1.getPosition()) {
                    if (fromUser) {
                        servo3.setPosition(servo3Target(progress, servo2.getPosition()));
                    }
                    servo1.setPosition(progress);
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        servo2SeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                if (progress != servo2.getPosition()) {
                    if (fromUser) {
                        servo3.setPosition(servo3Target(servo1.getPosition(), progress));
                    }
                    servo2.setPosition(progress);
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });
        servo3SeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {
                if (fromUser) {
                    int targetPosition = progress; // - (servo1.getPosition() - servo2.getPosition());
                    if (servo3.getPosition() != targetPosition) {
                        servo3.setPosition(targetPosition);
                    }
                }
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat, boolean fromUser) {

            }
        });

        servo1.addWatcher(value -> servo1SeekBar.setProgress(value));
        servo2.addWatcher(value -> servo2SeekBar.setProgress(value));
        servo3.addWatcher(value -> servo3SeekBar.setProgress(value));
    }

    private static double insertValue(double a1, double a2, double pos) {
        double value = (Math.cos(pos * Math.PI) + 1) / 2;
        return a1 * value + a2 * (1 - value);
    }

    private static double insertValue(double a1, double a2, double b1, double b2, double b) {
        return insertValue(a1, a2, (b - b1) / (b2 - b1));
    }


    private int servo3Target(int servo1Target, int servo2Target) {
        if (congdongSwitch.isChecked()) {
            int target = servo3.getPosition() + (servo1.getPosition() - servo2.getPosition()) - (servo1Target - servo2Target);
            if (servo3.isLegalPosition(target)) {
                return target;
            } else if (target > servo3.getAngleRangeMax()) {
                return servo3.getAngleRangeMax();
            } else {
                return servo3.getAngleRangeMin();
            }
        } else {
            return servo3.getPosition();
        }
    }

    private void onMove(int angle, int strength) {
        int leftStrength = (int)insertValue(0, leftSpeedSeekBar.getProgress(), strength / 100d);
        int rightStrength = (int)insertValue(0, rightSpeedSeekBar.getProgress(), strength / 100d);
        double l, r;
        if (angle >= 30 && angle < 90) {
            // right small turn
            l = 1;
            r = insertValue(0.6, 1, 30, 90, angle);
        } else if (angle >= 90 && angle < 150) {
            // left small turn
            l = insertValue(1, 0.6, 90, 150, angle);
            r = 1;
        } else if (angle >= 150 && angle < 210) {
            // left in-place turn
            l = 1;
            r = -1;
        } else if (angle >= 210 && angle < 270) {
            // left small turn (backward)
            l = insertValue(-0.6, -1, 210, 270, angle);
            r = -1;
        } else if (angle >= 270 && angle < 330) {
            // right small turn (backward)
            l = -1;
            r = insertValue(-1, -0.6, 270, 330, angle);
        } else {
            l = -1;
            r = 1;
        }
        motorLeft.setSpeed((int)(l * leftStrength));
        motorRight.setSpeed((int)(r * rightStrength));
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
        runOnUiThread(() -> debugTextView.setText(message));
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

    @FunctionalInterface
    public interface StatusCallback {
        void onStatus(int status, String message);
    }

    private class ConnectionThread extends Thread {

        private BluetoothSocket socket;
        private StatusCallback callback;
        private InputStream inputStream;
        private OutputStream outputStream;
        private boolean manuallyClosed = false;
        private byte[] buffer = new byte[1024];
        int bufferLen = 0;


        public static final int DISCONNECTED = 1;
        public static final int ERROR_DISCONNECTED = 3;
        public static final int MESSAGE = 2;
        public static final int VOLTAGE = 4;
        public static final int PING = 5;


        public ConnectionThread(BluetoothSocket socket, StatusCallback callback) {
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
                callback.onStatus(MESSAGE, "Connected!");
                while (true) {
                    try {
                        if (inputStream == null) {
                            callback.onStatus(MESSAGE, "?????" + (++count));
                        } else {
                            while (inputStream.available() > 0) {
                                int b = inputStream.read();
                                if (b == 0x0A) {
                                    bufferLen = 0;
                                } else {
                                    if (bufferLen < 100) {
                                        buffer[bufferLen++] = (byte) b;
                                    }
                                }
                                if (bufferLen == 2) {
                                    int lo = buffer[0] & 0b11111;
                                    int hi = buffer[1] & 0b11111;
                                    int vol = lo | hi << 5;
                                    double voltage = vol / 1024d * 5 * 3;
                                    callback.onStatus(VOLTAGE, String.format(Locale.US, "%.2fV", voltage));
                                    callback.onStatus(PING, (System.currentTimeMillis() - lastSendTime) + "ms");
                                }
                            }
                            //callback.onStatus(MESSAGE, message);
                        }
                    } catch (IOException e) {
                        callback.onStatus(ERROR_DISCONNECTED, "Read from input stream failed.");
                        closeConnection();
                        break;
                    }
                }
            } catch (IOException e) {
                callback.onStatus(ERROR_DISCONNECTED, "Failed to open I/O stream.");
                e.printStackTrace();
                Log.d(TAG, e.getMessage());
            }
        }

        public void writeBytes(byte[] bytes, int off, int len) {
            try {
                outputStream.write(bytes, off, len);
                outputStream.flush();
            } catch (IOException e) {
                callback.onStatus(ERROR_DISCONNECTED, "Write bytes failed.");
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
