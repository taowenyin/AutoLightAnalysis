package siso.edu.cn.autolightanalysis;

import android.os.AsyncTask;

import com.google.android.things.pio.UartDevice;

import java.io.IOException;

public class SerialAsyncTask extends AsyncTask<String, Void, Void> {

    private UartDevice uartDevice = null;

    public SerialAsyncTask(UartDevice uartDevice) {
        this.uartDevice = uartDevice;
    }

    @Override
    protected Void doInBackground(String... strings) {

        // 发送读取光谱的指令
        try {
            uartDevice.write(strings[0].getBytes(), strings[0].getBytes().length);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
