package io.openmessaging.util;

import java.io.IOException;

public final  class LinuxCmdUtil {
    public static void dropCaches() {
        try {
            Runtime.getRuntime().exec("echo 1 > /proc/sys/vm/drop_caches").destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
