package siso.edu.cn.autolightanalysis;

import android.os.AsyncTask;
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

    private MainActivity activity = null;

    public static final String TAG = "===SerialAsyncTask===";

    public SerialAsyncTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Byte... bytes) {

        // 把接收到的串口数据转化为字节数组
        byte[] spectrumData = ArrayUtils.toPrimitive(bytes);
        float[] lightData = null;
        float[] darkData = null;

        // 如果光谱的亮、暗基准数据存在，那么就获取
        if (activity.hasDarkData() && activity.hasLightData()) {
            lightData = ArrayUtils.toPrimitive(activity.getLightData().toArray(new Float[]{}));
            darkData = ArrayUtils.toPrimitive(activity.getDarkData().toArray(new Float[]{}));
        }

        // 清空实时串口图表数据对象
        activity.getNormalSpectrumData().clear();
        // 清空实时串口数据对象
        activity.getNormalData().clear();

        // 填充数据集
        for (int i = 0, j = 0; i < spectrumData.length; i += 4, j++) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            float data = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));

            // 加入有数据
            if (activity.hasDarkData() && activity.hasLightData()) {

                // 获得基准数据
                float light = lightData[i];
                float dark = darkData[i];

                if (light != dark) {
                    data = (data - dark) / (light - dark);
                }
            }

            // 添加图表节点对象
            activity.getNormalSpectrumData().add(new Entry(j, data));
            // 添加数据
            activity.getNormalData().add(data);
        }

        // TODO: 19-10-25
//        // 设置图表数据
//        LineDataSet lightDataSet = (LineDataSet) activity.getSpectrumLineChart().getData().getDataSetByIndex(0);
//        lightDataSet.setValues(activity.getNormalSpectrumData());

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        // TODO: 19-10-25
        // 向Fragment传递数据
        activity.setFragmentSerialData();


//        activity.getSpectrumLineChart().getData().notifyDataChanged();
//        activity.getSpectrumLineChart().notifyDataSetChanged();
//        activity.getSpectrumLineChart().invalidate();

        // 关闭进度对话框
        if (activity.getDataPreprocessingDialog() != null &&
                activity.getDataPreprocessingDialog().getDialog() != null &&
                activity.getDataPreprocessingDialog().getDialog().isShowing()) {
            activity.getDataPreprocessingDialog().dismiss();

            Log.i(TAG, "Close Dialog....");
        }

        Log.i(TAG, "Update Line Chart....");
    }
}
