package siso.edu.cn.autolightanalysis;

import android.content.SharedPreferences;
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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

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
        AnalysisSpectrumFragment.OnFragmentMonitorListener,
        PreferenceFragment.OnFragmentInteractionListener {

    public static final String TAG = "===MainActivity===";
    private static final String UART_DEVICE_NAME = "UART0";

    // 顶部工具栏
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

    // 亮和暗数据是否已经保存
    private boolean hasLightData = false, hasDarkData = false;

    private AnalysisBaseInfoFragment analysisBaseInfoFragment = null;
    private AnalysisSpectrumFragment analysisSpectrumFragment = null;
    private AnalysisIndexFragment analysisIndexFragment = null;
    private AnalysisPredictionFragment analysisPredictionFragment = null;
    private PreferenceFragment preferenceFragment = null;
    private AboutFragment aboutFragment = null;

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

        analysisFragments.add(this.analysisBaseInfoFragment = AnalysisBaseInfoFragment.newInstance(this.getResources().getString(R.string.table_base_info_text)));
        analysisFragments.add(this.analysisSpectrumFragment = AnalysisSpectrumFragment.newInstance(this.getResources().getString(R.string.table_spectrum_text)));
        analysisFragments.add(this.analysisIndexFragment = AnalysisIndexFragment.newInstance(this.getResources().getString(R.string.table_index_text)));
        analysisFragments.add(this.analysisPredictionFragment = AnalysisPredictionFragment.newInstance(this.getResources().getString(R.string.table_prediction_text)));
        analysisFragments.add(this.preferenceFragment = PreferenceFragment.newInstance(this.getResources().getString(R.string.table_preference_text)));
        analysisFragments.add(this.aboutFragment = AboutFragment.newInstance(this.getResources().getString(R.string.table_about_text)));

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

                    // 如果列表中已经有数据，那么就更新数据
                    for (int i = 0; i < spectrumSerialData.size(); i++) {
                        if (spectrumSerialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.NORMAL_DATA)) {
                            spectrumSerialData.get(i).put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                            // 关闭进度对话框
                            if (dataPreprocessingDialog != null &&
                                    dataPreprocessingDialog.getDialog() != null &&
                                    dataPreprocessingDialog.getDialog().isShowing()) {
                                dataPreprocessingDialog.dismiss();
                            }

                            return;
                        }
                    }

                    // 如果没有数据，那么就创建数据
                    Map<String, Object> itemData = new HashMap<String, Object>();
                    itemData.put(Command.SPECTRUM_ITEM_NAME_KEY, Command.NORMAL_DATA);
                    itemData.put(Command.SPECTRUM_ITEM_DATA_KEY, data);
                    itemData.put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                    itemData.put(Command.SPECTRUM_ITEM_SHOW_KEY, true);
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

        analysisContent.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {
                Log.i(TAG, "onPageScrolled i = " + i);
            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1) {
                    // 更新图表
                    analysisSpectrumFragment.updateSpectrumData(spectrumSerialData);
                }
                // 切换到指标页
                if (i == 2) {
                    // 获取配置参数
                    SharedPreferences preferences = getSharedPreferences(
                            getResources().getString(R.string.preference_name), MODE_PRIVATE);

                    if (spectrumSerialData.size() == 3 &&
                            preferences.contains(getResources().getString(R.string.preference_type_key)) &&
                            preferences.contains(getResources().getString(R.string.preference_is_packing_key)) &&
                            preferences.contains(getResources().getString(R.string.preference_packing_type_key))) {
                        // 计算指标
                        analysisIndexFragment.calculateIndex(preferences, spectrumSerialData);
                    } else {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.preprocessing_no_data_text),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) {

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
                // 关闭串口
                closeUart();
                // 打开串口
                openUart();
            }
        });

        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });
    }

    // 打开串口
    private void openUart() {
        if (uartDevice == null) {
            try {
                // 打开串口
                uartDevice = manager.openUartDevice(UART_DEVICE_NAME);
                // 配置串口
                configureUartFrame(uartDevice);
                // 注册串口接收的回调函数
                uartDevice.registerUartDeviceCallback(MainActivity.this);

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
        }
    }

    // 关闭串口
    private void closeUart() {
        try {
            if (uartDevice != null) {
                // 关闭串口
                uartDevice.unregisterUartDeviceCallback(MainActivity.this);
                uartDevice.close();
                uartDevice = null;
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to close UART device", e);
        }
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
                    serialDataBuffer.clear();

                    SharedPreferences preferences = getSharedPreferences(
                            getResources().getString(R.string.preference_name), MODE_PRIVATE);
                    int integrationTime = Integer.valueOf(
                            preferences.getString(getResources().getString(R.string.preference_integration_time_key), "-1"));
                    String cmd = Command.SET_INTEGRATION_TIME + String.format("%04d;", integrationTime);
                    // 发送读取光谱的指令
                    uartDevice.write(cmd.getBytes(), cmd.getBytes().length);
                    // 设置当前指令类型
                    currentCommand = Command.SET_INTEGRATION_TIME;
                }
            }

            if (Command.SET_INTEGRATION_TIME.equals(currentCommand)) {
                if (serialDataBuffer.size() == Command.MAX_INTEGRATION_DATA_LENGTH) {
                    // 接收数据正确后才可以操作
                    serialDataBuffer.clear();

                    // 系统准备完毕
                    isSysReady = true;

                    try {
                        // 发送读取光谱的指令
                        uartDevice.write(Command.READ_SPECTRUM.getBytes(), Command.READ_SPECTRUM.getBytes().length);
                        // 设置当前指令类型
                        currentCommand = Command.READ_SPECTRUM;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            if (Command.SET_SMOOTH.equals(currentCommand)) {
                if (serialDataBuffer.size() == Command.MAX_SMOOTH_DATA_LENGTH) {
                    // 接收数据正确后才可以操作
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
    public void onUpdateSpectrumItemShow(int position, boolean isShow) {
        spectrumSerialData.get(position).put(Command.SPECTRUM_ITEM_SHOW_KEY, isShow);
        // 更新图表
        analysisSpectrumFragment.updateSpectrumData(spectrumSerialData);
    }

    @Override
    public void onUpdateSpectrumItemChecked(int position, boolean checked) {
        spectrumSerialData.get(position).put(Command.SPECTRUM_ITEM_STATUS_KEY, checked);
        // 更新图表
        analysisSpectrumFragment.updateSpectrumData(spectrumSerialData);
    }

    @Override
    public void onUpdateSpectrumUndo() {
        for (int i = 0; i < spectrumSerialData.size(); i++) {
            spectrumSerialData.get(i).put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
        }
        // 更新图表
        analysisSpectrumFragment.updateSpectrumData(spectrumSerialData);
    }

    @Override
    public void onUpdateSpectrumItemDelete() {
        ArrayList<Map<String, Object>> deleteList = new ArrayList<Map<String, Object>>();

        for (int i = 0;i < spectrumSerialData.size(); i++) {
            boolean delete = (boolean) spectrumSerialData.get(i).get(Command.SPECTRUM_ITEM_STATUS_KEY);
            if (delete) {
                deleteList.add(spectrumSerialData.get(i));
            }
        }
        spectrumSerialData.removeAll(deleteList);
        // 更新图表
        analysisSpectrumFragment.updateSpectrumData(spectrumSerialData);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }
}
