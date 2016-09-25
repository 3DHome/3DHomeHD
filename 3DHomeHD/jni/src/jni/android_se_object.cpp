#ifdef NDK
#include <jni.h>
#include <string>
#else
#include <nativehelper/jni.h>
#include <utils/Log.h>
#include <utils/String8.h>
#include <utils/String16.h>
#endif

#ifdef GLES_20
#include <GLES2/gl2.h>
#endif

#include <list>
#include <string>
#include <vector>
#include <algorithm>
#include <limits>
#include <assert.h>
#include <sys/system_properties.h>

#include "SE_DynamicLibType.h"

#include <stdio.h>
#include <assert.h>
#include <dlfcn.h>
#include <GLES2/gl2.h>
#include "SE_Log.h"
#include "SE_Common.h"
#include "SE_Application.h"
#include "SE_ParticleSystemManager.h"
#include "SE_SystemCommandFactory.h"
#include "SE_SystemCommand.h"
#include "SE_InputEvent.h"
#include "SE_Struct.h"
#include "SE_UserCommand.h"
#include "SE_Spatial.h"
#include "SE_Camera.h"
#include "SE_SpatialTravel.h"
#include "SE_SceneManager.h"
#include "SE_ResourceManager.h"
#include "SE_MotionEventController.h"
#include "SE_ID.h"
#include "SE_Geometry.h"
#include "SE_Mesh.h"
#include "SE_GeometryData.h"
#include "SE_SimObjectManager.h"
#include "SE_InputManager.h"
#include "SkBitmap.h"
#include "SE_TextureCoordData.h"
//#include "SE_NewGeometry.h"
#include "SE_CommonNode.h"
#include "SE_DataValueDefine.h"
#include "SE_OctreeNode.h"
#include "SE_ObjectManager.h"
//#include "SE_SpatialManager.h"
#include "SE_Light.h"


/********************add by liusong*************************/
static jclass classObject;

static jfieldID nativeBitmapID = 0;
static jfieldID objectNameID = 0;
static jfieldID objectIndexID = 0;
static jfieldID isNodeID = 0;
static jfieldID objectDataID = 0;
static jfieldID localTransParasID = 0;
static jfieldID userTransParasID = 0;
static jfieldID bvTypeID = 0;
/*static jfieldID imageWID = 0;
static jfieldID imageHID = 0;
*/
static jfieldID layerIndexID = 0;

static jmethodID methodInitObject;
static jmethodID methodGetShaderTypeID;
static jmethodID methodGetRenderTypeID;
static jmethodID methodGetRenderStateID;
static jmethodID methodGetVertexArray;
static jmethodID methodGetTexVertexArray;
static jmethodID methodGetFaceArray;
static jmethodID methodGetNeedBlending;
static jmethodID methodGetNeedDepthTest;
static jmethodID methodGetAlpha;
static jmethodID methodGetRenderID;
static jmethodID methodGetShadowID;
static jmethodID methodGetColor;

static jmethodID methodGetImageKey;
static jmethodID methodGetImageName;
static jmethodID methodGetImageType;

static jmethodID methodGetBitmap;
static jmethodID methodGetParentName;
static jmethodID methodGetParentIndex;
static jmethodID methodGetSceneName;
static jmethodID methodGetTranslate;
static jmethodID methodGetRotate;
static jmethodID methodGetScale;
static jmethodID methodGetObjectType;
static jmethodID methodGetLineWidth;

static jfieldID isNewBitmapPlatform(JNIEnv* env) {
    char buf[PROP_VALUE_MAX];
    __system_property_get("ro.build.version.sdk", buf);
    int versionCode = atoi(buf);
    //return versionCode >= 21;

    jclass bitmapClass = env->FindClass("android/graphics/Bitmap");
    if (versionCode < 21) { //pre-lolipop
        return env->GetFieldID(bitmapClass, "mNativeBitmap", "I");
    } else if (versionCode < 23) { // lolipop 21, 22
        return env->GetFieldID(bitmapClass, "mNativeBitmap", "J");
    } else {
        return env->GetFieldID(bitmapClass, "mNativePtr", "J");
    }
}

SE_Scene* findScene(const char* sceneName);
static bool isEmpty(const char* str)
{
    return !strcmp(str, "");
}
SE_Spatial* findSpatial(JNIEnv* env, jobject obj)
{

    jstring name = (jstring)env->GetObjectField(obj, objectNameID);
    const char* name8 = env->GetStringUTFChars(name, 0);
    int objectIndex = env->GetIntField(obj, objectIndexID);
    SE_Spatial* spatial = SE_Application::getInstance()->getSceneManager()->getMainScene()->findSpatialByName(name8, objectIndex);
    if (!spatial) {
        if(_DEBUG)
            LOGD("BORQS## object %s not founded #######################\n", name8);
    }
    env->ReleaseStringUTFChars(name, name8);
    return spatial;
}

void getObjectData(JNIEnv* env, jobject objData, SE_ObjectCreator* creator)
{

    int imageType = env->CallIntMethod(objData, methodGetImageType);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();
    if (imageType == 2)
    {
        jfloatArray jColorArray = (jfloatArray)env->CallObjectMethod(objData, methodGetColor);
        float* colorArray = env->GetFloatArrayElements(jColorArray, 0);
        SE_Vector3f color = SE_Vector3f(colorArray[0], colorArray[1], colorArray[2]);
        creator->setColor(color);
        env->ReleaseFloatArrayElements(jColorArray, colorArray, 0);
    } else {
        jstring imageKey = (jstring)env->CallObjectMethod(objData, methodGetImageName);
        const char* imageKey8 = env->GetStringUTFChars(imageKey, 0);
        SE_ImageDataID imageDataid =  SE_ImageDataID(imageKey8);
        creator->setImageDataID(imageDataid);
        jfloatArray jTexVertexArray = (jfloatArray)env->CallObjectMethod(objData, methodGetTexVertexArray);
        int texVertexSize = env->GetArrayLength(jTexVertexArray)/2;
        float* texVertexArray = env->GetFloatArrayElements(jTexVertexArray, 0);
        SE_Vector2f* texVertex = new SE_Vector2f[texVertexSize];
        env->ReleaseFloatArrayElements(jTexVertexArray, texVertexArray, 0);
        for (int i=0; i<texVertexSize; i++)
        {
            texVertex[i] = SE_Vector2f(texVertexArray[2*i],texVertexArray[2*i+1]);
        }
      //  resourceManager->insertPathImageData(imageKey8, NULL, false);  
      //  resourceManager->setIdPath(imageKey8, imageKey8, true);
        
        creator->setTextureCoorArray(texVertex, texVertexSize);
        env->ReleaseStringUTFChars(imageKey, imageKey8);      
    }
    int bvType = env->GetIntField(objData, bvTypeID);
    creator->setBvType(bvType);
    jstring shaderType = (jstring)env->CallObjectMethod(objData, methodGetShaderTypeID);
    const char* shaderType8 = env->GetStringUTFChars(shaderType, 0);
    creator->setShaderType(shaderType8);
    jstring renderType = (jstring)env->CallObjectMethod(objData, methodGetRenderTypeID);
    const char* renderType8 = env->GetStringUTFChars(renderType, 0);
    creator->setRenderType(renderType8);
    int renderState = env->CallIntMethod(objData, methodGetRenderStateID);
    creator->setRenderState(renderState);
    float alpha = env->CallFloatMethod(objData, methodGetAlpha);
    creator->setAlpha(alpha);
    jfloatArray jVertexArray = (jfloatArray)env->CallObjectMethod(objData, methodGetVertexArray);
    int vertexSize = env->GetArrayLength(jVertexArray)/3;
    float* vertexArray = env->GetFloatArrayElements(jVertexArray, 0);
    SE_Vector3f* vertex = new SE_Vector3f[vertexSize];
    for (int i=0; i<vertexSize; i++)
    {
        vertex[i] = SE_Vector3f(vertexArray[3*i],vertexArray[3*i+1],  vertexArray[3*i+2]);
    }
    creator->setVertexArray(vertex, vertexSize);
    
    jintArray jFaceArray = (jintArray)env->CallObjectMethod(objData, methodGetFaceArray);
    int facesSize = env->GetArrayLength(jFaceArray)/3;
    int* faceArray = env->GetIntArrayElements(jFaceArray, 0);
    SE_Vector3i* faces = new SE_Vector3i[facesSize];
    for (int i=0; i<facesSize; i++)
    {
        faces[i] = SE_Vector3i(faceArray[3*i],faceArray[3*i+1],  faceArray[3*i+2]);
    }
    int* facet = new int[facesSize];
    for (int i = 0; i< facesSize; i++)
    {
        facet[i] = i;
    }
    creator->setVertexIndexArray(faces, facesSize);
    creator->setFacetIndexArray(facet, facesSize);

    SE_Vector3i* texFaces = new SE_Vector3i[facesSize];
    memcpy(texFaces, faces, sizeof(SE_Vector3i) * facesSize);
    creator->setTextureCoorIndexArray(texFaces, facesSize);
    
    env->ReleaseStringUTFChars(renderType, renderType8);
    env->ReleaseStringUTFChars(shaderType, shaderType8);
    env->ReleaseFloatArrayElements(jVertexArray, vertexArray, 0);
    env->ReleaseIntArrayElements(jFaceArray, faceArray, 0);

    jobject transParas = env->GetObjectField(objData, localTransParasID);
    jfloatArray localTranslateArray = (jfloatArray)env->CallObjectMethod(transParas, methodGetTranslate);
    float* localTranslate = env->GetFloatArrayElements(localTranslateArray, 0);
    jfloatArray localRotateArray = (jfloatArray)env->CallObjectMethod(transParas, methodGetRotate);
    float* localRotate = env->GetFloatArrayElements(localRotateArray, 0);
    jfloatArray localScaleArray = (jfloatArray)env->CallObjectMethod(transParas, methodGetScale);
    float* localScale = env->GetFloatArrayElements(localScaleArray, 0);
    SE_Vector3f v = SE_Vector3f(localTranslate[0], localTranslate[1], localTranslate[2]);
    SE_Quat q;
    q.set(localRotate[0], SE_Vector3f(localRotate[1], localRotate[2], localRotate[3]));
    SE_Vector3f s = SE_Vector3f(localScale[0], localScale[1], localScale[2]);
    creator->setLocalRotate(q);
    creator->setLocalTranslate(v);
    creator->setLocalScale(s);
    
    jobject userTransParas = env->GetObjectField(objData, userTransParasID);
    jfloatArray userTranslateArray = (jfloatArray)env->CallObjectMethod(userTransParas, methodGetTranslate);
    float* userTranslate = env->GetFloatArrayElements(userTranslateArray, 0);
    jfloatArray userRotateArray = (jfloatArray)env->CallObjectMethod(userTransParas, methodGetRotate);
    float* userRotate = env->GetFloatArrayElements(userRotateArray, 0);
    jfloatArray userScaleArray = (jfloatArray)env->CallObjectMethod(userTransParas, methodGetScale);
    float* userScale = env->GetFloatArrayElements(userScaleArray, 0);
    SE_Vector3f uv = SE_Vector3f(userTranslate[0], userTranslate[1], userTranslate[2]);
    SE_Quat uq;
    uq.set(userRotate[0], SE_Vector3f(userRotate[1], userRotate[2], userRotate[3]));
    SE_Vector3f us = SE_Vector3f(userScale[0], userScale[1], userScale[2]);
    creator->setUserRotate(uq);
    creator->setUserTranslate(uv);
    creator->setUserScale(us);
    int objectType = env->CallIntMethod(objData, methodGetObjectType);
    float lineWidth = env->CallFloatMethod(objData, methodGetLineWidth);
    creator->setObjectType(objectType);
    creator->setLineWidth(lineWidth);
    int layerIndex = env->GetIntField(objData, layerIndexID);
    creator->setLayerIndex(layerIndex);
    env->ReleaseFloatArrayElements(localTranslateArray, localTranslate, 0);
    env->ReleaseFloatArrayElements(localRotateArray, localRotate, 0);
    env->ReleaseFloatArrayElements(localScaleArray, localScale, 0);
    env->ReleaseFloatArrayElements(userTranslateArray, userTranslate, 0);
    env->ReleaseFloatArrayElements(userRotateArray, userRotate, 0);
    env->ReleaseFloatArrayElements(userScaleArray, userScale, 0);

}

static void se_addUserObject(JNIEnv* env, jobject obj)
{
    jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);   
    const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
    if(_DEBUG)
        LOGD("## add user object ### sceneName = %s\n", sceneName8); 
    SE_Scene* scene = findScene(sceneName8);
    if (!scene) {
        if(_DEBUG)
            LOGD("## add user object failed, scene not found### scene name = %s\n", sceneName8);
        env->ReleaseStringUTFChars(sceneName, sceneName8); 
        return;
    }
    jstring name = (jstring)env->GetObjectField(obj, objectNameID);
    const char* name8 = env->GetStringUTFChars(name, 0);
    int objectIndex = env->GetIntField(obj, objectIndexID);
    if(_DEBUG)
        LOGD("## add user object ### name = %s\n", name8); 

    SE_Spatial* hasExist = findSpatial(env, obj);
    if(hasExist) {
        if (_DEBUG)
            LOGD("## add user object ### hasExist "); 
        return;
    }
   
    jstring parentName = (jstring)env->CallObjectMethod(obj, methodGetParentName);
    int parentIndex = env->CallIntMethod(obj, methodGetParentIndex);
    const char* parentName8 = env->GetStringUTFChars(parentName, NULL);
    if (_DEBUG)
        LOGD("## add user object ### parentName = %s\n", parentName8); 
    SE_Spatial* parent = SE_Application::getInstance()->getSceneManager()->findSpatialByName(parentName8, parentIndex);
    if (!parent)
    {
         if (_DEBUG)
             LOGD("## add user object, but parent not found### parent name = %s\n", parentName8);
          parent = new SE_CommonNode(SE_ID::createSpatialID());
          parent->setSpatialName(parentName8);
          parent->setCloneIndex(parentIndex);
          //save spatial name to map
          /*SE_SpatialManager* sm = SE_Application::getInstance()->getSpatialManager();
          sm->set(parent->getSpatialName(),parent);*/
          parent->setIsEntirety(true);
          scene->addSpatial(scene->getRoot(), parent);
    }
    bool isNode = env->GetBooleanField(obj, isNodeID);
    if (isNode)
    {
        if (_DEBUG)
            LOGD("## add user object ### isNode name = %s\n", name8); 
        SE_CommonNode* node = new SE_CommonNode(SE_ID::createSpatialID());
        node->setSpatialName(name8);
        node->setCloneIndex(objectIndex);
        //save spatial name to map
        /*SE_SpatialManager* sm = SE_Application::getInstance()->getSpatialManager();
        sm->set(node->getSpatialName(),node);*/

        scene->addSpatial(parent, node);
        node->setIsEntirety(true);
        return;
    }    

    jobject objData = env->GetObjectField(obj, objectDataID);
    if (!objData) {
        return;
    }

    SE_ObjectCreator* creator = new SE_ObjectCreator();
    creator->setObjectName(name8);

    getObjectData(env, objData, creator);
    SE_Spatial* spatial = creator->create(scene, parent);
    spatial->setSpatialName(name8);
    spatial->setIsEntirety(true);
    //save spatial name to map
    /*SE_SpatialManager* sm = SE_Application::getInstance()->getSpatialManager();
    sm->set(spatial->getSpatialName(),spatial);*/

     if (spatial->getScene()->getSceneManagerType() == OCTREESCENEMANAGER)
    {
        OctreeSceneManager* octreeSceneManager = spatial->getScene()->getOctreeSceneManager();
        OctreeNode* onode =  octreeSceneManager->createSceneNode(spatial->getSpatialName());
        onode->setSpatial(spatial);
        octreeSceneManager->updateOctreeNode(onode);
        //spatial->getScene()->getOctreeSceneManager()->addObject(spatial->getSpatialName());
    }
    delete creator;
    env->ReleaseStringUTFChars(name, name8);
    env->ReleaseStringUTFChars(sceneName, sceneName8); 
}

static jboolean se_isImageExist(JNIEnv* env, jobject clazz, jstring path) {
    const char* imagePath = env->GetStringUTFChars(path, NULL);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();
    SE_ImageData* existdata = resourceManager->getImageDataFromPath(imagePath);
    if(existdata) {
        if (_DEBUG)
            LOGD("BORQS## Image is Exist ### %s\n", imagePath); 
        env->ReleaseStringUTFChars(path, imagePath);
        return true;
    } else {
        if (_DEBUG)
           LOGD("BORQS## Image is not Exist ### %s\n", imagePath); 
        env->ReleaseStringUTFChars(path, imagePath);
        return false;
    }
}

static jint se_loadImageData(JNIEnv* env, jobject clazz, jstring path)
{
    if (_DEBUG)
        LOGD("BORQS## loadImageData ###"); 
    const char* imagePath = env->GetStringUTFChars(path, NULL);
    SE_ImageData* newImgd = SE_ImageCodec::loadAsset(imagePath);
    env->ReleaseStringUTFChars(path, imagePath);
    return (int)newImgd;
}

static jint se_loadImageData_II(JNIEnv* env, jobject clazz,jobject jbitmap)
{
    if (_DEBUG)
        LOGD("BORQS## loadImageData II ###"); 
#ifdef NDK
    SE_ImageData* newImgd = SE_ImageCodec::load(env, jbitmap);
#else
    SkBitmap* bitmap = (SkBitmap*)env->GetIntField(jbitmap, nativeBitmapID);
    SE_ImageData* newImgd = SE_ImageCodec::load(bitmap);
#endif
/*
    SE_ImageCodec::resizeImageData(newImgd);
*/
    return (int)newImgd;
}


static void se_addImageData(JNIEnv* env, jobject clazz, jstring imageKey, jint imageDataObj)
{
    if (imageDataObj == 0) {
        return;
    }
    const char* imageKey8 = env->GetStringUTFChars(imageKey, NULL);
    if (_DEBUG)
        LOGD("BORQS## addImageData ###imageKey = %s\n", imageKey8);

    SE_ImageData* newImgd = (SE_ImageData*)imageDataObj;
    newImgd->setName(imageKey8);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();

    resourceManager->insertPathImageData(imageKey8, newImgd, true);
    env->ReleaseStringUTFChars(imageKey,imageKey8);
}

static jstring se_getImageName(JNIEnv* env, jobject obj) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        SE_SimObject * simObject = spatial->getCurrentAttachedSimObj();
        if (simObject) {
            SE_Mesh* mesh = simObject->getMesh();
            if (mesh) {
                 SE_Texture* texture = mesh->getTexture(0);
                 if (texture) {
                      SE_TextureUnit* texUnit = texture->getTextureUnit(0);
                      if (texUnit) {
                          SE_ImageDataID imgID = texUnit->getImageDataID(0);
                          return env->NewStringUTF(imgID.getStr());
                      }
                 }
            }
        }
    }
    return NULL;
}

static jstring se_getImageKey(JNIEnv* env, jobject obj, jstring name) {
    const char* name8 = env->GetStringUTFChars(name, NULL);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();
    const char* path = resourceManager->getIdPath(name8);
    env->ReleaseStringUTFChars(name,name8);
    return env->NewStringUTF(path);  
}

static void se_removeImageData(JNIEnv* env, jobject clazz, jstring key)
{
    if (_DEBUG)
        LOGD("BORQS## removeImageData ###"); 
#if 0
    const char* imageKey = env->GetStringUTFChars(key, NULL);
    SE_ImageDataID imageDataid(imageKey);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();
    resourceManager->removeImageData(imageDataid);
    env->ReleaseStringUTFChars(key, imageKey);
#endif
}

static void se_applyImage(JNIEnv* env, jobject clazz, jstring imageName, jstring imageKey)
{
    const char* imageName8 = env->GetStringUTFChars(imageName, NULL);
    const char* imageKey8 = env->GetStringUTFChars(imageKey, NULL);
    SE_ResourceManager *resourceManager = SE_Application::getInstance()->getResourceManager();
    const char* oldKey = resourceManager->getIdPath(imageName8);
    if (oldKey && strcmp(oldKey, imageKey8) == 0) {
       env->ReleaseStringUTFChars(imageName,imageName8);
       env->ReleaseStringUTFChars(imageKey,imageKey8);
       return;
    }
    SE_ImageDataID imageDataid =  SE_ImageDataID(imageName8);
    resourceManager->unregisterRes(SE_ResourceManager::IMAGE_RES, &imageDataid);

    resourceManager->insertPathImageData(imageKey8, NULL, false);  
    resourceManager->setIdPath(imageName8, imageKey8, true);
    resourceManager->registerRes(SE_ResourceManager::IMAGE_RES, &imageDataid);
    env->ReleaseStringUTFChars(imageName,imageName8);
    env->ReleaseStringUTFChars(imageKey,imageKey8);
}



static void se_updateVertex(JNIEnv* env, jobject obj, jfloatArray vertex)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        float* newVertex = env->GetFloatArrayElements(vertex, 0);
        SE_Mesh* mesh = spatial->getCurrentAttachedSimObj()->getMesh();
        SE_GeometryData* geometryData = mesh->getGeometryData();
        SE_Vector3f* oldVertex = geometryData->getVertexArray();
        int vertexNum = geometryData->getVertexNum();
        SE_Surface* surface = mesh->getSurface(0);
        for (int i=0; i< vertexNum; i++) {
            oldVertex[i].x = newVertex[3*i];
            oldVertex[i].y = newVertex[3*i+1];
            oldVertex[i].z = newVertex[3*i+2];
        }
        surface->upDateFaceVertex();
        spatial->updateWorldTransform();
        spatial->updateBoundingVolume();
        env->ReleaseFloatArrayElements(vertex, newVertex, 0);
    }
    
}

static void se_updateTexture(JNIEnv* env, jobject obj, jfloatArray texVertex)
{    
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## updateTexture### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* spatial = findSpatial(env, obj);
    float* newTexVertex = env->GetFloatArrayElements(texVertex, 0);
    if (spatial) {
        SE_Mesh* mesh = spatial->getCurrentAttachedSimObj()->getMesh();
        SE_Texture* tex = mesh->getTexture(0);
        SE_Surface* surface = mesh->getSurface(0);
        SE_TextureUnit* texUnit = tex->getTextureUnit(0);
        SE_TextureCoordData* texCoordData = texUnit->getTextureCoordData();
        SE_Vector2f* texVertexArray = texCoordData->getTexVertexArray();
        int num = texCoordData->getTexVertexNum();
        for (int i=0; i< num; i++) {    
            texVertexArray[i].x = newTexVertex[2*i];
            texVertexArray[i].y = newTexVertex[2*i+1];
        }
        surface->upDateFaceTexVertex(0);
    }
    env->ReleaseFloatArrayElements(texVertex, newTexVertex, 0);
}
static void se_translateLocal(JNIEnv* env, jobject obj,jfloatArray translate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## translate local### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ls = env->GetFloatArrayElements(translate, 0);
        SE_Vector3f TransLocation = SE_Vector3f(ls[0],ls[1],ls[2]);
        spatial->setLocalTranslateIncremental(TransLocation);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(translate, ls, 0);
    }   
}

static void se_scaleLocal(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## scale local### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ss = env->GetFloatArrayElements(scale, 0);
        spatial->setLocalScaleIncremental(SE_Vector3f(ss[0],ss[1],ss[2]));
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(scale, ss, 0);
    }   
}

static void se_rotateLocal(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## rotate local### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {

        float* rs = env->GetFloatArrayElements(rotate, 0);
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));
        spatial->setLocalRotateIncremental(newRotate);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(rotate, rs, 0);
    }   
}


static void se_operateObject(JNIEnv* env, jobject obj, jfloatArray translate, jfloatArray rotate, jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## operateObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/

    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ls = env->GetFloatArrayElements(translate, 0);
        float* rs = env->GetFloatArrayElements(rotate, 0);
        float* ss = env->GetFloatArrayElements(scale, 0);
        SE_Vector3f newTranslate = SE_Vector3f(ls[0],ls[1],ls[2]);
        SE_Vector3f newScale = SE_Vector3f(ss[0],ss[1],ss[2]);
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));
        spatial->setUserTranslate(newTranslate);
        spatial->setUserRotate(newRotate);
        spatial->setUserScale(newScale);
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(translate, ls, 0);
        env->ReleaseFloatArrayElements(rotate, rs, 0);
        env->ReleaseFloatArrayElements(scale, ss, 0);
    }   
}

static void se_rotateObject(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## rotateObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {

        float* rs = env->GetFloatArrayElements(rotate, 0);
        //SE_Matrix4f postMatrix;
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));
        spatial->setUserRotateIncremental(newRotate);
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(rotate, rs, 0);
    }   
}

static void se_rotatePoint(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## rotateObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {

        float* rs = env->GetFloatArrayElements(rotate, 0);
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));    
        SE_Matrix3f preMatrix = spatial->getUserRotateMatrix();
        SE_Matrix3f postMatrix = newRotate.toMatrix3f();
        SE_Matrix3f nowMatrix = postMatrix.mul(preMatrix);
        SE_Quat nowRotate = nowMatrix.toQuat();
        spatial->setUserRotate(nowRotate);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(rotate, rs, 0);
    }   
}

static void se_translateObject(JNIEnv* env, jobject obj,jfloatArray translate, jboolean collisionDetect)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## translateObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ls = env->GetFloatArrayElements(translate, 0);
        SE_Vector3f TransLocation = SE_Vector3f(ls[0],ls[1],ls[2]);
        bool flag = true;
        if (collisionDetect)
        {
            SE_Scene* scene = spatial->getScene();
            if (scene) 
            {
                if (scene->moveCollisionDetect(spatial, TransLocation))
                {
                    flag = false;
                }
            }
        }
       
        if (flag)
        {
            spatial->setUserTranslateIncremental(TransLocation);
            spatial->updateWorldTransform();
            //spatial->getParent()->updateBoundingVolume();
        }
        env->ReleaseFloatArrayElements(translate, ls, 0);
    }   
}

static void se_scaleObject(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## scaleObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ss = env->GetFloatArrayElements(scale, 0);
        spatial->setUserScaleIncremental(SE_Vector3f(ss[0],ss[1],ss[2]));
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(scale, ss, 0);
    }   
}

static void se_setRotate(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setRotate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {

        float* rs = env->GetFloatArrayElements(rotate, 0);
        //SE_Matrix4f postMatrix;
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));
        spatial->setUserRotate(newRotate);
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(rotate, rs, 0);
    }   
}

static void se_setTranslate(JNIEnv* env, jobject obj,jfloatArray translate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setTranslate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ls = env->GetFloatArrayElements(translate, 0);
        SE_Vector3f newTranslate = SE_Vector3f(ls[0],ls[1],ls[2]);
        spatial->setUserTranslate(newTranslate);
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(translate, ls, 0);
    }   
}

static void se_setScale(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setScale### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ss = env->GetFloatArrayElements(scale, 0);
        SE_Vector3f newScale = SE_Vector3f(ss[0],ss[1],ss[2]);
        spatial->setUserScale(newScale);
        spatial->updateWorldTransform();
        //spatial->getParent()->updateBoundingVolume();
        env->ReleaseFloatArrayElements(scale, ss, 0);
    }   
}

static void se_setLocalRotate(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## set local rotate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {

        float* rs = env->GetFloatArrayElements(rotate, 0);
        SE_Quat newRotate;
        newRotate.set(rs[0], SE_Vector3f(rs[1], rs[2], rs[3]));
        spatial->setLocalRotate(newRotate);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(rotate, rs, 0);
    }   
}

static void se_setLocalTranslate(JNIEnv* env, jobject obj,jfloatArray translate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## set local translate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ls = env->GetFloatArrayElements(translate, 0);
        SE_Vector3f newTranslate = SE_Vector3f(ls[0],ls[1],ls[2]);
        spatial->setLocalTranslate(newTranslate);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(translate, ls, 0);
    }   
}

static void se_setLocalScale(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## set local scale### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* ss = env->GetFloatArrayElements(scale, 0);
        SE_Vector3f newScale = SE_Vector3f(ss[0],ss[1],ss[2]);
        spatial->setLocalScale(newScale);
        spatial->updateWorldTransform();
        env->ReleaseFloatArrayElements(scale, ss, 0);
    }   
}

//-----------
static void se_getUserRotate(JNIEnv* env, jobject obj,jfloatArray rotate)
{
    /*if(_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getUserRotate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Quat quat = spatial->getUserRotate();
	env->SetFloatArrayRegion(rotate, 0, 4, quat.toRotate().d);
    }   
}

static void se_getUserTranslate(JNIEnv* env, jobject obj,jfloatArray translate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getUserTranslate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector3f ts = spatial->getUserTranslate();
	env->SetFloatArrayRegion(translate, 0, 3, ts.d);
    }   
}

static void se_getUserScale(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getUserScale### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector3f s = spatial->getUserScale();
	env->SetFloatArrayRegion(scale, 0, 3, s.d);
    }   
}

static void se_getLocalScale(JNIEnv* env, jobject obj,jfloatArray scale)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getLocalScale### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector3f s = spatial->getLocalScale();
	env->SetFloatArrayRegion(scale, 0, 3, s.d);
    }   
}

static void se_getLocalRotate(JNIEnv* env, jobject obj, jfloatArray rotate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getLocalRotate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Quat quat = spatial->getLocalRotate();
	env->SetFloatArrayRegion(rotate, 0, 4, quat.toRotate().d);
    }

}
static void se_getLocalTranslate(JNIEnv* env, jobject obj, jfloatArray translate)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getLocalTranslate### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector3f translate3f = spatial->getLocalTranslate();
	env->SetFloatArrayRegion(translate, 0, 3, translate3f.d);
    }

}

static void se_getAbsoluteTranslate(JNIEnv* env, jobject obj, jfloatArray center)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getCenter### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector4f v4(0, 0, 0, 1);
        v4 = spatial->getWorldTransform().map(v4);
 	env->SetFloatArrayRegion(center, 0, 3, v4.d);
    }
}
static void se_toWorldCoordinate(JNIEnv* env, jobject obj, jfloatArray points, jfloatArray outPoints)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if(spatial == NULL)
        return;
    const SE_Matrix4f& worldMatrix = spatial->getWorldTransform();
    float* pointFloatArray = env->GetFloatArrayElements(points, 0);
    float* outPointFloatArray = env->GetFloatArrayElements(outPoints, 0);
    int arraySize = env->GetArrayLength(points);
    SE_Vector4f v;
    if(arraySize == 3)
    {
        v = SE_Vector4f(pointFloatArray[0], pointFloatArray[1], pointFloatArray[2], 0);
    }
    else
    {
        v = SE_Vector4f(pointFloatArray[0], pointFloatArray[1], pointFloatArray[2], pointFloatArray[3]);
    }
    SE_Vector4f outv = worldMatrix.map(v);
    outPointFloatArray[0] = outv[0];
    outPointFloatArray[1] = outv[1];
    outPointFloatArray[2] = outv[2];
    outPointFloatArray[3] = outv[3];
    env->ReleaseFloatArrayElements(points, pointFloatArray, 0);
    env->ReleaseFloatArrayElements(outPoints, outPointFloatArray, 0);
    
}
static void se_clearLocalBoundingVolume(JNIEnv* env, jobject object) {
    SE_Spatial* spatial = findSpatial(env, object);
    if(spatial == NULL)
        return;
    spatial->clearLocalBoundingVolume();
}
static void se_createLocalBoundingVolume(JNIEnv* env, jobject object)
{
    SE_Spatial* spatial = findSpatial(env, object);
    //LOGI("## se_createLocalBoundingVolume spatial = %p ##\n", spatial);
    if(spatial == NULL)
        return;
    spatial->createLocalBoundingVolume();
}
static void se_getLocalBoundingVolume(JNIEnv* env, jobject object, jfloatArray outData)
{
    SE_Spatial* spatial = findSpatial(env, object);
    //LOGI("## se_getLocalBoundingVolume spatial = %p ##\n", spatial);
    if(spatial == NULL)
        return;
    SE_BoundingVolume* bv = spatial->getLocalBoundingVolume();
    //LOGI("## bv = %p ##\n", bv);
    if(bv == NULL)
        return;
    SE_AABBBV* aabbBV = (SE_AABBBV*)bv;
    SE_AABB aabb = aabbBV->getGeometry();
    SE_Vector3f min = aabb.getMin();
    SE_Vector3f max = aabb.getMax();
    float* outDataArray = env->GetFloatArrayElements(outData, 0);
    outDataArray[0] = min.x;
    outDataArray[1] = min.y;
    outDataArray[2] = min.z;
    outDataArray[3] = max.x;
    outDataArray[4] = max.y;
    outDataArray[5] = max.z;
    
    env->ReleaseFloatArrayElements(outData, outDataArray, 0);
}
static void se_rotateMap(JNIEnv* env, jclass clazz, jfloatArray quat, jfloatArray vector, jfloatArray outData)
{
    float* quatArray = env->GetFloatArrayElements(quat, 0);
    float* vectorArray = env->GetFloatArrayElements(vector, 0);
    float* outDataArray = env->GetFloatArrayElements(outData, 0);
    SE_Quat q(quatArray[0], quatArray[1], quatArray[2], quatArray[3]);
    SE_Vector3f v(vectorArray[0], vectorArray[1], vectorArray[2]);
    SE_Vector3f ret = q.map(v);
    outDataArray[0] = ret.x;
    outDataArray[1] = ret.y;
    outDataArray[2] = ret.z;
    
    env->ReleaseFloatArrayElements(quat, quatArray, 0);
    env->ReleaseFloatArrayElements(vector, vectorArray, 0);
    env->ReleaseFloatArrayElements(outData, outDataArray, 0);
}
static void set_setReferenceFrameMatrix(JNIEnv* env, jobject obj, jfloatArray rotate, jfloatArray scale, jfloatArray translate)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if(spatial == NULL)
        return;
    float* rotateArray = env->GetFloatArrayElements(rotate, 0);
    float* scaleArray = env->GetFloatArrayElements(scale, 0);
    float* translateArray = env->GetFloatArrayElements(translate, 0);
    
    SE_Quat q;
    q.set(rotateArray[0], SE_Vector3f(rotateArray[1], rotateArray[2], rotateArray[3]));
    SE_Vector3f s(scaleArray[0], scaleArray[1], scaleArray[2]);
    SE_Vector3f t(translateArray[0], translateArray[1], translateArray[1]);
    spatial->setSpatialFrameMatrix(q, s, t);
    spatial->updateWorldTransform();
    env->ReleaseFloatArrayElements(rotate, rotateArray, 0);
    env->ReleaseFloatArrayElements(scale, scaleArray, 0);
    env->ReleaseFloatArrayElements(translate, translateArray, 0);
}
static void se_setReferenceFrameMatrix(JNIEnv* env, jobject obj, jfloatArray c0, jfloatArray c1, jfloatArray c2, jfloatArray c3)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if(spatial == NULL)
        return;
    float* c0Array = env->GetFloatArrayElements(c0, 0);
    float* c1Array = env->GetFloatArrayElements(c1, 0);
    float* c2Array = env->GetFloatArrayElements(c2, 0);
    float* c3Array = env->GetFloatArrayElements(c3, 0);
    
    SE_ASSERT(env->GetArrayLength(c0) == 4);
    SE_ASSERT(env->GetArrayLength(c1) == 4);
    SE_ASSERT(env->GetArrayLength(c2) == 4);
    SE_ASSERT(env->GetArrayLength(c4) == 4);
    
    SE_Vector4f v1(c0Array[0], c0Array[1], c0Array[2], c0Array[3]);
    SE_Vector4f v2(c1Array[0], c1Array[1], c1Array[2], c1Array[3]);
    SE_Vector4f v3(c2Array[0], c2Array[1], c2Array[2], c2Array[3]);
    SE_Vector4f v4(c3Array[0], c3Array[1], c3Array[2], c3Array[3]);
    spatial->setSpatialFrameMatrix(v1, v2, v3, v4);
    spatial->updateWorldTransform();
    
    
    env->ReleaseFloatArrayElements(c0, c0Array, 0);
    env->ReleaseFloatArrayElements(c1, c1Array, 0);
    env->ReleaseFloatArrayElements(c2, c2Array, 0);
    env->ReleaseFloatArrayElements(c3, c3Array, 0);
    
}
static void se_toObjectCoordinate(JNIEnv* env, jobject obj, jfloatArray points, jfloatArray outPoints)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if(spatial == NULL)
        return;
    const SE_Matrix4f& worldMatrix = spatial->getWorldTransform();
    SE_Matrix4f inverseMatrix = worldMatrix.inverse();
    float* pointFloatArray = env->GetFloatArrayElements(points, 0);
    float* outPointFloatArray = env->GetFloatArrayElements(outPoints, 0);
    int arraySize = env->GetArrayLength(points);
    SE_Vector4f v;
    if(arraySize == 3)
    {
        v = SE_Vector4f(pointFloatArray[0], pointFloatArray[1], pointFloatArray[2], 0);
    }
    else
    {
        v = SE_Vector4f(pointFloatArray[0], pointFloatArray[1], pointFloatArray[2], pointFloatArray[3]);
    }
    SE_Vector4f outv = inverseMatrix.map(v);
    outPointFloatArray[0] = outv[0];
    outPointFloatArray[1] = outv[1];
    outPointFloatArray[2] = outv[2];
    outPointFloatArray[3] = outv[3];
    env->ReleaseFloatArrayElements(points, pointFloatArray, 0);
    env->ReleaseFloatArrayElements(outPoints, outPointFloatArray, 0);

}
static void se_getWorldBoundingVolumeCenter(JNIEnv* env, jobject obj, jfloatArray center)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getCenter### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        SE_Vector3f bCenter = spatial->getCenter();
 	env->SetFloatArrayRegion(center, 0, 3, bCenter.d);
    }
}

static void se_applyLight(JNIEnv* env, jobject obj, jstring lightName) {
    SE_Spatial* spatial = findSpatial(env, obj);
    jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);
    const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
    SE_Scene* scene = findScene(sceneName8);
    const char* lightName8 = env->GetStringUTFChars(lightName, NULL);
    if(scene->getLight(lightName8)) {
         spatial->addLightNameToList(lightName8);
         scene->sceneApplyLight();
    }
    env->ReleaseStringUTFChars(lightName, lightName8);
    env->ReleaseStringUTFChars(sceneName, sceneName8);
}

static void se_unApplyLight(JNIEnv* env, jobject obj, jstring lightName) {
    SE_Spatial* spatial = findSpatial(env, obj);
    const char* lightName8 = env->GetStringUTFChars(lightName, NULL);
    env->ReleaseStringUTFChars(lightName, lightName8);
    jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);
    const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
    SE_Scene* scene = findScene(sceneName8);
    scene->sceneApplyLight();
    env->ReleaseStringUTFChars(sceneName, sceneName8);
}

static void se_changeParent(JNIEnv* env, jobject obj, jstring parentName, jint parentIndex) {
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## set object Parent### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    const char* parentName8 = env->GetStringUTFChars(parentName, NULL);
    SE_Spatial* parent = SE_Application::getInstance()->getSceneManager()->findSpatialByName(parentName8, parentIndex);
    if (parent)
    {
        SE_Spatial* spatial = findSpatial(env, obj);
        if (spatial) {          
            spatial->getScene()->removeSpatial(spatial);
            if (spatial->getParent()) {
                spatial->getParent()->setChildrenHasTransform(true);
                spatial->getParent()->updateWorldTransform();
            }
            parent->getScene()->addSpatial(parent, spatial);
            spatial->setChildrenHasTransform(true);
            parent->updateWorldTransform();
        }
    }
    env->ReleaseStringUTFChars(parentName, parentName8);
}
static void printObjectName(JNIEnv* env, jobject obj) {
    jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
    const char* name = env->GetStringUTFChars(stringName, 0);
    LOGD("BORQS## release name = %s ####\n", name);
    env->ReleaseStringUTFChars(stringName, name);
}
static void se_release(JNIEnv* env, jobject obj) {
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## release### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);   
    const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
    if (_DEBUG)
        LOGD("## release user object ### sceneName = %s\n", sceneName8); 
    SE_Scene* scene = findScene(sceneName8);
    if(scene) {
         SE_Spatial* spatial = findSpatial(env, obj);
         if (spatial) {
            SE_Spatial* leader = spatial->getLeader();
            if (leader)
            {
                leader->removeFollower(spatial);
            }
            SE_Spatial* rs = scene->removeSpatial(spatial);
            printObjectName(env, obj);
            if (rs) delete rs;
         }
    }
    env->ReleaseStringUTFChars(sceneName, sceneName8);
}

static jint se_remove(JNIEnv* env, jobject obj) {
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## remove### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);   
    const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
    if (_DEBUG)
        LOGD("## remove user object ### sceneName = %s\n", sceneName8); 
    SE_Scene* scene = findScene(sceneName8);
    if(scene) {
         SE_Spatial* spatial = findSpatial(env, obj);
         if (spatial) {
            SE_Spatial* leader = spatial->getLeader();
            if (leader)
            {
                leader->removeFollower(spatial);
            }
            SE_Spatial* rs = scene->removeSpatial(spatial);
            env->ReleaseStringUTFChars(sceneName, sceneName8);
            if (rs) return (int)rs;
         }
    }
    env->ReleaseStringUTFChars(sceneName, sceneName8);
    return 0;
}

static void se_setVisible(JNIEnv* env, jobject obj, jboolean visible)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setVisible### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        spatial->setVisible(visible);
    }
}

static void se_setAlpha(JNIEnv* env, jobject obj, jfloat alpha)
{
    /*if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setAlpha### name = %s, alpha = %f\n", name, alpha);
        env->ReleaseStringUTFChars(stringName, name);
    }*/
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        spatial->setAlpha(alpha);
    }
}

static void se_showAllNode(JNIEnv* env, jobject clazz, jboolean visible)
{
    SE_CommonNode *root = (SE_CommonNode*)SE_Application::getInstance()->getSceneManager()->getMainScene()->getRoot();
    if (!visible) {
        root->hideAllNode();
    } else {
        root->showAllNode();
    }
}

static jboolean se_cloneObjectOld(JNIEnv* env, jobject clazz, jobject parent, jint index, jboolean copy, jstring status)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(clazz, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## cloneObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* srcSpatial = findSpatial(env, clazz);
    LOGI("### clone srcSpatial = %p ##\n", srcSpatial);
    if(srcSpatial)
    {
        SE_Spatial* parentSpatial = findSpatial(env, parent);
        LOGI("## clone parentSpatial = %p ###\n", parentSpatial);
        if (!parentSpatial) {
            parentSpatial = srcSpatial->getParent();
        }
        const char* slist = env->GetStringUTFChars(status, 0);
        SE_Spatial* dest = srcSpatial->clone(parentSpatial, index, copy, slist);
        dest->setChildrenHasTransform(true);
        parentSpatial->updateWorldTransform();
#if 0
        SE_BlendState *rs_blend = new SE_BlendState();
        rs_blend->setBlendProperty(SE_BlendState::BLEND_ENABLE);
        dest->setRenderState(SE_Spatial::BLENDSTATE,rs_blend);
        dest->updateRenderState();
#endif
        env->ReleaseStringUTFChars(status, slist);
        return true;
    }
    else
    {
        if(_DEBUG)
            LOGD("## The src not found! ##\n\n");
        return false;
    }
}

static jboolean se_cloneObject(JNIEnv* env, jobject clazz, jobject parent, jint index)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(clazz, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## cloneObject### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* srcSpatial = findSpatial(env, clazz);
        
    if(srcSpatial)
    {
        std::string  status = srcSpatial->getChildrenStatus();
        SE_Spatial* parentSpatial = findSpatial(env, parent);
        if (!parentSpatial) {
            parentSpatial = srcSpatial->getParent();
        }
        SE_Spatial* dest = srcSpatial->clone(parentSpatial, index, true, status.c_str());
        dest->setChildrenHasTransform(true);
        parentSpatial->updateWorldTransform();
#if 0
        SE_BlendState *rs_blend = new SE_BlendState();
        rs_blend->setBlendProperty(SE_BlendState::BLEND_ENABLE);
        dest->setRenderState(SE_Spatial::BLENDSTATE,rs_blend);
        dest->updateRenderState();
#endif
        return true;
    }
    else
    {
        if(_DEBUG)
            LOGD("## The src not found! ##\n\n");
        return false;
    }
}

static void se_setShaderType(JNIEnv* env, jobject obj, jstring type)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setShaderType### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        const char* type8 = env->GetStringUTFChars(type, 0);
        spatial->setShaderType(type8);
        env->ReleaseStringUTFChars(type, type8);
    }
}

static void se_setRenderType(JNIEnv* env, jobject obj, jstring type)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## setRenderType### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        const char* type8 = env->GetStringUTFChars(type, 0);
        spatial->setRenderType(type8);
        env->ReleaseStringUTFChars(type, type8);
    }
}

static jstring se_getShaderType(JNIEnv* env, jobject obj)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getShaderType### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        return env->NewStringUTF(spatial->getShaderType());
    }
    return NULL;
}

static jstring se_getRenderType(JNIEnv* env, jobject obj)
{
    if (_DEBUG) {
        jstring stringName = (jstring)env->GetObjectField(obj, objectNameID);
        const char* name = env->GetStringUTFChars(stringName, 0);
        LOGD("BORQS## getRenderType### name = %s\n", name);
        env->ReleaseStringUTFChars(stringName, name);
    }
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        return env->NewStringUTF(spatial->getRenderType());
    }
    return NULL;
}

static jboolean se_isEnableDepth(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
          return spatial->isNeedDepthTest();
    }
    return false;
}

static jboolean se_isEnableBlend(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
          return spatial->isSpatialEffectHasAttribute(SE_SpatialAttribute::BLENDABLE);
    }
    return false;
}

static void se_changeRenderState(JNIEnv* env, jobject obj,jint state,jboolean enable) {
    SE_Spatial* spatial = findSpatial(env, obj);
    
    if (spatial) {
       spatial->changeRenderState(state,enable);
    }   
    
}

static void se_setSelected(JNIEnv* env, jobject obj, jboolean selected) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        if (_DEBUG)
            LOGD("BORQS## setSelected ### spatial name = %s\n", spatial->getSpatialName());
        spatial->setSpatialRuntimeAttribute(SE_SpatialAttribute::SELECTED,selected);
    }
}

static void se_setTouchable(JNIEnv* env, jobject obj, jboolean touchable) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        if (_DEBUG)
            LOGD("BORQS## setTouchable ### spatial name = %s\n", spatial->getSpatialName());
        spatial->setSpatialStateAttribute(SE_SpatialAttribute::TOUCHABLE,touchable);
    }
}


static void se_setBlendingable(JNIEnv* env, jobject obj,jboolean enable) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
           spatial->setSpatialEffectAttribute(SE_SpatialAttribute::BLENDABLE,enable);           
    }
}

static void se_setUseUserColor(JNIEnv* env, jobject obj,jfloatArray usercolor4f)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setUseUserColor ### spatial name = %s\n", spatial->getSpatialName());
        float* uc = env->GetFloatArrayElements(usercolor4f, 0);
        SE_Vector4f useUserColor = SE_Vector4f(uc[0],uc[1],uc[2],uc[3]);
        spatial->setUserColor(useUserColor.xyz());
        spatial->setUseUserColor(useUserColor.w);
        env->ReleaseFloatArrayElements(usercolor4f, uc, 0);
    }   
}

static void se_setNeedCullFace_JNI(JNIEnv* env, jobject obj, jboolean enable)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
          if (_DEBUG)
              LOGD("BORQS## setNeedCullFace ### spatial name = %s\n", spatial->getSpatialName());
          spatial->setSpatialEffectAttribute(SE_SpatialAttribute::CULLFACE,enable);
    }
}
static void se_setNeedGenerateMirror(JNIEnv* env, jobject obj, jstring mirrorName, jint mirrorPlane)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {       
        if (_DEBUG)
            LOGD("BORQS## setNeedGenerateMirror ### spatial name = %s\n", spatial->getSpatialName());        
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::MIRRORGENERATOR,true);
        const char* mirrorName8 = env->GetStringUTFChars(mirrorName, 0);
        spatial->setMirrorInfo(mirrorName8,(SE_MirrorPlane)mirrorPlane); 
        env->ReleaseStringUTFChars(mirrorName, mirrorName8);
    }
}
static void se_setNeedRenderMirror(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setObjectAsMirror ### spatial name = %s\n", spatial->getSpatialName());        
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::MIRRORRENDER,true);
    }
}

static void se_setNeedGenerateShadow(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);        
    if(spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setNeedGenerateShadow ### spatial name = %s\n", spatial->getSpatialName());        
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::SHADOWGENERATOR,true); 
    }
}

static void se_setNeedRenderShadow(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);      
    if(spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setNeedRenderShadow ### spatial name = %s\n", spatial->getSpatialName());        
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::SHADOWRENDER,true); 
      
    }
}

static void se_setNeedAlphaTest(JNIEnv* env, jobject obj, jboolean enable)
{
    SE_Spatial* spatial = findSpatial(env, obj);      
    if(spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setNeedAlphaTest ### spatial name = %s\n", spatial->getSpatialName());
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::ALPHATEST,enable); 
    }
}

static void se_setIsMiniBox(JNIEnv* env, jobject obj, jboolean miniBox)
{
    SE_Spatial* spatial = findSpatial(env, obj);      
    if(spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setIsMiniBox ### spatial name = %s\n", spatial->getSpatialName());
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::MINIBOX,miniBox); 
    }
}

static void se_uvAnimation(JNIEnv* env, jobject obj, jfloatArray texcoordoffset, jboolean enableUVAnimation)
{
    SE_Spatial* spatial = findSpatial(env, obj);      
    if(spatial)
    {
        spatial->setSpatialEffectAttribute(SE_SpatialAttribute::UVANIMATION,enableUVAnimation); 
        if(enableUVAnimation) {
            float* to = env->GetFloatArrayElements(texcoordoffset, 0);
            SE_Vector2f texoffset = SE_Vector2f(to[0],to[1]);
            spatial->setTexCoordOffset(texoffset);        
            env->ReleaseFloatArrayElements(texcoordoffset, to, 0);
        }
    }
}

static void se_setShadowObjectVisibility(JNIEnv* env, jobject obj, jboolean enable)
{
    SE_Spatial* spatial = findSpatial(env, obj);      
    if(spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setShadowObjectVisibility ### spatial name = %s\n", spatial->getSpatialName());
        spatial->setShadowObjectVisibility(enable);
    }
}

static void se_setLeader(JNIEnv* env, jobject obj, jstring lfname, jint leaderIndex)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if(_DEBUG) LOGD("BORQS## setLeader###");
        const char* lName8 = env->GetStringUTFChars(lfname, 0);
        SE_Spatial* leader = SE_Application::getInstance()->getSceneManager()->findSpatialByName(lName8, leaderIndex);
        if (leader)
        {
            if(_DEBUG) LOGD("BORQS##, set leader### fffffname = %s\n", lName8);
            spatial->setLeader(leader);
        }
        env->ReleaseStringUTFChars(lfname, lName8);
    }
}

static jobject se_getLeader(JNIEnv* env, jobject obj)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if(_DEBUG) LOGD("BORQS## setLeader###");
        SE_Spatial* leader = spatial->getLeader();
        if (leader)
        {
            jobject myObject = env->NewObject(classObject, methodInitObject, env->NewStringUTF(leader->getSpatialName()), spatial->getCloneIndex());
            return myObject;
        }
    }
    return NULL;
}

static void se_addFollower(JNIEnv* env, jobject obj, jstring followerName, jint followerIndex)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if(_DEBUG) LOGD("BORQS## addFollower###");
        const char* fName8 = env->GetStringUTFChars(followerName, 0);
        SE_Spatial* follower = SE_Application::getInstance()->getSceneManager()->findSpatialByName(fName8, followerIndex);
        if (follower)
        {
            if(_DEBUG) LOGD("BORQS##, add follower### fffffname = %s\n", fName8);
            spatial->addFollower(follower);
        }
        env->ReleaseStringUTFChars(followerName, fName8);
    }
}

static void se_removeFollower(JNIEnv* env, jobject obj, jstring followerName, jint followerIndex)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if(_DEBUG) LOGD("BORQS## test removeFollower###");
        const char* fName8 = env->GetStringUTFChars(followerName, 0);
        SE_Spatial* follower = SE_Application::getInstance()->getSceneManager()->findSpatialByName(fName8, followerIndex);
        if (follower)
        {
            spatial->removeFollower(follower);
        }
        env->ReleaseStringUTFChars(followerName, fName8);
    }
}

static void se_setTexCoordXYReverse(JNIEnv* env, jobject obj, jboolean x, jboolean y)
{
	SE_Spatial* spatial = findSpatial(env, obj);
	if (spatial)
	{
		spatial->setTexCoordXYReverse(x, y);
	}
}

static void se_setIsEntirety(JNIEnv* env, jobject obj, jboolean isEntirety)
{
	SE_Spatial* spatial = findSpatial(env, obj);
	if (spatial)
	{
		spatial->setIsEntirety(isEntirety);
	}
}

static jboolean se_isObjectSelected(JNIEnv* env, jobject obj, jint X, jint Y, jfloatArray selPoint)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial) {
        jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);   
        const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
        SE_Scene* scene =  findScene(sceneName8);
        if (scene) {
            SE_Camera* camera = scene->getCamera();
            if (camera)
            {
                SE_Ray ray = camera->screenCoordinateToRay(X, Y);
                SE_SimObject* so = NULL;
                if (scene->getSceneManagerType() == DEFAULT)
                {    
                    SE_FindSpatialCollision spatialCollision(ray);
                    spatial->travel(&spatialCollision, true);
                    so = spatialCollision.getCollisionObject();
                    if (so)
                    { 
                        SE_Vector3f worldIntersectPoint =  spatialCollision.getIntersectPoint();
                        SE_Matrix4f w2m = spatial->getWorldTransform().inverse();
                        SE_Vector3f localIntersectPoint = w2m.map(SE_Vector4f(worldIntersectPoint,1.0)).xyz();
	                env->SetFloatArrayRegion(selPoint, 0, 3, localIntersectPoint.d);
                        env->ReleaseStringUTFChars(sceneName, sceneName8);
                        return true;
                    }
                }

            }
        }
        env->ReleaseStringUTFChars(sceneName, sceneName8);
    }
    return false;
}
static void se_addChild(JNIEnv* env, jobject obj, jint childObject)
{
    if (childObject > 0) {
        SE_Spatial* parentSpatial = findSpatial(env, obj);
        if (parentSpatial) {
            jstring sceneName = (jstring)env->CallObjectMethod(obj, methodGetSceneName);   
            const char* sceneName8 = env->GetStringUTFChars(sceneName, 0);
            SE_Scene* scene =  findScene(sceneName8);
            if (scene) {
                scene->addSpatial(parentSpatial, (SE_Spatial*)childObject);
            }
            env->ReleaseStringUTFChars(sceneName, sceneName8);
        }
    }
}

static void se_setLayerIndex(JNIEnv* env, jobject obj, jint index)
{
        SE_Spatial* spatial = findSpatial(env, obj);
        if (spatial) {
             spatial->updateWorldLayer();
             spatial->getWorldLayer()->setLayer(index);
        }
}

static void se_setNeedBlendSort(JNIEnv* env, jobject obj,jfloatArray blendaxis3f)
{
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        if (_DEBUG)
            LOGD("BORQS## setUseUserColor ### spatial name = %s\n", spatial->getSpatialName());
        float* ba = env->GetFloatArrayElements(blendaxis3f, 0);
        SE_Vector3f blendaxis = SE_Vector3f(ba[0],ba[1],ba[2]);
        
        if(blendaxis.x > 0.0)
        {
            spatial->setNeedBlendSortOnX(true);
        }
        else if(blendaxis.y > 0.0)
        {
            spatial->setNeedBlendSortOnY(true);
        }
        else
        {
            spatial->setNeedBlendSortOnZ(true);
        }        
        env->ReleaseFloatArrayElements(blendaxis3f, ba, 0);
    }   
}

static void se_showRBColor(JNIEnv* env, jobject obj, jboolean show) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
       spatial->setNeedBlackWhiteColor(show);
    }
}
static void se_setTexCoordXYTile(JNIEnv* env, jobject obj, jfloatArray tileValue2f) {
    SE_Spatial* spatial = findSpatial(env, obj);
    if (spatial)
    {
        float* tile = env->GetFloatArrayElements(tileValue2f, 0);
        SE_Vector2f tilev = SE_Vector2f(tilev[0],tilev[1]);
        spatial->setTexCoordXYTile(tilev.x,tilev.y);
        env->ReleaseFloatArrayElements(tileValue2f, tile, 0);
    }
}
static const char *classPathName = "com/borqs/se/engine/SEObject";

static JNINativeMethod methods[] = {
  {"addUserObject_JNI", "()V", (void*)se_addUserObject},
  {"loadImageData_JNI", "(Ljava/lang/String;)I",(void*)se_loadImageData},
  {"loadImageData_JNI", "(Landroid/graphics/Bitmap;)I",(void*)se_loadImageData_II},
  {"addImageData_JNI", "(Ljava/lang/String;I)V",(void*)se_addImageData},
  {"getImageName_JNI", "()Ljava/lang/String;",(void*)se_getImageName},
  {"getImageKey_JNI", "(Ljava/lang/String;)Ljava/lang/String;",(void*)se_getImageKey},
  {"applyImage_JNI","(Ljava/lang/String;Ljava/lang/String;)V",(void*)se_applyImage},
  {"updateVertex_JNI", "([F)V", (void*)se_updateVertex},
  {"updateTexture_JNI","([F)V", (void*)se_updateTexture},
  {"removeImageData_JNI","(Ljava/lang/String;)V",(void*)se_removeImageData},
  {"applyLight_JNI", "(Ljava/lang/String;)V", (void*)se_applyLight},
  {"unApplyLight_JNI", "(Ljava/lang/String;)V", (void*)se_unApplyLight},
  {"changeParent_JNI", "(Ljava/lang/String;I)V", (void*)se_changeParent},
  {"remove_JNI", "()I", (void*)se_remove},
  {"release_JNI", "()V", (void*)se_release},
  {"setVisible_JNI", "(Z)V", (void*)se_setVisible},
  {"setAlpha_JNI", "(F)V", (void*)se_setAlpha},
  {"showAllNode_JNI", "(Z)V", (void*)se_showAllNode},
  {"cloneObject_JNI","(Lcom/borqs/se/engine/SEObject;IZLjava/lang/String;)Z",(void*)se_cloneObjectOld},
  {"cloneObjectNew_JNI","(Lcom/borqs/se/engine/SEObject;I)Z",(void*)se_cloneObject},
  {"getAbsoluteTranslate_JNI","([F)V", (void*)se_getAbsoluteTranslate},
  {"setShaderType_JNI", "(Ljava/lang/String;)V",(void*)se_setShaderType},
  {"setRenderType_JNI", "(Ljava/lang/String;)V",(void*)se_setRenderType},
  {"getShaderType_JNI", "()Ljava/lang/String;",(void*)se_getShaderType},
  {"getRenderType_JNI", "()Ljava/lang/String;",(void*)se_getRenderType},
  {"changeRenderState_JNI", "(IZ)V",(void*)se_changeRenderState},
  {"setBlendingable_JNI", "(Z)V",(void*)se_setBlendingable},
  {"setSelected_JNI", "(Z)V",(void*)se_setSelected},
  {"setTouchable_JNI", "(Z)V",(void*)se_setTouchable},
  {"operateObject_JNI", "([F[F[F)V", (void*)se_operateObject},
  {"rotateObject_JNI", "([F)V", (void*)se_rotateObject},
  {"rotatePoint_JNI", "([F)V", (void*)se_rotatePoint},
  {"translateObject_JNI", "([FZ)V", (void*)se_translateObject},
  {"scaleObject_JNI", "([F)V", (void*)se_scaleObject},

  {"setRotate_JNI", "([F)V", (void*)se_setRotate},
  {"setTranslate_JNI", "([F)V", (void*)se_setTranslate},
  {"setScale_JNI", "([F)V", (void*)se_setScale},

  {"setLocalRotate_JNI", "([F)V", (void*)se_setLocalRotate},
  {"setLocalTranslate_JNI", "([F)V", (void*)se_setLocalTranslate},
  {"setLocalScale_JNI", "([F)V", (void*)se_setLocalScale},

  {"translateLocal_JNI", "([F)V", (void*)se_translateLocal},
  {"scaleLocal_JNI", "([F)V", (void*)se_scaleLocal},
  {"rotateLocal_JNI", "([F)V", (void*)se_rotateLocal},

  {"getUserRotate_JNI", "([F)V", (void*)se_getUserRotate},
  {"getUserTranslate_JNI", "([F)V", (void*)se_getUserTranslate},
  {"getUserScale_JNI", "([F)V", (void*)se_getUserScale},

  {"getLocalRotate_JNI", "([F)V", (void*)se_getLocalRotate},
  {"getLocalTranslate_JNI", "([F)V",(void*)se_getLocalTranslate},
  {"getLocalScale_JNI", "([F)V", (void*)se_getLocalScale},
  {"isEnableDepth_JNI", "()Z", (void*)se_isEnableDepth},
  {"isEnableBlend_JNI", "()Z", (void*)se_isEnableBlend},
  {"setUseUserColor_JNI", "([F)V", (void*)se_setUseUserColor},
  {"setNeedCullFace_JNI", "(Z)V", (void*)se_setNeedCullFace_JNI},
  {"setNeedGenerateMirror_JNI", "(Ljava/lang/String;I)V", (void*)se_setNeedGenerateMirror},
  {"setNeedRenderMirror_JNI", "()V", (void*)se_setNeedRenderMirror},
  {"setNeedGenerateShadow_JNI", "()V", (void*)se_setNeedGenerateShadow},
  {"setNeedRenderShadow_JNI", "()V", (void*)se_setNeedRenderShadow},
  {"setNeedAlphaTest_JNI", "(Z)V", (void*)se_setNeedAlphaTest},
  {"setIsMiniBox_JNI", "(Z)V", (void*)se_setIsMiniBox},
  {"uvAnimation_JNI", "([FZ)V", (void*)se_uvAnimation},
  {"setShadowObjectVisibility_JNI", "(Z)V", (void*)se_setShadowObjectVisibility},
  {"setLeader_JNI", "(Ljava/lang/String;I)V", (void*)se_setLeader},
  {"getLeader_JNI", "()Lcom/borqs/se/engine/SEObject;", (void*)se_getLeader},
  {"removeFollower_JNI", "(Ljava/lang/String;I)V", (void*)se_removeFollower},
  {"addFollower_JNI", "(Ljava/lang/String;I)V", (void*)se_addFollower},
  {"getWorldBoundingVolumeCenter_JNI", "([F)V", (void*)se_getWorldBoundingVolumeCenter},
  
  {"setTexCoordXYReverse_JNI", "(ZZ)V", (void*)se_setTexCoordXYReverse},
  {"isObjectSelected_JNI","(II[F)Z", (void*)se_isObjectSelected},
  {"setIsEntirety_JNI","(Z)V", (void*)se_setIsEntirety},
  {"addChild_JNI","(I)V", (void*)se_addChild},
  {"setLayerIndex_JNI","(I)V", (void*)se_setLayerIndex},
  {"se_setNeedBlendSort_JNI", "([F)V", (void*)se_setNeedBlendSort},
  {"showRBColor_JNI", "(Z)V", (void*)se_showRBColor},
  {"isImageExist_JNI", "(Ljava/lang/String;)Z", (void*)se_isImageExist},
  {"toWorldCoordinate_JNI", "([F[F)V", (void*)se_toWorldCoordinate},
  {"toObjectCoordinate_JNI", "([F[F)V", (void*)se_toObjectCoordinate},
  {"setReferenceFrameMatrix_JNI", "([F[F[F[F)V", (void*) se_setReferenceFrameMatrix},
  {"setReferenceFrameMatrix_JNI", "([F[F[F)V", (void*) set_setReferenceFrameMatrix},
  {"rotateMap_JNI", "([F[F[F)V", (void*)se_rotateMap},
  {"getLocalBoundingVolume_JNI", "([F)V", (void*) se_getLocalBoundingVolume},
  {"createLocalBoundingVolume_JNI", "()V", (void*) se_createLocalBoundingVolume},
  {"clearLocalBoundingVolume_JNI", "()V", (void*) se_clearLocalBoundingVolume},

{"setTexCoordXYTile_JNI", "([F)V", (void*)se_setTexCoordXYTile},
};

static int registerNativeMethods(JNIEnv* env, const char* className,
    JNINativeMethod* gMethods, int numMethods)
{
    jclass clazz;

    clazz = env->FindClass(className);
    if (clazz == NULL) {
        fprintf(stderr,
            "Native registration unable to find class '%s'\n", className);
        return JNI_FALSE;
    }
    classObject = (jclass)env->NewGlobalRef(clazz);
    methodInitObject = env->GetMethodID(classObject, "<init>", "(Ljava/lang/String;I)V");

    nativeBitmapID = isNewBitmapPlatform(env);

    methodGetParentName = env->GetMethodID(clazz, "getParentName", "()Ljava/lang/String;");
    methodGetParentIndex = env->GetMethodID(clazz, "getParentIndex", "()I");
    methodGetSceneName = env->GetMethodID(clazz, "getSceneName", "()Ljava/lang/String;");
    objectDataID = env->GetFieldID(clazz, "mObjectData", "Lcom/borqs/se/engine/SEObjectData;");
    objectNameID = env->GetFieldID(clazz, "mName", "Ljava/lang/String;");
    objectIndexID = env->GetFieldID(clazz, "mIndex", "I");
    isNodeID = env->GetFieldID(clazz, "mIsNode", "Z");

    jclass dataClass =  env->FindClass("com/borqs/se/engine/SEObjectData");
    localTransParasID = env->GetFieldID(dataClass, "mLocalTransParas", "Lcom/borqs/se/engine/SETransParas;");
    userTransParasID = env->GetFieldID(dataClass, "mUserTransParas", "Lcom/borqs/se/engine/SETransParas;");
    bvTypeID = env->GetFieldID(dataClass, "mBVType", "I");
/*
    imageWID = env->GetFieldID(dataClass, "mImageWidth", "I");
    imageHID = env->GetFieldID(dataClass, "mImageHeight", "I");
*/
    layerIndexID = env->GetFieldID(dataClass, "mLayerIndex", "I");
    methodGetShaderTypeID = env->GetMethodID(dataClass, "getShaderType", "()Ljava/lang/String;");
    methodGetRenderTypeID = env->GetMethodID(dataClass, "getRenderType", "()Ljava/lang/String;");
    methodGetRenderStateID = env->GetMethodID(dataClass, "getRenderState", "()I");
    methodGetVertexArray = env->GetMethodID(dataClass, "getVertexArray", "()[F");
    methodGetTexVertexArray = env->GetMethodID(dataClass, "getTexVertexArray", "()[F");
    methodGetFaceArray = env->GetMethodID(dataClass, "getFaceArray", "()[I");
    methodGetAlpha = env->GetMethodID(dataClass, "getAlpha", "()F");
    methodGetColor = env->GetMethodID(dataClass, "getColor", "()[F");

    methodGetImageKey = env->GetMethodID(dataClass, "getImageKey", "()Ljava/lang/String;");
    methodGetImageName = env->GetMethodID(dataClass, "getImageName", "()Ljava/lang/String;");
    methodGetImageType = env->GetMethodID(dataClass, "getImageType", "()I");

    methodGetBitmap = env->GetMethodID(dataClass, "getBitmap", "()Landroid/graphics/Bitmap;");
    methodGetObjectType = env->GetMethodID(dataClass, "getObjectType", "()I");
    methodGetLineWidth = env->GetMethodID(dataClass, "getLineWidth", "()F");
    jclass transParasClass =  env->FindClass("com/borqs/se/engine/SETransParas");
    methodGetTranslate = env->GetMethodID(transParasClass, "getTranslate", "()[F");
    methodGetRotate = env->GetMethodID(transParasClass, "getRotate", "()[F");
    methodGetScale = env->GetMethodID(transParasClass, "geScale", "()[F");
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        fprintf(stderr, "RegisterNatives failed for '%s'\n", className);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int registerNatives(JNIEnv* env)
{
  if (!registerNativeMethods(env, classPathName,
                 methods, sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }

  return JNI_TRUE;
}


int register_com_android_se_SEObject(JNIEnv* env)
{
    return registerNatives(env);
}
