package siso.edu.cn.autolightanalysis;

import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class AnalysisPageAdapter extends FragmentPagerAdapter {

    private List<Fragment> fragments = null;
    private List<String> indicators = null;

    public AnalysisPageAdapter(FragmentManager fm, List<Fragment> fragments, List<String> indicators) {
        super(fm);

        this.fragments = fragments;
        this.indicators = indicators;
    }

    @Override
    public Fragment getItem(int i) {
        return this.fragments.get(i);
    }

    @Override
    public int getCount() {
        return this.fragments.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return this.indicators.get(position);
    }
}
