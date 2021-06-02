package io.philbrick.minecraft.nbt;

public class NBTException extends RuntimeException {
    String message;

    public NBTException(String message) {
        this.message = message;
    }
}
