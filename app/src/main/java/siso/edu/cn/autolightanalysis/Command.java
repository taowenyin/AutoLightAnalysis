package siso.edu.cn.autolightanalysis;

public class Command {
    // 最大光谱数据长度
    public static final int MAX_SPECTRUM_DATA_LENGTH = 4096;

    // 读取设备内部温度指令
    public static final String READ_INTERNAL_TEMPERATURE = "R;";

    // 读取光谱指令
    public static final String READ_SPECTRUM = "S;";

}
