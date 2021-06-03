package com.pygostylia.osprey.nbt;

public class NBTException extends RuntimeException {
    String message;

    public NBTException(String message) {
        this.message = message;
    }
}
