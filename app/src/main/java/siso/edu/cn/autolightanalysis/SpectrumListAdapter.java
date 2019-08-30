package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class SpectrumListAdapter extends BaseAdapter {

    private Map<String, List<Byte>> serialData = null;
    private ViewHolder holder = null;
    // 当前是否处于多选状态
    private boolean isShowCheckBox = false;
    // 存放CheckBox状态值
    private SparseBooleanArray stateCheckedMap = new SparseBooleanArray();

    private Context context = null;

    public SpectrumListAdapter(Context context, Map<String, List<Byte>> serialData, SparseBooleanArray stateCheckedMap) {
        this.context = context;
        this.serialData = serialData;
        this.stateCheckedMap = stateCheckedMap;
    }

    @Override
    public int getCount() {
        return serialData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = View.inflate(context, R.layout.spectrum_list_item, null);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.spectrumItemSelect = convertView.findViewById(R.id.spectrum_item_select);
        holder.spectrumItemName = convertView.findViewById(R.id.spectrum_item_name);
        holder.spectrumItemIcon = convertView.findViewById(R.id.spectrum_item_icon);

        if (isShowCheckBox) {
            holder.spectrumItemSelect.setVisibility(View.VISIBLE);
        } else {
            holder.spectrumItemSelect.setVisibility(View.GONE);
        }
        holder.spectrumItemSelect.setChecked(stateCheckedMap.get(position));
        if (position == 0) {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_light_36dp);
            holder.spectrumItemName.setText(Command.LIGHT_DATA);
        } else if (position == 1) {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_dark_36dp);
            holder.spectrumItemName.setText(Command.DARK_DATA);
        } else {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_normal_36dp);
            holder.spectrumItemName.setText(String.format(Command.NORMAL_DATA, position));
        }

        return convertView;
    }

    public boolean isShowCheckBox() {
        return isShowCheckBox;
    }

    public void setShowCheckBox(boolean showCheckBox) {
        this.isShowCheckBox = showCheckBox;
    }

    public class ViewHolder {
        public TextView spectrumItemName;
        public CheckBox spectrumItemSelect;
        public ImageView spectrumItemIcon;
    }
}
