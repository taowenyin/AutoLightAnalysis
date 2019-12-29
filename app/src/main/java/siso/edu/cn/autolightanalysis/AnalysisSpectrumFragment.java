package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class AnalysisSpectrumFragment extends Fragment {
    public static final String TAG = "===AnalysisSpectrumFragment===";

    private static final String ARG_PARAM_TITLE = "title";

    private Context context = null;

    private String title = StringUtils.EMPTY;

    private OnFragmentMonitorListener mListener = null;
    private MainActivity mainActivity = null;

    private LineChart spectrumLineChart = null;
    private ListView spectrumList = null;
    private LinearLayout spectrumListOperateBar = null;
    private ImageButton spectrumItemUndo = null;
    private ImageButton spectrumItemDelete = null;
    private View rootView = null;

    private SpectrumListAdapter spectrumListAdapter = null;

    // 折线图数据集
    private ArrayList<ILineDataSet> spectrumDataLineSets = new ArrayList<ILineDataSet>();
    // 串口数据
    private ArrayList<Map<String, Object>> serialData = new ArrayList<Map<String, Object>>();
    // 串口列表数据
    private ArrayList<Map<String, Object>> serialListData = new ArrayList<Map<String, Object>>();

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

        // 传递Handler
        mainActivity = (MainActivity) context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fragment_analysis_spectrum, container, false);

            spectrumLineChart = rootView.findViewById(R.id.spectrum_line_chart);
            spectrumList = rootView.findViewById(R.id.spectrum_list);
            spectrumListOperateBar = rootView.findViewById(R.id.spectrum_list_operate_bar);
            spectrumItemUndo = rootView.findViewById(R.id.spectrum_item_undo);
            spectrumItemDelete = rootView.findViewById(R.id.spectrum_item_delete);

            spectrumListAdapter = new SpectrumListAdapter(getContext(), serialListData);
            spectrumList.setAdapter(spectrumListAdapter);
            // 设置ListView为多选项
            spectrumList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            // 折线图数据集
            LineData spectrumLineData = new LineData(spectrumDataLineSets);
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
                    // TODO: 19-12-29 根据实际情况修改
                    return String.valueOf(value / 100000 + "万");
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
                        mListener.onUpdateSpectrumItemChecked(position, !holder.spectrumItemSelect.isChecked());
                    } else {
                        Boolean isShow = (Boolean) serialData.get(position).get(Command.SPECTRUM_ITEM_SHOW_KEY);
                        if (!isShow) {
                            mListener.onUpdateSpectrumItemShow(position, true);
                        } else {
                            mListener.onUpdateSpectrumItemShow(position, false);
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
                    spectrumListAdapter.notifyDataSetChanged();

                    return true;
                }
            });

            spectrumItemUndo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 隐藏多选按钮
                    spectrumListAdapter.setShowCheckBox(false);
                    // 隐藏操作菜单
                    spectrumListOperateBar.setVisibility(View.GONE);
                    mListener.onUpdateSpectrumUndo();
                }
            });

            spectrumItemDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.onUpdateSpectrumItemDelete();
                    // 隐藏多选按钮
                    spectrumListAdapter.setShowCheckBox(false);
                    // 隐藏操作菜单
                    spectrumListOperateBar.setVisibility(View.GONE);
                }
            });
        } else {
            ViewGroup parent = (ViewGroup) rootView.getParent();
            if (parent != null) {
                parent.removeView(rootView);
            }
        }

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentMonitorListener) {
            mListener = (OnFragmentMonitorListener) context;
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
        // 清空所有线
        spectrumDataLineSets.clear();
        // 清空数据
        serialData.clear();
        // 清空列表数据
        serialListData.clear();

        for (int i = 0; i < spectrumSerialData.size(); i++) {
            Map<String, Object> lineData = spectrumSerialData.get(i);

            String name = (String) lineData.get(Command.SPECTRUM_ITEM_NAME_KEY);
            boolean isShow = (boolean) lineData.get(Command.SPECTRUM_ITEM_SHOW_KEY);
            boolean status = (boolean) lineData.get(Command.SPECTRUM_ITEM_STATUS_KEY);

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

            if (isShow) {
                // 添加数据线对象
                spectrumDataLineSets.add(createLineStyle(spectrumLine, name));
            }
            // 添加图表数据
            serialData.add(lineData);

            Map<String, Object> listItem = new HashMap<>();
            listItem.put(Command.SPECTRUM_ITEM_NAME_KEY, name);
            listItem.put(Command.SPECTRUM_ITEM_SHOW_KEY, isShow);
            listItem.put(Command.SPECTRUM_ITEM_STATUS_KEY, status);
            serialListData.add(listItem);
        }

        // 显示数据
        spectrumLineChart.getData().notifyDataChanged();
        spectrumLineChart.notifyDataSetChanged();
        spectrumLineChart.invalidate();

        // 显示列表
        spectrumListAdapter.notifyDataSetChanged();
    }

    private LineDataSet createLineStyle(ArrayList<Entry> spectrumLine, String label) {
        LineDataSet spectrumSet = new LineDataSet(spectrumLine, label);
        switch (label) {
            case Command.NORMAL_DATA:
                // 设置数据线的颜色
                spectrumSet.setColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
                // 设置数据点的颜色
                spectrumSet.setCircleColor(getResources().getColor(R.color.colorBlue, getActivity().getTheme()));
                break;
            case Command.LIGHT_DATA:
                // 设置数据线的颜色
                spectrumSet.setColor(getResources().getColor(R.color.colorLight, getActivity().getTheme()));
                // 设置数据点的颜色
                spectrumSet.setCircleColor(getResources().getColor(R.color.colorLight, getActivity().getTheme()));
                break;
            case Command.DARK_DATA:
                // 设置数据线的颜色
                spectrumSet.setColor(getResources().getColor(R.color.colorDark, getActivity().getTheme()));
                // 设置数据点的颜色
                spectrumSet.setCircleColor(getResources().getColor(R.color.colorDark, getActivity().getTheme()));
                break;
        }

        // 设置数据线的线宽
        spectrumSet.setLineWidth(2f);
        // 设置数据点的半径
        spectrumSet.setCircleRadius(4f);
        // 不绘制空心圆
        spectrumSet.setDrawCircleHole(false);
        // 设置数据点的文字大小
        spectrumSet.setValueTextSize(10f);
        // 不对折线图进行填充
        spectrumSet.setDrawFilled(false);
        // 图例的高度
        spectrumSet.setFormLineWidth(3f);
        // 图例的宽度
        spectrumSet.setFormSize(8f);
        // 设置折线图为弧线
        spectrumSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        return spectrumSet;
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

    public interface OnFragmentMonitorListener {
        void onUpdateSpectrumItemShow(int position, boolean isShow);
        void onUpdateSpectrumItemChecked(int position, boolean checked);
        void onUpdateSpectrumUndo();
        void onUpdateSpectrumItemDelete();
    }

    public interface OnHasDarkDataListener {
        void OnHasDarkData(boolean hasDarkData);
    }

    public interface OnHasLightDataListener {
        void OnHasLightData(boolean hasLightData);
    }
}
