package siso.edu.cn.autolightanalysis;

import android.graphics.Color;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

public class DataPreprocessingDialog extends DialogFragment {

    private View rootView = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 设置背景为透明
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setCancelable(false); // 不允许按键返回

        rootView = inflater.inflate(R.layout.fragment_data_preprocessing_dialog, container);

        ImageView progressImg = rootView.findViewById(R.id.progress_img);
        Drawable drawable = progressImg.getDrawable();
        if (drawable instanceof Animatable) {
            ((Animatable) drawable).start();
        }

        return rootView;
    }

}
