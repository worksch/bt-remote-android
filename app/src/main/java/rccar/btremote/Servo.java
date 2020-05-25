package rccar.btremote;

import java.util.ArrayList;

public class Servo implements ByteSerializable {

    public interface ServoWatcher {
        void onPositionChanged(int position);
    }

    private int position = 0;
    private final String name;
    private final int maxAngle;
    private final int angleRangeMin, angleRangeMax;
    private ArrayList<ServoWatcher> watchers = new ArrayList<>();

    public Servo(String name, int maxAngle) {
        this.name = name;
        this.maxAngle = maxAngle;
        this.angleRangeMin = 0;
        this.angleRangeMax = maxAngle;
    }

    public Servo(String name, int maxAngle, int angleRangeMin, int angleRangeMax) {
        this.name = name;
        this.maxAngle = maxAngle;
        this.angleRangeMin = angleRangeMin;
        this.angleRangeMax = angleRangeMax;
    }

    public void addWatcher(ServoWatcher watcher) {
        this.watchers.add(watcher);
    }

    public void setPosition(int position) {
        if (this.position != position) {
            this.position = position;
            watchers.forEach(watcher -> watcher.onPositionChanged(position));
        }
    }

    public boolean isLegalPosition(int position) {
        return position >= angleRangeMin && position <= angleRangeMax;
    }

    public int getAngleRangeMax() {
        return angleRangeMax;
    }

    public int getAngleRangeMin() {
        return angleRangeMin;
    }

    public String getName() {
        return name;
    }

    @Override
    public int toBytes(byte[] bytes, int off) {
        int position = this.position * 180 / maxAngle;
        if (position > angleRangeMax) {
            position = angleRangeMax;
        } else if (position < angleRangeMin) {
            position = angleRangeMin;
        }
        byte lo = 0, hi = 0;
        lo |= (position >> 4) & (0xf);
        hi |= position & (0x0f);
        bytes[off] = lo;
        bytes[off + 1] = hi;
        return 2;
    }

    public int getPosition() {
        return position;
    }
}
