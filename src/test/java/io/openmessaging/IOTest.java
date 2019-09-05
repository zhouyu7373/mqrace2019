package io.openmessaging;

import sun.security.util.BitArray;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class IOTest {

    private static final String FILE_PRE = "/Users/liuchao56/data/";


    public static void main(String[] args) throws IOException {
        File data_file = new File(FILE_PRE + '_' + 0 + ".data");

        FileChannel data_channel = FileChannel.open(data_file.toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE,
                StandardOpenOption.CREATE, StandardOpenOption.SPARSE);

        ByteBuffer buf = ByteBuffer.allocate(200);
        data_channel.read(buf, 0);
        buf.flip();
       for(int i=0;i< 4;i++) {
           buf.getLong();
           buf.getLong();
           buf.position(buf.position() + 34);
           System.out.println();
       }
    }
}
