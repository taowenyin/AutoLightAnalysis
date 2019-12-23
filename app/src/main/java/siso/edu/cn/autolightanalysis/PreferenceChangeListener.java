package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.content.SharedPreferences;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;

public class PreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Context context = null;
    private PreferenceFragmentCompat preference = null;

    public PreferenceChangeListener(Context context, PreferenceFragmentCompat preference) {
        this.context = context;
        this.preference = preference;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String label = StringUtils.EMPTY;

        if (key.equals(context.getResources().getString(R.string.preference_is_packing_key))
                || key.equals(context.getResources().getString(R.string.preference_smooth_key))) {
            return;
        }

        if (key.equals(context.getResources().getString(R.string.preference_type_key))) {
             String[] labelArray = context.getResources().getStringArray(R.array.product_type_array);
             label = labelArray[Integer.valueOf(sharedPreferences.getString(key, context.getResources().getString(R.string.preference_uninitialized)))];
        }

        if (key.equals(context.getResources().getString(R.string.preference_packing_type_key))) {
            String[] labelArray = context.getResources().getStringArray(R.array.packing_type_array);
            label = labelArray[Integer.valueOf(sharedPreferences.getString(key, context.getResources().getString(R.string.preference_uninitialized)))];
        }

        if (key.equals(context.getResources().getString(R.string.preference_packing_form_key))) {
            String[] labelArray = context.getResources().getStringArray(R.array.packing_form_array);
            label = labelArray[Integer.valueOf(sharedPreferences.getString(key, context.getResources().getString(R.string.preference_uninitialized)))];
        }

        if (key.equals(context.getResources().getString(R.string.preference_smooth_count_key)) ||
                key.equals(context.getResources().getString(R.string.preference_integration_time_key))) {
            label = sharedPreferences.getString(key, context.getResources().getString(R.string.preference_uninitialized));
        }

        preference.findPreference(key).setSummary(label);
    }
}
