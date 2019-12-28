package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AnalysisSpectrumFragment extends Fragment {
    public static final String TAG = "===AnalysisSpectrumFragment===";

    private static final String ARG_PARAM_TITLE = "title";

    private Context context = null;

    private String title = StringUtils.EMPTY;

    private OnFragmentInteractionListener mListener = null;
    private MainActivity mainActivity = null;

    private LineChart spectrumLineChart = null;
    private ListView spectrumList = null;
    private LinearLayout spectrumListOperateBar = null;
    private ImageButton spectrumItemUndo = null;
    private ImageButton spectrumItemDelete = null;

    private SpectrumListAdapter spectrumListAdapter = null;

    // 创建普通光谱数据集和数据线
    private ArrayList<Entry> normalSpectrumData = new ArrayList<Entry>();
    // 折线图数据集
    private ArrayList<ILineDataSet> spectrumDataSets = new ArrayList<ILineDataSet>();

    // 串口数据
    private ArrayList<Map<String, Object>> serialData = new ArrayList<Map<String, Object>>();

    private Handler handler = null;

    public AnalysisSpectrumFragment() {
        // Required empty public constructor
    }

    public static AnalysisSpectrumFragment newInstance(String title) {
        AnalysisSpectrumFragment fragment = new AnalysisSpectrumFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_TITLE, title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            title = getArguments().getString(ARG_PARAM_TITLE);
        }

        // 初始化接收消息对象
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };

        // 传递Handler
        mainActivity = (MainActivity) context;
        mainActivity.setHandler(handler);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_analysis_spectrum, container, false);

        spectrumLineChart = rootView.findViewById(R.id.spectrum_line_chart);
        spectrumList = rootView.findViewById(R.id.spectrum_list);
        spectrumListOperateBar = rootView.findViewById(R.id.spectrum_list_operate_bar);
        spectrumItemUndo = rootView.findViewById(R.id.spectrum_item_undo);
        spectrumItemDelete = rootView.findViewById(R.id.spectrum_item_delete);

        spectrumListAdapter = new SpectrumListAdapter(getContext(), serialData);
        spectrumList.setAdapter(spectrumListAdapter);
        // 设置ListView为多选项
        spectrumList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        LineDataSet normalSpectrumSet = new LineDataSet(normalSpectrumData, Command.NORMAL_TMP_DATA);
        // 设置数据线的颜色
        normalSpectrumSet.setColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
        // 设置数据线的线宽
        normalSpectrumSet.setLineWidth(2f);
        // 设置数据点的颜色
        normalSpectrumSet.setCircleColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
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
                            mainActivity.OnHasDarkData(false);
                        }
                        if (name.equals(Command.LIGHT_DATA)) {
                            // 保存了亮数据
                            mainActivity.OnHasLightData(false);
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

        return rootView;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void updateSpectrumData(ArrayList<Map<String, Object>> spectrumSerialData) {
        for (int i = 0; i < spectrumSerialData.size(); i++) {
            Map<String, Object> lineData = spectrumSerialData.get(i);
            String name = (String) lineData.get(Command.SPECTRUM_ITEM_NAME_KEY);
            boolean status = (boolean) lineData.get(Command.SPECTRUM_ITEM_STATUS_KEY);
            boolean isShow = (boolean) lineData.get(Command.SPECTRUM_ITEM_SHOW_KEY);

            ArrayList<Entry> spectrumLine = new ArrayList<Entry>();

            if (!name.equals(Command.NORMAL_DATA)) {
                ArrayList<Integer> data = (ArrayList<Integer>) lineData.get(Command.SPECTRUM_ITEM_DATA_KEY);
                for (int j = 0; j < data.size(); j++) {
                    Entry entry = new Entry();
                    entry.setX(j);
                    int value = data.get(j).intValue();
                    entry.setY((float) value);
                    spectrumLine.add(entry);
                }
            } else {
                ArrayList<Float> data = (ArrayList<Float>) lineData.get(Command.SPECTRUM_ITEM_DATA_KEY);
                for (int j = 0; j < data.size(); j++) {
                    Entry entry = new Entry(j, data.get(j));
                    spectrumLine.add(entry);
                }
            }

            Log.i(TAG, "");
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
            newSpectrumSet.setColor(getResources().getColor(R.color.colorLight, getActivity().getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorLight, getActivity().getTheme()));
        } else if (spectrumName.equals(Command.DARK_DATA)) {
            newSpectrumSet.setColor(getResources().getColor(R.color.colorDark, getActivity().getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorDark, getActivity().getTheme()));
        } else {
            newSpectrumSet.setColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
            newSpectrumSet.setCircleColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
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

    public LineChart getSpectrumLineChart() {
        return spectrumLineChart;
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }

    public interface OnHasDarkDataListener {
        void OnHasDarkData(boolean hasDarkData);
    }

    public interface OnHasLightDataListener {
        void OnHasLightData(boolean hasLightData);
    }
}
