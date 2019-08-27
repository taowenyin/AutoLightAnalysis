package siso.edu.cn.autolightanalysis;

import android.os.Handler;
import android.os.Message;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;

public class SerialHandler extends Handler {

    public static final int READ_SPECTRUM = 0x01;
    public static final int LIGHT_SPECTRUM = 0x02;
    public static final int DARK_SPECTRUM = 0x03;

    public static final String DATA = "DATA";

    private MainActivity activity = null;

    public SerialHandler(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        byte[] spectrumData = msg.getData().getByteArray(DATA);

        // 清空数据
        switch (msg.what) {
            case READ_SPECTRUM:
                activity.getNormalSpectrumData().clear();
                break;
            case LIGHT_SPECTRUM:
                break;
            case DARK_SPECTRUM:
                break;
        }

        // 填充数据集
        for (int i = 0; i < spectrumData.length; i += 4) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            int data = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));

            switch (msg.what) {
                case READ_SPECTRUM:
                    activity.getNormalSpectrumData().add(new Entry(i, data));
                    break;
                case LIGHT_SPECTRUM:
                    break;
                case DARK_SPECTRUM:
                    break;
            }
        }

        // 设置图表数据
        switch (msg.what) {
            case READ_SPECTRUM:
                LineDataSet lightDataSet = (LineDataSet) activity.getSpectrumLineChart().getData().getDataSetByIndex(2);
                lightDataSet.setValues(activity.getNormalSpectrumData());
                break;
            case LIGHT_SPECTRUM:
                break;
            case DARK_SPECTRUM:
                break;
        }

        // 显示数据
        activity.getSpectrumLineChart().getData().notifyDataChanged();
        activity.getSpectrumLineChart().notifyDataSetChanged();
        activity.getSpectrumLineChart().invalidate();
    }
}
