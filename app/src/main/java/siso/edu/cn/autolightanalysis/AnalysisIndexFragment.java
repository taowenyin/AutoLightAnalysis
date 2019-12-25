package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;


public class AnalysisIndexFragment extends Fragment {
    private static final String ARG_PARAM_TITLE = "title";

    private String title = StringUtils.EMPTY;

    private OnFragmentInteractionListener mListener;

    public AnalysisIndexFragment() {
        // Required empty public constructor
    }

    public static AnalysisIndexFragment newInstance(String title) {
        AnalysisIndexFragment fragment = new AnalysisIndexFragment();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_analysis_index, container, false);
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void calculateIndex(SharedPreferences preferences, ArrayList<Map<String, Object>> spectrumData) {
        for (int i = 0; i < spectrumData.size(); i++) {
            if (spectrumData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.NORMAL_DATA)) {
                ArrayList<Float> data = (ArrayList<Float>) spectrumData.get(i).get(Command.SPECTRUM_ITEM_DATA_KEY);

                String productType = preferences.getString(getResources().getString(R.string.preference_type_key), "-1");
                boolean isPacking = preferences.getBoolean(getResources().getString(R.string.preference_is_packing_key), false);
                String packingType = preferences.getString(getResources().getString(R.string.preference_packing_type_key), "-1");

                return;
            }
        }
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
    }
}
