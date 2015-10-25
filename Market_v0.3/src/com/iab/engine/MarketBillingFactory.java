package com.iab.engine;

import android.content.Context;

import com.borqs.market.utils.MarketConfiguration;
import com.iab.engine.google.util.IabHelper.QueryInventoryFinishedListener;
import com.iab.framework.MarketIab;

public class MarketBillingFactory {

    public static MarketBilling createMarketBilling(Context context) {
//        if(MarketConfiguration.billingType == MarketBillingResult.TYPE_IAP) {
//            return new IAPPurchase(context);
//        }else {
            return new MarketIab(context, null);
//        }
    }

}
