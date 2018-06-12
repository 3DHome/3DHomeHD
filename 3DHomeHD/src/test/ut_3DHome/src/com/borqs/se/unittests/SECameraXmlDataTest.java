package com.borqs.se.unittests;

import com.borqs.framework3d.home3d.HouseSceneInfo;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.home3d.ProviderUtils;
import com.borqs.se.engine.SECameraData;
import com.borqs.se.engine.SESceneInfo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/// test camera data from xml file, and share the test cases with SECameraDataTest, which
/// loads data from db.
public class SECameraXmlDataTest extends SECameraDataTest {
    @Override
    protected void setupData() {
        String[] fileNames = HomeUtils.queryNativeSceneFiles(mContext);
        for (String fileName : fileNames) {
            try {
                InputStream is = HomeUtils.openNativeSceneXmlStream(mContext, fileName);
                XmlPullParser parser = HomeUtils.getNativeSceneXmlParser(is);
                if (null != parser && SESceneInfo.DEFAULT_SCENE_NAME.equalsIgnoreCase(parser.getAttributeValue(null, ProviderUtils.SceneInfoColumns.SCENE_NAME))) {
                    XmlUtils.nextElement(parser);
                    while (true) {
                        SECameraData tmpData = SECameraData.parseFromXml(parser);
                        if (null != tmpData) {
                            if (SECameraData.CAMERA_TYPE_DEFAULT.equals(tmpData.mType)) {
                                mDefault = tmpData;
                            } else if (SECameraData.CAMERA_TYPE_UP.equals(tmpData.mType)) {
                                mUp = tmpData;
                            } else if (SECameraData.CAMERA_TYPE_DOWN.equals(tmpData.mType)) {
                                mDown = tmpData;
                            } else if (SECameraData.CAMERA_TYPE_NEAR.equals(tmpData.mType)) {
                                mNear = tmpData;
                            } else if (SECameraData.CAMERA_TYPE_FAR.equals(tmpData.mType)) {
                                mFar = tmpData;
                            }
                        } else {
                            break;
                        }
                    }

                    mHouseInfo = new HouseSceneInfo();
                    mHouseInfo.parseFromXml(parser);
                }
                is.close();
            } catch (XmlPullParserException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
