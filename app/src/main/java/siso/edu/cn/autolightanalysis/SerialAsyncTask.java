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

public class SerialAsyncTask extends AsyncTask<Byte, Void, Void> {

    private MainActivity activity = null;

    public static final String TAG = "===SerialAsyncTask===";

    public SerialAsyncTask(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    protected Void doInBackground(Byte... bytes) {

        byte[] spectrumData = ArrayUtils.toPrimitive(bytes);

        // 清空数据
        activity.getNormalSpectrumData().clear();

        // 填充数据集
        for (int i = 0; i < spectrumData.length; i += 4) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            int data = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));

            activity.getNormalSpectrumData().add(new Entry(i, data));
        }

        // 设置图表数据
        LineDataSet lightDataSet = (LineDataSet) activity.getSpectrumLineChart().getData().getDataSetByIndex(2);
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
