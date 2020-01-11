package siso.edu.cn.autolightanalysis;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;


public class AnalysisIndexFragment extends Fragment {
    private static final String ARG_PARAM_TITLE = "title";

    private String title = StringUtils.EMPTY;

    private TextView indexDmbTxt = null, indexOmbTxt = null, indexMmbTxt = null;

    private View root = null;

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
        if (root == null) {
            root = inflater.inflate(R.layout.fragment_analysis_index, container, false);

            indexDmbTxt = root.findViewById(R.id.index_dmb_txt);
            indexMmbTxt = root.findViewById(R.id.index_mmb_txt);
            indexOmbTxt = root.findViewById(R.id.index_omb_txt);
        } else {
            ViewGroup parent = (ViewGroup) root.getParent();
            if (parent != null) {
                parent.removeView(root);
            }
        }

        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void calculateIndex(SharedPreferences preferences, ArrayList<Map<String, Object>> spectrumData) {
        for (int i = 0; i < spectrumData.size(); i++) {
            if (spectrumData.get(i).get(Command.SPECTRUM_ITEM_NAME_KEY).equals(Command.NORMAL_DATA)) {
                ArrayList<Float> data = (ArrayList<Float>) spectrumData.get(i).get(Command.SPECTRUM_ITEM_DATA_KEY);

                int productType = Integer.valueOf(
                        preferences.getString(getResources().getString(R.string.preference_type_key), "-1"));
                boolean isPacking = preferences.getBoolean(getResources().getString(R.string.preference_is_packing_key), false);
                int packingTypeIndex = Integer.valueOf(
                        preferences.getString(getResources().getString(R.string.preference_packing_type_key), "-1"));
                Algorithm.PackingType type = Algorithm.PackingType.values()[packingTypeIndex];

                double[] prediction = {3.15, 2.9, 267};

                if (!isPacking) {
                    if (productType == 0) {
                        prediction = Algorithm.NoPacking.beefPrediction(
                                data.get(698), data.get(741), data.get(785), data.get(800));
                    }
                    if (productType == 1) {
                        prediction = Algorithm.NoPacking.porkPrediction(
                                data.get(698), data.get(741), data.get(785), data.get(800));
                    }
                } else {
                    if (productType == 0) {
                        prediction = Algorithm.Packing.beefPrediction(
                                data.get(698), data.get(741), data.get(785), data.get(800), type);
                    }
                }

                if (prediction != null) {
                    DecimalFormat df = new DecimalFormat("0.00");
                    indexDmbTxt.setText(df.format(prediction[0]));
                    indexOmbTxt.setText(df.format(prediction[1]));
                    indexMmbTxt.setText(df.format(prediction[2]));
                }
            }
        }
    }
}
