package siso.edu.cn.autolightanalysis;

import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // 顶部工具栏
    private ToggleButton connectDeviceBtn = null;
    private Button saveLightSpectrumBtn = null;
    private Button saveDarkSpectrumBtn = null;
    private Button readSpectrumBtn = null;
    private ImageButton exitBtn = null;

    // 标签页内容
    private TabLayout analysisTable = null;
    private NoScrollViewPager analysisContent = null;
    private List<String> analysisTableIndicators = new ArrayList<String>();
    private List<Fragment> analysisFragments = new ArrayList<Fragment>();

    // 串口对象
    private UartDevice uartDevice = null;
    private PeripheralManager manager = PeripheralManager.getInstance();

    // 数据传输的对话框
    private DataPreprocessingDialog dataPreprocessingDialog = new DataPreprocessingDialog();

    // 串口缓存数据
    private ArrayList<Byte> serialDataBuffer = new ArrayList<Byte>();
    // 串口数据转换后的数据
    private ArrayList<Integer> serialData = null;
    // 图表串口数据
    private ArrayList<Map<String, Object>> spectrumSerialData = new ArrayList<Map<String, Object>>();

    // 保存当前的指令
    private String currentCommand = StringUtils.EMPTY;
    // 系统已经准备完毕
    private boolean isSysReady = false;
    // 串口接收数据
    private Handler serialHandler = null;
    // 向标签页发送实时串口数据对象
    private Handler handler = null;

    // 亮和暗数据是否已经保存
    private boolean hasLightData = false, hasDarkData = false;

    // TODO: 19-10-25
    // 接收到的光谱数据
    private ArrayList<Float> normalData = new ArrayList<Float>();
    // 创建普通光谱数据集和数据线
    private ArrayList<Entry> normalSpectrumData = new ArrayList<Entry>();
    // 折线图数据集
    private ArrayList<ILineDataSet> spectrumDataSets = new ArrayList<ILineDataSet>();
    // 已经有亮和暗数据


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectDeviceBtn = findViewById(R.id.connect_device_btn);
        saveLightSpectrumBtn = findViewById(R.id.save_light_spectrum_btn);
        saveDarkSpectrumBtn = findViewById(R.id.save_dark_spectrum_btn);
        readSpectrumBtn = findViewById(R.id.read_spectrum_btn);
        exitBtn = findViewById(R.id.exit_btn);
        analysisTable = findViewById(R.id.analysis_table);
        analysisContent = findViewById(R.id.analysis_content);

        // 不可侧滑
        analysisContent.setNoScroll(true);

        // 设置标签页标签和关联页面
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
        // 绑定标签页和标签页内容
        analysisTable.setupWithViewPager(analysisContent);

        // 接收传输数据处理结果
        serialHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // 收到串口原始数据
                if (msg.what == SerialAsyncTask.SERIAL_DATA_MSG_KEY) {
                    // 获取串口数据
                    serialData = msg.getData().getIntegerArrayList(SerialAsyncTask.SERIAL_DATA_KEY);

                    for (int i = 0; i < spectrumSerialData.size(); i++) {
                        Map<String, Object> itemData = spectrumSerialData.get(i);
                        String name = (String) itemData.get(Command.SPECTRUM_ITEM_NAME_KEY);
                        if (name.equals(Command.DARK_DATA)) {
                            hasDarkData = true;
                        }
                        if (name.equals(Command.LIGHT_DATA)) {
                            hasLightData = true;
                        }
                    }

                    if (hasLightData && hasDarkData) {
                        // 启动数据处理线程，优化性能
                        new CalculateAsyncTask(serialHandler, spectrumSerialData).execute(serialData.toArray(new Integer[]{}));
                    } else {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.preprocessing_no_light_dark_text),
                                Toast.LENGTH_LONG).show();

                        // 关闭进度对话框
                        if (dataPreprocessingDialog != null &&
                                dataPreprocessingDialog.getDialog() != null &&
                                dataPreprocessingDialog.getDialog().isShowing()) {
                            dataPreprocessingDialog.dismiss();
                        }
                    }
                }
                // 收到经过计算后的数据
                if (msg.what == CalculateAsyncTask.CALCULATE_DATA_MSG_KEY) {
                    ArrayList<Float> data = new ArrayList<Float>(Arrays.asList(
                            ArrayUtils.toObject(msg.getData().getFloatArray(CalculateAsyncTask.CALCULATE_DATA_KEY))));

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, String.format(Command.NORMAL_DATA, spectrumSerialData.size() - 2));
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);
                    spectrumSerialData.add(itemData);

                    // 关闭进度对话框
                    if (dataPreprocessingDialog != null &&
                            dataPreprocessingDialog.getDialog() != null &&
                            dataPreprocessingDialog.getDialog().isShowing()) {
                        dataPreprocessingDialog.dismiss();
                    }
                }
            }
        };

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

                        try {
                            // 读取串口数据时清空串口缓存数据
                            serialDataBuffer.clear();
                            // 发送读取光谱的指令，解决开机时数据错误的问题
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

                // 判断是否已经收到了光谱数据
                if (serialData == null) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_no_data_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    // 对数据进行深度拷贝
                    ArrayList<Integer> data = Command.DeepCopyInteger(serialData);

                    // 如果列表中已经有数据，那么就更新数据
                    for (int i = 0; i < spectrumSerialData.size(); i++) {
                        if (spectrumSerialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.LIGHT_DATA)) {
                            spectrumSerialData.get(i).put(Command.SPECTRUM_ITEM_DATA_KEY, data);

                            updateSpectrumView(spectrumSerialData);

                            return;
                        }
                    }

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, Command.LIGHT_DATA);
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);
                    spectrumSerialData.add(itemData);

                    updateSpectrumView(spectrumSerialData);
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

                // 判断是否已经收到了光谱数据
                if (serialData == null) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.preprocessing_no_data_text),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    // 对数据进行深度拷贝
                    ArrayList<Integer> data = Command.DeepCopyInteger(serialData);

                    // 如果列表中已经有数据，那么就更新数据
                    for (int i = 0; i < spectrumSerialData.size(); i++) {
                        if (spectrumSerialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.DARK_DATA)) {
                            spectrumSerialData.get(i).put(Command.SPECTRUM_ITEM_DATA_KEY, data);

                            updateSpectrumView(spectrumSerialData);

                            return;
                        }
                    }

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, Command.DARK_DATA);
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);
                    spectrumSerialData.add(itemData);

                    updateSpectrumView(spectrumSerialData);
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
    }

    // 串口配置
    private void configureUartFrame(UartDevice uart) throws IOException {
        uart.setBaudrate(115200);
        uart.setDataSize(8);
        uart.setParity(UartDevice.PARITY_NONE);
        uart.setStopBits(1);
    }

    // 串口数据读取函数
    private void readUartBuffer(UartDevice uart) throws IOException {
        final int BUFFER_SIZE = 200;
        byte[] buffer = new byte[BUFFER_SIZE];

        // 每次读取有效数据的长度
        int count = 0;
        while ((count = uart.read(buffer, buffer.length)) > 0) {
            for (int i = 0; i < count; i++) {
                serialDataBuffer.add(buffer[i]);
            }

            // 读取光谱数据
            if (Command.READ_SPECTRUM.equals(currentCommand)) {
                if (serialDataBuffer.size() == Command.MAX_SPECTRUM_DATA_LENGTH) {
                    // 启动数据处理线程，优化性能
                    new SerialAsyncTask(serialHandler).execute(serialDataBuffer.toArray(new Byte[]{}));
                    // 显示经度对话框
                    dataPreprocessingDialog.setTextId(R.string.preprocessing_read_data_text);
                    dataPreprocessingDialog.show(getSupportFragmentManager(), MainActivity.class.getName());
                }
            }

            // 读取内部温度数据
            if (Command.READ_INTERNAL_TEMPERATURE.equals(currentCommand)) {
                if (serialDataBuffer.size() != Command.MAX_TEMPERATURE_DATA_LENGTH) {
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
                } else {
                    // 接收数据正确后才可以操作
                    isSysReady = true;
                    serialDataBuffer.clear();
                }
            }
        }
    }

    // TODO: 19-12-24
    // 更新图表
    private void updateSpectrumView(ArrayList<Map<String, Object>> data) {
//        // 通知Fragment更新数据
//        Message serialDataMsg = new Message();
//        Bundle bundle = new Bundle();
//        bundle.putFloatArray(Command.SERIAL_DATA_KEY,
//                ArrayUtils.toPrimitive(normalData.toArray(new Float[]{})));
//        serialDataMsg.setData(bundle);



//        // 通知Fragment更新数据
//        Message serialDataMsg = new Message();
//        Bundle bundle = new Bundle();
//        bundle.putParcelableArrayList(spectrumSerialData);
//        serialDataMsg.setData(bundle);
    }

    // 向Fragment传递数据
    public void setFragmentSerialData() {
        if (handler != null) {
            // 通知Fragment更新数据
            Message serialDataMsg = new Message();
            Bundle bundle = new Bundle();
            bundle.putFloatArray(Command.SERIAL_DATA_KEY,
                    ArrayUtils.toPrimitive(normalData.toArray(new Float[]{})));
            serialDataMsg.setData(bundle);

            handler.sendMessage(serialDataMsg);
        }
    }

    // TODO: 19-10-20 要改
    public LineChart getSpectrumLineChart() {
//         return spectrumLineChart;
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
