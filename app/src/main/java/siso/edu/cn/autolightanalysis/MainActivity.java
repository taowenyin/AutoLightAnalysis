package siso.edu.cn.autolightanalysis;

import android.graphics.DashPathEffect;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.MPPointF;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.common.primitives.Bytes;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private LineChart spectrumLineChart = null;

    private UartDevice uartDevice = null;
    private PeripheralManager manager = PeripheralManager.getInstance();

    // 创建白光谱数据集和数据线
    private List<Entry> lightSpectrumData = new ArrayList<Entry>();
    // 创建暗光谱数据集和数据线
    private List<Entry> darkSpectrumData = new ArrayList<Entry>();
    // 折线图数据集
    private List<ILineDataSet> spectrumDataSets = new ArrayList<ILineDataSet>();

    // 串口数据
    private List<Byte> serialDataBuffer = new ArrayList<Byte>();
    // 串口数据异步传输
    private Handler serialHandler = new SerialHandler(this);

    // 保存当前的指令
    private String currentCommand = StringUtils.EMPTY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectDeviceBtn = findViewById(R.id.connect_device_btn);
        saveLightSpectrumBtn = findViewById(R.id.save_light_spectrum_btn);
        saveDarkSpectrumBtn = findViewById(R.id.save_dark_spectrum_btn);
        readSpectrumBtn = findViewById(R.id.read_spectrum_btn);
        toggleAutoReadSpectrumBtn = findViewById(R.id.toggle_auto_read_spectrum_btn);
        spectrumLineChart = findViewById(R.id.spectrum_line_chart);

        LineDataSet lightSpectrumSet = new LineDataSet(lightSpectrumData, getResources().getString(R.string.light_spectrum_legend));
        // 设置数据线的颜色
        lightSpectrumSet.setColor(getResources().getColor(R.color.colorLight, getTheme()));
        // 设置数据线的线宽
        lightSpectrumSet.setLineWidth(2f);
        // 设置数据点的颜色
        lightSpectrumSet.setCircleColor(getResources().getColor(R.color.colorLight, getTheme()));
        // 设置数据点的半径
        lightSpectrumSet.setCircleRadius(4f);
        // 不绘制空心圆
        lightSpectrumSet.setDrawCircleHole(false);
        // 设置数据点的文字大小
        lightSpectrumSet.setValueTextSize(10f);
        // 不对折线图进行填充
        lightSpectrumSet.setDrawFilled(false);
        // 图例的高度
        lightSpectrumSet.setFormLineWidth(3f);
        // 图例的宽度
        lightSpectrumSet.setFormSize(8f);
        // 设置折线图为弧线
        lightSpectrumSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);

        LineDataSet darkSpectrumSet = new LineDataSet(darkSpectrumData, getResources().getString(R.string.dark_spectrum_legend));
        // 设置数据线的颜色
        darkSpectrumSet.setColor(getResources().getColor(R.color.colorDark, getTheme()));
        // 设置数据线的线宽
        darkSpectrumSet.setLineWidth(2f);
        // 设置数据点的颜色
        darkSpectrumSet.setCircleColor(getResources().getColor(R.color.colorDark, getTheme()));
        // 设置数据点的半径
        darkSpectrumSet.setCircleRadius(4f);
        // 不绘制空心圆
        darkSpectrumSet.setDrawCircleHole(false);
        // 设置数据点的文字大小
        darkSpectrumSet.setValueTextSize(10f);
        // 不对折线图进行填充
        darkSpectrumSet.setDrawFilled(false);
        // 图例的高度
        darkSpectrumSet.setFormLineWidth(3f);
        // 图例的宽度
        darkSpectrumSet.setFormSize(8f);
        // 设置折线图为弧线
        darkSpectrumSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // 添加数据线
        spectrumDataSets.add(lightSpectrumSet);
        spectrumDataSets.add(darkSpectrumSet);
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

            }
        });

        saveDarkSpectrumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                LineDataSet lightDataSet = (LineDataSet) spectrumLineChart.getData().getDataSetByIndex(0);
//                lightSpectrumData.add(new Entry(0, 2));
//                lightSpectrumData.add(new Entry(1, 4));
//                lightSpectrumData.add(new Entry(2, 3));
//                lightSpectrumData.add(new Entry(3, 4));
//                lightDataSet.setValues(lightSpectrumData);
//
//                LineDataSet darkDataSet = (LineDataSet) spectrumLineChart.getData().getDataSetByIndex(1);
//                darkSpectrumData.add(new Entry(0, 4));
//                darkSpectrumData.add(new Entry(1, 8));
//                darkSpectrumData.add(new Entry(2, 6));
//                darkSpectrumData.add(new Entry(3, 8));
//                darkDataSet.setValues(darkSpectrumData);
//
//                spectrumLineChart.getData().notifyDataChanged();
//                spectrumLineChart.notifyDataSetChanged();
//                spectrumLineChart.invalidate();
            }
        });

        readSpectrumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // 发送读取光谱的指令
                    uartDevice.write(Command.READ_SPECTRUM.getBytes(), Command.READ_INTERNAL_TEMPERATURE.getBytes().length);
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
                if (isChecked) {
                    readSpectrumBtn.setEnabled(false);
                } else {
                    readSpectrumBtn.setEnabled(true);
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
                if (serialDataBuffer.size() >= Command.MAX_SPECTRUM_DATA_LENGTH) {
                    // 构建消息数据
                    Bundle data = new Bundle();
                    data.putByteArray(SerialHandler.DATA, Bytes.toArray(serialDataBuffer));
                    Message dataMsg = new Message();
                    dataMsg.what = SerialHandler.READ_SPECTRUM;
                    dataMsg.setData(data);

                    // 发送消息
                    serialHandler.sendMessage(dataMsg);
                }
            }
        }
    }

    public List<Entry> getLightSpectrumData() {
        return lightSpectrumData;
    }

    public List<Entry> getDarkSpectrumData() {
        return darkSpectrumData;
    }

    public List<ILineDataSet> getSpectrumDataSets() {
        return spectrumDataSets;
    }

    public LineChart getSpectrumLineChart() {
        return spectrumLineChart;
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
