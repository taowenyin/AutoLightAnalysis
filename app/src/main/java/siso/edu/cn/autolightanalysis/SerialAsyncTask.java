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

        byte[] spectrumData = ArrayUtils.toPrimitive(bytes);
        byte[] lightData = null;
        byte[] darkData = null;

        if (activity.hasDarkData() && activity.hasLightData()) {
            lightData = ArrayUtils.toPrimitive(activity.getLightData().toArray(new Byte[]{}));
            darkData = ArrayUtils.toPrimitive(activity.getDarkData().toArray(new Byte[]{}));
        }

        // 清空数据
        activity.getNormalSpectrumData().clear();

        // 填充数据集
        for (int i = 0, j = 0; i < spectrumData.length; i += 4, j++) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            int data = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));

            if (activity.hasDarkData() && activity.hasLightData()) {

                byte light0 = lightData[i];
                byte light1 = lightData[i + 1];
                byte light2 = lightData[i + 2];
                byte light3 = lightData[i + 3];
                int light = ((light3 & 0xFF) << 24) | ((light2 & 0xFF) << 16) | ((light1 & 0xFF) << 8) | ((light0 & 0xFF));

                byte dark0 = darkData[i];
                byte dark1 = darkData[i + 1];
                byte dark2 = darkData[i + 2];
                byte dark3 = darkData[i + 3];

                int dark = ((dark3 & 0xFF) << 24) | ((dark2 & 0xFF) << 16) | ((dark1 & 0xFF) << 8) | ((dark0 & 0xFF));

                if (light != dark) {
                    data = (data - dark) / (light - dark);
                }
            }

            activity.getNormalSpectrumData().add(new Entry(j, data));
        }

        // 设置图表数据
        LineDataSet lightDataSet = (LineDataSet) activity.getSpectrumLineChart().getData().getDataSetByIndex(0);
        lightDataSet.setValues(activity.getNormalSpectrumData());

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        // 显示数据
        activity.getSpectrumLineChart().getData().notifyDataChanged();
        activity.getSpectrumLineChart().notifyDataSetChanged();
        activity.getSpectrumLineChart().invalidate();

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
