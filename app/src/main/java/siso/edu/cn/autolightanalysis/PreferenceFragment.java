package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat;

import org.apache.commons.lang3.StringUtils;


public class PreferenceFragment extends PreferenceFragmentCompat {
    private static final String ARG_PARAM_TITLE = "title";

    private String title = StringUtils.EMPTY;

    private OnFragmentInteractionListener mListener;

    // Preference监听
    private PreferenceChangeListener preferenceListener = null;

    public static String TAG = "===PreferenceFragment===";

    public PreferenceFragment() {
        // Required empty public constructor
    }

    public static PreferenceFragment newInstance(String title) {
        PreferenceFragment fragment = new PreferenceFragment();
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
        setPreferencesFromResource(R.xml.preferences, rootKey);
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

        String key = getResources().getString(R.string.preference_smooth_count_key);
        findPreference(key).setSummary(getPreferenceManager().getSharedPreferences().getString(key,
                getResources().getString(R.string.preference_uninitialized)));

        key = getResources().getString(R.string.preference_integration_time_key);
        findPreference(key).setSummary(getPreferenceManager().getSharedPreferences().getString(key,
                getResources().getString(R.string.preference_uninitialized)));
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
