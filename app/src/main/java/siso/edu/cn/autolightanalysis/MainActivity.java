package siso.edu.cn.autolightanalysis;

import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        UartDeviceCallback,
        AnalysisBaseInfoFragment.OnFragmentInteractionListener,
        AnalysisSpectrumFragment.OnFragmentInteractionListener,
        AnalysisSpectrumFragment.OnHasDarkDataListener,
        AnalysisSpectrumFragment.OnHasLightDataListener,
        AnalysisIndexFragment.OnFragmentInteractionListener,
        AnalysisPredictionFragment.OnFragmentInteractionListener,
        PreferenceFragment.OnFragmentInteractionListener,
        AboutFragment.OnFragmentInteractionListener {

    public static final String TAG = "===MainActivity===";
    private static final String UART_DEVICE_NAME = "UART0";

    private ToggleButton connectDeviceBtn = null;
    private Button saveLightSpectrumBtn = null;
    private Button saveDarkSpectrumBtn = null;
    private Button readSpectrumBtn = null;
    private ImageButton exitBtn = null;
    private Switch toggleAutoReadSpectrumBtn = null;
    private EditText toggleAutoReadSpectrumIntervalEdt = null;

    private DataPreprocessingDialog dataPreprocessingDialog = new DataPreprocessingDialog();

    private TabLayout analysisTable = null;
    private NoScrollViewPager analysisContent = null;
    private List<String> analysisTableIndicators = new ArrayList<String>();

    private UartDevice uartDevice = null;
    private PeripheralManager manager = PeripheralManager.getInstance();


    // 标签页的内容
    private List<Fragment> analysisFragments = new ArrayList<Fragment>();



    // 串口缓存数据
    private ArrayList<Byte> serialDataBuffer = new ArrayList<Byte>();
    // 串口数据
    private ArrayList<Map<String, Object>> serialData = new ArrayList<Map<String, Object>>();
    // 创建普通光谱数据集和数据线
    private ArrayList<Entry> normalSpectrumData = new ArrayList<Entry>();
    // 折线图数据集
    private ArrayList<ILineDataSet> spectrumDataSets = new ArrayList<ILineDataSet>();
    // 已经有亮和暗数据
    private boolean hasLightData = false, hasDarkData = false;

    // 自动读取频谱数据的定时器
    private Timer readTimer = null;
    private TimerTask readTimerTask = null;

    // 保存当前的指令
    private String currentCommand = StringUtils.EMPTY;
    // 系统已经准备完毕
    private boolean isSysReady = false;

    // 发送实时串口数据
    private Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectDeviceBtn = findViewById(R.id.connect_device_btn);
        saveLightSpectrumBtn = findViewById(R.id.save_light_spectrum_btn);
        saveDarkSpectrumBtn = findViewById(R.id.save_dark_spectrum_btn);
        readSpectrumBtn = findViewById(R.id.read_spectrum_btn);
        exitBtn = findViewById(R.id.exit_btn);
        toggleAutoReadSpectrumBtn = findViewById(R.id.toggle_auto_read_spectrum_btn);
        toggleAutoReadSpectrumIntervalEdt = findViewById(R.id.toggle_auto_read_spectrum_interval_edt);
        analysisTable = findViewById(R.id.analysis_table);
        analysisContent = findViewById(R.id.analysis_content);

        // 不可侧滑
        analysisContent.setNoScroll(true);

        // 设置标签页的关联页面
        analysisTableIndicators.add(this.getResources().getString(R.string.table_base_info_text));
        analysisTableIndicators.add(this.getResources().getString(R.string.table_spectrum_text));
        analysisTableIndicators.add(this.getResources().getString(R.string.table_index_text));
        analysisTableIndicators.add(this.getResources().getString(R.string.table_prediction_text));
        analysisTableIndicators.add(this.getResources().getString(R.string.table_preference_text));
        analysisTableIndicators.add(this.getResources().getString(R.string.table_about_text));

        analysisFragments.add(AnalysisBaseInfoFragment.newInstance(this.getResources().getString(R.string.table_base_info_text)));
        analysisFragments.add(AnalysisSpectrumFragment.newInstance(this.getResources().getString(R.string.table_spectrum_text)));
        analysisFragments.add(AnalysisIndexFragment.newInstance(this.getResources().getString(R.string.table_index_text)));
        analysisFragments.add(AnalysisPredictionFragment.newInstance(this.getResources().getString(R.string.table_prediction_text)));
        analysisFragments.add(PreferenceFragment.newInstance(this.getResources().getString(R.string.table_preference_text)));
        analysisFragments.add(AboutFragment.newInstance(this.getResources().getString(R.string.table_about_text)));

        analysisContent.setAdapter(new AnalysisPageAdapter(getSupportFragmentManager(), analysisFragments, analysisTableIndicators));
        analysisTable.setupWithViewPager(analysisContent);

        connectDeviceBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    try {
                        // 打开串口
                        uartDevice = manager.openUartDevice(UART_DEVICE_NAME);
                        // 配置串口
                        configureUartFrame(uartDevice);
                        // 注册串口接收的回调函数
                        uartDevice.registerUartDeviceCallback(MainActivity.this);
                        // 打开串口后使能其他功能
                        saveLightSpectrumBtn.setEnabled(true);
                        saveDarkSpectrumBtn.setEnabled(true);
                        readSpectrumBtn.setEnabled(true);
                        toggleAutoReadSpectrumBtn.setEnabled(true);

                        try {
                            // 读取串口数据时清空串口缓存数据
                            serialDataBuffer.clear();
                            // 发送读取光谱的指令
                            uartDevice.write(Command.READ_INTERNAL_TEMPERATURE.getBytes(), Command.READ_INTERNAL_TEMPERATURE.getBytes().length);
                            // 设置当前指令类型
                            currentCommand = Command.READ_INTERNAL_TEMPERATURE;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to access UART device", e);
                    }
                } else {
                    try {
                        // 关闭串口
                        uartDevice.unregisterUartDeviceCallback(MainActivity.this);
                        uartDevice.close();
                        // 打开串口后使能其他功能
                        saveLightSpectrumBtn.setEnabled(false);
                        saveDarkSpectrumBtn.setEnabled(false);
                        toggleAutoReadSpectrumBtn.setChecked(false);
                        toggleAutoReadSpectrumBtn.setEnabled(false);
                        readSpectrumBtn.setEnabled(false);
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to close UART device", e);
                    }
                }
            }
        });

        saveLightSpectrumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSysReady) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_adjust_system_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (serialDataBuffer.size() == 0) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_no_data_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    ArrayList<Byte> data = Command.DeepCopy(serialDataBuffer);

                    // 如果列表中已经有数据，那么就更新数据
                    for (int i = 0; i < serialData.size(); i++) {
                        if (serialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.LIGHT_DATA)) {
                            serialData.get(i).put(Command.SPECTRUM_ITEM_DATA_KEY, data);

                            // 通知Fragment更新数据
                            Message serialDataMsg = new Message();
                            Bundle bundle = new Bundle();
                            // TODO: 19-10-20 要改
                            // bundle.putParcelableArrayList(serialData);
                            serialDataMsg.setData(bundle);

                            handler.sendMessage(serialDataMsg);

                            return;
                        }
                    }

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, Command.LIGHT_DATA);
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);

                    serialData.add(itemData);

                    // 通知Fragment更新数据
                    Message serialDataMsg = new Message();
                    Bundle bundle = new Bundle();
                    // TODO: 19-10-20 要改
                    // bundle.putParcelableArrayList(serialData);
                    serialDataMsg.setData(bundle);

                    handler.sendMessage(serialDataMsg);

                    // 保存了亮数据
                    hasLightData = true;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        saveDarkSpectrumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSysReady) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_adjust_system_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (serialDataBuffer.size() == 0) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_no_data_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    ArrayList<Byte> data = Command.DeepCopy(serialDataBuffer);

                    // 如果列表中已经有数据，那么就更新数据
                    for (int i = 0; i < serialData.size(); i++) {
                        if (serialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.DARK_DATA)) {
                            serialData.get(i).put(Command.SPECTRUM_ITEM_DATA_KEY, data);

                            // 通知Fragment更新数据
                            Message serialDataMsg = new Message();
                            Bundle bundle = new Bundle();
                            // TODO: 19-10-20 要改
                            // bundle.putParcelableArrayList(serialData);
                            serialDataMsg.setData(bundle);

                            return;
                        }
                    }

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, Command.DARK_DATA);
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);

                    serialData.add(itemData);

                    // 通知Fragment更新数据
                    Message serialDataMsg = new Message();
                    Bundle bundle = new Bundle();
                    // TODO: 19-10-20 要改
                    // bundle.putParcelableArrayList(serialData);
                    serialDataMsg.setData(bundle);

                    // 保存了暗数据
                    hasDarkData = true;
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        readSpectrumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isSysReady) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_adjust_system_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    // 读取串口数据时清空串口缓存数据
                    serialDataBuffer.clear();
                    // 发送读取光谱的指令
                    uartDevice.write(Command.READ_SPECTRUM.getBytes(), Command.READ_SPECTRUM.getBytes().length);
                    // 设置当前指令类型
                    currentCommand = Command.READ_SPECTRUM;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });

        toggleAutoReadSpectrumBtn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isSysReady) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_adjust_system_text),
                            Toast.LENGTH_LONG).show();

                    toggleAutoReadSpectrumBtn.setChecked(false);
                    return;
                }

                if (isChecked) {
                    if (toggleAutoReadSpectrumIntervalEdt.getText().toString().equals(StringUtils.EMPTY) ||
                            Integer.valueOf(toggleAutoReadSpectrumIntervalEdt.getText().toString()) >= 5) {
                        readSpectrumBtn.setEnabled(false);

                        // 开启自动就禁用保存
                        saveLightSpectrumBtn.setEnabled(false);
                        saveDarkSpectrumBtn.setEnabled(false);

                        // 开始自动读取
                        startReadTimer();
                    } else {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.auto_read_spectrum_warning_label),
                                Toast.LENGTH_LONG).show();
                        toggleAutoReadSpectrumBtn.setChecked(false);
                    }

                } else {
                    readSpectrumBtn.setEnabled(true);

                    // 关闭自动就开启保存
                    saveLightSpectrumBtn.setEnabled(true);
                    saveDarkSpectrumBtn.setEnabled(true);

                    // 停止自动读取
                    stopReadTimer();
                }
            }
        });
    }

    // 串口配置
    public void configureUartFrame(UartDevice uart) throws IOException {
        uart.setBaudrate(115200);
        uart.setDataSize(8);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(1);
    }

    // 串口数据读取函数
    public void readUartBuffer(UartDevice uart) throws IOException {
        final int BUFFER_SIZE = 200;
        byte[] buffer = new byte[BUFFER_SIZE];

        // 每次读取有效数据的长度
        int count = 0;
        while ((count = uart.read(buffer, buffer.length)) > 0) {
            for (int i = 0; i < count; i++) {
                serialDataBuffer.add(buffer[i]);
            }

            if (Command.READ_SPECTRUM.equals(currentCommand)) {
                if (serialDataBuffer.size() == Command.MAX_SPECTRUM_DATA_LENGTH) {
                    // 启动数据处理线程，优化性能
                    new SerialAsyncTask(this).execute(serialDataBuffer.toArray(new Byte[]{}));
                    if (!toggleAutoReadSpectrumBtn.isChecked()) {
                        // 显示经度对话框
                        dataPreprocessingDialog.setTextId(R.string.preprocessing_read_data_text);
                        dataPreprocessingDialog.show(getSupportFragmentManager(), MainActivity.class.getName());
                    }
                }
            }

            if (Command.READ_INTERNAL_TEMPERATURE.equals(currentCommand)) {
                if (serialDataBuffer.size() > Command.MAX_TEMPERATURE_DATA_LENGTH) {
                    try {
                        // 读取串口数据时清空串口缓存数据
                        serialDataBuffer.clear();
                        // 发送读取光谱的指令
                        uartDevice.write(Command.READ_INTERNAL_TEMPERATURE.getBytes(), Command.READ_INTERNAL_TEMPERATURE.getBytes().length);
                        // 设置当前指令类型
                        currentCommand = Command.READ_INTERNAL_TEMPERATURE;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (serialDataBuffer.size() == Command.MAX_TEMPERATURE_DATA_LENGTH) {
                    // 接收数据正确后才可以操作
                    isSysReady = true;
                    serialDataBuffer.clear();
                }
            }
        }
    }

    public DataPreprocessingDialog getDataPreprocessingDialog() {
        return dataPreprocessingDialog;
    }

    public boolean hasLightData() {
        return hasLightData;
    }

    public boolean hasDarkData() {
        return hasDarkData;
    }

    public ArrayList<Byte> getLightData() {
        for (int i = 0; i < serialData.size(); i++) {
            String name = (String) serialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY);
            if (name.equals(Command.LIGHT_DATA)) {
                return (ArrayList<Byte>) serialData.get(i).get(Command.SPECTRUM_ITEM_DATA_KEY);
            }
        }

        return null;
    }

    public ArrayList<Byte> getDarkData() {
        for (int i = 0; i < serialData.size(); i++) {
            String name = (String) serialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY);
            if (name.equals(Command.DARK_DATA)) {
                return (ArrayList<Byte>) serialData.get(i).get(Command.SPECTRUM_ITEM_DATA_KEY);
            }
        }

        return null;
    }

    // 启动自动读取定时器
    private void startReadTimer() {
        if (readTimer == null) {
            readTimer = new Timer();
        }

        if (readTimerTask == null) {
            readTimerTask = new TimerTask() {
                @Override
                public void run() {
                    try {
                        // 读取串口数据时清空串口缓存数据
                        serialDataBuffer.clear();
                        // 发送读取光谱的指令
                        uartDevice.write(Command.READ_SPECTRUM.getBytes(), Command.READ_INTERNAL_TEMPERATURE.getBytes().length);
                        // 设置当前指令类型
                        currentCommand = Command.READ_SPECTRUM;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
        }

        if (readTimer != null && readTimerTask != null) {
            readTimer.schedule(readTimerTask,
                    0,
                    Integer.valueOf(
                            toggleAutoReadSpectrumIntervalEdt.getText().toString().equals(StringUtils.EMPTY) ?
                            "5" : toggleAutoReadSpectrumIntervalEdt.getText().toString()) * 1000);
        }
    }

    // 停止自动读取定时器
    private void stopReadTimer() {
        if (readTimer != null) {
            readTimer.cancel();
            readTimer = null;
        }

        if (readTimerTask != null) {
            readTimerTask.cancel();
            readTimerTask = null;
        }
    }

    public List<Entry> getNormalSpectrumData() {
        return normalSpectrumData;
    }

    // TODO: 19-10-20 要改
    public LineChart getSpectrumLineChart() {
        // return spectrumLineChart;
        return null;
    }

    // 获取Fragment的Handler
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (uartDevice != null) {
            try {
                uartDevice.registerUartDeviceCallback(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (uartDevice != null) {
            uartDevice.unregisterUartDeviceCallback(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (uartDevice != null) {
            try {
                uartDevice.close();
                uartDevice = null;
            } catch (IOException e) {
                Log.w(TAG, "Unable to close UART device", e);
            }
        }
    }

    @Override
    public boolean onUartDeviceDataAvailable(UartDevice uartDevice) {
        // Read available data from the UART device
        try {
            readUartBuffer(uartDevice);
        } catch (IOException e) {
            Log.w(TAG, "Unable to access UART device", e);
        }

        return true;
    }

    @Override
    public void onUartDeviceError(UartDevice uart, int error) {
        Log.w(TAG, uart + ": Error event " + error);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void OnHasDarkData(boolean hasDarkData) {
        this.hasDarkData = hasDarkData;
    }

    @Override
    public void OnHasLightData(boolean hasLightData) {
        this.hasLightData = hasLightData;
    }
}
