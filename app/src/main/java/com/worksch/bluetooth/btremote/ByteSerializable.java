package com.worksch.bluetooth.btremote;

public interface ByteSerializable {

    int toBytes(byte[] bytes, int off);

    String getName();

}
