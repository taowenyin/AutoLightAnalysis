package siso.edu.cn.autolightanalysis;

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

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>View$OnUnhandledKeyEventListener
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends AppCompatActivity implements UartDeviceCallback {

    public static final String TAG = "===MainActivity===";
    private static final String UART_DEVICE_NAME = "UART0";

    private ToggleButton connectDeviceBtn = null;
    private Button saveLightSpectrumBtn = null;
    private Button saveDarkSpectrumBtn = null;
    private Button readSpectrumBtn = null;
    private Switch toggleAutoReadSpectrumBtn = null;
    private EditText toggleAutoReadSpectrumIntervalEdt = null;
    private LineChart spectrumLineChart = null;
    private DataPreprocessingDialog dataPreprocessingDialog = new DataPreprocessingDialog();
    private ListView spectrumList = null;
    private LinearLayout spectrumListOperateBar = null;
    private ImageButton spectrumItemUndo = null;
    private ImageButton spectrumItemDelete = null;

    private UartDevice uartDevice = null;
    private PeripheralManager manager = PeripheralManager.getInstance();
    private SpectrumListAdapter spectrumListAdapter = null;

    // 创建普通光谱数据集和数据线
    private ArrayList<Entry> normalSpectrumData = new ArrayList<Entry>();
    // 折线图数据集
    private ArrayList<ILineDataSet> spectrumDataSets = new ArrayList<ILineDataSet>();

    // 串口缓存数据
    private ArrayList<Byte> serialDataBuffer = new ArrayList<Byte>();
    // 串口数据
    private ArrayList<Map<String, Object>> serialData = new ArrayList<Map<String, Object>>();
    // 已经有亮和暗数据
    private boolean hasLightData = false, hasDarkData = false;

    // 自动读取频谱数据的定时器
    private Timer readTimer = null;
    private TimerTask readTimerTask = null;

    // 保存当前的指令
    private String currentCommand = StringUtils.EMPTY;
    // 系统已经准备完毕
    private boolean isSysReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectDeviceBtn = findViewById(R.id.connect_device_btn);
        saveLightSpectrumBtn = findViewById(R.id.save_light_spectrum_btn);
        saveDarkSpectrumBtn = findViewById(R.id.save_dark_spectrum_btn);
        readSpectrumBtn = findViewById(R.id.read_spectrum_btn);
        toggleAutoReadSpectrumBtn = findViewById(R.id.toggle_auto_read_spectrum_btn);
        toggleAutoReadSpectrumIntervalEdt = findViewById(R.id.toggle_auto_read_spectrum_interval_edt);
        spectrumLineChart = findViewById(R.id.spectrum_line_chart);
        spectrumList = findViewById(R.id.spectrum_list);
        spectrumListOperateBar = findViewById(R.id.spectrum_list_operate_bar);
        spectrumItemUndo = findViewById(R.id.spectrum_item_undo);
        spectrumItemDelete = findViewById(R.id.spectrum_item_delete);

        spectrumListAdapter = new SpectrumListAdapter(this, serialData);
        spectrumList.setAdapter(spectrumListAdapter);
        // 设置ListView为多选项
        spectrumList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        LineDataSet normalSpectrumSet = new LineDataSet(normalSpectrumData, Command.NORMAL_TMP_DATA);
        // 设置数据线的颜色
        normalSpectrumSet.setColor(getResources().getColor(R.color.colorBlue, getTheme()));
        // 设置数据线的线宽
        normalSpectrumSet.setLineWidth(2f);
        // 设置数据点的颜色
        normalSpectrumSet.setCircleColor(getResources().getColor(R.color.colorBlue, getTheme()));
        // 设置数据点的半径
        normalSpectrumSet.setCircleRadius(4f);
        // 不绘制空心圆
        normalSpectrumSet.setDrawCircleHole(false);
        // 设置数据点的文字大小
        normalSpectrumSet.setValueTextSize(10f);
        // 不对折线图进行填充
        normalSpectrumSet.setDrawFilled(false);
        // 图例的高度
        normalSpectrumSet.setFormLineWidth(3f);
        // 图例的宽度
        normalSpectrumSet.setFormSize(8f);
        // 设置折线图为弧线
        normalSpectrumSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 添加数据线
        spectrumDataSets.add(normalSpectrumSet);
        // 折线图数据集
        LineData spectrumLineData = new LineData(spectrumDataSets);
        // 折线图显示数据
        spectrumLineData.setDrawValues(true);

        // 设置图例的类型
        spectrumLineChart.getLegend().setForm(Legend.LegendForm.LINE);
        // 删除图表的Description
        spectrumLineChart.getDescription().setEnabled(true);
        // 图表描述
        spectrumLineChart.getDescription().setText(getResources().getString(R.string.line_chart_description));
        // 绘制图表背景
        spectrumLineChart.setDrawGridBackground(true);
        // 设置支持触控手势
        spectrumLineChart.setTouchEnabled(true);
        // 设置缩放
        spectrumLineChart.setDragEnabled(true);
        // 设置推动
        spectrumLineChart.setScaleEnabled(true);
        // 如果禁用,扩展可以在x轴和y轴分别完成
        spectrumLineChart.setPinchZoom(true);
        // 显示边框
        spectrumLineChart.setDrawBorders(true);
        // 添加无数据时的说明
        spectrumLineChart.setNoDataText(getResources().getString(R.string.line_chart_no_data));
        // 设置X轴的位置
        spectrumLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        // 关闭X轴的网格线
        spectrumLineChart.getXAxis().setDrawGridLines(true);
        // 设置X轴坐标之间的最小间隔
        spectrumLineChart.getXAxis().setGranularity(0.5f);
        // 设置X轴标签的倾斜度
        spectrumLineChart.getXAxis().setLabelRotationAngle(0);
        // 右侧的Y轴不显示
        spectrumLineChart.getAxisRight().setEnabled(false);
        // 关闭Y轴的网格线
        spectrumLineChart.getAxisLeft().setDrawGridLines(true);
        spectrumLineChart.getAxisLeft().setValueFormatter(new IndexAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf(value / 1000 + "万");
            }
        });
        // 设置数据
        spectrumLineChart.setData(spectrumLineData);

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

                            // 通知数据更新
                            spectrumListAdapter.notifyDataSetChanged();

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

                    // 通知数据更新
                    spectrumListAdapter.notifyDataSetChanged();

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

                            // 通知数据更新
                            spectrumListAdapter.notifyDataSetChanged();

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

                    // 通知数据更新
                    spectrumListAdapter.notifyDataSetChanged();

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

        spectrumList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (spectrumListAdapter.isShowCheckBox()) {
                    // 获取选中对象的组件
                    SpectrumListAdapter.ViewHolder holder = (SpectrumListAdapter.ViewHolder) view.getTag();
                    // 设置CheckBox的状态
                    holder.spectrumItemSelect.toggle();

                    serialData.get(position).put(Command.SPECTRUM_ITEM_STATUS_KEY, holder.spectrumItemSelect.isChecked());
                    spectrumList.setItemChecked(position, holder.spectrumItemSelect.isChecked());
                    spectrumListAdapter.notifyDataSetChanged();
                } else {
                    Boolean isShow = (Boolean) serialData.get(position).get(Command.SPECTRUM_ITEM_SHOW_KEY);

                    if (!isShow) {
                        addNewSpectrumLine(serialData.get(position));
                    } else {
                        removeSpectrumLine(serialData.get(position));
                    }
                }
            }
        });

        spectrumList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                // 显示操作菜单
                spectrumListOperateBar.setVisibility(View.VISIBLE);
                // 显示多选按钮
                spectrumListAdapter.setShowCheckBox(true);

                // 获取选中对象的组件
                SpectrumListAdapter.ViewHolder holder = (SpectrumListAdapter.ViewHolder) view.getTag();
                // 设置CheckBox的状态
                holder.spectrumItemSelect.setChecked((Boolean) serialData.get(position).get(Command.SPECTRUM_ITEM_STATUS_KEY));
                spectrumList.setItemChecked(position, holder.spectrumItemSelect.isChecked());
                spectrumListAdapter.notifyDataSetChanged();

                return true;
            }
        });

        spectrumItemUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 全部设置为False
                for (int i = 0; i < serialData.size(); i++) {
                    serialData.get(i).put(Command.SPECTRUM_ITEM_STATUS_KEY, false);
                }
                // 隐藏多选按钮
                spectrumListAdapter.setShowCheckBox(false);
                // 隐藏操作菜单
                spectrumListOperateBar.setVisibility(View.GONE);

                spectrumListAdapter.notifyDataSetChanged();
            }
        });

        spectrumItemDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < serialData.size(); i++) {
                    if (true == (Boolean) serialData.get(i).get(Command.SPECTRUM_ITEM_STATUS_KEY)) {
                        // 如果数据表显示则删除数据表显示
                        Boolean isShow = (Boolean) serialData.get(i).get(Command.SPECTRUM_ITEM_SHOW_KEY);
                        String name = (String) serialData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY);
                        if (isShow) {
                            removeSpectrumLine(serialData.get(i));
                        }
                        if (name.equals(Command.DARK_DATA)) {
                            hasDarkData = false;
                        }
                        if (name.equals(Command.LIGHT_DATA)) {
                            // 保存了亮数据
                            hasLightData = false;
                        }

                        // 删除数据
                        serialData.remove(i);

                        // 索引重新置位
                        i = -1;
                    }
                }
                // 隐藏多选按钮
                spectrumListAdapter.setShowCheckBox(false);
                // 隐藏操作菜单
                spectrumListOperateBar.setVisibility(View.GONE);

                spectrumListAdapter.notifyDataSetChanged();
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

    public List<Entry> getNormalSpectrumData() {
        return normalSpectrumData;
    }

    public LineChart getSpectrumLineChart() {
        return spectrumLineChart;
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

    private void addNewSpectrumLine(Map<String, Object> spectrumDataMap) {
        // 获取数据集中的数据
        ArrayList<Byte> spectrumDataList = (ArrayList<Byte>) spectrumDataMap.get(Command.SPECTRUM_ITEM_DATA_KEY);
        ArrayList<Entry> newSpectrumLine = new ArrayList<Entry>();

        byte[] spectrumData = ArrayUtils.toPrimitive(spectrumDataList.toArray(new Byte[]{}));

        // 填充数据集
        for (int i = 0, j = 0; i < spectrumData.length; i += 4, j++) {
            byte byte0 = spectrumData[i];
            byte byte1 = spectrumData[i + 1];
            byte byte2 = spectrumData[i + 2];
            byte byte3 = spectrumData[i + 3];

            int data = ((byte3 & 0xFF) << 24) | ((byte2 & 0xFF) << 16) | ((byte1 & 0xFF) << 8) | ((byte0 & 0xFF));

            newSpectrumLine.add(new Entry(j, data));
        }

        // 获取数据名称
        String spectrumName = (String) spectrumDataMap.get(Command.SPECTRUM_ITEM_NAME_KEY);

        LineDataSet newSpectrumSet = new LineDataSet(newSpectrumLine, spectrumName);

        // 设置数据线和数据点的颜色
        if (spectrumName.equals(Command.LIGHT_DATA)) {
            newSpectrumSet.setColor(getResources().getColor(R.color.colorLight, getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorLight, getTheme()));
        } else if (spectrumName.equals(Command.DARK_DATA)) {
            newSpectrumSet.setColor(getResources().getColor(R.color.colorDark, getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorDark, getTheme()));
        } else {
            newSpectrumSet.setColor(getResources().getColor(R.color.colorBlue, getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorBlue, getTheme()));
        }

        // 设置数据线的线宽
        newSpectrumSet.setLineWidth(2f);
        // 设置数据点的半径
        newSpectrumSet.setCircleRadius(4f);
        // 不绘制空心圆
        newSpectrumSet.setDrawCircleHole(false);
        // 设置数据点的文字大小
        newSpectrumSet.setValueTextSize(10f);
        // 不对折线图进行填充
        newSpectrumSet.setDrawFilled(false);
        // 图例的高度
        newSpectrumSet.setFormLineWidth(3f);
        // 图例的宽度
        newSpectrumSet.setFormSize(8f);
        // 设置折线图为弧线
        newSpectrumSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 添加数据线
        spectrumDataSets.add(newSpectrumSet);

        // 显示数据
        spectrumLineChart.getData().notifyDataChanged();
        spectrumLineChart.notifyDataSetChanged();
        spectrumLineChart.invalidate();

        // 修改显示状态
        spectrumDataMap.put(Command.SPECTRUM_ITEM_SHOW_KEY, true);
    }

    private void removeSpectrumLine(Map<String, Object> spectrumDataMap) {
        // 获取数据名称
        String spectrumName = (String) spectrumDataMap.get(Command.SPECTRUM_ITEM_NAME_KEY);
        // 获取要删除的数据集
        ILineDataSet dataSet = spectrumLineChart.getData().getDataSetByLabel(spectrumName, true);
        // 删除数据集
        spectrumLineChart.getData().removeDataSet(dataSet);

        // 显示数据
        spectrumLineChart.getData().notifyDataChanged();
        spectrumLineChart.notifyDataSetChanged();
        spectrumLineChart.invalidate();

        // 修改显示状态
        spectrumDataMap.put(Command.SPECTRUM_ITEM_SHOW_KEY, false);
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
}
