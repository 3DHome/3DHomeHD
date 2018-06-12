package com.borqs.framework3d.home3d;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.util.Xml;

import com.borqs.se.engine.SEScene;
import com.borqs.se.engine.SEVector.SERotate;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.home3d.XmlUtils;
import com.borqs.se.home3d.ModelInfo;
import com.borqs.se.home3d.SettingsActivity;
import com.borqs.se.engine.SESceneManager;

public class SEMountPointManager {
	private static final String TAG = "SEMountPointManager";
	private SEScene mScene;
	private static class MountPointGroupData {
		int containerType;
		String groupName;
        String containerName;
	}
	//key is the geometry object name of the mount points owner
	//it is defined in models_config.xml
	private HashMap<String, ArrayList<SEMountPointChain>> mAllMountPoints = new HashMap<String, ArrayList<SEMountPointChain>>();	 
	private HashMap<String, ArrayList<MountPointGroupData>> mAllMountPointGroups = new HashMap<String, ArrayList<MountPointGroupData>>();
	private HashMap<String, SEDockAnimationDefine> mAllDockAnimation = new HashMap<String, SEDockAnimationDefine>();
	public SEMountPointManager(SEScene scene)
	{
		mScene = scene;
	}
	private InputStream getAssetInputStream(String filePath)
	{
		InputStream is = null;
		try
		{
			is = SESceneManager.getInstance().getContext().getAssets().open(filePath);
		}
		catch(IOException e)
		{}
		return is;
	}
	private boolean isXmlTagEqual(String tag, String v)
	{
	    return tag.equalsIgnoreCase(v);
	}
	private String getXmlAttributeValue(XmlPullParser parser, String name)
	{
		return parser.getAttributeValue(null, name);
	}
	private boolean stringArrayContain(String[] arrays, String name)
	{
		for(int i = 0 ; i < arrays.length ; i++)
		{
			if(arrays[i].equals(name))
				return true;
		}
		return false;
	}
    // objectName is the object name
    // parser is a new XmlPullParser
    // is is the stream for xml file
    // mountPointType is "matrix" or others
	public ArrayList<SEMountPointChain> parseMountPointXml(String objectName, XmlPullParser parser, InputStream is, String mountPointType)
	{
		ArrayList<SEMountPointChain> mountPointChainList = new ArrayList<SEMountPointChain>();
      	try
       	{
		    parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            SEMountPointChain currentMountPointChain = null;
		    while(eventType != XmlPullParser.END_DOCUMENT)
		    {
		        String tag = parser.getName();
		        switch(eventType)
		        {
		            case XmlPullParser.START_TAG:
		            {
                        if(tag.equalsIgnoreCase("mountPoints")) {
                            String containerTypeStr = parser.getAttributeValue(null, "containerType");
                            String containerName = parser.getAttributeValue(null, "containerName");
                            String groupName = parser.getAttributeValue(null, "groupName");
                            Log.i(TAG, "containerName = " + containerName + ", containerType = " + containerTypeStr + ", groupName = " + groupName);
                            int containerType = Integer.parseInt(containerTypeStr);
                            SEMountPointChain mpc = null;
                            if(containerType == 1)
                            {
                                ModelInfo objectModelInfo = mScene.mSceneInfo.findModelInfo(objectName);
                                if(objectModelInfo != null &&
                                    stringArrayContain(objectModelInfo.mChildNames, groupName)) {
                                    mpc = new SEMountPointChain(groupName, SEMountPointChain.DEFAULT_VESSEL_NAME, containerName);
                                    currentMountPointChain = mpc;
                                } else {
                                    Log.i(TAG, "groupname error");
                                    assert(false);
                                }
                            } else {
                                mpc = new SEMountPointChain(null, groupName, containerName);
                                currentMountPointChain = mpc;
                            }
                            ArrayList<MountPointGroupData> mpGroupDataList = mAllMountPointGroups.get(objectName);
                            if(mpGroupDataList == null) {
                                 mpGroupDataList = new ArrayList<MountPointGroupData>();
                                 mAllMountPointGroups.put(objectName, mpGroupDataList);
                            }
                            //mAllMountPointGroups.put(objectName, groupName);
                            MountPointGroupData mpGroupData = new MountPointGroupData();
                            mpGroupData.containerType = containerType;
                            mpGroupData.groupName = groupName;
                            mpGroupData.containerName = containerName;
                            mpGroupDataList.add(mpGroupData);
                            if(mountPointType != null && mountPointType.equals("matrix")) {
                                currentMountPointChain.setRowColType(SEMountPointChain.ROW_COL_MATRIX);
                            } else {
                                currentMountPointChain.setRowColType(SEMountPointChain.ROW_COL_FREE);
                            }
                        } else if(tag.equalsIgnoreCase("mountPointData")) {
                            String name = parser.getAttributeValue(null, "mountPointName");
                            String tx = parser.getAttributeValue(null, "translatex");
                            String ty = parser.getAttributeValue(null, "translatey");
                            String tz = parser.getAttributeValue(null, "translatez");
                            String rx = parser.getAttributeValue(null, "rotatex");
                            String ry = parser.getAttributeValue(null, "rotatey");
                            String rz = parser.getAttributeValue(null, "rotatez");
                            String ra = parser.getAttributeValue(null, "rotateAngle");
                            String sx = parser.getAttributeValue(null, "scalex");
                            String sy = parser.getAttributeValue(null, "scaley");
                            String sz = parser.getAttributeValue(null, "scalez");
                            Log.i(TAG, "mount point name = " + name + ", tx = " + tx + ", ty = " + ty + ", tz = " + tz
                                    + ", rx = " + rx + ", ry = " + ry + ", rz = " + rz + ", ra = " + ra + ", sx = " + sx +
                                    ", sy = " + sy + ", sz = " + sz);

                            SEVector3f translate = new SEVector3f(tx, ty, tz);
                            SEVector3f scale = new SEVector3f(sx, sy, sz);
                            SERotate rotate = new SERotate(ra, rx, ry, rz);
                            SEMountPointData mpd = new SEMountPointData(name, translate, scale ,rotate);
                            currentMountPointChain.addMountPointData(mpd);
                        }
		            }
		            break;
		            case XmlPullParser.END_TAG:
		            {
		               	if(tag.equalsIgnoreCase("mountPoints")) {
		            		    mountPointChainList.add(currentMountPointChain);
		            		    currentMountPointChain.createMountPointWithSequence(mScene);
		            		    currentMountPointChain.createMountPointIndexNeighbor();
		            		    currentMountPointChain.createMountPointBoundary();
		            	    }
		            }
		            break;
		        }
		        eventType = parser.next();
		    }
       	} catch (IOException e) {
	    	    
	    } catch(XmlPullParserException e) {
	    	
	    }
      	return mountPointChainList;
	}
	private InputStream getInputStreamByPackagePath(String packageName, String path)
	{
		InputStream is = null;
		if(packageName.equals("assets")) {
		    is = getAssetInputStream(path);
		} else {
			Log.i(TAG, "can not support package : | " + packageName + " , path: " + path + "| mount point config");
		}
		return is;
	}
	private void createModelInfoToDB(String packageName, String base, String objectName)
	{
		ModelInfo dbModelInfo = mScene.mSceneInfo.findModelInfo(objectName);
		if(dbModelInfo != null)
			return;
		InputStream is = getInputStreamByPackagePath(packageName, base + "/" + objectName + "/models_config.xml");
        XmlPullParser parser2 = Xml.newPullParser();
        try {
            parser2.setInput(is, "utf-8");
            XmlUtils.beginDocument(parser2, "config");
            ModelInfo config = ModelInfo.CreateFromXml(parser2);
            config.saveToDB();
        }
        catch(XmlPullParserException e)
        {}
        catch(IOException e)
        {}
	}
	private void parseMountPointDefineXml(XmlPullParser parser, InputStream is)
	{
		mAllMountPoints.clear();
      	try
       	{
		    parser.setInput(is, "utf-8");
            int eventType = parser.getEventType();
            String currentObjectName = null;
		    while(eventType != XmlPullParser.END_DOCUMENT)
		    {
		        String tag = parser.getName();
		        switch(eventType)
		        {
		            case XmlPullParser.START_TAG:
		            {
                        if(isXmlTagEqual(tag, "MountPointConfig")) {
                            String objectName = getXmlAttributeValue(parser, "objectname");
                            String packageName = getXmlAttributeValue(parser, "packagename");
                            String path = getXmlAttributeValue(parser, "path");
                            String type = getXmlAttributeValue(parser, "type");
                            currentObjectName = objectName;
                            InputStream istreamMountPoint = getInputStreamByPackagePath(packageName, path);
                            if(istreamMountPoint != null)
                            {
                                XmlPullParser parserMountPoint = Xml.newPullParser();
                                //createModelInfoToDB(packageName, "base" , objectName);
                                ArrayList<SEMountPointChain> mountPointChain = parseMountPointXml(objectName, parserMountPoint, istreamMountPoint, type);
                                mAllMountPoints.put(objectName, mountPointChain);
                            }
                        } else if(isXmlTagEqual(tag, "Animation")) {
                            String animType = getXmlAttributeValue(parser, "type");
                            String animTrace = getXmlAttributeValue(parser, "trace");
                            String animCountStr = getXmlAttributeValue(parser, "framecount");
                            int count = Integer.parseInt(animCountStr);
                            SEDockAnimationDefine define = new SEDockAnimationDefine(currentObjectName, count, animType, animTrace);
                            mAllDockAnimation.put(currentObjectName, define);
                        }
		            }
		            break;
		            case XmlPullParser.END_TAG:
		            {
		            	    
		            }
		            break;
		        }
		        eventType = parser.next();
		    }
       	}
      	catch (IOException e)
	    {
	    	    
	    }
	    catch(XmlPullParserException e)
	    {
	    	
	    }
	}
	public SEDockAnimationDefine getDockAnimationDefine(String objectName) {
	    return mAllDockAnimation.get(objectName);
	}
	public String getMountPointGroupName(String objectName)
	{
		//return mAllMountPointGroups.get(objectName);
		ArrayList<MountPointGroupData> mpGroupDataList = mAllMountPointGroups.get(objectName);
        if(mpGroupDataList == null) {
            return null;
        }
		for(int i = 0 ; i < mpGroupDataList.size(); i++) {
			MountPointGroupData mpgd = mpGroupDataList.get(i);
			if(mpgd.containerType == 1) {
				return mpgd.groupName;
			}
		}
		return null;
	}
	public void loadFromXml(String packageName, String root, Context context)
	{
		String mountPointDefineFilePath ;
		if(SettingsActivity.getPreferRotation(context) == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
			mountPointDefineFilePath = root + "/mount_point_config.xml";
		}else {
			mountPointDefineFilePath = root + "/mount_point_config_port.xml";
		}
		
		InputStream is = getInputStreamByPackagePath(packageName, mountPointDefineFilePath);
       	XmlPullParser parser = Xml.newPullParser();
        parseMountPointDefineXml(parser, is);
	}
	//objectName is the object 
	public SEMountPointChain getMountPointChain(String objectName, String vesselName, String vesselGroupName, String containerName) {
		SEMountPointChain mpc = getMountPointChainByVesselBindName(objectName, vesselGroupName, containerName);
		if(mpc != null ) {
	        return mpc;
		}
		mpc = getVesselDefaulMountPointChain(vesselName, vesselGroupName, containerName);
		return mpc;
    }
	//objName is the object name which contain mount point chain. it must be vessel object
	public SEMountPointChain getVesselDefaulMountPointChain(String objName, String vesselBindName, String containerName)
	{
		ArrayList<SEMountPointChain> mpList = mAllMountPoints.get(objName);
		if(mpList == null)
			return null;
		ModelInfo modelInfo = mScene.mSceneInfo.findModelInfo(objName);
		for(int k = 0 ; k < modelInfo.mChildNames.length ; k++)
		{
			String vName = modelInfo.mChildNames[k];
			if(vesselBindName != null && vName.equals(vesselBindName) == false) {
				continue;
			}
			for(int i = 0 ; i < mpList.size() ; i++)
			{
				SEMountPointChain mpc = mpList.get(i);
				if(mpc.getObjectName() != null && mpc.getObjectName().equals(vName) && mpc.getContainerName().equals(containerName))
				{
					assert(mpc.getVesselName().equals(SEMountPointChain.DEFAULT_VESSEL_NAME));
					return mpc;
				}
			}
		}
		return null;
		
	}
	//objName is object name which contain vesselBindName.
	//vesselBindName is the vesselName which will contain the object defined by object name
	public SEMountPointChain getMountPointChainByVesselBindName(String objName, String vesselBindName, String containerName)
	{
		ArrayList<SEMountPointChain> mpList = mAllMountPoints.get(objName);
		if(mpList == null)
			return null;
        if(containerName == null)
            return null;
		for(int i = 0 ; i < mpList.size() ; i++)
		{
			SEMountPointChain mpc = mpList.get(i);
			if(vesselBindName != null && mpc.getVesselName() != null && 
					 mpc.getVesselName().equals(vesselBindName) && mpc.getContainerName().equals(containerName))
			{
				return mpc;
			}
		}
		return null;
	}
}
