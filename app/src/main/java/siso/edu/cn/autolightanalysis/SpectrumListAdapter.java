package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

public class SpectrumListAdapter extends BaseAdapter {

    private ArrayList<Map<String, Object>> serialData = null;

    private ViewHolder holder = null;
    // 当前是否处于多选状态
    private boolean isShowCheckBox = false;

    private Context context = null;

    public SpectrumListAdapter(Context context, ArrayList<Map<String, Object>> serialData) {
        this.context = context;
        this.serialData = serialData;
    }

    @Override
    public int getCount() {
        return serialData.size();
    }

    @Override
    public Object getItem(int position) {
        return serialData.get(position);
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
        holder.spectrumItemSelect.setChecked((Boolean) serialData.get(position).get(Command.SPECTRUM_ITEM_STATUS_KEY));

        // 获取数据项的内容
        String itemName = (String) serialData.get(position).get(Command.SPECTRUM_ITEM_NAME_KEY);
        if (itemName.equals(Command.LIGHT_DATA)) {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_light_36dp);
        } else if (itemName.equals(Command.DARK_DATA)) {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_dark_36dp);
        } else {
            holder.spectrumItemIcon.setImageResource(R.drawable.ic_normal_36dp);
        }
        holder.spectrumItemName.setText(itemName);

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
