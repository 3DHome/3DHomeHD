package com.borqs.framework3d.home3d;

/**
 * Created by b050 on 7/19/13.
 */
import com.borqs.se.engine.SESceneManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import android.util.Xml;
import com.borqs.se.widget3d.ObjectInfo;
public class TypeManager {
    private HashMap<String, VesselType> mVesselTypeDefine = new HashMap<String, VesselType>();
    private HashMap<String, ArrayList<VesselType>> mObjectPlacements = new HashMap<String , ArrayList<VesselType>>();
    private HashMap<String, ArrayList<VesselType>> mPlacementInModelInfo = new HashMap<String, ArrayList<VesselType>>();
    private static TypeManager mInstance;
    //public final static int INVALID_TYPE = 0;
    //public final static int WALL_SHELF_TYPE = 1;
    //public final static int DOCK_TYPE = 2;
    //public final static int WALL_TYPE = 3;
    public class VesselType {
        private final String mTypeName;
        private int mType = -1;
        public VesselType(String s) {
            mTypeName = s;
            if(mTypeName.equals("wall_shelf")) {
                mType = ObjectInfo.SLOT_TYPE_WALL_SHELF;
            } else if(mTypeName.equals("dock")) {
                mType = ObjectInfo.SLOT_TYPE_DESKTOP;
            } else if(mTypeName.equals("wall")) {
                mType = ObjectInfo.SLOT_TYPE_WALL;
            }
        }
        public boolean equals(Object o) {
            VesselType t = (VesselType) o;
            return mTypeName.equals(t.mTypeName);
        }
        public String getVesselTypeName() {
            return mTypeName;
        }
        public int getType() {
            return mType;
        }
    }
    public void addVesselDefine(String vesselName, VesselType type) {
        mVesselTypeDefine.put(vesselName, type);
    }
    public String[] getVesselNames() {
        Set<String> keySet = mVesselTypeDefine.keySet();
        return (String[])keySet.toArray();
    }
    public VesselType[] getVesselTypes() {

        Collection<VesselType> t = mVesselTypeDefine.values();;
        return (VesselType[])t.toArray();
    }
    private void addTypeToList(VesselType t , ArrayList<VesselType> list) {
        for(VesselType obj : list) {
            if(obj.equals(t)) {
                return;
            }
        }
        list.add(t);
    }
    public ArrayList<VesselType> getObjectPlacements(String objectName) {
        if(objectName.startsWith("app")) {
            objectName = "app*";
        } else if(objectName.startsWith("widget")) {
            objectName = "widget*";
        } else if(objectName.endsWith("_sdcard")) {
            int len = objectName.length();
            len -= 7;
            objectName = objectName.substring(0, len);
        } else if(objectName.startsWith("shortcut")) {
            objectName = "shortcut*";
        }
        ArrayList<VesselType> typesInModel = mPlacementInModelInfo.get(objectName);
        if(typesInModel != null) {
            return typesInModel;
        }
        ArrayList<VesselType> types = mObjectPlacements.get(objectName);
        if(types == null) {
            types = new ArrayList<VesselType>();
            types.add(new VesselType("wall"));
        }
        return types;
    }
    public void setObjectPlacementFromModelInfo(String objectName, String vesselTypeName) {
        ArrayList<VesselType> types =  mPlacementInModelInfo.get(objectName);
        if(types != null) {
            addTypeToList(new VesselType(vesselTypeName), types);
        } else {
            types = new ArrayList<VesselType>();
            addTypeToList(new VesselType(vesselTypeName), types);
            mPlacementInModelInfo.put(objectName, types);
        }
    }
    public void setObjectPlacementFromModelInfo(String objectName, ArrayList<String> vesselTypeNames) {
        for(String vesselTypeName : vesselTypeNames) {
            setObjectPlacementFromModelInfo(objectName, vesselTypeName);
        }
    }
    public void setObjectPlacement(String objectName, String vesselTypeName) {
        ArrayList<VesselType> types =  mObjectPlacements.get(objectName);
        if(types != null) {
            addTypeToList(new VesselType(vesselTypeName), types);
        } else {
            types = new ArrayList<VesselType>();
            addTypeToList(new VesselType(vesselTypeName), types);
            mObjectPlacements.put(objectName, types);
        }
    }
    public void createFromAssetXml(String name) {
        InputStream is = null;
        try
        {
            String path = "base" + "/" + name;
            is = SESceneManager.getInstance().getContext().getAssets().open(path);
             XmlPullParser parser = Xml.newPullParser();
            createFromXmlStream(parser, is);
            is.close();
        }
        catch(IOException e)
        {}

    }
    public void createFromXmlStream(XmlPullParser parser, InputStream is) {
        try
        {
            parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            while(eventType != XmlPullParser.END_DOCUMENT)
            {
                String tag = parser.getName();
                switch(eventType)
                {
                    case XmlPullParser.START_TAG:
                    {
                        if(tag.equalsIgnoreCase("vessel")) {
                            String vesselName = parser.getAttributeValue(null, "name");
                            String vesselTypeName = parser.getAttributeValue(null, "type");
                            addVesselDefine(vesselName, new VesselType(vesselTypeName));
                        } else if(tag.equalsIgnoreCase("objectPlacement")) {
                            String objectname = parser.getAttributeValue(null, "name");
                            String placement = parser.getAttributeValue(null, "placement");
                            setObjectPlacement(objectname, placement);
                        }
                    }

                }
                eventType = parser.next();
            }
        } catch (IOException e) {

        } catch(XmlPullParserException e) {

        }

    }
    private TypeManager() {

    }
    //This function is not thread safe. please use it at GL thread
    public static TypeManager getInstance() {
        if(mInstance == null) {
            mInstance = new TypeManager();
        }
        return mInstance;
    }
    public boolean canPlaceOnVessel(String objectName, int vesselType) {
        ArrayList<TypeManager.VesselType> types = getObjectPlacements(objectName);
        if(types == null) {
            return false;
        }
        for(TypeManager.VesselType type : types) {
            if(type.getType() == vesselType) {
                return true;
            }
        }
        return false;
    }

}
