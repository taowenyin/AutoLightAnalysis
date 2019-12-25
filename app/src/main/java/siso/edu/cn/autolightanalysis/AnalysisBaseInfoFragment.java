package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;


public class AnalysisBaseInfoFragment extends PreferenceFragmentCompat {
    private static final String ARG_PARAM_TITLE = "title";

    private String title = StringUtils.EMPTY;

    private OnFragmentInteractionListener mListener;

    // Preference监听
    private PreferenceChangeListener preferenceListener = null;

    public static String TAG = "===AnalysisBaseInfoFragment===";

    public AnalysisBaseInfoFragment() {
        // Required empty public constructor
    }

    public static AnalysisBaseInfoFragment newInstance(String title) {
        AnalysisBaseInfoFragment fragment = new AnalysisBaseInfoFragment();
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
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        // 设置配置信息名
        getPreferenceManager().setSharedPreferencesName(getString(R.string.preference_name));
        setPreferencesFromResource(R.xml.base, rootKey);
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

            preferenceListener = new PreferenceChangeListener(getContext(), this);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(preferenceListener);

        String key = getResources().getString(R.string.preference_type_key);
        String[] labelArray = getResources().getStringArray(R.array.product_type_array);
        String value = getPreferenceManager().getSharedPreferences().getString(key, "-1");
        findPreference(key).setSummary(value.equals("-1") ? getResources().getString(R.string.preference_uninitialized) : labelArray[Integer.valueOf(value)]);

        key = getResources().getString(R.string.preference_packing_type_key);
        labelArray = getResources().getStringArray(R.array.packing_type_array);
        value = getPreferenceManager().getSharedPreferences().getString(key, "-1");
        findPreference(key).setSummary(value.equals("-1") ? getResources().getString(R.string.preference_uninitialized) : labelArray[Integer.valueOf(value)]);

        key = getResources().getString(R.string.preference_packing_form_key);
        labelArray = getResources().getStringArray(R.array.packing_form_array);
        value = getPreferenceManager().getSharedPreferences().getString(key, "-1");
        findPreference(key).setSummary(value.equals("-1") ? getResources().getString(R.string.preference_uninitialized) : labelArray[Integer.valueOf(value)]);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
