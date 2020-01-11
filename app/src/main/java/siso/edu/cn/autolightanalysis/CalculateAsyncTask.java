package siso.edu.cn.autolightanalysis;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.Map;

public class CalculateAsyncTask extends AsyncTask<Integer, Void, Void> {
    public static final String TAG = "===CalculateAsyncTask===";

    public static final String CALCULATE_DATA_KEY = "calculate_data_key";
    public static final int CALCULATE_DATA_MSG_KEY = 0x02;

    private ArrayList<Float> calculateData = new ArrayList<>();

    private Handler handler = null;
    private ArrayList<Map<String, Object>> spectrumSerialData = null;

    public CalculateAsyncTask(Handler handler, ArrayList<Map<String, Object>> spectrumSerialData) {
        this.handler = handler;
        this.spectrumSerialData = spectrumSerialData;
    }

    @Override
    protected Void doInBackground(Integer... integers) {
        ArrayList<Integer> lightData = null, darkData = null;
        calculateData.clear();

        for (int i = 0; i < spectrumSerialData.size(); i++) {
            Map<String, Object> itemData = this.spectrumSerialData.get(i);
            if (itemData.get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.LIGHT_DATA)) {
                lightData = (ArrayList<Integer>) itemData.get(Command.SPECTRUM_ITEM_DATA_KEY);
            }
            if (itemData.get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.DARK_DATA)) {
                darkData = (ArrayList<Integer>) itemData.get(Command.SPECTRUM_ITEM_DATA_KEY);
            }
        }

        if (lightData != null && darkData != null) {
            for (int i = 0; i < lightData.size(); i++) {
                float value = 0f;

                if (i >= 280 && i <= 1990) {
                    int light = lightData.get(i);
                    int dark = darkData.get(i);

                    value = Math.abs((float) (integers[i] - dark) / (light - dark));
                }
                calculateData.add(value);
            }

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putFloatArray(CALCULATE_DATA_KEY,
                    ArrayUtils.toPrimitive(calculateData.toArray(new Float[]{})));
            msg.setData(data);
            msg.what = CALCULATE_DATA_MSG_KEY;

            handler.sendMessage(msg);
        }


        return null;
    }
}
