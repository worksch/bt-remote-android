package com.worksch.bluetooth.btremote;

import static com.worksch.bluetooth.btremote.Motor.Direction.FORWARD;
import static com.worksch.bluetooth.btremote.Motor.Direction.REVERSE;
import static com.worksch.bluetooth.btremote.Motor.Direction.STOP;

public class Motor implements ByteSerializable {

    private final String name;
    private int speed = 0;
    private Direction direction = STOP;

    public Motor(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    private Direction opposite(Direction direction) {
        if (direction == STOP) {
            return STOP;
        } else if (direction == FORWARD) {
            return REVERSE;
        } else if (direction == REVERSE) {
            return FORWARD;
        }
        return FORWARD;
    }

    public enum Direction {
        FORWARD, REVERSE, STOP
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public void setDirectionAndSpeed(Direction direction, int speed) {
        this.direction = direction;
        this.speed = speed;
    }

    public Direction getDirection() {
        return direction;
    }

    public int getSpeed() {
        return speed;
    }

    @Override
    public int toBytes(byte[] bytes, int off) {
        int speed;
        Direction direction;
        if (this.speed >= 0) {
            speed = this.speed;
            direction = this.direction;
        } else {
            speed = -this.speed;
            direction = opposite(this.direction);
        }


        byte lo = 0, hi = 0;
        hi |= (speed >> 4) & (0x0f);
        lo |= speed & (0x0f);
        switch (direction) {
            case FORWARD:
                hi |= (0x10);
                break;
            case REVERSE:
                lo |= (0x10);
                break;
            case STOP:
                hi |= (0x10);
                lo |= (0x10);
                break;
        }
        bytes[off] = hi;
        bytes[off + 1] = lo;
        return 2;
    }
}
