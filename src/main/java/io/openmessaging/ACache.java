package io.openmessaging;

import java.nio.ByteBuffer;

public class ACache {

    private long startOffset;
    private long endOffset;

    public  ByteBuffer byteBuffer ;

    public boolean indexInCache(long s, long e) {
        return s >= startOffset && e <= endOffset;
    }


    public void add(int  size) {
        this.endOffset +=size;
    }

    public void addStart(int size){
        this.startOffset +=size;
        this.endOffset+=size;
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public ACache(ByteBuffer byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public long getStartOffset() {
        return startOffset;
    }
}
