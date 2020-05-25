package rccar.btremote;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

class ImageButtonListener implements View.OnClickListener, View.OnTouchListener {
    private String TAG = "ImageViewButtonListener";

    private String leftCmd  = "A";
    private String rightCnmd = "D";
    private String upCmd = "W";
    private String downCmd = "S";
    private String commonCmd = "Q;";
    private Vibrator vibrator;

    private int speedPwm = 1;

    private DirectionActivity.ConnectionThread connectionThread;
    private Context context;

    public ImageButtonListener(Context context, Vibrator vibrator) {
        this.context = context;
        this.vibrator = vibrator;
    }

    public void setConnectionThread(DirectionActivity.ConnectionThread connectionThread) {
        this.connectionThread = connectionThread;
    }

    public void setSpeedPwm(int speedPwm) {
        this.speedPwm = speedPwm;
    }

    @Override
    public void onClick(View view) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (view.getId()) {
            case R.id.btnleft:
                parseAction(event, leftCmd);
                break;
            case R.id.btnright:
                parseAction(event, rightCnmd);
                break;
            case R.id.btnup:
                parseAction(event, upCmd);
                break;
            case R.id.btndown:
                parseAction(event, downCmd);
                break;
        }
        return false;
    }

    private void parseAction(MotionEvent event, String cmd) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            sendUpCommand(commonCmd, event);
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            sendCommond(cmd, event);
        }
    }

    private boolean sendCommond(String cmd, MotionEvent event ) {
        if (this.connectionThread == null) {
            Toast.makeText(context, "没有连接到蓝牙设备", Toast.LENGTH_LONG).show();
            return false;
        }
        vibrator.vibrate(15);
        String outCmd = cmd + speedPwm + ";";
        connectionThread.writeBytes(outCmd.getBytes(), 0, outCmd.getBytes().length);
        Log.d(TAG, outCmd + event.toString());

        return true;
    }

    private boolean sendUpCommand(String cmd, MotionEvent event ) {
        if (this.connectionThread == null) {
            Toast.makeText(context, "没有连接到蓝牙设备", Toast.LENGTH_LONG).show();
            return false;
        }
        vibrator.vibrate(15);
        connectionThread.writeBytes(cmd.getBytes(), 0, cmd.getBytes().length);
        Log.d(TAG, cmd + event.toString());

        return true;
    }

}
