package siso.edu.cn.autolightanalysis;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.things.pio.UartDevice;
import com.google.common.primitives.Bytes;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class SerialAsyncTask extends AsyncTask<Byte, Void, Void> {
    public static final String TAG = "===SerialAsyncTask===";

    public static final String SERIAL_DATA_KEY = "serial_data_key";
    public static final int SERIAL_DATA_MSG_KEY = 0x01;

    private ArrayList<Integer> serialData = new ArrayList<>();
    private Handler handler = null;

    public SerialAsyncTask(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected Void doInBackground(Byte... bytes) {

        // 把接收到的串口数据转化为字节数组
        byte[] spectrumData = ArrayUtils.toPrimitive(bytes);
        serialData.clear();

        // 填充数据集
        for (int i = 0, j = 0; i < spectrumData.length; i += 4, j++) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            Integer itemData = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));
            serialData.add(itemData);
        }

        Message msg = new Message();
        Bundle data = new Bundle();
        data.putIntegerArrayList(SERIAL_DATA_KEY, serialData);
        msg.setData(data);
        msg.what = SERIAL_DATA_MSG_KEY;

        handler.sendMessage(msg);

        return null;
    }
}
