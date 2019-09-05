package io.openmessaging;


import java.util.ArrayList;
import java.util.List;

public class Index {

    private int count;
    private int beforeCurCount;
    private long baseT;
    private long[] aMin;
    private long[] aMax;

    private long[] aSumArray;


    private byte aSize;

    private long aOffset;

    public long getaOffset() {
        return aOffset;
    }

    public Index setaOffset(long aOffset) {
        this.aOffset = aOffset;
        return this;
    }

    public byte getaSize() {
        return aSize;
    }

    public Index setaSize(byte aSize) {
        this.aSize = aSize;
        return this;
    }

    private List<Long> lastA = new ArrayList<>();

    public Index setCount(int count) {
        this.count = count;
        return this;
    }

    public Index setBeforeCurCount(int beforeCurCount) {
        this.beforeCurCount = beforeCurCount;
        return this;
    }


    public int getCount() {
        return count;
    }

    public int getBeforeCurCount() {
        return beforeCurCount;
    }

    public long getBaseT() {
        return baseT;
    }

    public Index setBaseT(long baseT) {
        this.baseT = baseT;
        return this;
    }

    public long[] getaMin() {
        return aMin;
    }

    public Index setaMin(long[] aMin) {
        this.aMin = aMin;
        return this;
    }

    public long[] getaMax() {
        return aMax;
    }

    public Index setaMax(long[] aMax) {
        this.aMax = aMax;
        return this;
    }

    public long[] getaSumArray() {
        return aSumArray;
    }

    public Index setaSumArray(long[] aSumArray) {
        this.aSumArray = aSumArray;
        return this;
    }

    public List<Long> getLastA() {
        return lastA;
    }

    public Index setLastA(List lastA) {
        this.lastA = lastA;
        return this;
    }
}
