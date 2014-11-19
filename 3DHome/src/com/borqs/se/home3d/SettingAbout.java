package com.borqs.se.home3d;

import com.borqs.se.R;
import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

public class SettingAbout extends Activity {
    private int mClickCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayShowTitleEnabled(true);
        bar.setHomeButtonEnabled(true);
        bar.setDisplayHomeAsUpEnabled(true);
        TextView versionTV = (TextView) findViewById(R.id.version);
        String packageName = getPackageName();
        String version = "0.1.1(1001)";
        try {
            version = getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        versionTV.setText(String.format(SettingAbout.this.getString(R.string.about_version), String.valueOf(version)));
        View logo = findViewById(R.id.logo);

        logo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mClickCount++;
                if (mClickCount % 5 == 0) {
                    HomeManager.getInstance().debug(!HomeUtils.DEBUG);
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            return true;
        }

        return true;
    }
}
