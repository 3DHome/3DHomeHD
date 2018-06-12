package com.borqs.se;

import com.borqs.freehdhome.R;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;
import android.util.Log;

import com.borqs.se.engine.SEScene;
import com.borqs.se.home3d.HomeUtils;
import com.borqs.se.engine.SESceneManager;
import com.borqs.se.widget3d.NormalObject;
import com.borqs.se.widget3d.ObjectInfo;
import com.borqs.se.engine.SEObject;
import com.borqs.se.engine.SEVector.SEVector3f;
import com.borqs.se.engine.SEVector.SERotate;
public class SETest extends Activity{
	private static final String TAG = "SETest";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);

        ActionBar bar = getActionBar();
        bar.setDisplayOptions(ActionBar.DISPLAY_SHOW_TITLE);

        Button button = (Button)findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	    Log.i(TAG, "## click ##");
            	    SEScene currentScene = SESceneManager.getInstance().getCurrentScene();
            	    SEObject rootObj = currentScene.getContentObject();
            	    SEObject yuanzhuo = currentScene.findObject("yuanzhuo", 0);
            	    SEObject fangzhuo = currentScene.findObject("desk_fang", 0);
            	    SEObject deletedObj = null;
            	    SEObject parent = currentScene.findObject("home_root", 0);
            	    String name = null;
            	    if(yuanzhuo != null) {
            	    	    deletedObj = yuanzhuo;
            	    	    name = "desk_fang";
            	    } else {
            	    	    deletedObj = fangzhuo;
            	    	    name = "yuanzhuo";
            	    }
            	    if(deletedObj != null) {
            	        rootObj.removeChild(deletedObj, true);
            	    }
            	    addNewDock(rootObj.getScene(), parent, name);
            }
        });
        /*
        EditText editText = (EditText)findViewById(R.id.commandText);
        final String command = editText.getText().toString();
        Log.i(TAG, "text = " + command);
        EditText contextText = (EditText)findViewById(R.id.contentText);
        final String contentText = contextText.getText().toString();
        Log.i(TAG, "content = " + contentText);
        */
        Button commandButton = (Button)findViewById(R.id.commandOK);
        commandButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editText = (EditText)findViewById(R.id.commandText);
                final String command = editText.getText().toString();
                Log.i(TAG, "text = " + command);
                EditText contextText = (EditText)findViewById(R.id.contentText);
                final String contentText = contextText.getText().toString();
                Log.i(TAG, "content = " + contentText);
            	Log.i(TAG, "command = " + command + ", content = " + contentText);
                SEScene scene = SESceneManager.getInstance().getCurrentScene();
                handleCommand(scene, command, contentText);
            }
        });
        
        
        
    }
    private void handleCommand(SEScene scene, String command, String contentText) {
       	String[] strArray = contentText.split(" ");
    	String name = null, indexStr = null;
    	if(strArray.length == 2) {
    		name = strArray[0];
    		indexStr = strArray[1]; 
    	} else {
    		name = strArray[0];
    		indexStr = "0";
    	}
        if(command.equals("lt")) {
        	SEObject obj = scene.findObject(name, Integer.parseInt(indexStr));
        	if(obj != null) {
	        	float[] translate = new float[3];
	        	obj.getLocalTranslate_JNI(translate);
	        	float[] rotate = new float[4];
	        	obj.getLocalRotate_JNI(rotate);
	        	float[] scale = new float[3];
	        	obj.getLocalScale_JNI(scale);
	        	SEVector3f translateV = new SEVector3f(translate[0], translate[1], translate[2]);
	        	SEVector3f scaleV = new SEVector3f(scale[0], scale[1], scale[2]);
	        	SERotate r = new SERotate(rotate[0], rotate[1], rotate[2], rotate[3]);
	        	Log.i(TAG, "obj : " + name + " : index = " + indexStr + " : t = " + 
	        	      translateV.toString() + " : r = " + r.toString() + ": s = " + scaleV.toString());
	        }
        } else if(command.equals("lb")) {
        	SEObject obj = scene.findObject(name, Integer.parseInt(indexStr));
        	if(obj != null) {
        	    obj.createLocalBoundingVolume();
        	    SEVector3f minPoint = new SEVector3f();
        	    SEVector3f maxPoint = new SEVector3f();
        	    obj.getLocalBoundingVolume(minPoint, maxPoint);
        	    Log.i(TAG, "obj : " + name + ", index" + indexStr + " : minPoint = " + minPoint + " : maxPoit = " + maxPoint);
        	}
        } else if(command.equals("hb")) { 
        	//hide object
        	SEObject obj = scene.findObject(name, Integer.parseInt(indexStr));
        	if(obj != null) {
        		obj.setVisible(false, true);
        	}
        }
    }
    private void addNewDock(SEScene scene, SEObject parent, String name) {
    	    ObjectInfo objectInfo = ObjectInfo.create(name, scene.mSceneName, 
    	    		                                      "com.borqs.se.widget3d.DockNew", "home_root", true, 0, 
    	    		                                      null, null, null, null, null, null);
    	    objectInfo.saveToDB();
        objectInfo.setModelInfo(scene.mSceneInfo.findModelInfo(objectInfo.mName));
        NormalObject object = HomeUtils.getObjectByClassName(scene, objectInfo);
        object.load(parent, null);
    	    
    }
}
