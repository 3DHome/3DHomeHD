#include "SE_Factory.h"
#include "SE_CommonNode.h"
#include "SE_Ase.h"
#include "SE_Log.h"
#include "SE_GeometryData.h"
#include "SE_ResourceManager.h"
#include "SE_Vector.h"
#include "SE_Matrix.h"
#include "SE_Quat.h"
#include "SE_Utils.h"
#include "SE_Buffer.h"
#include "SE_ResFileHeader.h"
#include "SE_File.h"
#include "SE_Application.h"

#include "SE_Geometry.h"
//#include "SE_NewGeometry.h"
#include "SE_BoundingVolume.h"
#include "SE_MeshSimObject.h"
#include "SE_ImageData.h"
#include "SE_IO.h"
#include "SE_KeyFrame.h"
#include "SE_DataValueDefine.h"
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <assert.h>
#include <sstream>
#include <iostream>
#include "SE_ImageCodec.h"
#include "SE_LodNode.h"
#include "SE_File.h"
#include "SE_VertexBuffer.h"

#include "SE_MemLeakDetector.h"
#define ANDROID

#ifdef ANDROID
#undef COMPRESS_TEXTURE
#else
#define COMPRESS_TEXTURE
#endif

#ifdef COMPRESS_TEXTURE
#include "PVRTexLib.h"


//compress jpg png and others to pvrtc or etc1
static bool compressTexture(const char *filePath,const char *fileName)
{
    std::string file = std::string(filePath) + "\\" + std::string(fileName);
        // write to file specified
    std::string _file = fileName;

    size_t pos = _file.find('.');
    std::string name = _file.substr(0, pos);
    std::string writefile = std::string(filePath) + "\\" + name + ".pvr";
    if(SE_File::isExist(writefile.c_str()))
        return true;
    SE_ImageData* rawImage = SE_ImageCodec::loadARGB(file.c_str());

    if(!rawImage)
    {
        return false;
    }

    pvrtexlib::uint8 * data = (unsigned char*)rawImage->getData();

    if(!data)
    {
        delete rawImage;
        return false;
    }
    
    // get the utilities instance
    pvrtexlib::PVRTextureUtilities* PVRU = pvrtexlib::PVRTextureUtilities::getPointer();//pvrtexlib::PVRTextureUtilities();

    // make a CPVRTexture instance with data passed
    pvrtexlib::CPVRTexture sOriginalTexture( rawImage->getWidth(), // u32Width,
    rawImage->getHeight(), // u32Height,
    0, // u32MipMapCount,
    1, // u32NumSurfaces,
    false, // bBorder,
    false, // bTwiddled,
    false, // bCubeMap,
    false, // bVolume,
    false, // bFalseMips,
    true, // bHasAlpha
    false, // bVerticallyFlipped
    pvrtexlib::eInt8StandardPixelType, // ePixelType,
    0.0f, // fNormalMap,
    data // pPixelData
    );
    

    pvrtexlib::CPVRTextureHeader sProcessHeader(sOriginalTexture.getHeader());
    sProcessHeader.setFlipped(true);

    PVRU->ProcessRawPVR(sOriginalTexture,sProcessHeader);

    // create texture to encode to
    pvrtexlib::CPVRTexture sCompressedTexture(sOriginalTexture.getHeader());

    // set required encoded pixel type
    sCompressedTexture.setPixelType(pvrtexlib::PixelType::OGL_PVRTC2);

    // encode texture
    PVRU->CompressPVR(sOriginalTexture,sCompressedTexture,1);



    if(sCompressedTexture.writeToFile(writefile.c_str()) > 0)
    {
        delete rawImage;
        if(SE_Application::getInstance()->SEHomeDebug)
        LOGI("Write compressed PVR file[%s] success!!!\n",writefile.c_str());
        return true;
    }

    // handle any exceptions here
    if(SE_Application::getInstance()->SEHomeDebug)
    LOGI("\n\n\n Fail to compress texture");

    return false;
}
#endif

static int getFileLen(FILE* fp)
{
    int        pos;
    int        end;

    pos = ftell (fp);
    fseek (fp, 0, SEEK_END);
    end = ftell (fp);
    fseek (fp, pos, SEEK_SET);

    return end;

}
struct ase_t
{
    char* buffer;
    char* curpos;
    int len;
    ase_t()
    {
        len = 0;
        buffer = NULL;
        curpos = NULL;
    }
    ~ase_t()
    {
        delete buffer;        
    }
};
#define BUF_SIZE 1024
static ase_t ase; 
static char s_token[BUF_SIZE];
static char meshFileName[256];
static bool _verbose = false;
static const int MAGIC = 0xCFCFCFCF;
static const int VERSION = 0x01;
static const int COORDINATE = 0x00;
static const int ENDIAN = 0x00; /** 0: little endian, 1: big endian*/


static const short MATERIAL_ID = 0x0002;
static const short GEOMOBJECT_ID = 0x0003;
static const short CAMERA_ID = 0x0004;
static const short SUB_MATERIAL_ID = 0x0005;
static const short MESH_ID = 0x0006;


ASE_Loader::ASE_Loader() : mCurrGeomObject(NULL), mCurrMtl(NULL),mCurrSubMtl(NULL),mCurrMesh(NULL), mInSubDiffuse(false)
{
    mSceneObject = NULL;
    mMatStartPos = 0;
    mCurrGeometryObjectGroup = NULL;
    mQuoteMarkFinish = true;
    mCurrNode = NULL;
    mCurrSU = NULL;
    mCurrentCameraPos = NULL;
    mIsKeyMesh = false;

    mIsGroupNode = false;
}
ASE_Loader::~ASE_Loader()
{
    for(int i = 0; i < mSceneList.size(); ++i)
    {
        delete mSceneList[i];
    }
    mSceneList.clear();
}
/*
void ASE_Loader::Load()
{
    ASE_Load(meshFileName, _verbose);

}
*/
struct _MaterialData
{
    int subMaterialNum;
    ASE_MaterialData md;
    SE_MaterialDataID mid;
    SE_ImageDataID tid;

    ASE_MaterialData mSpecularData;
    SE_MaterialDataID mSpecularid;
    SE_ImageDataID sptid;
    _MaterialData()
    {
        subMaterialNum = 0;
    }
};
struct _GeomTexCoordData
{
    SE_GeometryDataID geomID;
    SE_TextureCoordDataID texCoordID;
};
static void writeHeader(SE_BufferOutput& output, int dataLen)
{
    output.writeInt(SE_MAGIC);
    output.writeInt(SE_VERSION);
    output.writeInt(SE_ENDIAN);
    output.writeInt(dataLen);
}
class _WriteSceneTravel : public SE_SpatialTravel
{
public:
    _WriteSceneTravel(SE_BufferOutput& out) : mOut(out)
    {}
    int visit(SE_Spatial* spatial)
    {
        spatial->write(mOut);
        return 0;
    }
    int visit(SE_SimObject* simObject)
    {
        return 0;
    }
public:
    SE_BufferOutput& mOut;
};
//static const int SLEEP_COUNT = 50;
void ASE_Loader::Write(SE_BufferOutput& output, SE_BufferOutput& outScene, const char* shaderPath, const char *cbfName,bool averageNormal)
{
        
    std::vector<SE_MeshID> meshIDVector;

    //save all normal and simple model data 
    //mSceneList[0] is normal data
    //mSceneList[1] is simple data
    for(int sceneIndex = 0; sceneIndex < mSceneList.size(); ++sceneIndex )
    {
    std::string fullpathname = cbfName;
    size_t pos = fullpathname.rfind('\\');
    std::string namecbf = fullpathname.substr(pos+1, fullpathname.size());

        if(sceneIndex > 0)
        {
            namecbf = namecbf + std::string("simple");
        }

        mSceneObject = mSceneList[sceneIndex];
//rewrite object name, use xxx.cbf\objname style
    std::list<ASE_GeometryObject*>::iterator tempIt;

    for(tempIt = mSceneObject->mGeomObjects.begin();
        tempIt != mSceneObject->mGeomObjects.end();
        tempIt++)
    {
        ASE_GeometryObject* go = *tempIt;
        std::string rawName = go->name;
        std::string fullName;
        if(go->mGroupName.empty())
        {
            fullName = rawName + "@" +  namecbf;
        }
        else
        {
            fullName = rawName + "@" + go->mGroupName + "@"+  namecbf;
        }
        
        strcpy(go->name,fullName.c_str());
    
    }

    int materialNum = mSceneObject->mMats.size();
    int numWhichHasSubmaterial = 0;
    int materialRealNum = materialNum;
    int i;
    for(i = 0 ; i < materialNum ; i++)
    {
        ASE_Material* srcm = &mSceneObject->mMats[i];        
        materialRealNum += srcm->numsubmaterials;
        if(srcm->numsubmaterials > 0)
        {
            numWhichHasSubmaterial++;
        }
    }
    std::vector<_MaterialData> materialVector(materialRealNum);
    std::vector<int> indexWhichHasSubmaterial(numWhichHasSubmaterial);
    int l = 0;
    int mi = 0;
    for(i = 0 ; i < materialNum ; i++)
    {
            ASE_Material* srcm = &mSceneObject->mMats[i];
            _MaterialData md;
            md.subMaterialNum = srcm->numsubmaterials;
            md.md = srcm->materialData;

            //for specular
            md.mSpecularData = srcm->specularMapData;

        materialVector[mi++] = md;
            if(srcm->numsubmaterials > 0)
            {
                indexWhichHasSubmaterial[l++] = i;
            }
    }
    std::vector<int>::iterator it;
    for(it = indexWhichHasSubmaterial.begin() ; it != indexWhichHasSubmaterial.end() ; it++)
    {
        int index = *it;
        ASE_Material* m = &mSceneObject->mMats[index];
        for(int j = 0 ; j < m->numsubmaterials ; j++)
        {
            _MaterialData md;
            md.subMaterialNum = 0;
            md.md = m->submaterials[j];
            materialVector[mi++] = md;

        }
    }
    std::vector<_MaterialData>::iterator itMaterial;
    output.writeShort(SE_MATERIALDATA_ID);
    output.writeInt(materialVector.size());
    int mmm = materialVector.size();
    //for(itMaterial = materialVector.begin() ; itMaterial != materialVector.end() ; itMaterial++)
    for(i = 0 ; i < materialVector.size() ; i++)
    {        

        /*
        std::string filename = materialVector[i].md.texName;
        size_t pos_ext = filename.find('.');
        std::string name = filename.substr(0, pos_ext);
        */
        char buf[20];
        memset(buf, 0, 20);
#ifdef WIN32
        _snprintf(buf, 19, "%d", i);
#else
        snprintf(buf, 19, "%d", i);
#endif
        std::string name = buf;
        std::string id = namecbf + "\\" + name;
        

        SE_MaterialDataID mid = id.c_str();
        mid.print();
        //SE_Util::sleep(SLEEP_COUNT);
        materialVector[i].mid = mid;
        mid.write(output);
        output.writeVector3f(materialVector[i].md.ambient);
        output.writeVector3f(materialVector[i].md.diffuse);
        output.writeVector3f(materialVector[i].md.specular);
        output.writeFloat(materialVector[i].md.shine);
        //output.writeFloat(materialVector[i].md.shineStrength);
        //shine is a float not a vector struct
        //output.writeVector3f(SE_Vector3f(0, 0, 0));
    }
    /////////////////////////////write texture data ///////////////
    output.writeShort(SE_IMAGEDATA_ID);
    int imageDataNum = 0;
    for(itMaterial = materialVector.begin() ; itMaterial != materialVector.end() ; itMaterial++)
    {
        std::string texStr(itMaterial->md.texName);
        if(texStr != "")
        {
            imageDataNum++;
        }

        //for specular
        std::string sptexStr(itMaterial->mSpecularData.texName);
        if(sptexStr != "")
        {
            imageDataNum++;
        }

    }
    output.writeInt(imageDataNum);
    for(itMaterial = materialVector.begin() ; itMaterial != materialVector.end() ; itMaterial++)
    {
        std::string texStr(itMaterial->md.texName);
        if(texStr != "")
        {
            size_t pos = texStr.find('.');
            std::string name = texStr.substr(0, pos);
            std::string ext = texStr.substr(pos + 1);

#ifdef COMPRESS_TEXTURE
            compressTexture(shaderPath,texStr.c_str());

            std::string filepath = shaderPath;
            std::string compressedName = name + ".pvr";

            std::string tidstr = compressedName + "@" + namecbf;
            SE_ImageDataID tid = tidstr.c_str();
            itMaterial->tid = tid;
            tid.write(output);
            if(1)//maybe etc1 format
            {
                output.writeInt(SE_ImageData::OGL_PVRTC2); // compressed image data type
            }            
            output.writeString(compressedName.c_str());

#else
            std::string filepath = shaderPath;
            std::string picName = name + "." + ext;;
            std::string tidstr = picName + "@" + namecbf;
            
            SE_ImageDataID tid = tidstr.c_str();
            itMaterial->tid = tid;
            tid.write(output);
            if(ext == "raw")
            {
                output.writeInt(SE_ImageData::RAW); // image data type
            }
            else if(ext == "png" || ext == "PNG")
            {
                output.writeInt(SE_ImageData::PNG);
            }
            else if(ext == "tga")
			{
				output.writeInt(SE_ImageData::TGA);
			}
            else if(ext == "jpg" || ext == "jpeg" || ext == "JPG")
            {
                output.writeInt(SE_ImageData::JPEG);
            }
                else
                {
                    output.writeInt(SE_ImageData::PNG);
                }
            output.writeString(texStr.c_str());
#endif
        }
            //for specular
            {
            std::string sptexStr(itMaterial->mSpecularData.texName);
            if(sptexStr != "")
            {
                size_t pos = sptexStr.find('.');
                std::string name = sptexStr.substr(0, pos);
                std::string ext = sptexStr.substr(pos + 1);

    #ifdef COMPRESS_TEXTURE
                compressTexture(shaderPath,sptexStr.c_str());

                std::string filepath = shaderPath;
                std::string compressedName = name + ".pvr";

                std::string tidstr = compressedName + "@" + namecbf;
                SE_ImageDataID tid = tidstr.c_str();
                itMaterial->sptid = tid;
                tid.write(output);
                if(1)//maybe etc1 format
                {
                    output.writeInt(SE_ImageData::OGL_PVRTC2); // compressed image data type
                }            
                output.writeString(compressedName.c_str());

    #else
                std::string filepath = shaderPath;
                std::string picName = name + "." + ext;;
                std::string tidstr = picName + "@" + namecbf;
                
                SE_ImageDataID tid = tidstr.c_str();                
                itMaterial->sptid = tid;
                tid.write(output);
                if(ext == "raw")
                {
                    output.writeInt(SE_ImageData::RAW); // image data type
                }
                    else if(ext == "png" || ext == "PNG")
                {
                    output.writeInt(SE_ImageData::PNG);
                }
                else if(ext == "tga")
			    {
				    output.writeInt(SE_ImageData::TGA);
			    }
                else if(ext == "jpg" || ext == "jpeg" || ext == "JPG")
                {
                    output.writeInt(SE_ImageData::JPEG);
                }
                output.writeString(sptexStr.c_str());
    #endif
        }
            
        }
    }
    /////////////////////////////write geom data /////////////////////////////////////////////
    output.writeShort(SE_GEOMETRYDATA_ID);
    int geomDataNum = mSceneObject->mGeomObjects.size();
    output.writeInt(geomDataNum);
    std::vector<_GeomTexCoordData> geomTexCoordData(geomDataNum);
    std::list<ASE_GeometryObject*>::iterator itGeomObj;
    int n = 0;
    SE_Matrix4f modelToWorldM, worldToModelM;
    SE_Matrix3f rotateM;
    SE_Quat rotateQ;
    SE_Vector3f rotateAxis, scale, translate;
    for(itGeomObj = mSceneObject->mGeomObjects.begin();
        itGeomObj != mSceneObject->mGeomObjects.end();
        itGeomObj++)
    {
        ASE_GeometryObject* go = *itGeomObj;
        ASE_Mesh* mesh = go->mesh;
        std::string gidstr = go->name;
        SE_GeometryDataID gid = gidstr.c_str();//SE_Application::getInstance()->createCommonID();
        //SE_Util::sleep(SLEEP_COUNT);
        rotateAxis.x = go->rotateAxis[0];
        rotateAxis.y = go->rotateAxis[1];
        rotateAxis.z = go->rotateAxis[2];
        scale.x = go->scale[0];
        scale.y = go->scale[1];
        scale.z = go->scale[2];
        translate.x = go->translate[0];
        translate.y = go->translate[1];
        translate.z = go->translate[2];
        rotateQ.set(go->rotateAngle, rotateAxis);
        rotateM = rotateQ.toMatrix3f();//.setRotateFromAxis(go->rotateAngle, rotateAxis);
        modelToWorldM.set(rotateM, scale, translate);
        worldToModelM = modelToWorldM.inverse();
        geomTexCoordData[n++].geomID = gid;
        gid.write(output);
        output.writeInt(mesh->numVertexes);
        output.writeInt(mesh->numFaces);

        //write face normal num
        output.writeInt(mesh->faceAndVertexNormal.size());
        //write vertex normal num
        output.writeInt(mesh->faceAndVertexNormal.size() * 3);

        std::vector<std::vector<int> > indexlist;

        int i;
        for(i = 0 ; i < mesh->numVertexes ; i++)
        {
            SE_Vector4f p(mesh->vertexes[i].x, mesh->vertexes[i].y, mesh->vertexes[i].z, 1.0f);
            p = worldToModelM.map(p);
            output.writeFloat(p.x);
            output.writeFloat(p.y);
            output.writeFloat(p.z);

            //every vertex has own facelist
            std::vector<int> belonetofacelist;
            indexlist.push_back(belonetofacelist);
            if(0)
                {
                    LOGI("Vertexes [%d].x = %f \n",i,p.x);
                    LOGI("Vertexes [%d].y = %f \n",i,p.y);
                    LOGI("Vertexes [%d].z = %f \n",i,p.z);
                }


        }
        for(i = 0 ; i < mesh->numFaces ; i++)
        {
            // which face the vertex belone to.
            indexlist[mesh->faces[i].vi[0]].push_back(i);
            indexlist[mesh->faces[i].vi[1]].push_back(i);
            indexlist[mesh->faces[i].vi[2]].push_back(i);

            output.writeInt(mesh->faces[i].vi[0]);
            output.writeInt(mesh->faces[i].vi[1]);
            output.writeInt(mesh->faces[i].vi[2]);
        }

        //generate face normal array
        SE_Vector3f prevNormal(1.0,0,0);
        for(int i = 0; i < mesh->numFaces; ++i)
        {
#if 0
            _Vector3i vertexIndex;
            vertexIndex.d[0] = mesh->faces[i].vi[0];
            vertexIndex.d[1] = mesh->faces[i].vi[1];
            vertexIndex.d[2] = mesh->faces[i].vi[2];

            SE_Vector3f localp[3];

            for(int i = 0; i < 3; ++i)
            {
                SE_Vector4f p(mesh->vertexes[vertexIndex.d[i]].x, mesh->vertexes[vertexIndex.d[i]].y,mesh->vertexes[vertexIndex.d[i]].z, 1.0f);
                p = worldToModelM.map(p);

                localp[i].set(p.x,p.y,p.z);            
            }

            SE_Vector3f e1 = localp[1] - localp[0];
            SE_Vector3f e2 = localp[2] - localp[1];

            SE_Vector3f facenormal;

            if(e1.length() <= 0.001 || e2.length() <= 0.001)
            {
                facenormal = prevNormal;//there is degenerate triangle
                LOGI("there is a degenerate triangle, face index = %d, vertex index x = %d,y = %d,z = %d\n",i,vertexIndex.d[0],vertexIndex.d[1],vertexIndex.d[2]);
            }
            else
            {
                facenormal = (e1.cross(e2)).normalize(); 
            }

            prevNormal = facenormal;
            //save face normal to list
            mesh->faceAndVertexNormal[i].faceNormal.x = facenormal.x;
            mesh->faceAndVertexNormal[i].faceNormal.y = facenormal.y;
            mesh->faceAndVertexNormal[i].faceNormal.z = facenormal.z;


            output.writeFloat(facenormal.x);
            output.writeFloat(facenormal.y);
            output.writeFloat(facenormal.z);
#endif

                output.writeFloat(mesh->faceAndVertexNormal[i].faceNormal.x);
                output.writeFloat(mesh->faceAndVertexNormal[i].faceNormal.y);
                output.writeFloat(mesh->faceAndVertexNormal[i].faceNormal.z);

                /*if(0)
            {
                LOGI("face normal [%d].x = %f \n",i,facenormal.x);
                LOGI("face normal [%d].y = %f \n",i,facenormal.y);
                LOGI("face normal [%d].z = %f \n\n",i,facenormal.z);
                }*/
            

        }

#if 0
        //generate vertex normal
        for(int i = 0; i < mesh->numFaces; ++i)
        {
            bool useAverageValue = averageNormal;
            _Vector3i vertexIndex;
            vertexIndex.d[0] = mesh->faces[i].vi[0];//p0
            vertexIndex.d[1] = mesh->faces[i].vi[1];//p1
            vertexIndex.d[2] = mesh->faces[i].vi[2];//p2

            if(!useAverageValue)
            {
                mesh->faceAndVertexNormal[i].faceVertexNoraml[0].x = mesh->faceAndVertexNormal[i].faceNormal.x;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[0].y = mesh->faceAndVertexNormal[i].faceNormal.y;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[0].z = mesh->faceAndVertexNormal[i].faceNormal.z;

                mesh->faceAndVertexNormal[i].faceVertexNoraml[1].x = mesh->faceAndVertexNormal[i].faceNormal.x;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[1].y = mesh->faceAndVertexNormal[i].faceNormal.y;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[1].z = mesh->faceAndVertexNormal[i].faceNormal.z;

                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].x = mesh->faceAndVertexNormal[i].faceNormal.x;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].y = mesh->faceAndVertexNormal[i].faceNormal.y;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].z = mesh->faceAndVertexNormal[i].faceNormal.z;
            }
            else
            {
                {
                    //use average value
                    int p0faceNum = indexlist[vertexIndex.d[0]].size();//how many faces use this vertex
                    SE_Vector3f p0normal;
                    for(int i = 0; i < p0faceNum; ++i)
                    {
                        SE_Vector3f facenormal;                    
                        int faceIndex = indexlist[vertexIndex.d[0]][i];
                        //get face normal
                        facenormal.x = mesh->faceAndVertexNormal[faceIndex].faceNormal.x;
                        facenormal.y = mesh->faceAndVertexNormal[faceIndex].faceNormal.y;
                        facenormal.z = mesh->faceAndVertexNormal[faceIndex].faceNormal.z;

                        p0normal = p0normal.add(facenormal);
                        
                    }
                    //get average value for this vertex
                    p0normal.x = p0normal.x / p0faceNum;
                    p0normal.y = p0normal.y / p0faceNum;
                    p0normal.z = p0normal.z / p0faceNum;

                    mesh->faceAndVertexNormal[i].faceVertexNoraml[0].x = p0normal.x;
                    mesh->faceAndVertexNormal[i].faceVertexNoraml[0].y = p0normal.y;
                    mesh->faceAndVertexNormal[i].faceVertexNoraml[0].z = p0normal.z;
                }



                {
                    int p1faceNum = indexlist[vertexIndex.d[1]].size();//how many faces use this vertex
                    SE_Vector3f p1normal;
                    for(int i = 0; i < p1faceNum; ++i)
                    {
                        SE_Vector3f facenormal;                    
                        int faceIndex = indexlist[vertexIndex.d[1]][i];
                        //get face normal
                        facenormal.x = mesh->faceAndVertexNormal[faceIndex].faceNormal.x;
                        facenormal.y = mesh->faceAndVertexNormal[faceIndex].faceNormal.y;
                        facenormal.z = mesh->faceAndVertexNormal[faceIndex].faceNormal.z;

                        p1normal = p1normal.add(facenormal);
                        
                    }
                    //get average value for this vertex
                    p1normal.x = p1normal.x / p1faceNum;
                    p1normal.y = p1normal.y / p1faceNum;
                    p1normal.z = p1normal.z / p1faceNum;

                    mesh->faceAndVertexNormal[i].faceVertexNoraml[1].x = p1normal.x;
                    mesh->faceAndVertexNormal[i].faceVertexNoraml[1].y = p1normal.y;
                    mesh->faceAndVertexNormal[i].faceVertexNoraml[1].z = p1normal.z;
                }



                int p2faceNum = indexlist[vertexIndex.d[2]].size();//how many faces use this vertex
                SE_Vector3f p2normal;
                for(int i = 0; i < p2faceNum; ++i)
                {
                    SE_Vector3f facenormal;                    
                    int faceIndex = indexlist[vertexIndex.d[2]][i];
                    //get face normal
                    facenormal.x = mesh->faceAndVertexNormal[faceIndex].faceNormal.x;
                    facenormal.y = mesh->faceAndVertexNormal[faceIndex].faceNormal.y;
                    facenormal.z = mesh->faceAndVertexNormal[faceIndex].faceNormal.z;

                    p2normal = p2normal.add(facenormal);
                    
                }
                //get average value for this vertex
                p2normal.x = p2normal.x / p2faceNum;
                p2normal.y = p2normal.y / p2faceNum;
                p2normal.z = p2normal.z / p2faceNum;

                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].x = p2normal.x;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].y = p2normal.y;
                mesh->faceAndVertexNormal[i].faceVertexNoraml[2].z = p2normal.z;

            }
        }
#endif

/*
        //write face normal
        SE_Matrix3f t = worldToModelM.toMatrix3f();
        SE_Matrix3f inverse = t.inverse();
        inverse = inverse.transpose();
        inverse.identity();
        for(int i = 0; i < mesh->faceAndVertexNormal.size(); ++i)
        {
            SE_Vector3f facenormal(mesh->faceAndVertexNormal[i].faceNormal.x, mesh->faceAndVertexNormal[i].faceNormal.y, mesh->faceAndVertexNormal[i].faceNormal.z);

                SE_Vector3f p = inverse.map(facenormal);          
                output.writeFloat(p.x);
                output.writeFloat(p.y);
                output.writeFloat(p.z);

                if(1)
                {
                    LOGI("face normal [%d].x = %f \n",i,p.x);
                    LOGI("face normal [%d].y = %f \n",i,p.y);
                    LOGI("face normal [%d].z = %f \n",i,p.z);
                }

        }
        */

        //write face vertex normal,the data is sort by face
        for(int i = 0; i < mesh->faceAndVertexNormal.size(); ++i)
        {                

                SE_Vector3f p(mesh->faceAndVertexNormal[i].faceVertexNoraml[0].x, mesh->faceAndVertexNormal[i].faceVertexNoraml[0].y, mesh->faceAndVertexNormal[i].faceVertexNoraml[0].z);
                //p = inverse.map(p);
                output.writeFloat(p.x);
                output.writeFloat(p.y);
                output.writeFloat(p.z);

                SE_Vector3f p1(mesh->faceAndVertexNormal[i].faceVertexNoraml[1].x, mesh->faceAndVertexNormal[i].faceVertexNoraml[1].y, mesh->faceAndVertexNormal[i].faceVertexNoraml[1].z);
                //p1 = inverse.map(p1);
                output.writeFloat(p1.x);
                output.writeFloat(p1.y);
                output.writeFloat(p1.z);

                SE_Vector3f p2(mesh->faceAndVertexNormal[i].faceVertexNoraml[2].x, mesh->faceAndVertexNormal[i].faceVertexNoraml[2].y, mesh->faceAndVertexNormal[i].faceVertexNoraml[2].z);
                //p2 = inverse.map(p2);
                output.writeFloat(p2.x);
                output.writeFloat(p2.y);
                output.writeFloat(p2.z);

        }
        
    }

    ////////////////////////write texture coordinate///////////////////////////////////////////////
    output.writeShort(SE_TEXCOORDDATA_ID);
    output.writeInt(geomDataNum);
    n = 0;
    for(itGeomObj = mSceneObject->mGeomObjects.begin();
    itGeomObj != mSceneObject->mGeomObjects.end();
    itGeomObj++)
    {
        ASE_GeometryObject* go = *itGeomObj;
        ASE_Mesh* mesh = go->mesh;

        std::string tcidstr = go->name;
        SE_TextureCoordDataID tcid = tcidstr.c_str();//SE_Application::getInstance()->createCommonID();
        //SE_Util::sleep(SLEEP_COUNT);
        tcid.write(output);
        geomTexCoordData[n++].texCoordID = tcid;
        output.writeInt(mesh->numTVertexes);

        if(mesh->numTVertexes > 0)        
        {
            
            output.writeInt(mesh->numFaces);
        }
        else
        {
            output.writeInt(0);
        }
        int i;
        if(mesh->numTVertexes > 0)
        {
            for(i = 0 ; i < mesh->numTVertexes ; i++)
            {
                output.writeFloat(mesh->tvertexes[i].s);
                output.writeFloat(mesh->tvertexes[i].t);
            }
            for(i = 0 ; i < mesh->numFaces ; i++)
            {
                output.writeInt(mesh->tfaces[i].vi[0]);
                output.writeInt(mesh->tfaces[i].vi[1]);
                output.writeInt(mesh->tfaces[i].vi[2]);
            }
        }
    }

    /////////////////write vertex buffer ///////// 
    output.writeShort(SE_VERTEXBUFFER_ID);
    output.writeInt(geomDataNum);
    n = 0;
    for(itGeomObj = mSceneObject->mGeomObjects.begin();
    itGeomObj != mSceneObject->mGeomObjects.end();
    itGeomObj++)
    {
        
        ASE_GeometryObject* go = *itGeomObj;
        ASE_Mesh* mesh = go->mesh;

        if(mesh->numFaceGroup > 0)
        {
            //save surface num
            output.writeInt(mesh->numFaceGroup);

            for(int i = 0; i < mesh->numFaceGroup; ++i)
            {
                SE_VertexBuffer vb;
                //generate every surface data

        SE_VertexBufferID vbid = SE_Application::getInstance()->createCommonID();
                //geomTexCoordData[n++].vbID = vbid;//cgh do not save id in here
                mesh->surfacesVbID.push_back(vbid);
                vbid.write(output);

                int faceNum = mesh->faceGroup[i].size();   

                std::list<int>::iterator it;
                int k = 0;
                int faceIndex = 0;

                _Vector3i *tfaces = new _Vector3i[faceNum];
                _Vector2f *textureCoord = new _Vector2f[faceNum * 3];

                //generate every surface texture coordinate data
                k = 0;
                for( it = mesh->faceGroup[i].begin();it != mesh->faceGroup[i].end();it++)
                {
                    //copy correct texture coordinate
                    textureCoord[k].d[0] = mesh->tvertexes[mesh->tfaces[(*it)].vi[0]].s;
                    textureCoord[k].d[1] = mesh->tvertexes[mesh->tfaces[(*it)].vi[0]].t;

                    k++;

                    textureCoord[k].d[0] = mesh->tvertexes[mesh->tfaces[(*it)].vi[1]].s;
                    textureCoord[k].d[1] = mesh->tvertexes[mesh->tfaces[(*it)].vi[1]].t;

                    k++;

                    textureCoord[k].d[0] = mesh->tvertexes[mesh->tfaces[(*it)].vi[2]].s;
                    textureCoord[k].d[1] = mesh->tvertexes[mesh->tfaces[(*it)].vi[2]].t;

                    k++;
                    
                }

                //generate every surface texture faces data
                k = 0;
                faceIndex = 0;
                for( it = mesh->faceGroup[i].begin();it != mesh->faceGroup[i].end();it++)
                {
                    tfaces[k].d[0] = faceIndex;//mesh->tfaces[(*it)].vi[0];
                    ++faceIndex;
                    

                    tfaces[k].d[1] = faceIndex;//mesh->tfaces[(*it)].vi[1];
                    ++faceIndex;
                    

                    tfaces[k].d[2] = faceIndex;//mesh->tfaces[(*it)].vi[2];
                    LOGI("tface[%d][012] = %d,%d,%d\n",k,tfaces[k].d[0],tfaces[k].d[1],tfaces[k].d[2]);
                    ++faceIndex;
                    k++;

                }

                rotateAxis.x = go->rotateAxis[0];
                rotateAxis.y = go->rotateAxis[1];
                rotateAxis.z = go->rotateAxis[2];
                scale.x = go->scale[0];
                scale.y = go->scale[1];
                scale.z = go->scale[2];
                translate.x = go->translate[0];
                translate.y = go->translate[1];
                translate.z = go->translate[2];
                rotateQ.set(go->rotateAngle, rotateAxis);
                rotateM = rotateQ.toMatrix3f();//.setRotateFromAxis(go->rotateAngle, rotateAxis);
                modelToWorldM.set(rotateM, scale, translate);
                worldToModelM = modelToWorldM.inverse();


                _Vector3f * vertexpos = new _Vector3f[faceNum*3];
                _Vector3i *faces = new _Vector3i[faceNum];

                _Vector3f *normal = new _Vector3f[faceNum*3];
                

                //generate every surface vertexpos data
                k = 0;
                for( it = mesh->faceGroup[i].begin();it != mesh->faceGroup[i].end();it++)
                {
                    SE_Vector4f p(mesh->vertexes[mesh->faces[(*it)].vi[0]].x, mesh->vertexes[mesh->faces[(*it)].vi[0]].y, mesh->vertexes[mesh->faces[(*it)].vi[0]].z, 1.0f);
                    p = worldToModelM.map(p);
                    //copy correct vertex
                    vertexpos[k].d[0] = p.x;
                    vertexpos[k].d[1] = p.y;
                    vertexpos[k].d[2] = p.z;

                    k++;

                    SE_Vector4f p1(mesh->vertexes[mesh->faces[(*it)].vi[1]].x, mesh->vertexes[mesh->faces[(*it)].vi[1]].y, mesh->vertexes[mesh->faces[(*it)].vi[1]].z, 1.0f);
                    p1 = worldToModelM.map(p1);
                    vertexpos[k].d[0] = p1.x;
                    vertexpos[k].d[1] = p1.y;
                    vertexpos[k].d[2] = p1.z;

                    k++;

                    SE_Vector4f p2(mesh->vertexes[mesh->faces[(*it)].vi[2]].x, mesh->vertexes[mesh->faces[(*it)].vi[2]].y, mesh->vertexes[mesh->faces[(*it)].vi[2]].z, 1.0f);
                    p2 = worldToModelM.map(p2);
                    vertexpos[k].d[0] = p2.x;
                    vertexpos[k].d[1] = p2.y;
                    vertexpos[k].d[2] = p2.z;

                    k++;
                }

                //generate every surface normal data
                k = 0;
                for( it = mesh->faceGroup[i].begin();it != mesh->faceGroup[i].end();it++)
                {
                    int _faceIndex = *it;
                    SE_Vector3f p(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].x, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].z);
                    //copy correct vertex
                    normal[k].d[0] = p.x;
                    normal[k].d[1] = p.y;
                    normal[k].d[2] = p.z;

                    k++;

                    SE_Vector3f p1(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].x,
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].z);
                    normal[k].d[0] = p1.x;
                    normal[k].d[1] = p1.y;
                    normal[k].d[2] = p1.z;

                    k++;

                    SE_Vector3f p2(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].x, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].z);
                    normal[k].d[0] = p2.x;
                    normal[k].d[1] = p2.y;
                    normal[k].d[2] = p2.z;

                    k++;
                }

                //generate every surface faces data
                k = 0;
                faceIndex = 0;
                for( it = mesh->faceGroup[i].begin();it != mesh->faceGroup[i].end();it++)
                {
                    faces[k].d[0] = faceIndex;//mesh->faces[(*it)].vi[0];
                    ++faceIndex;
                    faces[k].d[1] = faceIndex;//mesh->faces[(*it)].vi[1];
                    ++faceIndex;
                    faces[k].d[2] = faceIndex;//mesh->faces[(*it)].vi[2];
                    ++faceIndex;
                    k++;
                }


                vb.addVBState(SE_VertexBuffer::VBS_VERTEXPOS);
                //if there is a surface,must has a texture on it.
                vb.addVBState(SE_VertexBuffer::VBS_TEXTURE0);


                vb.createVertexBuffer(go->name,faceNum,faces,vertexpos,tfaces,textureCoord,NULL,NULL,normal);

                output.writeInt(vb.getVBState());

                int vertexdataNum = vb.vertexDataNum;
                output.writeInt(vertexdataNum);
                
                if(vb.hasVBState(SE_VertexBuffer::VBS_TEXTURE0))
                {
                    for(int i = 0; i < vertexdataNum; ++i)
                    {
                        output.writeFloat(vb.vertexData[i]);
                    }
                }
                else if(vb.hasVBState(SE_VertexBuffer::VBS_VERTEXPOS))
                {
                    for(int i = 0; i < vertexdataNum; ++i)
                    {
                        output.writeFloat(vb.vertexData[i]);
                    }
                }

                int indexdataNum = vb.indexNum;
                output.writeInt(indexdataNum);
                for(int i = 0; i < indexdataNum; ++i)
                {
                    output.writeInt(vb.indexData[i]);
                }

                //release
                delete[] vertexpos;
                delete[] faces;
                delete[] normal;

                if(vb.hasVBState(SE_VertexBuffer::VBS_TEXTURE0))
                {
                   delete[] tfaces;
                   delete[] textureCoord;
                }

            }
        }
        else
        {

            //save surface num
            output.writeInt(1);//just have 1 surface.

            SE_VertexBuffer vb;
            SE_VertexBufferID vbid = SE_Application::getInstance()->createCommonID();

            mesh->surfacesVbID.push_back(vbid);
        vbid.write(output);

        _Vector3f * vertexpos = new _Vector3f[mesh->numVertexes];
        _Vector3i *faces = new _Vector3i[mesh->numFaces];

            _Vector3f *normal = new _Vector3f[mesh->numFaces*3];

        _Vector3i *tfaces = NULL;
        _Vector2f *textureCoord = NULL;

        if(mesh->numVertexes > 0)
        {
            vb.addVBState(SE_VertexBuffer::VBS_VERTEXPOS);
        }

        if(mesh->numTVertexes > 0)
        {
            vb.addVBState(SE_VertexBuffer::VBS_TEXTURE0);

            tfaces = new _Vector3i[mesh->numFaces];
            textureCoord = new _Vector2f[mesh->numTVertexes];

            for(int i = 0; i < mesh->numTVertexes; ++i)
            {
                textureCoord[i].d[0] = mesh->tvertexes[i].s;
                textureCoord[i].d[1] = mesh->tvertexes[i].t;
            }

            for(int i = 0; i < mesh->numFaces; ++i)
            {
                tfaces[i].d[0] = mesh->tfaces[i].vi[0];
                tfaces[i].d[1] = mesh->tfaces[i].vi[1];
                tfaces[i].d[2] = mesh->tfaces[i].vi[2];

            }
        }


        rotateAxis.x = go->rotateAxis[0];
        rotateAxis.y = go->rotateAxis[1];
        rotateAxis.z = go->rotateAxis[2];
        scale.x = go->scale[0];
        scale.y = go->scale[1];
        scale.z = go->scale[2];
        translate.x = go->translate[0];
        translate.y = go->translate[1];
        translate.z = go->translate[2];
        rotateQ.set(go->rotateAngle, rotateAxis);
        rotateM = rotateQ.toMatrix3f();//.setRotateFromAxis(go->rotateAngle, rotateAxis);
        modelToWorldM.set(rotateM, scale, translate);
        worldToModelM = modelToWorldM.inverse();
        

        for(int i = 0 ; i < mesh->numVertexes ; ++i)
        {
            SE_Vector4f p(mesh->vertexes[i].x, mesh->vertexes[i].y, mesh->vertexes[i].z, 1.0f);
            p = worldToModelM.map(p);
            vertexpos[i].d[0] = p.x;
            vertexpos[i].d[1] = p.y;
            vertexpos[i].d[2] = p.z;
        }

        for(int i = 0 ; i < mesh->numFaces ; ++i)
        {
            faces[i].d[0] = mesh->faces[i].vi[0];
            faces[i].d[1] = mesh->faces[i].vi[1];
            faces[i].d[2] = mesh->faces[i].vi[2];
        }

            int k = 0;
            for(int i = 0 ; i < mesh->numFaces ; ++i)
            {
               int _faceIndex = i;
                SE_Vector3f p(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].x, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[0].z);
                    //copy correct vertex
                    normal[k].d[0] = p.x;
                    normal[k].d[1] = p.y;
                    normal[k].d[2] = p.z;

                    k++;

                    SE_Vector3f p1(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].x,
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[1].z);
                    normal[k].d[0] = p1.x;
                    normal[k].d[1] = p1.y;
                    normal[k].d[2] = p1.z;

                    k++;

                    SE_Vector3f p2(mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].x, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].y, 
                                    mesh->faceAndVertexNormal[_faceIndex].faceVertexNoraml[2].z);
                    normal[k].d[0] = p2.x;
                    normal[k].d[1] = p2.y;
                    normal[k].d[2] = p2.z;

                    k++;
            }

            vb.createVertexBuffer(go->name,mesh->numFaces,faces,vertexpos,tfaces,textureCoord,NULL,NULL,normal);

        output.writeInt(vb.getVBState());

        int vertexdataNum = vb.vertexDataNum;
        output.writeInt(vertexdataNum);
        
        if(vb.hasVBState(SE_VertexBuffer::VBS_TEXTURE0))
        {
            for(int i = 0; i < vertexdataNum; ++i)
            {
                output.writeFloat(vb.vertexData[i]);
            }
        }
        else if(vb.hasVBState(SE_VertexBuffer::VBS_VERTEXPOS))
        {
            for(int i = 0; i < vertexdataNum; ++i)
            {
                output.writeFloat(vb.vertexData[i]);
            }
        }

        int indexdataNum = vb.indexNum;
        output.writeInt(indexdataNum);
        for(int i = 0; i < indexdataNum; ++i)
        {
            output.writeInt(vb.indexData[i]);
        }

        //release
        delete[] vertexpos;
        delete[] faces;
            delete[] normal;

        if(vb.hasVBState(SE_VertexBuffer::VBS_TEXTURE0))
        {
           delete[] tfaces;
           delete[] textureCoord;
        }

    }

    }   

///////////////////// write shader program ////
    output.writeShort(SE_SHADERPROGRAMDATA_ID);
    int spNum = mSceneObject->mShaderObjects.size();
    output.writeInt(spNum);// shader program num;
    std::vector<SE_ProgramDataID> programDataVector(spNum);
    for(i = 0 ; i < spNum ; i++)
    {
        SE_ProgramDataID proID = SE_ProgramDataID(mSceneObject->mShaderObjects[i]->shaderID.c_str());//"main_shader";
        std::string vsn = mSceneObject->mShaderObjects[i]->vertexShaderName;
        std::string fsn = mSceneObject->mShaderObjects[i]->fragmentShaderName;
        programDataVector[i] = proID;
        proID.write(output);
        std::string str(shaderPath);
        std::string vertexShaderPath = str + SE_SEP + "shader" + SE_SEP + vsn;
        std::string fragmentShaderPath = str + SE_SEP + "shader" + SE_SEP + fsn;
        char* vertexShader = NULL;
        int vertexShaderLen = 0;
        char* fragmentShader = NULL;
        int fragmentShaderLen = 0;
        SE_IO::readFileAll(vertexShaderPath.c_str(), vertexShader, vertexShaderLen);
        SE_IO::readFileAll(fragmentShaderPath.c_str(), fragmentShader, fragmentShaderLen);
        output.writeString(mSceneObject->mShaderObjects[i]->shaderClassName.c_str());
        output.writeInt(vertexShaderLen);
        output.writeInt(fragmentShaderLen);
        output.writeBytes(vertexShader, vertexShaderLen);
        output.writeBytes(fragmentShader, fragmentShaderLen);
        delete[] vertexShader;
        delete[] fragmentShader;
    }
    /////////////// write renderer ///////
    output.writeShort(SE_RENDERERINFO_ID);
    int rendererNum = mSceneObject->mRendererObjects.size();
    output.writeInt(rendererNum);
    std::vector<SE_RendererID> rendererIDVector(rendererNum);
    for(i = 0 ; i < rendererNum ; i++)
    {
        ASE_Renderer* renderer = mSceneObject->mRendererObjects[i];
        rendererIDVector[i] = renderer->rendererID.c_str();
        output.writeString(renderer->rendererID.c_str());
        output.writeString(renderer->rendererClassName.c_str());
    }
///////////////////// write mesh //////////////// 

//add by liusong begin
    ASE_GeometryObjectGroup* group = findGroup("Dummy01");
    if (group) {
        mFrameMax = 0;
        findMaxFrameIndex(group);
        fillFrame(group);
        processMatrix(group);
        processNewMatrix(group, NULL);
    }
//add by liusong end
        //meshIDVector.resize(geomDataNum);
    output.writeShort(SE_MESHDATA_ID);
    output.writeInt(geomDataNum);
    n = 0;
    for(itGeomObj = mSceneObject->mGeomObjects.begin();
    itGeomObj != mSceneObject->mGeomObjects.end();
    itGeomObj++)
    {
        ASE_GeometryObject* go = *itGeomObj;
//add by liusong begin
        fillKeyMesh(go);
//add by liusong end
        ASE_Mesh* mesh = go->mesh;

            //SE_TimeUS currTime = SE_Time::getCurrentTimeUS();
            //std::stringstream ss;        
        std::string meshidstr = go->name;
            //ss<<currTime<<meshidstr;
            //ss>>meshidstr;
        SE_MeshID meshID = meshidstr.c_str();//SE_Application::getInstance()->createCommonID();
        //SE_Util::sleep(SLEEP_COUNT);
        meshID.write(output);
            meshIDVector.push_back(meshID);
        SE_GeometryDataID geomID = geomTexCoordData[n].geomID;
        SE_TextureCoordDataID texCoordID = geomTexCoordData[n].texCoordID;

        n++;
        geomID.write(output);

//////////////////////////////////add by liusong begin////////////////////////////

    int keyFrameSize = go->keyFrames.size();
    output.writeInt(keyFrameSize);
    if (keyFrameSize > 0)
    {
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        for(it = go->keyFrames.begin() ; it != go->keyFrames.end() ; it++)
            {
        SE_KeyFrame<ASE_Transform>* kf = *it;
        output.writeInt(kf->key);
        float* out = new float[16];
        kf->data.matrix.getSequence(out, 16);
        for(int i = 0; i < 16; i++)
        {
            output.writeFloat(out[i]);
        }
        ASE_Mesh* keyMesh = findAnimMesh(go->keyMeshs, kf->key);
        if(keyMesh) {
            output.writeInt(1);
            output.writeInt(keyMesh->numVertexes);
            for(i = 0 ; i < keyMesh->numVertexes ; i++)
            {
                SE_Vector4f p(keyMesh->vertexes[i].x, keyMesh->vertexes[i].y, keyMesh->vertexes[i].z, 1.0f);
                p = kf->data.matrix.inverse().map(p);
                output.writeFloat(p.x);
                output.writeFloat(p.y);
                output.writeFloat(p.z);
            }
        } else {
            output.writeInt(-1);
        }
            }
    }
///////////////////////////////add by liusong end///////////////////////////////////

        output.writeFloat(go->wireframeColor[0]);
        output.writeFloat(go->wireframeColor[1]);
        output.writeFloat(go->wireframeColor[2]);
        int texNum = 0;
        int materialref = go->materialref;
        int startpos = 0;
        int subMaterialStartPos = 0;
        _MaterialData mdData;
		std::vector<int> subMaterialHasTexV;
		std::list<int> subMaterialHasTex;
        if(materialref == -1)
        {
            output.writeInt(texNum);
            goto WRIET_SURFACE;
        }
        mdData = materialVector[materialref];
        if(mdData.subMaterialNum > 0)
        {
            int j;
            for(j = 0 ; j < (materialref - 1) ; j++)
            {
                _MaterialData d = materialVector[j];
                startpos += d.subMaterialNum;
            }
            int k = startpos;
            for(int j = 0 ; j < mdData.subMaterialNum ; j++)
            {
                _MaterialData subMaterialData = materialVector[materialNum + k];
                k++;
                std::string texStr(subMaterialData.md.texName);
                if(texStr != "")
                {
                    texNum++;
					subMaterialHasTex.push_back(k);
                }
            }
        }        
        else
        {
            std::string texStr(mdData.md.texName);
            if(texStr != "")
            {
                texNum = 1;
            }
#if 0
            //for specular
            std::string sptexStr(mdData.mSpecularData.texName);
            if(sptexStr != "")
            {
                texNum = 2;
            }
#endif
        }
		subMaterialHasTexV.resize(subMaterialHasTex.size());
		copy(subMaterialHasTex.begin(), subMaterialHasTex.end(), subMaterialHasTexV.begin());
        output.writeInt(texNum);
            if(mdData.subMaterialNum > 0)
            {
                int j;
            //for(j = 0 ; j < (materialref - 1) ; j++)
            //{
             //   _MaterialData d = materialVector[j];
             //   subMaterialStartPos += d.subMaterialNum;
            //}
            for(int j = 0 ; j < subMaterialHasTexV.size() ; j++)
                {
                _MaterialData subMaterialData = materialVector[materialNum + subMaterialHasTexV[j] - 1];
                //subMaterialStartPos++;
                    std::string texStr(subMaterialData.md.texName);
                    if(texStr != "")
                    {
                        output.writeInt(1);//current we just has one texture unit;
                        output.writeInt(0);//texture unit type is TEXTURE0
                        texCoordID.write(output);
                        output.writeInt(1);//image num use in the texture unit. current it is not mipmap. so the num is 1
                        subMaterialData.tid.write(output);
                        output.writeFloat(subMaterialData.md.u_tiling);//write utiling
                        output.writeFloat(subMaterialData.md.v_tiling);//write vtiling

                    }
                }
            }
            else
            {               
                std::string texStr(mdData.md.texName);
                //for specular
                std::string sptexStr(mdData.mSpecularData.texName);
                if(texStr != "")
                {
                    //for specular
                    if(sptexStr != "")
                    {
                        output.writeInt(2);//current we just has 2 texture unit;

                        output.writeInt(0);//texture unit type is TEXTURE0
                        texCoordID.write(output);
                        output.writeInt(1);//image num use in the texture unit. current it is not mipmap. so the num is 1
                        mdData.tid.write(output); 
                        output.writeFloat(mdData.md.u_tiling);//write utiling
                        output.writeFloat(mdData.md.v_tiling);//write vtiling

                        output.writeInt(1);//texture unit type is TEXTURE1
                        texCoordID.write(output);
                        output.writeInt(1);//image num use in the texture unit. current it is not mipmap. so the num is 1
                        mdData.sptid.write(output); 
                        output.writeFloat(1.0);//write specular utiling should be 1.0
                        output.writeFloat(1.0);//write specular vtiling should be 1.0
                        
                    }
                    else
                    {
                    output.writeInt(1);//current we just has one texture unit;
                    output.writeInt(0);//texture unit type is TEXTURE0
                    texCoordID.write(output);
                    output.writeInt(1);//image num use in the texture unit. current it is not mipmap. so the num is 1
                        mdData.tid.write(output); 
                        output.writeFloat(mdData.md.u_tiling);//write utiling
                        output.writeFloat(mdData.md.v_tiling);//write vtiling
                    }
                }
                else
                {
                    //if no texture image,the texture coordinate data should be record.
                    texCoordID.write(output);
                }
           }
        ///write surface
WRIET_SURFACE:
        if(mesh->numFaceGroup > 0)
        {
            SE_ASSERT(mesh->numFaceGroup <= mesh->faceGroup.size());
            output.writeInt(mesh->numFaceGroup);
            std::vector<std::list<int> >::iterator itFaceGroup;
            int indexM = startpos;
            int texIndex = 0;
            int vbindex = 0;
            for(itFaceGroup = mesh->faceGroup.begin() ; itFaceGroup != mesh->faceGroup.end(); itFaceGroup++)
            {
				if(itFaceGroup->size() == 0)
					continue;
                //write vbid
                mesh->surfacesVbID[vbindex].write(output);

                _MaterialData md = materialVector[materialNum + indexM];
                std::string texStr(md.md.texName);
                md.mid.write(output);
                output.writeInt(itFaceGroup->size());
                std::list<int>::iterator itFace;
                for(itFace = itFaceGroup->begin() ; itFace != itFaceGroup->end() ; 
                    itFace++)
                {
                    output.writeInt(*itFace);
                }
                programDataVector[0].write(output);
                output.writeString(DEFAULT_RENDERER);
                if(texStr != "")
                {
                    output.writeInt(texIndex);
                }
                else
                {
                    output.writeInt(-1);
                }
                indexM++;
                texIndex++;
                vbindex++;
            }
        } 
        else
        {
            output.writeInt(1); //just has one surface
            //write vbid
            mesh->surfacesVbID[0].write(output);           

            std::string texStr(mdData.md.texName);
            mdData.mid.write(output);
            output.writeInt(mesh->numFaces); // facets num;
            for(int f = 0 ; f < mesh->numFaces ; f++)
                output.writeInt(f);
            programDataVector[0].write(output);
            output.writeString(DEFAULT_RENDERER);
            if(texStr != "")
            {
                output.writeInt(0); // the texture index is 0;
            }
            else
            {
                output.writeInt(-1);
            }
        }
    }
    // write bone animation data //
    if(mSceneObject->mSkinJointController.size() > 0)
    {
        output.writeShort(SE_SKINJOINTCONTROLLER_ID);
        output.writeInt(mSceneObject->mSkinJointController.size());
        std::list<ASE_SkinJointController*>::iterator itSkinJointController;
        for(itSkinJointController = mSceneObject->mSkinJointController.begin() ; 
            itSkinJointController != mSceneObject->mSkinJointController.end() ; 
            itSkinJointController++)
        {
            ASE_SkinJointController* skinJointController = *itSkinJointController;
            output.writeInt(skinJointController->jointVector.size());
            for(int j = 0 ; j < skinJointController->jointVector.size() ; j++)
            {
                ASE_Bone* bone = skinJointController->jointVector[j];
                output.writeString(bone->name.c_str());
                output.writeInt(bone->matrixseqnum);
                for(int n = 0 ; n < bone->matrixseqnum ; n++)
                {
                    output.writeFloatArray(bone->matrixseq[n].m, 16);     
                }
                output.writeFloatArray(bone->matrixbase.m, 16);
            }
            for(int j = 0 ; j < skinJointController->jointVector.size() ; j++)
            {
                ASE_Bone* bone = skinJointController->jointVector[j];
                output.writeInt(bone->children.size());
                std::list<ASE_Bone*>::iterator it ;
                for(it = bone->children.begin() ; it != bone->children.end() ; it++)
                {
                    ASE_Bone* childBone = *it;
                    output.writeString(childBone->name.c_str());
                }
                /*
                if(bone->parent)
                {
                    output.writeString(bone->parent->name.c_str());
                }
                else
                {
                    output.writeString("####");
                }
                */
            }
            output.writeString(skinJointController->objName.c_str());
            output.writeInt(skinJointController->vertexJointVector.size());
            if(skinJointController->vertexJointVector.size() > 0)
            {
                for(int i = 0 ; i < skinJointController->vertexJointVector.size() ; i++)
                {
                    output.writeInt(skinJointController->vertexJointVector[i].size());
                    for(int j = 0 ; j < skinJointController->vertexJointVector[i].size(); j++)
                    {
                        output.writeInt(skinJointController->vertexJointVector[i][j].boneIndex);
                        output.writeFloat(skinJointController->vertexJointVector[i][j].weight);
    
                    }
                }
            }
        }
    }
    // write biped animation data //
    if(mSceneObject->mBipsController.size() > 0)
    {
        output.writeShort(SE_BIPEDCONTROLLER_ID);
        output.writeInt(mSceneObject->mBipsController.size());

        std::vector<ASE_BipedController*>::iterator itBipedController;
        for(itBipedController = mSceneObject->mBipsController.begin() ; 
            itBipedController != mSceneObject->mBipsController.end() ; 
            itBipedController++)
        {
            ASE_BipedController* bipedController = *itBipedController;

            output.writeString(bipedController->controllerId.c_str());

            output.writeInt(bipedController->oneBipAnimation.size());

            for(int i = 0; i < bipedController->oneBipAnimation.size(); ++i)
            {
                ASE_Biped *biped = bipedController->oneBipAnimation[i];
                output.writeString(biped->bipName.c_str());

                output.writeFloatArray(biped->bind_pose.m, 16);

                output.writeInt(biped->animationInfo.size());

                for(int j = 0; j < biped->animationInfo.size(); ++j)
                {
                    ASE_BipedKeyFrame *oneFrame = biped->animationInfo[j];

                    output.writeInt(oneFrame->frameIndex); 

                    output.writeFloat(oneFrame->rotateQ.x);
                    output.writeFloat(oneFrame->rotateQ.y);
                    output.writeFloat(oneFrame->rotateQ.z);
                    output.writeFloat(oneFrame->rotateQ.w);

                    output.writeFloat(oneFrame->translate.x);
                    output.writeFloat(oneFrame->translate.y);
                    output.writeFloat(oneFrame->translate.z);

                    output.writeFloat(oneFrame->scale.x);
                    output.writeFloat(oneFrame->scale.y);
                    output.writeFloat(oneFrame->scale.z);
                    
                }
            }

            //write child info
            for(int i = 0; i < bipedController->oneBipAnimation.size(); ++i)
            {
                ASE_Biped *biped = bipedController->oneBipAnimation[i];
                output.writeInt(biped->children.size());

                std::list<ASE_Biped*>::iterator it ;
                for(it = biped->children.begin() ; it != biped->children.end() ; it++)
                {
                    ASE_Biped *childbiped = *it;
                    output.writeString(childbiped->bipName.c_str());
                }
            }

            //write vertex and weight info
            output.writeInt(bipedController->bipAndObjInfo.size());
            for(int i = 0; i < bipedController->bipAndObjInfo.size(); ++i)
            {
                ASE_SkeletonUnit * su = bipedController->bipAndObjInfo[i];
                output.writeInt(su->bipedNum);

                su->objHasBiped += "@" + namecbf;
                output.writeString(su->objHasBiped.c_str());
                output.writeString(su->controllerId.c_str());

                //write bipName array
                for(int j = 0; j < su->bipedNamesOnObj.size(); ++j)
                {
                    output.writeString(su->bipedNamesOnObj[j].c_str());
                }

                //write vertex num
                output.writeInt(su->vertexNum);
                for(int j = 0; j < su->objVertexBlendInfo.size(); ++j)
                {
                    ASE_BipedWeight *w = su->objVertexBlendInfo[j];
                    //write vertex-weight-biped info
                    output.writeInt(w->bipedIndex.size());

                    for(int k = 0; k < w->bipedIndex.size(); ++k)
                    {
                        //write weight and index
                        output.writeInt(w->bipedIndex[k]);
                        output.writeFloat(w->weight[k]);
                    }

                }               


            }

        }
        
    }

    ///////write camera data///////
    if(mSceneObject->mBestPositon.size() > 0)
    {
        output.writeShort(SE_CAMERADATA_ID);
        output.writeInt(mSceneObject->mBestPositon.size());

        std::vector<ASE_CameraObjectPos*>::iterator it;
        for(it = mSceneObject->mBestPositon.begin() ; 
            it != mSceneObject->mBestPositon.end() ; 
            it++)
        {
            ASE_CameraObjectPos* cameraData = *it;

            output.writeString(cameraData->mCamraName.c_str());
            output.writeString(cameraData->mCameraType.c_str());            
            output.writeString(cameraData->mCameraTargetName.c_str());

            //write camera pos
            output.writeFloat(cameraData->mCameraPos.x);
            output.writeFloat(cameraData->mCameraPos.y);
            output.writeFloat(cameraData->mCameraPos.z);

            //write camera target pos
            output.writeFloat(cameraData->mCameraTargetPos.x);
            output.writeFloat(cameraData->mCameraTargetPos.y);
            output.writeFloat(cameraData->mCameraTargetPos.z);
        }
    }

    }

    

    /////// create scene //////////
    std::string fullpathname = cbfName;
    size_t pos = fullpathname.rfind('\\');
    std::string namecbf = fullpathname.substr(pos+1, fullpathname.size());

    SE_SpatialID spatialID = SE_Application::getInstance()->createCommonID();
    SE_CommonNode* rootNode = new SE_CommonNode(spatialID, NULL);
    rootNode->setBVType(AABB);
    std::string rootName = std::string("root") + "@" +namecbf;
    rootNode->setSpatialName(rootName.c_str());
    int n = 0;
    std::list<ASE_GeometryObject*>::iterator itGeomObj;

    mSceneObject = mSceneList[0];//normal model data
    for(itGeomObj = mSceneObject->mGeomObjects.begin();
    itGeomObj != mSceneObject->mGeomObjects.end();
    itGeomObj++)
    {
        ASE_GeometryObject* go = *itGeomObj;
        ASE_Mesh* mesh = go->mesh;
        SE_MeshID meshID = meshIDVector[n++];
        SE_SpatialID childID = SE_ID::createSpatialID();
        SE_Spatial* child = NULL;//fix a mem leak bug;
        SE_CommonNode* groupNode = NULL;
        SE_CommonNode *lodNode = NULL;
        
        std::string lodName;
        if(!go->mGroupName.empty())
        {
            
            size_t pos = go->mGroupName.rfind("_");
            lodName = go->mGroupName.substr(0, pos);
        }
        
        if(!go->mGroupName.empty())
        {
            bool islodnode = isLodGroup(go->mGroupName.c_str());
            if(islodnode)
            {
                /*
                if the geometry is a group member:
                1.new a CommondNode CM with a name,and root->add(CM)
                2.find the CM through name(maybe the next geometry is not a member of current CM)
                3.CM->add child,attach object
                */
                lodNode = (SE_CommonNode*)findGroupNode(rootNode,lodName.c_str(),LOD_NODE);
                if(!lodNode)
                {
                    //create a lod node
                    SE_SpatialID lodgroupID = SE_Application::getInstance()->createCommonID();
                    lodNode = new SE_LodNode(lodgroupID, rootNode);
                    rootNode->addChild(lodNode);
                    lodNode->setBVType(AABB);
                    lodNode->setGroupName(lodName.c_str());
                    lodNode->setNodeType(LOD_NODE);
                    
                    //set spatial name
                    lodNode->setSpatialName(lodName.c_str());
                }
            }
            
            if(!islodnode)
            {
                //the group is a child of the common node ROOT.
                groupNode = (SE_CommonNode*)findGroupNode(rootNode,go->mGroupName.c_str(),GROUP_NODE);
            }
            else
            {
                //the group is a child of the LOD node,must!
                groupNode = (SE_CommonNode*)findGroupNode(lodNode,go->mGroupName.c_str(),GROUP_NODE);
            }
            
            if(!groupNode)
            {
                SE_SpatialID groupID = SE_Application::getInstance()->createCommonID();                

                if(islodnode)
                {
                    groupNode = new SE_CommonNode(groupID, lodNode);
                    lodNode->addChild(groupNode);//if there is a lod node,it's child must be a group
                }
                else
                {
                    groupNode = new SE_CommonNode(groupID, rootNode);
                    rootNode->addChild(groupNode);
                }
                groupNode->setBVType(AABB);
                groupNode->setGroupName(go->mGroupName.c_str());
                groupNode->setNodeType(GROUP_NODE);

                //set spatial name
                groupNode->setSpatialName(go->mGroupName.c_str());
            }
            
                        
        }
        
        ASE_GeometryObjectGroup* parentGroup = findGroup(go->parentName);
        if(parentGroup) 
        {
            //child = new SE_NewGeometry(childID, rootNode);
        } 
        else if(groupNode)
        {
            child = new SE_Geometry(childID, groupNode); 
        }
        else
        {
            child = new SE_Geometry(childID, rootNode);
        }
        std::string mname = go->name;
        SE_Vector3f translate, scale, rotateAxis;
        translate.x = go->translate[0];
        translate.y = go->translate[1];
        translate.z = go->translate[2];
        scale.x = go->scale[0];
        scale.y = go->scale[1];
        scale.z = go->scale[2];
        rotateAxis.x = go->rotateAxis[0];
        rotateAxis.y = go->rotateAxis[1];
        rotateAxis.z = go->rotateAxis[2];
        SE_Quat q;
        q.set(go->rotateAngle, rotateAxis);
        SE_Matrix4f childMatrix;
        childMatrix.set(q.toMatrix3f(), scale, translate);
        std::list<ASE_GeometryObjectGroup*>::iterator itGroup;
        bool childAdded = false;
        if(parentGroup)
        {
            SE_Spatial* parentSpatial = parentGroup->parent.spatial;
            if(!parentSpatial)
            {
                SE_SpatialID parentid = SE_ID::createSpatialID();
                parentSpatial = new SE_CommonNode(parentid, rootNode);
                parentSpatial->setBVType(AABB);
                rootNode->addChild(parentSpatial);
                parentGroup->parent.spatial = parentSpatial;
            }
            parentSpatial->addChild(child);
            child->setParent(parentSpatial);
            std::list<ASE_GeometryObject*>::iterator itChildren = parentGroup->children.begin();
            for(; itChildren != parentGroup->children.end() ; itChildren++) 
            {
                ASE_GeometryObject* goChild = *itChildren;
                std::string newName = goChild->name;
                ASE_GeometryObjectGroup* childGroup = findGroup(newName);
                if(childGroup) 
                {
                    childGroup->parent.spatial = child;
                }
            }
            //SE_Vector3f translate, scale, rotateaxis;
            //float angle;
            //translate.x = group->parent.baseTranslate[0];
            //translate.y = group->parent.baseTranslate[1];
            //translate.z = group->parent.baseTranslate[2];
            //scale.x = group->parent.baseScale[0];
            //scale.y = group->parent.baseScale[1];
            //scale.z = group->parent.baseScale[2];
            //rotateaxis.x = group->parent.baseRotate[0];
            //rotateaxis.y = group->parent.baseRotate[1];
            //rotateaxis.z = group->parent.baseRotate[2];
            //angle = group->parent.baseRotate[3];
            //SE_Quat q;
            //q.set(angle, rotateaxis);
            //SE_Matrix4f parentMatrix;
            //parentMatrix.set(q.toMatrix3f(), scale, translate);
            //parentSpatial->setPrevMatrix(parentMatrix);
            //SE_Matrix4f parentMatrixInverse = parentMatrix.inverse();
            //childMatrix = parentMatrixInverse.mul(childMatrix);
            //child->setPrevMatrix(childMatrix);
            //child->setBVType(AABB);
            child->setLocalTranslate(translate);
            child->setLocalScale(scale);
            child->setLocalRotate(q);
            child->setBVType(AABB);
            childAdded = true;
        }

        if(!childAdded)
        {
            if(groupNode)
            {
                //the child is a member of a group,not root child
                groupNode->addChild(child);
            }
            else
            {
                rootNode->addChild(child);
            }
            child->setLocalTranslate(translate);
            child->setLocalScale(scale);
            child->setLocalRotate(q);
            child->setBVType(AABB);

            std::string goname = go->name;
            child->setSpatialName(goname.c_str());
        }

        SE_MeshSimObject* meshObj = new SE_MeshSimObject(meshID);
        meshObj->setName(go->name);
        child->attachSimObject(meshObj);
        //for release memory

        SE_SimObjectID id = SE_ID::createSimObjectID();
        meshObj->setID(id);

        /*if(!SE_Application::getInstance()->getSimObjectManager()->set(id, meshObj))
        {
            delete meshObj;
        }*/

        for(int meshid = 0; meshid < meshIDVector.size(); ++meshid)
        {
            std::string meshIDtmp = std::string(meshID.getStr()) + "simple";
            std::string meshIDStr = meshIDVector[meshid].getStr();

            if(meshIDtmp == meshIDStr)
            {
                SE_MeshID simple = meshIDtmp.c_str();
                SE_MeshSimObject* meshObjSimple  = new SE_MeshSimObject(simple);
                std::string simpleobjname = std::string(go->name) + "simple";
                meshObjSimple->setName(simpleobjname.c_str());
                child->attachSimObject(meshObjSimple);

                meshObjSimple->setNeedRender(false);
                SE_SimObjectID simpleid = SE_ID::createSimObjectID();
                meshObjSimple->setID(simpleid);
                /*if(!SE_Application::getInstance()->getSimObjectManager()->set(simpleid, meshObjSimple))
                {
                    delete meshObjSimple;
                }*/
                if(SE_Application::getInstance()->SEHomeDebug)
                LOGI("Found simple model mesh [%s]!\n\n ",simple.getStr());
                break;
            }
        }       
        
    }
    SE_SceneID sceneID = SE_Application::getInstance()->createCommonID();
    //SE_Util::sleep(SLEEP_COUNT);
    sceneID.write(outScene);
    _WriteSceneTravel wst(outScene);
    rootNode->travel(&wst, true);

    rootNode->unLoadSceneMustInvokeByCommand();
    delete rootNode;
    if(SE_Application::getInstance()->SEHomeDebug)
    LOGI("write end\n");
}
void ASE_Loader::findMaxFrameIndex(ASE_GeometryObjectGroup* group) {
    if (group->parent.keyFrames.size() > 0) {
    std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
    it = group->parent.keyFrames.end();
    it--;
    SE_KeyFrame<ASE_Transform>* kf = *it;
    if (mFrameMax < kf->key)
        mFrameMax = kf->key;
    }
    std::list<ASE_GeometryObject*>::iterator itChildren = group->children.begin();
    for(; itChildren != group->children.end() ; itChildren++) {
    ASE_GeometryObject* go = *itChildren;
    if (go->keyFrames.size() > 0) {
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        it = go->keyFrames.end();
        it--;
        SE_KeyFrame<ASE_Transform>* kf = *it;
        if (mFrameMax < kf->key)
            mFrameMax = kf->key;
    }
    std::string newName = go->name;
    ASE_GeometryObjectGroup* childGroup = findGroup(newName);
    if(childGroup)
        findMaxFrameIndex(childGroup);
    }
}

void ASE_Loader::fillFrame(ASE_GeometryObjectGroup* group) {
    if (group->parent.keyFrames.size() > 0) {
        std::list<SE_KeyFrame<ASE_Transform>*> keyFrames;
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        SE_KeyFrame<ASE_Transform>* oldkf = NULL;
        for(it = group->parent.keyFrames.begin() ; it != group->parent.keyFrames.end() ; it++)
        {
        SE_KeyFrame<ASE_Transform>* kf = *it;
        if(oldkf) {
            int oldkey = oldkf->key;
            int key = kf->key;
            float par = 1.0/(key-oldkey);
            for (int i=oldkey+1; i<=key; i++) {
                SE_KeyFrame<ASE_Transform>* newkf = new SE_KeyFrame<ASE_Transform>();
                newkf->key = i;
                newkf->data.rotate.set(kf->data.rotate.w * par, SE_Vector3f(kf->data.rotate.x, kf->data.rotate.y, kf->data.rotate.z));
                newkf->data.translate = kf->data.translate.mul(par);
                newkf->data.scale = SE_Vector3f(1, 1, 1);
                keyFrames.push_back(newkf);
            }
        } else {
            keyFrames.push_back(kf);
        }
        oldkf = kf;
        }
        if (oldkf) {
        for (int i=oldkf->key+1; i<=mFrameMax; i++) {
            SE_KeyFrame<ASE_Transform>* newkf = new SE_KeyFrame<ASE_Transform>();
            newkf->key = i;
            newkf->data.rotate.set(0, SE_Vector3f(0, 0, 0));
            newkf->data.translate = SE_Vector3f(0, 0, 0);
            newkf->data.scale = SE_Vector3f(1, 1, 1);
            keyFrames.push_back(newkf);
        }
        }
        group->parent.keyFrames = keyFrames;
    }
    std::list<ASE_GeometryObject*>::iterator itChildren = group->children.begin();
    for(; itChildren != group->children.end() ; itChildren++) {
    ASE_GeometryObject* go = *itChildren;
    std::list<SE_KeyFrame<ASE_Transform>*> keyFrames;
    if (go->keyFrames.size() > 0) {

        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        SE_KeyFrame<ASE_Transform>* oldkf = NULL;
        for(it = go->keyFrames.begin() ; it != go->keyFrames.end() ; it++)
        {
        SE_KeyFrame<ASE_Transform>* kf = *it;
        if(oldkf) {
            int oldkey = oldkf->key;
            int key = kf->key;
            float par = 1.0/(key-oldkey);
            for (int i=oldkey+1; i<=key; i++) {
                SE_KeyFrame<ASE_Transform>* newkf = new SE_KeyFrame<ASE_Transform>();
                newkf->key = i;
                newkf->data.rotate.set(kf->data.rotate.w * par, SE_Vector3f(kf->data.rotate.x, kf->data.rotate.y, kf->data.rotate.z));
                newkf->data.translate = kf->data.translate.mul(par);
                newkf->data.scale = SE_Vector3f(1, 1, 1);
                keyFrames.push_back(newkf);
            }
        } else {
            keyFrames.push_back(kf);
        }
        oldkf = kf;
        }
        if (oldkf) {
        for (int i=oldkf->key+1; i<=mFrameMax; i++) {
            SE_KeyFrame<ASE_Transform>* newkf = new SE_KeyFrame<ASE_Transform>();
            newkf->key = i;
            newkf->data.rotate.set(0, SE_Vector3f(0, 0, 0));
            newkf->data.translate = SE_Vector3f(0, 0, 0);
            newkf->data.scale = SE_Vector3f(1, 1, 1);
            keyFrames.push_back(newkf);
        }
        }
        go->keyFrames = keyFrames;
    } else {
        SE_KeyFrame<ASE_Transform>* frame0 = new SE_KeyFrame<ASE_Transform>();
        frame0->key = 0;
        frame0->data.rotate.set(go->rotateAngle, go->rotateAxis);
        frame0->data.scale = go->scale;
        frame0->data.translate = go->translate;
        frame0->data.matrix.set(frame0->data.rotate.toMatrix3f(),frame0->data.scale,frame0->data.translate);
        keyFrames.push_back(frame0);
        for (int i=1; i<=mFrameMax; i++) {
            SE_KeyFrame<ASE_Transform>* newkf = new SE_KeyFrame<ASE_Transform>();
            newkf->key = i;
            newkf->data.rotate.set(0, SE_Vector3f(0, 0, 0));
            newkf->data.translate = SE_Vector3f(0, 0, 0);
            newkf->data.scale = SE_Vector3f(1, 1, 1);
            keyFrames.push_back(newkf);
        }
        go->keyFrames = keyFrames;
    }
    std::string newName = go->name;
    ASE_GeometryObjectGroup* childGroup = findGroup(newName);
    if(childGroup)
        fillFrame(childGroup);
    }
}

void ASE_Loader::fillKeyMesh(ASE_GeometryObject* go) {
    if (go->keyMeshs.size() > 0) {
        std::list<ASE_Mesh*>::iterator it = go->keyMeshs.begin();
    ASE_Mesh* oldMesh = NULL;
    std::list<ASE_Mesh*> keyMeshs;
        keyMeshs.push_back(*it);
        it ++;
        for(; it != go->keyMeshs.end() ; it++)
    {
        ASE_Mesh* mesh = *it;
        if(oldMesh) {
        int oldkey = oldMesh->timeValue;
        int key = mesh->timeValue;
        float par = 1.0/(key-oldkey);
        for (int i=oldkey+1; i<=key; i++) {
            if (i == key) {
            keyMeshs.push_back(mesh);
            } else {
            ASE_Mesh* newMesh = new ASE_Mesh();
            newMesh->timeValue = i;
            int numVertexes = mesh->numVertexes;
            newMesh->numVertexes = numVertexes;
            ASE_Vertex *vertexes = new ASE_Vertex[numVertexes];
            newMesh->vertexes = vertexes;
            for(int j = 0; j < numVertexes; j++) {
                vertexes[j].x = oldMesh->vertexes[j].x + (i-oldkey)*(mesh->vertexes[j].x - oldMesh->vertexes[j].x)/(key-oldkey);
                vertexes[j].y = oldMesh->vertexes[j].y + (i-oldkey)*(mesh->vertexes[j].y - oldMesh->vertexes[j].y)/(key-oldkey);
                vertexes[j].z = oldMesh->vertexes[j].z + (i-oldkey)*(mesh->vertexes[j].z - oldMesh->vertexes[j].z)/(key-oldkey);
            }
            keyMeshs.push_back(newMesh);
            }
        }
        } else {
        keyMeshs.push_back(mesh);
        }
        oldMesh = mesh;
    }
    go->keyMeshs = keyMeshs;
    }
}
void ASE_Loader::processMatrix(ASE_GeometryObjectGroup* group) {
    if (group->parent.keyFrames.size() > 0) {
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        SE_KeyFrame<ASE_Transform>* oldkf = NULL;
        for(it = group->parent.keyFrames.begin() ; it != group->parent.keyFrames.end() ; it++)
        {
        SE_KeyFrame<ASE_Transform>* kf = *it;
        if(oldkf) {
            SE_Matrix4f postTrans, preTrans;
            SE_Matrix3f rotateM, translateM;
            rotateM.identity();
            translateM.identity();
            rotateM = rotateM.mul(kf->data.rotate.toMatrix3f());
            postTrans.set(rotateM,SE_Vector3f(0,0,0));
            preTrans.set(translateM,kf->data.translate);
            kf->data.matrix = preTrans.mul(oldkf->data.matrix).mul(postTrans);

            }
        oldkf = kf;
        }
    }
    std::list<ASE_GeometryObject*>::iterator itChildren = group->children.begin();
    for(; itChildren != group->children.end() ; itChildren++) {
    ASE_GeometryObject* go = *itChildren;
    if (go->keyFrames.size() > 0) {
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
        SE_KeyFrame<ASE_Transform>* oldkf = NULL;
        for(it = go->keyFrames.begin() ; it != go->keyFrames.end() ; it++)
        {
            SE_KeyFrame<ASE_Transform>* kf = *it;
            if(oldkf) {
                SE_Matrix4f postTrans, preTrans;
                SE_Matrix3f rotateM, translateM;
                rotateM.identity();
                translateM.identity();
                rotateM = rotateM.mul(kf->data.rotate.toMatrix3f());
                postTrans.set(rotateM,SE_Vector3f(0,0,0));
                preTrans.set(translateM,kf->data.translate);
                kf->data.matrix = preTrans.mul(oldkf->data.matrix).mul(postTrans);
            }
            oldkf = kf;
        }
        }
    std::string newName = go->name;
    ASE_GeometryObjectGroup* childGroup = findGroup(newName);
    if(childGroup)
        processMatrix(childGroup);
    }
}

void ASE_Loader::processNewMatrix(ASE_GeometryObjectGroup* group, ASE_GeometryObject* parent) {
    std::list<SE_KeyFrame<ASE_Transform>*> parentKeyFrames;
    if (group->parent.keyFrames.size() > 0) {
    parentKeyFrames = group->parent.keyFrames;
    } else {
    parentKeyFrames = parent->keyFrames;
    }
    std::list<ASE_GeometryObject*>::iterator itChildren = group->children.begin();
    for(; itChildren != group->children.end() ; itChildren++) {
    ASE_GeometryObject* go = *itChildren;
    if (go->keyFrames.size() > 0) {
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator parentIt = parentKeyFrames.begin();
        std::list<SE_KeyFrame<ASE_Transform>*>::iterator childIt = go->keyFrames.begin();
        SE_KeyFrame<ASE_Transform>* zeroFrame = *parentIt;
        SE_Matrix4f zeroMatrix = zeroFrame->data.matrix.inverse();
        for(int i= 0; i<=mFrameMax; i++)
        {
            SE_KeyFrame<ASE_Transform>* parentkf = *parentIt;
            SE_KeyFrame<ASE_Transform>* childkf = *childIt;
            childkf->data.matrix = parentkf->data.matrix.mul(zeroMatrix).mul(childkf->data.matrix);
            parentIt ++;
            childIt ++;
        }
    }
    std::string newName = go->name;
    ASE_GeometryObjectGroup* childGroup = findGroup(newName);
    if(childGroup)
        processNewMatrix(childGroup, go);
    }
}
//add by liusong end
void ASE_Loader::Write(const char* dataPath, const char* outFileName,bool averageNormal)
{
    SE_BufferOutput outBase, outScene;
    SE_BufferOutput outBaseHeader, outSceneHeader;

    std::string outBaseFileName(outFileName);
    std::string outSceneFileName(outFileName);

    outBaseFileName = outBaseFileName + "_basedata.cbf";
    outSceneFileName = outSceneFileName + "_scene.cbf";

    Write(outBase, outScene, dataPath,outBaseFileName.c_str(),averageNormal);
    writeHeader(outBaseHeader, outBase.getDataLen());
    writeHeader(outSceneHeader, outScene.getDataLen());
    

    SE_File fbase(outBaseFileName.c_str(), SE_File::WRITE);
    fbase.write(outBaseHeader);
    fbase.write(outBase);
    

    SE_File fscene(outSceneFileName.c_str(), SE_File::WRITE);
    fscene.write(outSceneHeader);
    fscene.write(outScene);
}
/*
** ASE_Load
*/
void ASE_Loader::Load( const char *filename, bool verbose,bool isSceneData)
{
    FILE *fp = fopen( filename, "rb" );
        
    if ( !fp )
        LOGE("File not found '%s'", filename);

    if(ase.len != 0)
    {
        delete ase.buffer;        
    }
    memset( &ase, 0, sizeof( ase ) );
    ase.len = getFileLen( fp );
    
    ase.curpos = ase.buffer = new char[ase.len];

    if(SE_Application::getInstance()->SEHomeDebug)
    LOGI( "Processing '%s'\n", filename );

    if ( fread( ase.buffer, ase.len, 1, fp ) != 1 )
    {
        fclose( fp );
        LOGE( "fread() != -1 for '%s'", filename );
    }

    fclose( fp );

    ASE_Process(isSceneData);
}




int ASE_Loader::CharIsTokenDelimiter( int ch )
{
    if(ch == '"')
    {
        if(mQuoteMarkFinish)
        {
            mQuoteMarkFinish = false;
        }
        else
        {
            mQuoteMarkFinish = true;
        }
    }

    if ( ch <= 32 && mQuoteMarkFinish)
        return 1;
    return 0;
}

int ASE_Loader::ASE_GetToken( bool restOfLine )
{
    int i = 0;

    if ( ase.buffer == 0 )
        return 0;

    if ( ( ase.curpos - ase.buffer ) == ase.len )
        return 0;

    // skip over crap
    while ( ( ( ase.curpos - ase.buffer ) < ase.len ) &&
            ( *ase.curpos <= 32 ) )
    {
        ase.curpos++;
    }

    while ( ( ase.curpos - ase.buffer ) < ase.len )
    {
        s_token[i] = *ase.curpos;

        ase.curpos++;
        i++;

        if ( ( CharIsTokenDelimiter( s_token[i-1] ) && !restOfLine) ||
             ( ( s_token[i-1] == '\n' ) || ( s_token[i-1] == '\r' ) ) )
        {
            s_token[i-1] = 0;
            break;
        }
    }

    s_token[i] = 0;

    return 1;
}

bool ASE_Loader::isLineEnd()
{
    int i = 0;

    if ( ase.buffer == 0 )
    {
        return false;
    }

    if ( ( ase.curpos - ase.buffer ) == ase.len )
    {
        return false;
    }

    // skip over crap
    if( ( ase.curpos - ase.buffer ) < ase.len )
    {
        s_token[i] = *ase.curpos;

        if (( s_token[i] == '\n' ) || ( s_token[i] == '\r' ))
        {
            return false;
        }
        else
        {
            return true;
        }
    }
  return false;
}

void ASE_Loader::ASE_ParseBracedBlock( ParserFun parser )
{
    int indent = 0;

    while ( ASE_GetToken( false ) )
    {
        if ( !strcmp( s_token, "{" ) )
        {
            indent++;
        }
        else if ( !strcmp( s_token, "}" ) )
        {
            --indent;
            if ( indent == 0 )
                break;
            else if ( indent < 0 )
                LOGE( "Unexpected '}'" );
        }
        else
        {
            if ( parser )
                (this->*parser)( s_token );
        }
    }
}

void ASE_Loader::ASE_SkipEnclosingBraces(  )
{
    int indent = 0;

    while ( ASE_GetToken( false ) )
    {
        if ( !strcmp( s_token, "{" ) )
        {
            indent++;
        }
        else if ( !strcmp( s_token, "}" ) )
        {
            indent--;
            if ( indent == 0 )
                break;
            else if ( indent < 0 )
                LOGE( "Unexpected '}'" );
        }
    }
}

void ASE_Loader::ASE_SkipRestOfLine(  )
{
    ASE_GetToken( true );
}

//biped
ASE_BipedController *ASE_Loader::findCurrBipedController(const char * controllerID)
{
    if(mSceneObject->mBipsController.size() == 0)
    {
        //first controller
        ASE_BipedController* bipedController = new ASE_BipedController;
        mSceneObject->mBipsController.push_back(bipedController);
        mCurrBipedController = bipedController;

        mCurrBipedController->controllerId = controllerID;

        return bipedController;
    }
    else
    {
        for(int i = 0; i < mSceneObject->mBipsController.size(); ++i)
        {
            ASE_BipedController* bipedController = mSceneObject->mBipsController[i];

            if(!strcmp(bipedController->controllerId.c_str(),controllerID))
            {
                //found!
                return bipedController;
            }
        }

        //Its a new controllerID
        ASE_BipedController* bipedController = new ASE_BipedController;
        mSceneObject->mBipsController.push_back(bipedController);
        mCurrBipedController = bipedController;

        mCurrBipedController->controllerId = controllerID;

        return bipedController;

    }
}
void ASE_Loader::ASE_KeyBIPEDINFO(const char* token)
{

    if(!strcmp(token , "*ID"))    
    {
        ASE_GetToken(false);
        std::string tempstr = s_token;
        std::string id = tempstr.substr(1,tempstr.length()-2); // delete " and "  

        mCurrBipedController = findCurrBipedController(id.c_str());

        if(!mCurrBipedController)
        {
            LOGI("Error.create controller fail.\n");
        }

        mCurrBipedController->controllerId = id.c_str();
    }
    else if(!strcmp(token , "*NUM"))    
    {
        ASE_GetToken(false);       
        int num = atoi(s_token);
        //mCurrBipedController->oneBipAnimation.resize(num);
    }
    else if(!strcmp(token, "*SKELETON"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeySKELETONINFO);
    }    
}

void ASE_Loader::ASE_KeySKELETONINFO(const char* token)
{
    if(!strcmp(token, "*JOINT_NUM"))
    {
        ASE_GetToken(false);
        int jointNum = atoi(s_token);
        //not use,now
    }
    else if(!strcmp(token, "*NODE_INFO"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyNODEINFO);
    }
    else if(!strcmp(token, "*TRANSFORM_INFO"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyTRANSFORMINFO_INFO);
    }
}

void ASE_Loader::ASE_KeyNODEINFO(const char* token)
{
    if(!strcmp(token, "*NODE"))
    {
        ASE_GetToken(false); //get parent node name

        ASE_Biped * parent = NULL;

        std::vector<ASE_Biped*>::iterator it;

        it = mCurrBipedController->oneBipAnimation.begin();

        for( ;it != mCurrBipedController->oneBipAnimation.end(); it++)
        {
            ASE_Biped* node = *it;

            if(!node)
            {
                break;
            }

            if(!strcmp(s_token , node->bipName.c_str()))
            {
                parent = node;
                break;
            }            
        }

        if(!parent) //this node maybe root
        {
            parent = new ASE_Biped();
            parent->bipName = s_token;
            mCurrBipedController->oneBipAnimation.push_back(parent);

        }

        
        ASE_GetToken(false); //get child num

        int childNum = atoi(s_token);

        //child begin
        for(int i = 0; i < childNum; ++i)
        {
            ASE_GetToken(false); // get child name
            
            //new ase biped 
            ASE_Biped* child = new ASE_Biped();
            child->bipName = s_token;
            child->parent = parent;

            //push biped to vector
            parent->children.push_back(child);

            //push child to same vector of parent, for find
            mCurrBipedController->oneBipAnimation.push_back(child);
            
        }
    }
}

void ASE_Loader::ASE_KeyTRANSFORMINFO_INFO(const char* token)
{
    if(!strcmp(token, "*TRANSFORM"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyTRANSFORMINFO);
    }
}

void ASE_Loader::ASE_KeyTRANSFORMINFO(const char* token)
{
    if(!strcmp(token, "*NODE_NAME"))
    {
        ASE_GetToken(false); //get node name

        ASE_Biped* node = NULL;

        std::vector<ASE_Biped*>::iterator it;

        it = mCurrBipedController->oneBipAnimation.begin();

        for( ;it != mCurrBipedController->oneBipAnimation.end(); it++)
        {
            ASE_Biped* n = *it;
            if(!strcmp(s_token , n->bipName.c_str()))
            {
                node = n;
                break;
            }            
        }

        if(!node)
        {
            LOGE( "Error!,find a orphan node!\n" );
            return;
        }        
        mCurrNode = node;
    }
    else if(!strcmp(token, "*BIND_POS"))
    {

        //get init rotate
        SE_Quat q;
        ASE_GetToken(false); //get "quat4.x"
        q.x = atof(s_token);

        ASE_GetToken(false); //get "quat4.y"
        q.y = atof(s_token);

        ASE_GetToken(false); //get "quat4.z"
        q.z = atof(s_token);

        ASE_GetToken(false); //get "quat4.w"
        q.w = atof(s_token);

        //get init scale
        SE_Vector3f scale;

        for(int i = 0; i < 3; ++i)
        {
            ASE_GetToken(false); //get  scale
            scale.d[i] = atof(s_token);

        }

        //get init translate
        SE_Vector3f translate;

        for(int i = 0; i < 3; ++i)
        {
            ASE_GetToken(false); //get  scale
            translate.d[i] = atof(s_token);

        }

        SE_Matrix4f bindpos;
        bindpos.set(q.toMatrix3f(),scale,translate);

        float data[16];
        bindpos.getSequence(data,16);

        for(int i = 0; i < 16; ++i)
        {
            mCurrNode->bind_pose.m[i] = data[i];//colum major
        }


    }
    else if(!strcmp(token, "*KEY_FRAME"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyKEYFRAMEINFO);
    }
}

void ASE_Loader::ASE_KeyKEYFRAMEINFO(const char* token)
{
    if(!strcmp(token, "*FRAME"))
    {
        ASE_GetToken(false); //get frame index
        std::string frameIndex = s_token;
        frameIndex = frameIndex.substr(0,frameIndex.length()-1);

        ASE_BipedKeyFrame * oneKeyFrame = new ASE_BipedKeyFrame();
        oneKeyFrame->frameIndex = atoi(frameIndex.c_str());

        SE_Quat q;
        ASE_GetToken(false); //get "quat4.x"
        q.x = atof(s_token);

        ASE_GetToken(false); //get "quat4.y"
        q.y = atof(s_token);

        ASE_GetToken(false); //get "quat4.z"
        q.z = atof(s_token);

        ASE_GetToken(false); //get "quat4.w"
        q.w = atof(s_token);        

        oneKeyFrame->rotateQ.x = q.x;
        oneKeyFrame->rotateQ.y = q.y;
        oneKeyFrame->rotateQ.z = q.z;
        oneKeyFrame->rotateQ.w = q.w;

        SE_Vector3f scale;

        for(int i = 0; i < 3; ++i)
        {
            ASE_GetToken(false); //get  scale
            scale.d[i] = atof(s_token);

        }

        SE_Vector3f translate;

        for(int i = 0; i < 3; ++i)
        {
            ASE_GetToken(false); //get  scale
            translate.d[i] = atof(s_token);

        }

        oneKeyFrame->translate.x = translate.x;
        oneKeyFrame->translate.y = translate.y;
        oneKeyFrame->translate.z = translate.z;

        oneKeyFrame->scale.x = scale.x;
        oneKeyFrame->scale.y = scale.y;
        oneKeyFrame->scale.z = scale.z;

        mCurrNode->animationInfo.push_back(oneKeyFrame);
    }
}

void ASE_Loader::ASE_KeyBIPEDVERTEXINFO(const char *token) 
{
    if(!strcmp(token , "*OBJHASBONE"))
    {
        ASE_GetToken(false); //get objName
        ASE_SkeletonUnit * su = new ASE_SkeletonUnit();

        std::string tempstr = s_token;
        std::string id = tempstr.substr(1,tempstr.length()-2); // delete " and "
        su->objHasBiped = id;
        mCurrSU = su;        
    }
    else if(!strcmp(token , "*SKELETONID"))
    {
        ASE_GetToken(false); //get controller id
        std::string tempstr = s_token;
        std::string id = tempstr.substr(1,tempstr.length()-2); // delete " and " 

        
        mCurrBipedController = findCurrBipedController(id.c_str());

        if(!mCurrBipedController)
        {
            LOGI("Error.create controller fail.\n");
        }

        mCurrSU->controllerId = id;
        mCurrBipedController->bipAndObjInfo.push_back(mCurrSU);
    }
    else if(!strcmp(token , "*BONENUM"))
    {
        ASE_GetToken(false);//get bipNum
        mCurrSU->bipedNum = atoi(s_token);
        mCurrSU->bipedNamesOnObj.resize(mCurrSU->bipedNum); //resize nameArray
    }
    else if(!strcmp(token , "*BONENAMEARRAY"))
    {
        for(int i = 0; i < mCurrSU->bipedNamesOnObj.size(); ++i)
        {
            ASE_GetToken(false);//get bip name
            std::string bipname = s_token;
            mCurrSU->bipedNamesOnObj[i] = bipname;

        }

    }
    else if(!strcmp(token , "*VERTEXINFO"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBIPEDWEIGHTINFO);
    }
}

void ASE_Loader::ASE_KeyBIPEDWEIGHTINFO(const char *token)
{
    if(!strcmp(token , "*VERTEXNUM"))
    {
        ASE_GetToken(false);//get vertex number
        int num = atoi(s_token);//not use now
        mCurrSU->vertexNum = num;
    }
    else if(!strcmp(token , "*VERTEX_WEIGHT"))
    {
        ASE_GetToken(false);//get vertex index
        ASE_GetToken(false);//get biped Number this vertex has binded
        int bipedNumberOnThisVertex = atoi(s_token);

        ASE_BipedWeight *bipW = new ASE_BipedWeight();        

        for(int i = 0; i < bipedNumberOnThisVertex; ++i)
        {
            ASE_GetToken(false); // get biped index
            int index = atoi(s_token);
            

            ASE_GetToken(false); // get vertex weight
            float weight = atof(s_token);

            if(weight == 0)
            {
                LOGI("Ignore 0 weight point!!\n");
                continue;//if weight is 0, don't record this info
            }
            bipW->bipedIndex.push_back(index);
            bipW->weight.push_back(weight);
        }

        mCurrSU->objVertexBlendInfo.push_back(bipW);
    }
}

//bone info
void ASE_Loader::ASE_KeyBONEINFO(const char* token)
{

    if(!strcmp(token , "*BONENUM"))    
    {
        ASE_GetToken(false);
        ASE_GetToken(false);
        int num = atoi(s_token);
        mCurrSkinJointController->jointVector.resize(num, 0);
    }
    else if(!strcmp(token, "*OBJHASBONE"))
    {
        ASE_GetToken(false);
        ASE_GetToken(false);
        mCurrSkinJointController->objName = s_token;
    }
    else if(!strcmp(token, "*BONERELATION"))
    {
        ASE_GetToken(false);
        int index = atoi(s_token);
        ASE_GetToken(false);
        int childCount = atoi(s_token);
        ASE_GetToken(false);
        ASE_GetToken(false);
        ASE_Bone* currBone = NULL;
        std::string currBoneName(s_token);
        bool findCurrBoneInChildrenList = false;
        for(int i = 0 ; i < mCurrSkinJointController->jointVector.size() ; i++)
        {
            ASE_Bone* bone = mCurrSkinJointController->jointVector[i];
            if(bone)
            {
                std::list<ASE_Bone*>::iterator it = bone->children.begin();
                for(; it != bone->children.end() ; it++)
                {
                    ASE_Bone* child = *it;
                    if(child->name == currBoneName)
                    {
                        findCurrBoneInChildrenList = true;
                        currBone = child;
                        break;
                    }
                }
            }
        }
        if(!findCurrBoneInChildrenList)
        {
            currBone = new ASE_Bone;
            currBone->name = currBoneName;
        }
        for(int i = 0 ; i < childCount ; i++)
        {
            ASE_GetToken(false);
            ASE_GetToken(false);
            ASE_Bone* child = new ASE_Bone;
            child->name = s_token;
            child->parent = currBone;
            currBone->children.push_back(child);
        }
        mCurrSkinJointController->jointVector[index] = currBone;
    }
    else if(!strcmp(token, "*VERTEXINFO"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBONEVERTEXINFO);
    }
    else if(!strcmp(s_token , "*BONEMATRIX"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBONEMATRIX);
    }
}
void ASE_Loader::ASE_KeyBONEMATRIXINFO(const char* token)
{
    if(!strcmp(token, "*NAME"))
    {
        ASE_GetToken(false);
        std::string boneName = s_token;
        int boneNum = mCurrSkinJointController->jointVector.size();
        ASE_Bone* bone = NULL;
        for(int i = 0 ; i < boneNum ; i++)
        {
            ASE_Bone* b = mCurrSkinJointController->jointVector[i];
            if(b->name == boneName)
            {
                bone = b;
                break;
            }
        }
        mCurrBone = bone;
    }
    else if(!strcmp(token, "*MATRIX"))
    {
        for(int i = 0 ; i < 16 ; i++)
        {
            ASE_GetToken(false);
            mCurrBone->matrixbase.m[i] = atof(s_token);
        }
    }
    else if(!strcmp(token, "*MATRIXARRAY"))
    {
        ASE_GetToken(false);
        int num = atoi(s_token);
        mCurrBone->matrixseqnum = num;
        mCurrBone->matrixseq = new ASE_Matrix4f[num];
        for(int i = 0 ; i < num ; i++)
        {
            for(int j = 0 ; j < 16 ; j++)
            {
                ASE_GetToken(false);
                mCurrBone->matrixseq[i].m[j] = atof(s_token);
            }
        }
    }
}
void ASE_Loader::ASE_KeyBONEMATRIX(const char* token)
{
    if(!strcmp(token, "*BONENUM"))
    {
        ASE_GetToken(false);
        int boneNum = atoi(s_token);
        int size = mCurrSkinJointController->jointVector.size();
        SE_ASSERT(size == boneNum);
    }
    else if(!strcmp(token, "*BONE"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBONEMATRIXINFO);
    }
}

void ASE_Loader::ASE_KeyBONEVERTEXINFO(const char* token)
{
    if(!strcmp(token, "*VERTEXNUM"))
    {
        ASE_GetToken(false);
        int vertexNum = atoi(s_token);
        mCurrSkinJointController->vertexJointVector.resize(vertexNum);
    }
    else if(!strcmp(token, "*VERTEX"))
    {
        ASE_GetToken(false);
        int index = atoi(s_token);
        ASE_GetToken(false);
        ASE_GetToken(false);
        int boneNum = atoi(s_token);
        mCurrSkinJointController->vertexJointVector[index].resize(boneNum);
        ASE_GetToken(false);
        for(int i = 0 ; i < boneNum ; i++)
        {
            ASE_GetToken(false);
            ASE_GetToken(false);
            ASE_GetToken(false);
            int boneIndex = atoi(s_token);
            ASE_GetToken(false);
            ASE_GetToken(false);
            ASE_GetToken(false);
            ASE_GetToken(false);
            float weight = atof(s_token);
            ASE_BoneWeight bw;
            bw.boneIndex = boneIndex;
            bw.weight = weight;
            mCurrSkinJointController->vertexJointVector[index][i] = bw;
        }
    }
}
void ASE_Loader::ASE_KeyMAP_SPECULAR( const char *token )
{
    char buffer[1024], buff1[1024], buff2[1024];
    char *buf1, *buf2;
    int i = 0, count;

    if ( !strcmp( token, "*BITMAP" ) )
    {
        ASE_GetToken( false );

        strcpy( buffer, s_token + 1 );
        if ( strchr( buffer, '"' ) )
                *strchr( buffer, '"' ) = 0;
        int len = strlen(buffer);
        buf1 = buffer + len - 1;
        for(i = len - 1 ; i >=0 ; i--)
        {
            if(buf1 && (*buf1) != '\\')
            {
                    buf1--;    
            }    
            else
            {
                    break;
            }
        }
        strncpy(buff1, buf1 + 1, 1024);
        if(mInSubDiffuse)
        {
            strncpy(mCurrSubMtl->texName, buff1, 256);
            LOGI("sub material texname : %s\n", mCurrSubMtl->texName);
        }
        else
        {
            strncpy(mCurrMtl->specularMapData.texName, buff1, 256);
            LOGI("specular texname : %s\n", mCurrMtl->specularMapData.texName);
        }
    }    
}
void ASE_Loader::ASE_KeyMAP_DIFFUSE( const char *token )
{
    char buffer[1024], buff1[1024], buff2[1024];
    char *buf1, *buf2;
    int i = 0, count;

    if ( !strcmp( token, "*BITMAP" ) )
    {
        ASE_GetToken( false );

        strcpy( buffer, s_token + 1 );
        if ( strchr( buffer, '"' ) )
                *strchr( buffer, '"' ) = 0;
        int len = strlen(buffer);
        buf1 = buffer + len - 1;
        for(i = len - 1 ; i >=0 ; i--)
        {
            if(buf1 && (*buf1) != '\\')
            {
                    buf1--;    
            }    
            else
            {
                    break;
            }
        }
        strncpy(buff1, buf1 + 1, 1024);
        if(mInSubDiffuse)
        {
            strncpy(mCurrSubMtl->texName, buff1, 256);
            LOGI("sub material texname : %s\n", mCurrSubMtl->texName);
        }
        else
        {
            strncpy(mCurrMtl->materialData.texName, buff1, 256);
            LOGI("material texname : %s\n", mCurrMtl->materialData.texName);
        }
    }    
    else if(!strcmp(token, "*UVW_U_TILING"))
    {
        ASE_GetToken( false );
        float ut = atof(s_token);
        mCurrMtl->materialData.u_tiling = ut;
    }
    else if(!strcmp(token, "*UVW_V_TILING"))
    {
        ASE_GetToken( false );
        float vt = atof(s_token);
        mCurrMtl->materialData.v_tiling = vt;
    }
}

void ASE_Loader::ASE_KeyMAP_BUMP( const char *token )
{
    char buffer[1024], buff1[1024], buff2[1024];
    char *buf1, *buf2;
    int i = 0, count;

    if ( !strcmp( token, "*BITMAP" ) )
    {
        ASE_GetToken( false );

        strcpy( buffer, s_token + 1 );
        if ( strchr( buffer, '"' ) )
                *strchr( buffer, '"' ) = 0;
        int len = strlen(buffer);
        buf1 = buffer + len - 1;
        for(i = len - 1 ; i >=0 ; i--)
        {
            if(buf1 && (*buf1) != '\\')
            {
                    buf1--;    
            }    
            else
            {
                    break;
            }
        }
        strncpy(buff1, buf1 + 1, 1024);
        
        strncpy(mCurrMtl->bumpMaterialData->texName, buff1, 256);
        LOGI("material bump texname : %s\n", mCurrMtl->bumpMaterialData->texName);
    }
}

void ASE_Loader::ASE_KeyMAP_GENERIC( const char *token )
{
    
        
}

void ASE_Loader::ASE_KeyMAP_SUBMATERIAL(const char* token)
{
    if ( !strcmp( token, "*MAP_DIFFUSE" ))
    {
        mInSubDiffuse = true;
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAP_DIFFUSE );
        mInSubDiffuse = false;
    }    
    else if(!strcmp( token, "*MATERIAL_AMBIENT"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        
        mCurrSubMtl->ambient[0] = r;
        mCurrSubMtl->ambient[1] = g;
        mCurrSubMtl->ambient[2] = b;
    }
    else if(!strcmp( token, "*MATERIAL_DIFFUSE"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        mCurrSubMtl->diffuse[0] = r;
        mCurrSubMtl->diffuse[1] = g;
        mCurrSubMtl->diffuse[2] = b;
    }
    else if(!strcmp( token, "*MATERIAL_SPECULAR"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        mCurrSubMtl->specular[0] = r;
        mCurrSubMtl->specular[1] = g;
        mCurrSubMtl->specular[2] = b;
    }
    //ASE_KeyMATERIAL(token);
}
void ASE_Loader::ASE_KeyMATERIAL( const char *token )
{
 //   ASE_Material_t currMtl = mMtlList[mCurrMtl];
    if ( !strcmp( token, "*MAP_DIFFUSE" ))
    {
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAP_DIFFUSE );
    }
    else if(!strcmp( token, "*MAP_SPECULAR" ))
    {
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAP_SPECULAR );
    }
    else if(!strcmp( token, "*MAP_BUMP" ))
    {
        mCurrMtl->bumpMaterialData = new ASE_MaterialData();
        mCurrMtl->numMaterials++;
        //for 3D Max 2011 - ASE version
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAP_BUMP );
    }
    else if(!strcmp( token, "*MATERIAL_AMBIENT"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        
        mCurrMtl->materialData.ambient[0] = r;
        mCurrMtl->materialData.ambient[1] = g;
        mCurrMtl->materialData.ambient[2] = b;
    }
    else if(!strcmp( token, "*MATERIAL_DIFFUSE"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        mCurrMtl->materialData.diffuse[0] = r;
        mCurrMtl->materialData.diffuse[1] = g;
        mCurrMtl->materialData.diffuse[2] = b;
    }
    else if(!strcmp( token, "*MATERIAL_SPECULAR"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        mCurrMtl->materialData.specular[0] = r;
        mCurrMtl->materialData.specular[1] = g;
        mCurrMtl->materialData.specular[2] = b;
    }
    else if(!strcmp( token, "*MATERIAL_SHINE"))
    {
        ASE_GetToken(false);
        float shine = atof(s_token);        
        mCurrMtl->materialData.shine = shine;        
    }
    else if(!strcmp( token, "*MATERIAL_SHINESTRENGTH"))
    {
        ASE_GetToken(false);
        float shinestrength = atof(s_token);        
        mCurrMtl->materialData.shineStrength = shinestrength;        
    }
    else if(!strcmp( token, "*NUMSUBMTLS"))
    {
        ASE_GetToken(false);
        LOGI("...sub mtl num : %s\n", s_token);
        int numsubmtl = atoi(s_token);
            //ASE_Material_t currMtl = mMtlList[mCurrMtl];
        mCurrMtl->numsubmaterials = numsubmtl;
        mCurrMtl->submaterials = new ASE_MaterialData[numsubmtl];
    }
    else if(!strcmp(token , "*SUBMATERIAL"))
    {
        ASE_GetToken(false);
        int nCurrSubMtl = atoi(s_token);
        mCurrSubMtl = &mCurrMtl->submaterials[nCurrSubMtl];
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAP_SUBMATERIAL );
    }
}

void ASE_Loader::ASE_KeyMATERIAL_LIST( const char *token )
{
    if ( !strcmp( token, "*MATERIAL_COUNT" ) )
    {
        ASE_GetToken( false );
        LOGI( "..num materials: %s\n", s_token  );
        int count = atoi(s_token);
        int precount = mSceneObject->mMats.size();
        std::vector<ASE_Material> copyMat = mSceneObject->mMats;
        mSceneObject->mMats.resize(precount + count);
        for(int i = 0 ; i < precount ; i++)
        {
            mSceneObject->mMats[i] = copyMat[i];
        }
        //FIXBUG:should init mMatStartPos,otherwise will invoke a out-of-range index on next step
        mMatStartPos = 0;
        mMatStartPos += precount;
        
    }
    else if ( !strcmp( token, "*MATERIAL" ) )
    {
        ASE_GetToken(false);
        LOGI(  "..material %s \n",  s_token  );
        int nCurrMtl = atoi(s_token) + mMatStartPos;
        mCurrMtl = &mSceneObject->mMats[nCurrMtl];
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMATERIAL );
    }
}
void ASE_Loader::ASE_KeyMESH_VERTEX_LIST( const char *token )
{
    ASE_Mesh *pMesh = mCurrMesh;

    if ( !strcmp( token, "*MESH_VERTEX" ) )
    {
        float x, y, z;
        ASE_GetToken( false );        // skip number
        int index = atoi(s_token);
        ASE_GetToken( false );
        x = atof(s_token);
        
        ASE_GetToken( false );
        y = atof(s_token);

        ASE_GetToken( false );
        z = atof(s_token);

        pMesh->vertexes[index].x = x ;
        pMesh->vertexes[index].y = y ;
        pMesh->vertexes[index].z = z ;


    }
    else
    {
        LOGE( "Unknown token '%s' while parsing MESH_VERTEX_LIST", token );
    }
}

void ASE_Loader::ASE_KeyMESH_FACE_LIST( const char *token )
{
    ASE_Mesh *pMesh = mCurrMesh;

    if ( !strcmp( token, "*MESH_FACE" ) )
    {
        ASE_GetToken( false );    // skip face number
        int index = atoi(s_token);
        ASE_GetToken( false );    // skip label
        ASE_GetToken( false );    // first vertex
        pMesh->faces[index].vi[0] = atoi( s_token );

        ASE_GetToken( false );    // skip label
        ASE_GetToken( false );    // second vertex
        pMesh->faces[index].vi[1] = atoi( s_token );

        ASE_GetToken( false );    // skip label
        ASE_GetToken( false );    // third vertex
        pMesh->faces[index].vi[2] = atoi( s_token );

        ASE_GetToken( true );

        char* p;
        if ( ( p = strstr( s_token, "*MESH_MTLID" ) ) != 0 )
        {
            p += strlen( "*MESH_MTLID" ) + 1;
            pMesh->faces[index].materialID = atoi( p );
        }
        else
        {
            LOGE( "No *MESH_MTLID found for face!" );
        }
    }
    else
    {
        LOGE( "Unknown token '%s' while parsing MESH_FACE_LIST", token );
    }
}

void ASE_Loader::ASE_KeyTFACE_LIST( const char *token )
{
    ASE_Mesh *pMesh = mCurrMesh;

    if ( !strcmp( token, "*MESH_TFACE" ) )
    {
        int a, b, c;

        ASE_GetToken( false );
        int index = atoi(s_token);
        ASE_GetToken( false );
        a = atoi( s_token );
        ASE_GetToken( false );
        b = atoi( s_token );
        ASE_GetToken( false );
        c = atoi( s_token );

        LOGI(  ".....tface: %d\n", index );
        pMesh->tfaces[index].vi[0] = a;
        pMesh->tfaces[index].vi[1] = b;
        pMesh->tfaces[index].vi[2] = c;

    }
    else
    {
        LOGE( "Unknown token '%s' in MESH_TFACE", token );
    }
}
void ASE_Loader::ASE_KeyMAPINGPCHANNELMESH_TVERTLIST( const char *token )
{
    if ( !strcmp( token, "*MESH_TVERT" ) )
    {

    }
}
void ASE_Loader::ASE_KeyMESH_TVERTLIST( const char *token )
{
    ASE_Mesh *pMesh = mCurrMesh;

    if ( !strcmp( token, "*MESH_TVERT" ) )
    {
        char u[80], v[80], w[80];

        ASE_GetToken( false );
        int index = atoi(s_token);
        ASE_GetToken( false );
        strcpy( u, s_token );

        ASE_GetToken( false );
        strcpy( v, s_token );

        ASE_GetToken( false );
        strcpy( w, s_token );

        pMesh->tvertexes[index].s = atof( u );
        pMesh->tvertexes[index].t = atof( v );
    }
    else
    {
        LOGE( "Unknown token '%s' while parsing MESH_TVERTLIST" );
    }
}

void ASE_Loader::ASE_KeyMESH_FACEANDVERTEX_NORMALS(const char *token)
{
    ASE_Mesh *pMesh = mCurrMesh;

    if ( !strcmp( token, "*MESH_FACENORMAL" ) )
    {
        struct ASE_Normal normal;
        ASE_GetToken(false);
        int faceindex = atoi(s_token);

        ASE_GetToken(false);
        float faceNormalx = atof(s_token);

        ASE_GetToken(false);
        float faceNormaly = atof(s_token);

        ASE_GetToken(false);
        float faceNormalz = atof(s_token);

        
        normal.faceNormal.x = faceNormalx;
        normal.faceNormal.y = faceNormaly;
        normal.faceNormal.z = faceNormalz;

        pMesh->faceAndVertexNormal.push_back(normal);
        pMesh->faceVertextNormalIndex = 0;

    }
    else if(!strcmp( token, "*MESH_VERTEXNORMAL" ))
    {
        int index = pMesh->faceAndVertexNormal.size() - 1;

        ASE_GetToken(false);

        ASE_GetToken(false);
        float faceVertexNormalx = atof(s_token);

        ASE_GetToken(false);
        float faceVertexNormaly = atof(s_token);

        ASE_GetToken(false);
        float faceVertexNormalz = atof(s_token);

        pMesh->faceAndVertexNormal[index].faceVertexNoraml[pMesh->faceVertextNormalIndex].x = faceVertexNormalx;
        pMesh->faceAndVertexNormal[index].faceVertexNoraml[pMesh->faceVertextNormalIndex].y = faceVertexNormaly;
        pMesh->faceAndVertexNormal[index].faceVertexNoraml[pMesh->faceVertextNormalIndex].z = faceVertexNormalz;

        pMesh->faceVertextNormalIndex++;



    }

}

void ASE_Loader::ASE_KeyMESH( const char *token )
{
    ASE_Mesh* pMesh = mCurrMesh;

//add by liusong begin
    if ( !strcmp( token, "*TIMEVALUE" ) )
    {
        ASE_GetToken( false );
        int time = atoi( s_token );
        if (mIsKeyMesh) {
            time = (time + 90)/ 180;
        while (findAnimMesh(mCurrGeomObject->keyMeshs,time)) {
            time ++;
        }
        pMesh->timeValue = time;
            mCurrGeomObject->keyMeshs.push_back(pMesh);
        }
        LOGI(  ".....timeValue: %d\n", pMesh->timeValue );
    }
    else if ( !strcmp( token, "*MESH_NUMVERTEX" ) )
//add by liusong end
    {
        ASE_GetToken( false );

        pMesh->numVertexes = atoi( s_token );
        LOGI(  ".....num vertexes: %d\n", pMesh->numVertexes );
    }
    else if ( !strcmp( token, "*MESH_NUMFACES" ) )
    {
        ASE_GetToken( false );
        pMesh->numFaces = atoi( s_token );
        LOGI(  ".....num faces: %d\n", pMesh->numFaces );
    }
    else if ( !strcmp( token, "*MESH_NUMTVFACES" ) )
    {
        ASE_GetToken( false );
        if ( atoi( s_token ) != pMesh->numFaces )
        {
            LOGE( "MESH_NUMTVFACES != MESH_NUMFACES" );
        }
    }
    else if ( !strcmp( token, "*MESH_NUMTVERTEX" ) )
    {
        ASE_GetToken( false );

        pMesh->numTVertexes = atoi( s_token );
        LOGI( ".....num tvertexes: %d\n", pMesh->numTVertexes );
    }
    else if ( !strcmp( token, "*MESH_MAPPINGCHANNEL" ) )
    {
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMAPINGPCHANNELMESH_TVERTLIST );
    }
    else if ( !strcmp( token, "*MESH_VERTEX_LIST" ) )
    {
        pMesh->vertexes = new ASE_Vertex[pMesh->numVertexes];//calloc( sizeof( aseVertex_t ) * pMesh->numVertexes, 1 );
        LOGI(  ".....parsing MESH_VERTEX_LIST\n"  );
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH_VERTEX_LIST );
    }
    else if ( !strcmp( token, "*MESH_TVERTLIST" ) )
    {        
        if(pMesh->tvertexes != NULL)
        {
            //if export "MESH_MAPPINGCHANNEL",this keyworld will duplicate
            delete pMesh->tvertexes;
        }
        pMesh->tvertexes = new ASE_TVertex[pMesh->numTVertexes];//calloc( sizeof( aseTVertex_t ) * pMesh->numTVertexes, 1 );
        LOGI(  ".....parsing MESH_TVERTLIST\n"  );
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH_TVERTLIST );
    }
    else if ( !strcmp( token, "*MESH_FACE_LIST" ) )
    {
        pMesh->faces = new ASE_Face[pMesh->numFaces]; //calloc( sizeof( aseFace_t ) * pMesh->numFaces, 1 );
        LOGI(  ".....parsing MESH_FACE_LIST\n"  );
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH_FACE_LIST );
    }
    else if ( !strcmp( token, "*MESH_TFACELIST" ) )
    {        
        if(pMesh->tfaces != NULL)
        {
            //if export "MESH_MAPPINGCHANNEL",this keyworld will duplicate
            delete pMesh->tfaces;
        }
        pMesh->tfaces = new ASE_Face[pMesh->numFaces];//calloc( sizeof( aseFace_t ) * pMesh->numFaces, 1 );
        LOGI(  ".....parsing MESH_TFACE_LIST\n"  );
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyTFACE_LIST );
    }
    else if ( !strcmp( token, "*MESH_NORMALS" ) )
    {
        LOGI(  ".....parsing MESH_NORMALS\n"  );
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH_FACEANDVERTEX_NORMALS);
    }
}

void ASE_Loader::ASE_KeyMESH_ANIMATION( const char *token )
{
    /*
    if ( !strcmp( token, "*MESH" ) )
    {
        mIsKeyMesh = true;
        ASE_Mesh* mesh = new ASE_Mesh;
        mCurrMesh =  mesh;
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH);
        mIsKeyMesh = false;
    }
    */
}

void ASE_Loader::ASE_KeyNODETM(const char* token)
{
    if(!strcmp(token, "*NODE_NAME"))
    {
        ASE_GetToken(false);
        std::string str = s_token;
        SE_Util::SplitStringList nameList = SE_Util::splitString(s_token, "\"");
        SE_ASSERT(nameList.size() == 1);
        std::string name = *nameList.begin();
        ASE_GeometryObjectGroup* group = findGroup(name);
        mCurrGeometryObjectGroup = group;
    }
    else if(!strcmp( token, "*TM_POS" ))
    {
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
            mCurrGeometryObjectGroup->parent.baseTranslate[0] = x;
            mCurrGeometryObjectGroup->parent.baseTranslate[1] = y;
            mCurrGeometryObjectGroup->parent.baseTranslate[2] = z;
        }
        else
        {
        mCurrGeomObject->translate[0] = x;
        mCurrGeomObject->translate[1] = y;
        mCurrGeomObject->translate[2] = z;
    }
    }
    else if(!strcmp( token, "*TM_ROTAXIS" ))
    {
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
            mCurrGeometryObjectGroup->parent.baseRotate[0] = x;
            mCurrGeometryObjectGroup->parent.baseRotate[1] = y;
            mCurrGeometryObjectGroup->parent.baseRotate[2] = z;
        }
        else
        {
            
        mCurrGeomObject->rotateAxis[0] = x;
        mCurrGeomObject->rotateAxis[1] = y;
        mCurrGeomObject->rotateAxis[2] = z;
    }
    }
    else if(!strcmp( token, "*TM_ROTANGLE"))
    {
        ASE_GetToken(false);
        float x = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
            //3dmax rotate is left-hand-rule
            mCurrGeometryObjectGroup->parent.baseRotate[3] = -x;
        }
        else
        {
        //3dmax rotate is left-hand-rule
        mCurrGeomObject->rotateAngle = -x * 180.0 / 3.1415926;
    }
    }
    else if(!strcmp( token, "*TM_SCALE"))
    {
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
            mCurrGeometryObjectGroup->parent.baseScale[0] = x;
            mCurrGeometryObjectGroup->parent.baseScale[1] = y;
            mCurrGeometryObjectGroup->parent.baseScale[2] = z;
        }
        else
        {
        mCurrGeomObject->scale[0] = x;
        mCurrGeomObject->scale[1] = y;
        mCurrGeomObject->scale[2] = z;
        }
        
    }
    else if(!strcmp( token, "*TM_SCALEAXIS"))
    {
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
        }
        else
        {
        mCurrGeomObject->scaleAxis[0] = x;
        mCurrGeomObject->scaleAxis[1] = y;
        mCurrGeomObject->scaleAxis[2] = z;
        }
    }
    else if(!strcmp( token, "*TM_SCALEAXISANG"))
    {}
}
void ASE_Loader::ASE_KeyGEOMOBJECT( const char *token )
{
    if ( !strcmp( token, "*NODE_NAME" ) )
    {
        ASE_GetToken( true );
        LOGI(  " %s\n", s_token  );
        strcpy( mCurrGeomObject->name, s_token + 1 );
        if ( strchr( mCurrGeomObject->name, '"' ) )
            *strchr( mCurrGeomObject->name, '"' ) = 0;
        if(!strcmp(mCurrGeomObject->name , "Camera01"))
        {
            LOGI("... has camera setting\n");
        }
    }
    else if ( !strcmp( token, "*NODE_PARENT" ) )
    {
        if(!mIsGroupNode)
        {
            ASE_GetToken(false);
            std::string str = s_token;
            SE_Util::SplitStringList parentnameList = SE_Util::splitString(s_token, "\"");
            SE_ASSERT(parentnameList.size() == 1);
            std::string parentname = *parentnameList.begin();
            ASE_GeometryObjectGroup* group = findGroup(parentname.c_str());
            if(!group)
            {
                group = new ASE_GeometryObjectGroup;
                mSceneObject->mGeometryObjectGroup.push_back(group);
                group->parent.name = parentname;
                group->children.push_back(mCurrGeomObject);
            }
            else
            {
                group->children.push_back(mCurrGeomObject);
            }
            mCurrGeomObject->parentName = parentname;
        }
        else
        {
            //the node is a member of the group
            ASE_GetToken(false);
                        std::string fullname = s_token;
            mCurrGeomObject->mGroupName = fullname.substr(1,fullname.size() - 2);

            mIsGroupNode = false;
        }
    }
    // ignore unused data blocks
    else if ( !strcmp( token, "*TM_ANIMATION" ) )
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyTMANIMATION);
    }
    // ignore unused data blocks
    else if ( !strcmp( token, "*MESH_ANIMATION" ) )
    {
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH_ANIMATION );
    }
    // ignore regular meshes that aren't part of animation
    else if ( !strcmp( token, "*MESH" ))
    {
        mCurrGeomObject->mesh = new ASE_Mesh;
        mCurrMesh =  mCurrGeomObject->mesh;
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMESH );
    }
    // according to spec these are obsolete
    else if ( !strcmp( token, "*MATERIAL_REF" ) )
    {
        ASE_GetToken( false );
        int index = atoi( s_token );
        mCurrGeomObject->materialref = mMatStartPos + index;
    }
    // loads a sequence of animation frames
    else if ( !strcmp( token, "*NODE_TM" ) )
    {
        mCurrGeometryObjectGroup = NULL;
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyNODETM );
    }
    else if(!strcmp(token, "*WIREFRAME_COLOR"))
    {
        ASE_GetToken(false);
        float r = atof(s_token);
        ASE_GetToken(false);
        float g = atof(s_token);
        ASE_GetToken(false);
        float b = atof(s_token);
        mCurrGeomObject->wireframeColor[0]= r;
        mCurrGeomObject->wireframeColor[1]= g;
        mCurrGeomObject->wireframeColor[2]= b;
    }

    // skip unused info
    else if ( !strcmp( token, "*PROP_MOTIONBLUR" ) ||
              !strcmp( token, "*PROP_CASTSHADOW" ) ||
              !strcmp( token, "*PROP_RECVSHADOW" ) )
    {
        ASE_SkipRestOfLine();
    }
}
void ASE_Loader::ASE_KeySHADER(const char* token)
{
    if(!strcmp(token, "*NUM"))
    {
        ASE_GetToken(false);
        int num = atoi(s_token);
        mSceneObject->mShaderObjects.resize(num);
    }
    else if(!strcmp(token, "*SHADER"))
    {
        ASE_Shader* shader = new ASE_Shader;
        ASE_GetToken(false);
        int index = atoi(s_token);
        ASE_GetToken(false);
        shader->shaderID = s_token;
        ASE_GetToken(false);
        shader->vertexShaderName = s_token;
        ASE_GetToken(false);
        shader->fragmentShaderName = s_token;
        ASE_GetToken(false);
        shader->shaderClassName = s_token;
        mSceneObject->mShaderObjects[index] = shader;
    }
}
void ASE_Loader::ASE_KeyRENDERER(const char* token)
{
    if(!strcmp(token, "*NUM"))
    {
        ASE_GetToken(false);
        int num = atoi(s_token);
        mSceneObject->mRendererObjects.resize(num);
    }
    else if(!strcmp(token, "*RENDERER"))
    {
        ASE_GetToken(false);
        int index = atoi(s_token);
        ASE_GetToken(false);
        std::string renderID = s_token;
        ASE_GetToken(false);
        std::string rendererClassName = s_token;
        ASE_Renderer* renderer = new ASE_Renderer;
        renderer->rendererID = renderID;
        renderer->rendererClassName = rendererClassName;
        mSceneObject->mRendererObjects[index] = renderer;
    }
}

//camera best positon
void ASE_Loader::ASE_KeyCAMERABESTPOSITIONINFO(const char* token)
{
    if(!strcmp(s_token, "*NODE_NAME"))
    {
        ASE_GetToken(false);//space
        if(mCurrentCameraPos)
        {
            //Do not save camera name, NODE_TM parse will do it later.
        }
        else
        {
            return;
        }
    }
    else if(!strcmp(s_token, "*CAMERA_TYPE"))
    {
        ASE_GetToken(false);//space
        if(mCurrentCameraPos)
        {
            mCurrentCameraPos->mCameraType = s_token;
        }
        else
        {
            return;
        }
    }
    else if(!strcmp(s_token, "*NODE_TM") && mCurrentCameraPos && mCurrentCameraPos->mCamraName.empty())
    {
        //this is the first(camera) postion data
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyCAMERAPOSITIONINFO);

    }
    else if(!strcmp(s_token, "*NODE_TM") && mCurrentCameraPos && !mCurrentCameraPos->mCamraName.empty())
    {
        //this is the second(camera.target) postion data
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyCAMERATARGETPOSITIONINFO);
    }
    else if(!strcmp(s_token, "*CAMERA_SETTINGS"))
    {
        //do nothing, not use now.
    }    
    
}

void ASE_Loader::ASE_KeyCAMERAPOSITIONINFO(const char* token)
{
    if(!strcmp(s_token, "*NODE_NAME"))
    {
        ASE_GetToken(false);//space
        if(mCurrentCameraPos)
        {
            mCurrentCameraPos->mCamraName = s_token;
        }
        else
        {
            return;
        }
    }
    else if(!strcmp(s_token, "*TM_POS"))
    {
        if(mCurrentCameraPos)
        {
            ASE_GetToken(false);//space
            float x = atof(s_token);

            ASE_GetToken(false);//space
            float y = atof(s_token);

            ASE_GetToken(false);//space
            float z = atof(s_token);

            mCurrentCameraPos->mCameraPos.x = x;
            mCurrentCameraPos->mCameraPos.y = y;
            mCurrentCameraPos->mCameraPos.z = z;
        }
        else
        {
            return;
        }
    }
}


void ASE_Loader::ASE_KeyCAMERATARGETPOSITIONINFO(const char* token)
{
    if(!strcmp(s_token, "*NODE_NAME"))
    {
        ASE_GetToken(false);//space
        if(mCurrentCameraPos)
        {
            mCurrentCameraPos->mCameraTargetName = s_token;
        }
        else
        {
            return;
        }
    }
    else if(!strcmp(s_token, "*TM_POS"))
    {
        if(mCurrentCameraPos)
        {
            ASE_GetToken(false);//space
            float x = atof(s_token);

            ASE_GetToken(false);//space
            float y = atof(s_token);

            ASE_GetToken(false);//space
            float z = atof(s_token);

            mCurrentCameraPos->mCameraTargetPos.x = x;
            mCurrentCameraPos->mCameraTargetPos.y = y;
            mCurrentCameraPos->mCameraTargetPos.z = z;
        }
        else
        {
            return;
        }
    }
}

void ASE_Loader::ASE_KeyGROUPNODE(const char* token)
{
    if(!strcmp(s_token, "*GEOMOBJECT"))
    {
        LOGI( "GEOMOBJECT in group\n"  );
        ASE_GeometryObject *obj = new ASE_GeometryObject;
        mSceneObject->mGeomObjects.push_back(obj);
        mCurrGeomObject = obj;
        mCurrGeometryObjectGroup = NULL;
        mIsGroupNode = true;
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyGEOMOBJECT );
    }
}
/*
** ASE_Process
*/
void ASE_Loader::ASE_Process(bool isSceneData)
{
    if(isSceneData)
    {        //there is no scene loaded
        ASE_SceneObject* normalScene = new ASE_SceneObject();
        mSceneList.push_back(normalScene);
        mSceneObject = normalScene;
    }
#ifdef DEBUG
    int geomCount = 0;
#endif
    while ( ASE_GetToken( false ) )
    {
        if ( !strcmp( s_token, "*3DSMAX_ASCIIEXPORT" ) ||
             !strcmp( s_token, "*COMMENT" ) )
        {
            ASE_SkipRestOfLine();
        }
        else if ( !strcmp( s_token, "*SCENE" ) )
        {
            ASE_SkipEnclosingBraces();
        }
        else if ( !strcmp( s_token, "*MATERIAL_LIST" ) )
        {
            LOGI( "MATERIAL_LIST\n");

            ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyMATERIAL_LIST );
        }
        else if ( !strcmp( s_token, "*GEOMOBJECT" ) )
        {
            LOGI( "GEOMOBJECT\n"  );
                    ASE_GeometryObject *obj = new ASE_GeometryObject;
            mSceneObject->mGeomObjects.push_back(obj);
            mCurrGeomObject = obj;
            mCurrGeometryObjectGroup = NULL;
            ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyGEOMOBJECT );
#ifdef DEBUG
            geomCount++;
#endif
        }    
        else if(!strcmp(s_token, "*HELPEROBJECT"))
        {
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyHELPEROBJECT);
        }
        else if(!strcmp(s_token, "*BONEINFO"))
        {
             ASE_SkinJointController* skinJointController = new ASE_SkinJointController;
             mSceneObject->mSkinJointController.push_back(skinJointController);
             mCurrSkinJointController = skinJointController;
             ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBONEINFO);
        }
        else if(!strcmp(s_token, "*SKELETON_INFO"))
        {            
             ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBIPEDINFO);
        }
        else if(!strcmp(s_token, "*VERTEXBONEINFO"))
        {
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyBIPEDVERTEXINFO);
        }
        else if(!strcmp(s_token, "*SHADERINFO"))
        {
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeySHADER);
        }
        else if(!strcmp(s_token, "*RENDERERINFO"))
        {
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyRENDERER);
        }
        else if(!strcmp(s_token, "*CAMERAOBJECT"))
        {
            mCurrentCameraPos = new ASE_CameraObjectPos();
            mSceneObject->mBestPositon.push_back(mCurrentCameraPos);
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyCAMERABESTPOSITIONINFO);
        }
        else if(!strcmp(s_token, "*GROUP"))
        {            
            ASE_GetToken(false);
            mCurrentGroupName = s_token;            
            ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyGROUPNODE);
        }
    }
#ifdef DEBUG
    LOGI(".. geomCount = %d \n", geomCount);
#endif

}
void ASE_Loader::LoadEnd()
{
    ASE_AdjustSubMtl();
}
void ASE_Loader::ASE_AdjustSubMtl()
{
    GeomObjectList::iterator it;
    for(it = mSceneObject->mGeomObjects.begin() ; it != mSceneObject->mGeomObjects.end() ; it++)
    {
        ASE_GeometryObject* obj = *it;
        if(obj->materialref == -1)
            continue;
        ASE_Material* pMat = &mSceneObject->mMats[obj->materialref];
        if(pMat->submaterials != NULL)
        {
            int subMatlNum = pMat->numsubmaterials;
            for(int i = 0 ; i < obj->mesh->numFaces ; i++)
            {
                obj->mesh->faces[i].materialID = obj->mesh->faces[i].materialID % subMatlNum;
            }
            std::vector<int> faceGroupSet(subMatlNum, 0);
            for(int i = 0 ; i < obj->mesh->numFaces ; i++)
            {
                faceGroupSet[obj->mesh->faces[i].materialID]++;
            }
			obj->mesh->numFaceGroup = 0;
            for(int i = 0 ; i < subMatlNum ; i++)
            {
                if(faceGroupSet[i] > 0)
                    obj->mesh->numFaceGroup++;
            }
            obj->mesh->faceGroup.clear();
            obj->mesh->faceGroup.resize(subMatlNum);
            for(int i = 0 ; i < obj->mesh->numFaces; i++)
            {
                std::list<int>* l = &(obj->mesh->faceGroup[obj->mesh->faces[i].materialID]);
                l->push_back(i);
            } 

        }
    }
}
SE_KeyFrame<ASE_Transform>* ASE_Loader::findKeyFrame(std::list<SE_KeyFrame<ASE_Transform>*>& keyFrames, unsigned int key)
{
    std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
    for(it = keyFrames.begin() ; it != keyFrames.end() ; it++)
    {
        SE_KeyFrame<ASE_Transform>* kf = *it;
        if(kf->key == key)
            return kf;
    }
    return NULL;
}

ASE_Mesh* ASE_Loader::findAnimMesh(std::list<ASE_Mesh*>& keyMeshs, unsigned int key)
{
    std::list<ASE_Mesh*>::iterator it;
    for(it = keyMeshs.begin() ; it != keyMeshs.end() ; it++)
    {
        ASE_Mesh* am = *it;
        if(am->timeValue == key)
            return am;
    }
    return NULL;
}

void ASE_Loader::ASE_KeyCONTROLROTTRACK(const char* token)
{
    if(!strcmp(token, "*CONTROL_ROT_SAMPLE"))
    {
        ASE_GetToken(false);
        unsigned int key = atoi(s_token) / 180;
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        ASE_GetToken(false);
        float w = atof(s_token);
        w = w * 180.0 / 3.1415926;
        if(mCurrGeometryObjectGroup)
        {
            SE_KeyFrame<ASE_Transform>* frame = findKeyFrame(mCurrGeometryObjectGroup->parent.keyFrames, key);
            if(frame == NULL)
            {
                frame = new SE_KeyFrame<ASE_Transform>;
                frame->key = key;
                mCurrGeometryObjectGroup->parent.keyFrames.push_back(frame);
            }
            if(key == 0)
            {
                frame->data.rotate.set(w, SE_Vector3f(x,y,z));
                frame->data.scale = mCurrGeometryObjectGroup->parent.baseScale;
                frame->data.matrix.set(frame->data.rotate.toMatrix3f(),frame->data.scale,frame->data.translate);
            }
            else
            {
                frame->data.rotate = SE_Quat(x,y,z,-w);
                frame->data.scale = SE_Vector3f(1,1,1);
            }
        }
        else if (mCurrGeomObject)
        {
            std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
            SE_KeyFrame<ASE_Transform>* frame = findKeyFrame(mCurrGeomObject->keyFrames, key);
            if(frame == NULL)
            {
                frame = new SE_KeyFrame<ASE_Transform>;
                frame->key = key;
                mCurrGeomObject->keyFrames.push_back(frame);
            }
            if(key == 0)
            {
                frame->data.rotate.set(mCurrGeomObject->rotateAngle, mCurrGeomObject->rotateAxis);
                frame->data.scale = mCurrGeomObject->scale;
                frame->data.translate = mCurrGeomObject->translate;
                frame->data.matrix.set(frame->data.rotate.toMatrix3f(),frame->data.scale,frame->data.translate);
            }
            else
            {
                frame->data.rotate = SE_Quat(x,y,z,-w);
                frame->data.scale = SE_Vector3f(1,1,1);
                frame->data.translate = SE_Vector3f(0,0,0);
            }
        }
    } 
}
void ASE_Loader::ASE_KeyCONTROLPOSTRACK(const char* token)
{
    if(!strcmp(token, "*CONTROL_POS_SAMPLE"))
    {
        ASE_GetToken(false);
        unsigned int key = atoi(s_token) / 180;
        ASE_GetToken(false);
        float x = atof(s_token);
        ASE_GetToken(false);
        float y = atof(s_token);
        ASE_GetToken(false);
        float z = atof(s_token);
        if(mCurrGeometryObjectGroup)
        {
            SE_KeyFrame<ASE_Transform>* frame = findKeyFrame(mCurrGeometryObjectGroup->parent.keyFrames, key);
            if(frame == NULL)
            {
                frame = new SE_KeyFrame<ASE_Transform>;
                frame->key = key;
                mCurrGeometryObjectGroup->parent.keyFrames.push_back(frame);
            }

            std::list<SE_KeyFrame<ASE_Transform>*>::iterator it;
            for(it = mCurrGeometryObjectGroup->parent.keyFrames.begin() ; it != mCurrGeometryObjectGroup->parent.keyFrames.end() ; it++)
            {
                SE_KeyFrame<ASE_Transform>* kf = *it;
                x = x - kf->data.translate.x;
                y = y - kf->data.translate.y;
                z = z - kf->data.translate.z;
            }
    
            frame->data.translate.x = x;
            frame->data.translate.y = y;
            frame->data.translate.z = z;
        }
        else if (mCurrGeomObject)
        {
        
        }
    }
}
void ASE_Loader::ASE_KeyTMANIMATION(const char* token)
{
    if(!strcmp(token, "NODE_NAME"))
    {
        ASE_GetToken(false);
        if(!strcmp(token, "Dummy01")) {
        std::string str = s_token;
        ASE_GeometryObjectGroup* group = findGroup(str);
        mCurrGeometryObjectGroup = group;
    }
    }
    else if(!strcmp(token, "*CONTROL_POS_TRACK"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyCONTROLPOSTRACK);
    }
    else if(!strcmp(token, "*CONTROL_ROT_TRACK"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyCONTROLROTTRACK);
    }
}
void ASE_Loader::ASE_KeyHELPEROBJECT(const char* token)
{
    if(!strcmp(token, "*NODE_NAME"))
    {
        ASE_GetToken(false);
        std::string str = s_token;
        SE_Util::SplitStringList nameList = SE_Util::splitString(s_token, "\"");
        SE_ASSERT(nameList.size() == 1);
        std::string name = *nameList.begin();
        ASE_GeometryObjectGroup* group = findGroup(name);
        if(!group)
        {
            group = new ASE_GeometryObjectGroup;
            group->parent.name = name;
            mSceneObject->mGeometryObjectGroup.push_back(group);
            //mCurrGeometryObjectGroup = group;
        }
    }
    else if(!strcmp(token, "*NODE_TM"))
    {
        ASE_ParseBracedBlock( &ASE_Loader::ASE_KeyNODETM );
    }
    else if(!strcmp(token, "*TM_ANIMATION"))
    {
        ASE_ParseBracedBlock(&ASE_Loader::ASE_KeyTMANIMATION);
    }
}
///////////////////////////////
ASE_GeometryObjectGroup* ASE_Loader::findGroup(std::string parentname)
{
    std::list<ASE_GeometryObjectGroup*>::iterator it = mSceneObject->mGeometryObjectGroup.begin();
    for(; it != mSceneObject->mGeometryObjectGroup.end() ; it++)
    {
        int found = parentname.find((*it)->parent.name);
        if(found!=std::string::npos)
        {
            return *it;
        }
    }
    return NULL;
}

SE_Spatial *ASE_Loader::findGroupNode(SE_Spatial *root,const char *groupname,NodeTypes type)
{
    return ((SE_CommonNode *)root)->getGroupNode(groupname,type);
}


bool ASE_Loader::isLodGroup(const char *groupname)
{
    std::string name = groupname;    
    size_t pos = name.find("_");
    std::string lodname = name.substr(0, pos);
    return lodname.compare("LOD") == 0 ;
}
