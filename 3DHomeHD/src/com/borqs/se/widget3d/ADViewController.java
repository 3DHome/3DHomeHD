package com.borqs.se.widget3d;

import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.borqs.freehdhome.R;
import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SECamera.CameraChangedListener;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.ADViewIntegrated.AdListener;
import com.google.ads.Ad;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import com.google.ads.AdRequest.ErrorCode;
import com.iab.engine.google.util.IabHelper;
import com.iab.engine.google.util.IabResult;
import com.iab.engine.google.util.Inventory;
import com.iab.engine.google.util.Purchase;

import com.support.StaticUtil;

public class ADViewController implements CameraChangedListener {
    private static final String TAG = "ADViewController";
    private final static int TYPE_ADMOD = 0;
    private final static int TYPE_NULL = -1;
    private static final String SKU_AD_REMOVAL = "sku_ad_remove";

    //private static String AD_UNIT = "a150d168b3b6e1f"; // shared id with generic version
    private static String AD_UNIT = "a151c957cf5b853"; // separate id
    private int mCurShowType = TYPE_NULL;

    private boolean mHadLoadedADMod = false;
    private SESceneManager mSESceneManager;
    private ADViewIntegrated mADViewIntegrated;
    private Context mContext;
    private SEScene mScene;
    private static ADViewController mADViewController;

    private List<ModelInfo> mFlyers;
    private int mCurrentFlyerIndex;
    private Flyer mCurrentFlyer;
    private boolean isFly;

    private ADViewController() {
        mSESceneManager = SESceneManager.getInstance();
        mContext = mSESceneManager.getContext();
        mCurrentFlyerIndex = 0;
        checkAdPolicy();
    }

    public static ADViewController getInstance() {
        if (mADViewController == null) {
            mADViewController = new ADViewController();
        }
        return mADViewController;
    }

    public void onPause() {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.setVisibility(View.GONE);
        }
    }

    public void onResume() {
        if (mADViewIntegrated == null) {
            initAds();
        } else {
            mADViewIntegrated.setVisibility(View.VISIBLE);
        }

        postCheckLoadedAd();
    }

    public void onDestory() {
        deleteAD();
    }

    public void setSEScene(SEScene scene) {
        mScene = scene;
    }

    private void initAds() {
        initADIntegrated();
        initADMod();
    }

    private void initADIntegrated() {
        mADViewIntegrated = new ADViewIntegrated(mContext);
        LinearLayout.LayoutParams adParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        mADViewIntegrated.setFocusable(true);
        mADViewIntegrated.setLayoutParams(adParams);
        mADViewIntegrated.setOrientation(LinearLayout.VERTICAL);
        mADViewIntegrated.setTranslationY(-200);
        mADViewIntegrated.setAdListener(new AdListener() {

            public void onFailedToReceiveAd(int index, String arg0) {

            }

            public void onReceiveAd(int index) {
                if (index == TYPE_ADMOD) {
                    mHadLoadedADMod = true;
                    mCurShowType = TYPE_ADMOD;
                }
            }

            public void onLoadADS(int index) {
                if (index == TYPE_ADMOD) {
                    if (HomeUtils.DEBUG) {
                        Log.d(HomeUtils.TAG, "request loadAdmob");
                    }
                    enqueueAdRequest();

                }
            }

            public void onStopLoadAD(int index) {
                if (index == TYPE_ADMOD) {
                    com.google.ads.AdView admod = (AdView) mADViewIntegrated.getChildAt(TYPE_ADMOD);
                    admod.stopLoading();

                }
            }

        });
        mSESceneManager.getWorkSpace().addView(mADViewIntegrated);
    }

    private void initADMod() {
        AdSize adSize;
        DisplayMetrics metric = new DisplayMetrics();
        mSESceneManager.getGLActivity().getWindowManager().getDefaultDisplay().getMetrics(metric);
        if (metric.widthPixels >= 1500) {
            adSize = AdSize.IAB_BANNER;
        } else {
            adSize = AdSize.BANNER;
        }
        com.google.ads.AdView admod = new com.google.ads.AdView(mSESceneManager.getGLActivity(), adSize,
                AD_UNIT);
        RelativeLayout.LayoutParams myLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        admod.setLayoutParams(myLayoutParams);
        if (HomeUtils.DEBUG) {
            Log.d(HomeUtils.TAG, "init Admob");
        }
        mADViewIntegrated.addView(admod, TYPE_ADMOD);
        admod.setAdListener(new com.google.ads.AdListener() {

            public void onDismissScreen(Ad arg0) {
                StaticUtil.onEvent(mContext, "ADMOB_DISMISS_AD", arg0.toString());
            }

            public void onFailedToReceiveAd(Ad arg0, ErrorCode arg1) {
                if (HomeUtils.DEBUG) {
                    Log.d(HomeUtils.TAG, "onFailedToReceiveAdmob");
                }
                StaticUtil.onEvent(mContext, "ADMOB_FAILED_TO_RECEIVE_AD", arg0.toString() + arg1.toString());
                onAdRequestResult(false, arg1.toString());
                if (mADViewIntegrated != null)
                    mADViewIntegrated.onFailedToReceiveAd(TYPE_ADMOD, arg1.toString());

            }

            public void onLeaveApplication(Ad arg0) {
                StaticUtil.onEvent(mContext, "ADMOB_LEAVE_APP", arg0.toString());
            }

            public void onPresentScreen(Ad arg0) {
                StaticUtil.onEvent(mContext, "ADMOB_PRESENT_AD", arg0.toString());
            }

            public void onReceiveAd(Ad arg0) {
                if (HomeUtils.DEBUG) {
                    Log.d(HomeUtils.TAG, "onReceiveAdmob");
                }
                StaticUtil.onEvent(mContext, "ADMOB_RECEIVED_AD", arg0.toString());
                onAdRequestResult(true, null);
                if (mADViewIntegrated != null)
                    mADViewIntegrated.onReceiveAd(TYPE_ADMOD);

            }

        });
    }

    private void deleteAD() {
        if (mADViewIntegrated != null) {
            if (HomeUtils.DEBUG) {
                Log.d(HomeUtils.TAG, "delete Admob");
            }
            StaticUtil.onEvent(mContext, "ADMOB_DESTROY_AD", "" + System.currentTimeMillis());
            stopAD();
            com.google.ads.AdView admod = (AdView) mADViewIntegrated.getChildAt(TYPE_ADMOD);
            mADViewIntegrated.removeView(admod);
            admod.removeAllViews();
            admod.destroy();
            admod.setAdListener(null);
            mSESceneManager.getWorkSpace().removeView(mADViewIntegrated);
            mADViewIntegrated = null;
            mHadLoadedADMod = false;
        }
    }

    public void loadAD() {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.loadAD(TYPE_ADMOD);
        }
    }

    private void stopAD() {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.stopLoadAD(TYPE_ADMOD);
        }
    }

    public View getView() {
        if (mADViewIntegrated == null || mCurShowType == TYPE_NULL) {
            return null;
        }
        return mADViewIntegrated.getChildAt(TYPE_ADMOD);
    }

    public boolean hasLoadedADMod() {
        return mHadLoadedADMod;
    }

    public void postCheckLoadedAd() {
        if (!mHadLoadedADMod) {
            if (null == mADViewIntegrated) {
                initAds();
            }
            resetTryingState();
            enqueueAdRequest();
        }
    }

    public void startToFly() {
        postCheckLoadedAd();
    }

    // AD request policy:
    // 1. serializing request session, no more than 1 request session,
    // synchronize by lock object
    // 2. actual request Runnable perform or postpone the task depend on
    // available networking.
    // 3. triggers of request task
    // a) onResume, perform with interval delay if 1+ success and last failed,
    // otherwise immediately
    // b) after the loaded ad was resume by flying object, e.g., airship flying
    // finish, similar a)
    // c) get failed result then retry requesting session
    private static final long MIN_REQUEST_INTERVAL = 30000; // 30 seconds
    private final Object mRequestLock = new Object();
    private boolean mIsRequesting = false;

    private boolean isNetworkingAvailable() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeInfo = cm.getActiveNetworkInfo();
        if (null != activeInfo) {
            return true;
        }

        return false;
    }

    private void enqueueAdRequest() {
        final long interval = (mHadLoadedADMod && mTryCount > 0) ? MIN_REQUEST_INTERVAL : 0;
        enqueueAdRequest(interval);
    }

    private Runnable mAdRequestTask = new Runnable() {
        @Override
        public void run() {
            if (null != mADViewIntegrated) {
                final AdView adView = (AdView) mADViewIntegrated.getChildAt(TYPE_ADMOD);
                synchronized (mRequestLock) {
                    if (isNetworkingAvailable()) {
                        Log.d(TAG, "enqueueAdRequest, emit ad request session.");
                        mIsRequesting = true;
                        StaticUtil.onEvent(mContext, "ADMOB_REQUEST_AD", "" + System.currentTimeMillis());
                        adView.loadAd(new AdRequest());
                    } else {
                        enqueueAdRequest(MIN_REQUEST_INTERVAL);
                    }
                }
            }
        }
    };

    private void enqueueAdRequest(long interval) {
        Log.v(TAG, "enqueueAdRequest, interval = " + interval + ", mTryCount =" + mTryCount);

        synchronized (mRequestLock) {
            if (mIsRequesting) {
                Log.i(TAG, "enqueueAdRequest, skip with existing request session.");
                return;
            }

            mSESceneManager.getHandler().removeCallbacks(mAdRequestTask);
            mSESceneManager.getHandler().postDelayed(mAdRequestTask, interval);
        }
    }

    // Retry policy:
    // 1. retry if and only if a request of AD failed.
    // 2. reset the count of retrying if
    // a) successfully receive AD;
    // b) onResume from background;
    // c) the trying count exceed the max loop count
    // 3. the interval of retrying request count in the count of retrying
    private static int RETRY_LOOP_COUNT = 5;
    private int mTryCount = 0;
    private StringBuilder mTryReason = new StringBuilder();

    private void resetTryingState() {
        mTryReason.setLength(0);
        mTryCount = 0;
    }

    private void retryToLoadAd(String reason) {
        enqueueAdRequest(MIN_REQUEST_INTERVAL * (mTryCount + 1));
        mTryReason.append(mTryCount).append(reason);

        if (mTryCount > RETRY_LOOP_COUNT) {
            Log.i(TAG, "retryToLoadAd, reasons = " + mTryReason.toString());
            resetTryingState();
        }
    }

    private void onAdRequestResult(boolean result, String reason) {
        synchronized (mRequestLock) {
            mIsRequesting = false;
            stopAD();

            if (result) {
                resetTryingState();
            } else {
                mTryCount++;
                retryToLoadAd(reason);
            }
        }
    }

    private ModelInfo selectFlyer() {
        ModelInfo modelInfo = null;
        // To find type "Airship" model from the current scene
        mFlyers = SESceneManager.getInstance().getModelManager().findModelInfoByType("Airship");
        // Cycle select sky objects
        if (mFlyers.size() > 0) {
            if (mCurrentFlyerIndex >= mFlyers.size()) {
                mCurrentFlyerIndex = 0;
            }
            modelInfo = mFlyers.get(mCurrentFlyerIndex);
            mCurrentFlyerIndex++;
        }
        return modelInfo;
    }

    /**
     * will call back by flyer while its animation finished
     */
    public void notifyFinish() {
        isFly = false;
        if (mCurrentFlyer != null) {
            mCurrentFlyer.getParent().removeChild(mCurrentFlyer, true);
            SESceneManager.getInstance().getModelManager().unRegister(mCurrentFlyer);
        }
    }

    @Override
    public void onCameraChanged() {
        if (SettingsActivity.isAdDismissed(mContext)) {
            if (isFly && mCurrentFlyer != null) {
                mCurrentFlyer.stopFlyDelayed(0);
            }
        } else if ((mScene.getCamera().isSkySight()) && !isFly) {
            isFly = true;
            final ModelInfo modelInfo = selectFlyer();
            if (modelInfo != null) {
                ObjectInfo objInfo = new ObjectInfo();
                objInfo.setModelInfo(modelInfo);
                objInfo.mSceneName = mScene.mSceneName;
                mCurrentFlyer = (Flyer) HomeUtils.getObjectByClassName(mScene, objInfo);
                mScene.getContentObject().addChild(mCurrentFlyer, false);
                if(mCurrentFlyer != null) {
                	mCurrentFlyer.load(mScene.getContentObject(), new Runnable() {
                		@Override
                		public void run() {
                			mCurrentFlyer.fly();
                		}
                	});
                }
            }
        } else if (isFly && mScene.getCamera().getSightValue() < 0.25f && mCurrentFlyer != null) {
            // stop and destroy flyer after 2s if move camera sight to wall
            mCurrentFlyer.stopFlyDelayed(2000);
        } else if (isFly && mScene.getCamera().getSightValue() >= 0.25f && mCurrentFlyer != null) {
            // cancel stop and destroy flyer if try to move camera sight to sky
            mCurrentFlyer.cancelStopFly();
        }
    }

    public void requestCatchImage(long delay) {
        if (!mIsPremium && mADViewIntegrated != null) {
            mADViewIntegrated.requestCatchImage(delay);
        }
    }

    public void stopCatchImage() {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.stopCatchImage();
        }
    }

    public void doClick() {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.doClick();
        }
    }

    public void setImageKey(String imageKey) {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.setImageKey(imageKey);
        }
    }

    public void setImageSize(int w, int h) {
        if (mADViewIntegrated != null) {
            mADViewIntegrated.setImageSize(w, h);
        }
    }


//    private static final String licenseName = "com.borqs.qiupu";
//    private static final String licenseSignature = ""; // todo: public signature
    private void checkAdPolicy() {
        // 1. check license apk existing
        // 2. check signature of the license apk
    }

    public static boolean isRemoveAD() {
        return mIsPremium;
    }


    // Does the user have the premium upgrade?
    private static boolean mIsPremium = false;

    // (arbitrary) request code for the purchase flow
    private static final int RC_PURCHASE_REQUEST = 11001;
    private static final int RC_CHECK_REQUEST = 11002;

    // The helper object
    IabHelper mHelper;
    public void initIab() {
        try {
            // Create the helper, passing it our context and the public key to verify signatures with
            Log.d(TAG, "Creating IAB helper.");
            mHelper = new IabHelper(mSESceneManager.getGLActivity(), getPublicKey());

            // enable debug logging (for a production application, you should set this to false).
            mHelper.enableDebugLogging(true);
            // Start setup. This is asynchronous and the specified listener
            // will be called once setup completes.
            startSetupIab();
        } catch (Exception e) {
            showIabException("init IAB", e);
        }
    }

    private void startSetupIab() {
        try {
            Log.d(TAG, "Starting setup.");
            if (null == mHelper || mIabSetupDone) {
                Log.d(TAG, "startSetupIab, skip without help instance or setup already.");
            }
            mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                public void onIabSetupFinished(IabResult result) {
                    Log.d(TAG, "Setup finished.");

                    if (!result.isSuccess()) {
                        mIabSetupDone = false;
                        // Oh noes, there was a problem.
                        complain("Problem setting up in-app billing: " + result, false);
                        return;
                    }

                    mIabSetupDone = true;

                    // Hooray, IAB is fully set up. Now, let's get an inventory of stuff we own.
                    Log.d(TAG, "Setup successful. Querying inventory.");
                    mHelper.queryInventoryAsync(mGotInventoryListener);
                }
            });
        } catch (Exception e) {
            showIabException("startSetup Iab", e);
        }
    }

    private void showIabException(String message, Exception e) {
        final String errorMsg = message + " exception = " +
                (null == e ? "null" : e.getMessage());
        complain(errorMsg, true);
//        HomeUtils.reportError(errorMsg);
//        Log.e(TAG, errorMsg);
    }

    private String getPublicKey() {
        return mContext.getString(R.string.iab_google_public);
    }

    void complain(String message, boolean alert) {
        Log.e(TAG, "**** IAB Error: " + message);
        HomeUtils.reportError(TAG + "**** IAB Error: " + message);
        if (alert) {
            alert("Warning: " + message);
        }
    }

    void alert(final String message) {
        mSESceneManager.runInUIThread(new Runnable() {
            @Override
            public void run() {
//                AlertDialog.Builder bld = new AlertDialog.Builder(mSESceneManager.getGLActivity());
//                bld.setMessage(message);
//                bld.setNeutralButton("OK", null);
                Log.d(TAG, "Showing alert dialog: " + message);
//                bld.create().show();
                Toast.makeText(mSESceneManager.getGLActivity(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Listener that's called when we finish querying the items and subscriptions we own
    IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        public void onQueryInventoryFinished(IabResult result, Inventory inventory, List<Purchase> allPurchases) {
            Log.d(TAG, "Query inventory finished, size = " + (null == allPurchases ? 0 : allPurchases.size()));
            if (result.isFailure()) {
                complain("Failed to query inventory: " + result, false);
                onAdPurchaseChecked();
                return;
            }

            Log.d(TAG, "Query inventory was successful.");

            /*
             * Check for items we own. Notice that for each purchase, we check
             * the developer payload to see if it's correct! See
             * verifyDeveloperPayload().
             */

            // Do we have the premium upgrade?
            Purchase premiumPurchase = inventory.getPurchase(getPremiumId());
            mIsPremium = (premiumPurchase != null && verifyDeveloperPayload(premiumPurchase));
            Log.d(TAG, "User is " + (mIsPremium ? "PREMIUM" : "NOT PREMIUM"));

            if (!mIsPremium) {
                onAdPurchaseChecked();
            }

            // Do we have the infinite gas plan?
//            Purchase infiniteGasPurchase = inventory.getPurchase(getInfiniteGasId());
//            mSubscribedToInfiniteGas = (infiniteGasPurchase != null &&
//                    verifyDeveloperPayload(infiniteGasPurchase));
//            Log.d(TAG, "User " + (mSubscribedToInfiniteGas ? "HAS" : "DOES NOT HAVE")
//                    + " infinite gas subscription.");
//            if (mSubscribedToInfiniteGas) mTank = TANK_MAX;

            // Check for gas delivery -- if we own gas, we should fill up the tank immediately
//            Purchase gasPurchase = inventory.getPurchase(getGasId());
//            if (gasPurchase != null && verifyDeveloperPayload(gasPurchase)) {
//                Log.d(TAG, "We have gas. Consuming it.");
//                mHelper.consumeAsync(inventory.getPurchase(getGasId()), mConsumeFinishedListener);
//                return;
//            }

            updateUi();
            setWaitScreen(false);
            Log.d(TAG, "Initial inventory query finished; enabling main UI.");
        }
    };

    private void setWaitScreen(boolean waiting) {
    }

    private void updateUi() {
        if (mIsPremium && null != mSESceneManager) {
            mSESceneManager.runInUIThread(new Runnable() {
                @Override
                public void run() {
                    stopCatchImage();
                    deleteAD();
                }
            });
        }
    }

    private String getPremiumId() {
        return SKU_AD_REMOVAL;
    }

    /** Verifies the developer payload of a purchase. */
    boolean verifyDeveloperPayload(Purchase p) {
        String payload = p.getDeveloperPayload();

        /*
         * TODO: verify that the developer payload of the purchase is correct. It will be
         * the same one that you sent when initiating the purchase.
         *
         * WARNING: Locally generating a random string when starting a purchase and
         * verifying it here might seem like a good approach, but this will fail in the
         * case where the user purchases an item on one device and then uses your app on
         * a different device, because on the other device you will not have access to the
         * random string you originally generated.
         *
         * So a good developer payload has these characteristics:
         *
         * 1. If two different users purchase an item, the payload is different between them,
         *    so that one user's purchase can't be replayed to another user.
         *
         * 2. The payload must be such that you can verify it even when the app wasn't the
         *    one who initiated the purchase flow (so that items purchased by the user on
         *    one device work on other devices owned by the user).
         *
         * Using your own server to store and verify developer payloads across app
         * installations is recommended.
         */

        return true;
    }

    private static boolean mIabSetupDone = false;
    private boolean checkDoneSetup (String message) {
        if (!mIabSetupDone) {
            final String errorMsg = "IAB-error, " + message + ", Google Play service was not startup.";
            complain(errorMsg, true);
            startSetupIab();
            return false;
        }

        return true;
    }

    // User clicked the "Upgrade to Premium" button.
    public boolean onUpgradeAppIntent() {
        try {
            if (!checkDoneSetup("Purchase AD")) {
                return false;
            }

            Log.d(TAG, "Upgrade button clicked; launching purchase flow for upgrade.");
            setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
            String payload = "";

            mHelper.launchPurchaseFlow(mSESceneManager.getGLActivity(), getPremiumId(), RC_PURCHASE_REQUEST,
                    mPurchaseFinishedListener, payload);
        } catch (Exception e) {
            showIabException("purchase AD", e);
        }
        return true;
    }

    public boolean onAdPurchaseChecked() {
        try {
            if (!checkDoneSetup("check AD")) {
                return false;
            }

            Log.d(TAG, "onAdPurchaseChecked; check buy item flow for upgrade.");
//            setWaitScreen(true);

        /* TODO: for security, generate your payload here for verification. See the comments on
         *        verifyDeveloperPayload() for more info. Since this is a SAMPLE, we just use
         *        an empty string, but on a production app you should carefully generate this. */
            String payload = "";

            IabHelper.OnIabPurchaseFinishedListener listener = new IabHelper.OnIabPurchaseFinishedListener() {
                public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
                    Log.d(TAG, "onAdPurchaseChecked; result " + result);
                    final int response = result.getResponse();
                    if (response == IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED) {
                        mIsPremium = false;
                    } else if (response == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
                        mIsPremium = true;
                    }
                    Log.d(TAG, "onAdPurchaseChecked; response = " + response
                            + ", mIsPremium = " + mIsPremium);
                }
            };
            mHelper.checkBuyIntentFlow(getPremiumId(), RC_CHECK_REQUEST,
                    listener, payload);
        } catch (Exception e) {
            showIabException("check AD", e);
        }
        return true;
    }

    // Callback for when a purchase is finished
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            Log.d(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
            if (result.isFailure()) {
                complain("Error purchasing: " + result, false);
                StaticUtil.onEvent(mContext, "AD_REMOVAL_PAY_FAIL", "" + System.currentTimeMillis());
                setWaitScreen(false);
                return;
            }

            if (!verifyDeveloperPayload(purchase)) {
                complain("Error purchasing. Authenticity verification failed.", false);
                StaticUtil.onEvent(mContext, "AD_REMOVAL_PAY_FAIL", "" + System.currentTimeMillis());
                setWaitScreen(false);
                return;
            }

            Log.d(TAG, "Purchase successful.");

            /*if (purchase.getSku().equals(getGasId())) {
                // bought 1/4 tank of gas. So consume it.
                Log.d(TAG, "Purchase is gas. Starting gas consumption.");
                mHelper.consumeAsync(purchase, mConsumeFinishedListener);
            }
            else */if (purchase.getSku().equals(getPremiumId())) {
                // bought the premium upgrade!
                StaticUtil.onEvent(mContext, "AD_REMOVAL_PAY", "" + System.currentTimeMillis());
                Log.d(TAG, "Purchase is premium upgrade. Congratulating user.");
                alert("Thank you for upgrading to premium!");
                mIsPremium = true;
                updateUi();
                setWaitScreen(false);
            }
//            else if (purchase.getSku().equals(getInfiniteGasId())) {
//                // bought the infinite gas subscription
//                Log.d(TAG, "Infinite gas subscription purchased.");
//                alert("Thank you for subscribing to infinite gas!");
//                mSubscribedToInfiniteGas = true;
//                mTank = TANK_MAX;
//                updateUi();
//                setWaitScreen(false);
//            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        try {
            // detect already existing response code from google play service.
            if (null != data && requestCode == RC_PURCHASE_REQUEST) {
                Object object = data.getExtras().get(IabHelper.RESPONSE_CODE);
                long responseCode = 0;
                if (object instanceof Integer) {
                    responseCode = ((Integer)object).intValue();
                } else if (object instanceof Long) {
                    responseCode = ((Long)object).longValue();
                }
                if (responseCode == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
//                    String purchaseData = data.getStringExtra(IabHelper.RESPONSE_INAPP_PURCHASE_DATA);
//                    JSONObject o = new JSONObject(purchaseData);
//                    if (SKU_AD_REMOVAL.equals(o.optString("productId"))) {
                        mIsPremium = true;
                    updateUi();
                    setWaitScreen(false);
//                    }
                }
//            } else if (requestCode == RC_CHECK_REQUEST) {
//                // todo: check the AD purchase item
//                if (null != data) {
//                    Object object = data.getExtras().get(IabHelper.RESPONSE_CODE);
//                    long responseCode = 0;
//                    if (object instanceof Integer) {
//                        responseCode = ((Integer)object).intValue();
//                    } else if (object instanceof Long) {
//                        responseCode = ((Long)object).longValue();
//                    }
//                    if (responseCode == IabHelper.BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED) {
//                        mIsPremium = true;
//                        updateUi();
//                        setWaitScreen(false);
//                    } else if (responseCode == IabHelper.BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED) {
//                        mIsPremium = false;
//                        updateUi();
//                        setWaitScreen(false);
//                    }
//                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Pass on the activity result to the helper for handling
        if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
//            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
    }

    public void dismissFlyer(boolean dismiss) {
        if (dismiss) {
            // remove flyers
            if (null != mCurrentFlyer && isFly) {
                mCurrentFlyer.stopFlyDelayed(0);
                StaticUtil.onEvent(mContext, "AD_REMOVAL_DISMISS", "" + System.currentTimeMillis());
            }
        } else {
            // check and add flyers
        }
    }
}
