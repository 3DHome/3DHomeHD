package com.borqs.se.download;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.borqs.freehdhome.R;
import com.borqs.market.json.Product.SupportedMod;
import com.borqs.market.utils.MarketUtils;
import com.support.StaticActivity;

public class LocalThemeActivity extends StaticActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.theme_local);
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(R.string.themes);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        Button download = (Button) findViewById(R.id.download);
        download.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent intent = new Intent(LocalThemeActivity.this, OnlineThemeActivity.class);
//                startActivity(intent);
                MarketUtils.startProductListIntent(LocalThemeActivity.this,
                        MarketUtils.CATEGORY_THEME, true,SupportedMod.PORTRAIT);
            }

        });
    }
}
