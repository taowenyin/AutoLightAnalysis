package siso.edu.cn.autolightanalysis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class Command {
    // 最大光谱数据长度
    public static final int MAX_SPECTRUM_DATA_LENGTH = 4096;

    // 读取设备内部温度指令
    public static final String READ_INTERNAL_TEMPERATURE = "R;";

    // 读取光谱指令
    public static final String READ_SPECTRUM = "S;";

    public static final String LIGHT_DATA = "Light";

    public static final String DARK_DATA = "Dark";

    public static final String NORMAL_DATA = "Nor-%d";

    // 通过序列化和反序列化实现List的深度拷贝
    public static ArrayList<Byte> DeepCopy(ArrayList<Byte> src) throws IOException, ClassNotFoundException {

        // 序列化
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objectOut = new ObjectOutputStream(byteOut);
        objectOut.writeObject(src);

        // 反序列化
        ByteArrayInputStream byteIn = new ByteArrayInputStream(byteOut.toByteArray());
        ObjectInputStream objectIn = new ObjectInputStream(byteIn);

        return (ArrayList<Byte>) objectIn.readObject();
    }

}
