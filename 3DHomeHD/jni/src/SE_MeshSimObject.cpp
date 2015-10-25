#include "SE_DynamicLibType.h"
#include "SE_MeshSimObject.h"
#include "SE_Vector.h"
#include "SE_Matrix.h"
#include "SE_Quat.h"
#include "SE_Mesh.h"
#include "SE_GeometryData.h"
#include "SE_DataTransfer.h"
#include "SE_Application.h"
#include "SE_Common.h"
#include "SE_ResourceManager.h"
#include "SE_Buffer.h"
#include "SE_Mesh.h"
#include "SE_Log.h"
#include "SE_BoundingVolume.h"
#include "SE_RenderUnit.h"
#include "SE_Spatial.h"
#include "SE_BoundingVolume.h"
#include "SE_Geometry3D.h"
#include "SE_DataValueDefine.h"
#include "SE_Spatial.h"
#include "SE_CommonNode.h"
#include "SE_ObjectManager.h"
#include "SE_RenderUnitManager.h"
#include "SE_RenderTarget.h"
#include "SE_RenderTargetManager.h"
#include "SE_SceneManager.h"
#include "SE_MemLeakDetector.h"
#include "SE_RenderManager.h"
#include "SE_Geometry.h"
#include "SE_Camera.h"

IMPLEMENT_OBJECT(SE_MeshSimObject)
SE_MeshSimObject::SE_MeshSimObject(SE_Spatial* spatial) : SE_SimObject(spatial), mMesh(NULL), mOwnMesh(NOT_OWN)
{
    mSelected = false;
}

SE_MeshSimObject::SE_MeshSimObject(SE_Mesh* mesh, SE_OWN_TYPE ownMesh) : mMesh(NULL), mOwnMesh(NOT_OWN)
{
    SE_ResourceManager* resourceManager = SE_Application::getInstance()->getResourceManager();
    resourceManager->registerRes(SE_ResourceManager::OBJECT_MESH_RES, &mMeshID);
    mMesh = mesh;
    mOwnMesh = ownMesh;
    mSelected = false;
}

SE_MeshSimObject::SE_MeshSimObject(const SE_MeshID& meshID ) : mMesh(NULL), mOwnMesh(NOT_OWN)
{
    SE_ResourceManager* resourceManager = SE_Application::getInstance()->getResourceManager();
    

    SE_MeshTransfer* meshTransfer = SE_Application::getInstance()->getResourceManager()->getMeshTransfer(meshID);
    if(meshTransfer)
    {
        mMesh = meshTransfer->createMesh(SE_Application::getInstance()->getResourceManager());
        mOwnMesh = OWN;

        //insert object mesh to resource manager
        resourceManager->setObjectMesh(meshID,mMesh);

        resourceManager->registerRes(SE_ResourceManager::OBJECT_MESH_RES, &mMeshID);
        resourceManager->registerRes(SE_ResourceManager::MESH_RES, &mMeshID);
    } 
    mMeshID = meshID;
    mSelected = false;

}
SE_MeshSimObject::~SE_MeshSimObject()
{
#ifdef USE_RUMANAGER
    for(int i = 0; i < getSurfaceNum();++i)
    {
        std::string indexStr = SE_Util::intToString(i);
        std::string renderUnitName = std::string(getName()) + "_" + indexStr;
        std::string renderUnitName_img = std::string(getName()) + "_" + indexStr + "img";
        std::string renderUnitName_mirr = std::string(getName()) + "_" + indexStr + "mirr";

        SE_RenderUnitManager* srum = SE_Application::getInstance()->getRenderUnitManager();
        srum->remove(renderUnitName.c_str());
        srum->remove(renderUnitName_img.c_str());
        srum->remove(renderUnitName_mirr.c_str());
    }
#endif

    //use unregister instead delete,mesh may be used by clone object
    /*if(mOwnMesh == OWN)
    {
        delete mMesh;
    } */

    SE_ResourceManager* resourceManager = SE_Application::getInstance()->getResourceManager();
	if(mMesh != NULL){
    SE_MeshID id = mMesh->getMeshID();
    resourceManager->unregisterRes(SE_ResourceManager::OBJECT_MESH_RES, &id);
	}
    //SE_ImageDataID imageid = mSecondImageKey.c_str();
    //resourceManager->unregisterRes(SE_ResourceManager::IMAGE_RES, &imageid);

    
}
SE_Mesh* SE_MeshSimObject::getMesh()
{
    return mMesh;
}
void SE_MeshSimObject::setMesh(SE_Mesh* mesh, SE_OWN_TYPE own)
{
    //just for clone
    SE_ResourceManager* resourceManager = SE_Application::getInstance()->getResourceManager();
    SE_MeshID id = mesh->getMeshID();
    resourceManager->registerRes(SE_ResourceManager::OBJECT_MESH_RES, &id);

    if(mMesh)
    {
        resourceManager->unregisterRes(SE_ResourceManager::OBJECT_MESH_RES, &mMeshID);
        //delete mMesh;
    }
    mMesh = mesh;
}
void SE_MeshSimObject::onClick()
{
    mSelected = true;
}
void SE_MeshSimObject::read(SE_BufferInput& input)
{
    mMeshID.read(input);
    //Just load information from file,do not inflate object    
    SE_SimObject::read(input);
}
void SE_MeshSimObject::write(SE_BufferOutput& output)
{
    output.writeString("SE_MeshSimObject");
    mMeshID.write(output);
    SE_SimObject::write(output);
}
int SE_MeshSimObject::getSurfaceNum()
{
    if(!mMesh)
        return 0;
    return mMesh->getSurfaceNum();
}
void SE_MeshSimObject::getSurfaceFacet(int surfaceIndex, int*& facets, int& faceNum)
{
    if(!mMesh)
    {
        facets = NULL;
        faceNum = 0;
        return;
    }
    SE_Surface* surface = mMesh->getSurface(surfaceIndex);
    facets = surface->getFacetArray();
    faceNum = surface->getFacetNum();
}

SE_SimObject::RenderUnitVector SE_MeshSimObject::createRenderUnit()
{
    if(!mMesh)
        return RenderUnitVector();
    RenderUnitVector ruv;

    SE_SimObject* simple = getSpatial()->getSimpleObject();

    if(!simple)
    {
        simple = this;
    }    

    bool enableshadow = SE_Application::getInstance()->getEnableShadow();
    bool enablemirror = SE_Application::getInstance()->getEnableMirror();
    bool enablelighting = SE_Application::getInstance()->getEnableLighting();
    bool enableDof =  SE_Application::getInstance()->getCurrentCamera()->isNeedDof();

#if 0
    if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::SHADOWGENERATOR) && enableshadow)
    {
        SE_RenderTarget* shadowrt = SE_Application::getInstance()->getRenderTargetManager()->getShadowTarget();
        //if(SE_Application::getInstance()->getSpatialManager()->isNeedUpdateShadowMap() || !shadowrt->isInit())
        if(SE_Application::getInstance()->getRenderManager()->isNeedUpdateShadowMap() || !shadowrt->isInit())
        {           
        
        int surfaceNum = simple->getSurfaceNum();
        //generate a new ru to render to fbo
    for(int i = 0 ; i < surfaceNum ; i++)
    {
        std::string indexStr = SE_Util::intToString(i);
        std::string renderUnitName = std::string(getName()) + "_" + indexStr;
            SE_Surface* surface = NULL;
            
            surface = simple->getMesh()->getSurface(i);

        //SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);

#ifdef USE_RUMANAGER
        SE_RenderUnitManager* srum = SE_Application::getInstance()->getRenderUnitManager();
        SE_TriSurfaceRenderUnit* tsru = (SE_TriSurfaceRenderUnit*)srum->find(renderUnitName.c_str());

        if(!tsru)
        {
            tsru = new SE_TriSurfaceRenderUnit(surface);
            srum->insert(renderUnitName.c_str(),tsru);
        }
        else
        {
            tsru->reset();
        }
#else
            SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);
#endif

                tsru->setLayer(*getSpatial()->getWorldLayer());
        tsru->setName(renderUnitName.c_str());
        tsru->setPrimitiveType(getPrimitiveType());
                tsru->setRenderableSpatial(this->getSpatial());
        if(!isUseWorldMatrix())
        {
            tsru->setWorldTransform(getSpatial()->getWorldTransform().mul(getLocalMatrix()));
        }
        else
        {
            tsru->setWorldTransform(getWorldMatrix());
        }
            //SE_RenderState** rs = getRenderState();
                for(int j = 0 ; j < RENDERSTATE_NUM ; j++)
        {
                        //tsru->setRenderState((RENDER_STATE_TYPE)j, rs[j], NOT_OWN);
                        tsru->setRenderState((RENDER_STATE_TYPE)j, getSpatial()->getRenderState((RENDER_STATE_TYPE)j), NOT_OWN);
        }

            tsru->setBlendState(false);            

            tsru->setShaderName(surface->getProgramDataID().getStr());
            tsru->setRenderName(surface->getRendererID().getStr());
                tsru->setEnableCullFace(false);
                //tsru->setRenderToFbo(true);

            //need render shadow
                tsru->setNeedGenerateShadow(true);
            if(1)
            {
                //vsm
            tsru->setShaderName(VSM_SHADER);
            tsru->setRenderName(VSM_RENDERER);
                SE_Application::getInstance()->getRenderManager()->setNeedBlurShadow(true);
            }
            else
            {
                tsru->setShaderName(DEFAULTNOIMG_SHADER);
                tsru->setRenderName(DEFAULTNOIMG_RENDERER); 
            }

            std::string fboname = surface->getRenderToFboName();
            if(fboname.empty())
            {
                tsru->setRenderToFboName(getSpatial()->getSpatialName());
            }
            else
            {
                tsru->setRenderToFboName(fboname.c_str());
            }
            
            ruv.push_back(tsru);
        }

        }
    }
#endif

#if 0
    if(this->getSpatial()->isNeedGenerateMirror() && enablemirror)
    {
                SE_Spatial* _mirrorobj = this->getSpatial()->getMirrorObject();
        if(_mirrorobj)
                {

                SE_RenderTarget* mirrorrt = SE_Application::getInstance()->getRenderTargetManager()->get(_mirrorobj->getSpatialName());
                if(!mirrorrt)
                {
                RenderUnitVector mirrorCreator = createUnitForTarget();
                for(int i = 0; i < mirrorCreator.size(); i++)
                    {
                    ruv.push_back(mirrorCreator[i]);
                    }

                    }
                    else
                    {

                SE_Camera* ca = SE_Application::getInstance()->getCurrentCamera();

                bool cameraChanged = ca->isChanged();

                    if(SE_Application::getInstance()->getRenderManager()->isNeedUpdateMirrorMap() || !mirrorrt->isInit() || cameraChanged)
                    {

                    RenderUnitVector mirrorCreator = createUnitForTarget();
                    for(int i = 0; i < mirrorCreator.size(); i++)
                {
                        ruv.push_back(mirrorCreator[i]);
                }
            }
            }
            }
            }
#else

    SE_Camera* c = SE_Application::getInstance()->getCurrentCamera();
    if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::MIRRORGENERATOR) &&( SE_Application::getInstance()->getRenderManager()->isNeedUpdateMirrorMap() || c->isChanged()) )
    {
        SE_Application::getInstance()->getRenderManager()->insertMirrorGenerator(this->getSpatial());
    }
#endif


        //render object
        int surfaceNum = getSurfaceNum();
        for(int i = 0 ; i < surfaceNum; i++)
    {
        if(enableshadow)
        {
            if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::SHADOWOBJECT))
            {
                //if enable shadow, disable draw shadow object
                this->getSpatial()->setShadowObjectVisibility(false);
            }
        }
        else
        {
            if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::SHADOWOBJECT))
            {
                //if disable shadow, enable draw shadow object
                this->getSpatial()->setShadowObjectVisibility(true);
            }
        }
        std::string indexStr = SE_Util::intToString(i);
        std::string renderUnitName = std::string(getName()) + "_" + indexStr;
        SE_Surface* surface = mMesh->getSurface(i);      
        //SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);

#ifdef USE_RUMANAGER
            SE_RenderUnitManager* srum = SE_Application::getInstance()->getRenderUnitManager();
            SE_TriSurfaceRenderUnit* tsru = (SE_TriSurfaceRenderUnit*)srum->find(renderUnitName.c_str());

            if(!tsru)
            {
                tsru = new SE_TriSurfaceRenderUnit(surface);
                srum->insert(renderUnitName.c_str(),tsru);
            }
            else
            {
                tsru->reset();
            }
#else
            SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);
#endif

            if(this->getSpatial()->isSpatialHasRuntimeAttribute(SE_SpatialAttribute::IGNORVERTEXBUFFER))
            {
                tsru->setDontUseVertexBuffer(true);
            }

            
            tsru->setTexCoordXYReverse(this->getSpatial()->getTexCoordXYReverse());


            if(enableDof)
            {
                tsru->setName(renderUnitName.c_str());
                tsru->setPrimitiveType(getPrimitiveType());
                tsru->setRenderableSpatial(this->getSpatial());
                if(!isUseWorldMatrix())
                {
                    tsru->setWorldTransform(getSpatial()->getWorldTransform().mul(getLocalMatrix()));
                }
                else
                {
                    tsru->setWorldTransform(getWorldMatrix());
                }
                    //SE_RenderState** rs = getRenderState();
                for(int j = 0 ; j < RENDERSTATE_NUM ; j++)
                {
                    //tsru->setRenderState((RENDER_STATE_TYPE)j, rs[j], NOT_OWN);
                    tsru->setRenderState((RENDER_STATE_TYPE)j, getSpatial()->getRenderState((RENDER_STATE_TYPE)j), NOT_OWN);
                }

                //fix me:
                tsru->setBlendState(false);       
    
                tsru->setLayer(*getSpatial()->getWorldLayer());
                bool needCullFace = getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::CULLFACE);
                tsru->setEnableCullFace(needCullFace);

                if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::LIGHTING))
                {
                    tsru->setShaderName(DOFLIGHTGEN_SHADER);
                    tsru->setRenderName(DOFLIGHTGEN_RENDERER);
                }
                else
                {
                    tsru->setShaderName(DOFGEN_SHADER);
                    tsru->setRenderName(DOFGEN_RENDERER);
                }
                //tsru->setRenderToFbo(true);
                tsru->setNeedDofGen(true);
                SE_Application::getInstance()->getRenderManager()->setNeedBlurShadow(true);

            }
            else
            {
                SE_Application::getInstance()->getRenderManager()->setNeedBlurShadow(false);
        
        tsru->setName(renderUnitName.c_str());
        tsru->setPrimitiveType(getPrimitiveType());
        tsru->setRenderableSpatial(this->getSpatial());

                SE_AABBBV* aabbbv = (SE_AABBBV*)getSpatial()->getWorldBoundingVolume();

                if(aabbbv)
                {
                    SE_Vector3f min = aabbbv->getGeometry().getMin();
                    SE_Vector3f max = aabbbv->getGeometry().getMax();
                    tsru->setMinBounding(min);
                    tsru->setMaxBounding(max);
                }

                //tsru->setNewImageKey(mSecondImageKey.c_str());

        if(!isUseWorldMatrix())
        {
            tsru->setWorldTransform(getSpatial()->getWorldTransform().mul(getLocalMatrix()));
        }
        else
        {
            tsru->setWorldTransform(getWorldMatrix());
        }
            //SE_RenderState** rs = getRenderState();
                for(int j = 0 ; j < RENDERSTATE_NUM ; j++)
        {
                        //tsru->setRenderState((RENDER_STATE_TYPE)j, rs[j], NOT_OWN);
                        tsru->setRenderState((RENDER_STATE_TYPE)j, getSpatial()->getRenderState((RENDER_STATE_TYPE)j), NOT_OWN);
        }
    
                if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::BLENDABLE))
        {
            tsru->setBlendState(true);
                tsru->setAlpha(this->getSpatial()->getAlpha());

        }
        else
        {
            tsru->setBlendState(false);
        }
    
                if(this->getSpatial()->isNeedForeverBlend())
                {
                    tsru->setBlendState(true);
                    this->getSpatial()->setSpatialEffectAttribute(SE_SpatialAttribute::BLENDABLE,true);
                    SE_Vector4f d = this->getSpatial()->getEffectData();
                    if(d.z > 0.1)
                    {
                        tsru->setAlpha(d.z);
                    }
                    else
                    {
                        tsru->setAlpha(this->getSpatial()->getAlpha());
                }
                }

                //for shadow object
                if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::SHADOWOBJECT))
        {
            tsru->setBlendState(true);
            tsru->setAlpha(1.0);      
#if 0
                    SE_Layer* l = this->getSpatial()->getWorldLayer();
                    if(this->getSpatial()->isSelected())
                    {
                        
                        l->setLayer(2);
                    }
                    else
                    {
                        l->setLayer(0);
                    }
#endif
        }
    
                    tsru->setLayer(*getSpatial()->getWorldLayer());
                
            bool needCullFace = getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::CULLFACE);
            tsru->setEnableCullFace(needCullFace);
            tsru->setShaderName(DEFAULT_SHADER);
            tsru->setRenderName(DEFAULT_RENDERER);
            tsru->setIsUseFbo(false);                

                //one object has just one effect,now
                /*int mirrorGeneratorCount = SE_Application::getInstance()->getSpatialManager()->mirrorGeneratorCount();
                int shadowGeneratorCount = SE_Application::getInstance()->getSpatialManager()->shadowGeneratorCount();*/
                int mirrorGeneratorCount = SE_Application::getInstance()->getRenderManager()->mirrorGeneratorCount();
                int shadowGeneratorCount = SE_Application::getInstance()->getRenderManager()->shadowGeneratorCount();
                if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::ALPHATEST))
            {
                tsru->setShaderName(ALPHATEST_SHADER);
                tsru->setRenderName(ALPHATEST_RENDERER);
            }
                else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::FLAGWAVE))
                {
                    tsru->setShaderName(FLAGWAVE_SHADER);
                    tsru->setRenderName(FLAGWAVE_RENDERER);   
                    tsru->setEnableCullFace(false);
                }
                else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::CLOAKFLAGWAVE))
                {
                    tsru->setShaderName(CLOAKFLAGWAVE_SHADER);
                    tsru->setRenderName(CLOAKFLAGWAVE_RENDERER);   
                    tsru->setEnableCullFace(false);
                }
                else if(this->getSpatial()->isNeedDrawLine())
                {
                    tsru->setShaderName(DRAWLINE_SHADER);
                    tsru->setRenderName(DRAWLINE_RENDERER);
                    tsru->setDontUseVertexBuffer(true);
                   
                }
            else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::SHADOWRENDER) && enableshadow)
                {
                    //need render shadow,disable other fbo function
                if(shadowGeneratorCount > 0)
                {
                    tsru->setNeedRenderShadow(true);
                    tsru->setFboReplaceCurrentTexture(false);
                    tsru->setShaderName(SHADOWMAP_SHADER);
                    tsru->setRenderName(SHADOWMAP_RENDERER);       
                    tsru->setNeedUseBluredShadowMap(true);
                tsru->setIsUseFbo(true);
                }
                else
                {
                    tsru->setNeedRenderShadow(false);
                    tsru->setNeedUseBluredShadowMap(false);
                    tsru->setIsUseFbo(false);
                }
            }
            else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::MIRRORRENDER) && enablemirror)
            {
                    tsru->setNeedRenderMirror(true);
                    tsru->setFboReplaceCurrentTexture(false);
                    tsru->setShaderName(MIRROR_SHADER);
                    tsru->setRenderName(MIRROR_RENDERER);  
                tsru->setIsUseFbo(true);
                    if (this->getSpatial()->getAlpha() < 1.0)
                    {
                    tsru->setBlendState(true);
                    }
                            
#if 0
                            SE_Layer* l = this->getSpatial()->getWorldLayer();
                            l->setLayer(3);
#endif

                            tsru->setLayer(*getSpatial()->getWorldLayer());      

                tsru->setAlpha(this->getSpatial()->getAlpha());
                tsru->setMirrorObject(this->getSpatial());
                tsru->setMirrorPlan(this->getSpatial()->getMirrorPlan());
                   
            }
            else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::UVANIMATION))
            {
                tsru->setShaderName(UVANIMATION_SHADER);
                tsru->setRenderName(UVANIMATION_RENDERER); 

                tsru->setTexCoordOffset(this->getSpatial()->getTexCoordOffset());
            }
            else if(this->getSpatial()->isNeedParticle())
            {
                tsru->setShaderName(PARTICLE_SHADER);
                tsru->setRenderName(PARTICLE_RENDERER);                
                    if(this->getSpatial()->isNeedDepthTest())
                    {
                        tsru->setDepthState(true);
                    }
                    else
                    {
                        tsru->setDepthState(false);
                    }
            }
            else if(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::LIGHTING) && enablelighting)
            {
                if(this->getMesh()->getSurface(0)->hasSpotLight())
                {
                    tsru->setShaderName(SIMPLELIGHTING_SPOT_SHADER);
                    tsru->setRenderName(SIMPLELIGHTING_SPOT_RENDERER); 
                }
                else
                {
#ifdef WIN32
                tsru->setShaderName(SIMPLELIGHTING_SHADER);
                tsru->setRenderName(SIMPLELIGHTING_RENDERER); 
#else
                        tsru->setShaderName(SIMPLELIGHTING_SHADER);
                        tsru->setRenderName(SIMPLELIGHTING_RENDERER); 
#endif
                }

                //for "pc_face" object
                tsru->setNeedSpecLight(this->getSpatial()->isNeedSpecLight());

                }
                else
                {
                    //do not need render shadow
            //set replace current mesh texture
                //bool replace = getMesh()->getSurface(0)->isNeedFboReplaceCurrentTexture();
                //tsru->setFboReplaceCurrentTexture(replace);

                ////which mesh texture should be replace with fbo(0-3)
                //tsru->setFboReplaceTextureIndex(getMesh()->getSurface(0)->getFboReplaceTextureIndex());

                //std::string fboname = surface->getUsedFboName();
                //if(fboname.empty())
                //{
                //    tsru->setUsedFboName(getSpatial()->getSpatialName());
                //}
                //else
                //{
                //    tsru->setUsedFboName(fboname.c_str());
                //}
                }

#if 0        
                
                    tsru->setRenderToFbo(true);
//just for demo
                   
            std::string spatialname = this->getSpatial()->getSpatialName();            

            //
                    if(spatialname == "guang@group_house@ceshi_basedata.cbf" || spatialname == "guang1@group_house@ceshi_basedata.cbf" || spatialname == "guang2@group_house@ceshi_basedata.cbf"
                       || spatialname == "guang3@group_house@ceshi_basedata.cbf" || spatialname == "guang4@group_house@ceshi_basedata.cbf" || spatialname == "guang05@group_house@ceshi_basedata.cbf"
                       || spatialname == "guang06@group_house@ceshi_basedata.cbf")
                    {
                        SE_Layer* l = this->getSpatial()->getWorldLayer();
                        l->setLayer(2);
                        tsru->setLayer(*l);
                        tsru->setNeedColorEffect(true);
                        tsru->setBlendState(true);
                        tsru->setAlpha(1.0);
                    }
                    if(spatialname == "Object50@group_house@ceshi_basedata.cbf" || spatialname == "Object51@group_house@ceshi_basedata.cbf" || spatialname == "Object52@group_house@ceshi_basedata.cbf"
                       || spatialname == "Object53@group_house@ceshi_basedata.cbf" || spatialname == "Object54@group_house@ceshi_basedata.cbf" || spatialname == "Object55@group_house@ceshi_basedata.cbf"
                       || spatialname == "Object02@group_house@ceshi_basedata.cbf" || spatialname == "Object56@group_house@ceshi_basedata.cbf")
                    {
                        SE_Layer* l = this->getSpatial()->getWorldLayer();
                        l->setLayer(1);
                        tsru->setLayer(*l);
                    }

                    if(spatialname == "wenzi@group_house@ceshi_basedata.cbf" || spatialname == "Box01@group_house@ceshi_basedata.cbf"
                        || spatialname == "Box02@group_house@ceshi_basedata.cbf" || spatialname == "Box03@group_house@ceshi_basedata.cbf"
                        || spatialname == "Plane18@group_house@ceshi_basedata.cbf")
                    {
                        tsru->setNeedHighLight(true);
                    }
#endif

                  
            }
                 
        ruv.push_back(tsru);
    }    

#if 0
    //forward additive
    if(this->getSpatial()->isNeedLighting() && enablelighting && !this->getSpatial()->isNeedBlend()  && 0)
    {
        
        for(int i = 0 ; i < surfaceNum; i++)
        {
            std::string indexStr = SE_Util::intToString(i);
            std::string renderUnitName = std::string(getName()) + "_" + indexStr;
            SE_Surface* surface = mMesh->getSurface(i);      
            //SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);

    #ifdef USE_RUMANAGER
                SE_RenderUnitManager* srum = SE_Application::getInstance()->getRenderUnitManager();
                SE_TriSurfaceRenderUnit* tsru = (SE_TriSurfaceRenderUnit*)srum->find(renderUnitName.c_str());

                if(!tsru)
                {
                    tsru = new SE_TriSurfaceRenderUnit(surface);
                    srum->insert(renderUnitName.c_str(),tsru);
                }
                else
                {
                    tsru->reset();
                }
    #else
                SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);
    #endif

            
            tsru->setName(renderUnitName.c_str());
            tsru->setPrimitiveType(getPrimitiveType());
            tsru->setRenderableSpatial(this->getSpatial());
            if(!isUseWorldMatrix())
            {
                tsru->setWorldTransform(getSpatial()->getWorldTransform().mul(getLocalMatrix()));
            }
            else
            {
                tsru->setWorldTransform(getWorldMatrix());
            }

            //SE_RenderState** rs = getRenderState();
            for(int j = 0 ; j < RENDERSTATE_NUM ; j++)
            {
                //tsru->setRenderState((RENDER_STATE_TYPE)j, rs[j], NOT_OWN);
                tsru->setRenderState((RENDER_STATE_TYPE)j, getSpatial()->getRenderState((RENDER_STATE_TYPE)j), NOT_OWN);
            }


            //additive do not need blend
            tsru->setBlendState(false);   
                
            tsru->setLayer(*getSpatial()->getWorldLayer());
            bool needCullFace = getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::CULLFACE);
            tsru->setEnableCullFace(needCullFace);
            tsru->setShaderName(DEFAULT_SHADER);
            tsru->setRenderName(DEFAULT_RENDERER);
            tsru->setIsUseFbo(false);

            tsru->setIsAdditive(1);

            //one object has just one effect,now
            
            if(this->getSpatial()->isNeedLighting() && enablelighting)
            {
    #ifdef WIN32
                tsru->setShaderName(SIMPLELIGHTING_SHADER);
                tsru->setRenderName(SIMPLELIGHTING_RENDERER); 
    #else
                tsru->setShaderName(SIMPLELIGHTINGPLUGIN_SHADER);
                tsru->setRenderName(SIMPLELIGHTINGPLUGIN_RENDERER); 
    #endif
                
            }            
            else
            {
                //do not need render shadow
                //set replace current mesh texture
                //bool replace = getMesh()->getSurface(0)->isNeedFboReplaceCurrentTexture();
                //tsru->setFboReplaceCurrentTexture(replace);

                ////which mesh texture should be replace with fbo(0-3)
                //tsru->setFboReplaceTextureIndex(getMesh()->getSurface(0)->getFboReplaceTextureIndex());

                //std::string fboname = surface->getUsedFboName();
                //if(fboname.empty())
                //{
                //    tsru->setUsedFboName(getSpatial()->getSpatialName());
                //}
                //else
                //{
                //    tsru->setUsedFboName(fboname.c_str());
                //}
            }
                 
            ruv.push_back(tsru);
        }
    }
#endif

    return ruv;
}

SE_RenderUnit* SE_MeshSimObject::createWireRenderUnit()
{

    //generate world geometry data
    SE_GeometryData data;
    SE_Matrix4f m2w = this->getSpatial()->getWorldTransform();
    SE_GeometryData::transform(mMesh->getGeometryData(),m2w,&data);

    int faceNum = data.getFaceNum();
    int vertexNum = data.getVertexNum();
    SE_Vector3f* vertex = data.getVertexArray();
    SE_Vector3i* faces = data.getFaceArray();
    
    SE_Segment* seg = new SE_Segment[faceNum * 3];
    int n = 0 ;
    for(int i = 0 ; i < faceNum ; i++)
    {
        SE_Vector3i* f = &faces[i];
        seg[n++].set(vertex[f->x], vertex[f->y]);
        seg[n++].set(vertex[f->y], vertex[f->z]);
        seg[n++].set(vertex[f->z], vertex[f->x]);
    }
    SE_RenderUnit* ru = new SE_LineSegRenderUnit(seg, faceNum * 3, SE_Vector3f(0, 1, 0));
    delete[] seg;
    return ru;
}

SE_SimObject * SE_MeshSimObject::clone(int index,bool createNewMesh)
{
    SE_MeshSimObject * dest = new SE_MeshSimObject();
    SE_Mesh* m = NULL;

    if(createNewMesh)
    {
        SE_MeshTransfer* meshTransfer = SE_Application::getInstance()->getResourceManager()->getMeshTransfer(mMeshID);
        m = mMesh->clone();
        if(m)
        {
            std::string newmeshid = mMeshID.getStr();
            newmeshid += SE_Util::intToString(index);
            SE_MeshID newid = newmeshid.c_str();
            m->setMeshID(newid);
            SE_Application::getInstance()->getResourceManager()->setObjectMesh(newid,m);            

            //change imagekey 
            SE_TextureUnit* tu = m->getSurface(0)->getTexture()->getTextureUnit(0);
            SE_ImageDataID *idarray = tu->getImageDataID();
            std::string cloneImageID = idarray[0].getStr();
            
            const char* path = SE_Application::getInstance()->getResourceManager()->getIdPath(cloneImageID.c_str());
            if(path) {
                std::string cloneIndex = SE_Util::intToString(index);
                std::string sep = "#";
                cloneImageID = cloneImageID + sep + cloneIndex;
                idarray[0] = cloneImageID.c_str();
                SE_Application::getInstance()->getResourceManager()->setIdPath(cloneImageID.c_str(), path, true);
                SE_ImageDataID imageDataid =  SE_ImageDataID(cloneImageID.c_str());
                SE_Application::getInstance()->getResourceManager()->registerRes(SE_ResourceManager::IMAGE_RES, &imageDataid);
            }
        }
    }
    else
    {
        m = this->getMesh();     
    }
    dest->setMesh(m,NOT_OWN);

    //use same mesh to create clone object not renew

#if 0
    SE_MeshTransfer* meshTransfer = SE_Application::getInstance()->getResourceManager()->getMeshTransfer(mMeshID);
    
    if(meshTransfer && 0)
    {
        dest = new SE_MeshSimObject(mMeshID);//mesh transfer is exist,create object from mesh id.
    }
    else
    {
        SE_Mesh* mesh = mMesh->clone();
        dest = new SE_MeshSimObject(mesh,OWN);
    }

    if(!dest)
    {
        return NULL;
    }
#endif

   /* std::string preName = getPrefixName();

    char buff[512] = {0};
    sprintf(buff,"%d",index);
    std::string num(buff);
    std::string destPreName = preName + "_clone_" + num;
    std::string newDestName = objName.replace(0,preName.length(),destPreName);
    */

    char buff[512] = {0};
    sprintf(buff,"%d",index);
    std::string num(buff);

    std::string objName = getName();
    int x = objName.find("_clone_");
    std::string destName;
    if (x > 0) {
        destName = objName.substr(x + 7) + num;
    } else {
        destName = objName + "_clone_" + num;
    }
    dest->setName(destName.c_str());    

    return dest;
}
bool SE_MeshSimObject::replaceImage(const char *filePath,int imaIndex) 
{
    //return getMesh()->getTexture(0)->getTextureUnit(0)->replaceImageData(filePath,imaIndex);
    return false;
}

void SE_MeshSimObject::inflate()
{
    SE_MeshTransfer* meshTransfer = SE_Application::getInstance()->getResourceManager()->getMeshTransfer(mMeshID);
    if(!meshTransfer)
    {
        //mMeshID = SE_MeshID::INVALID;
        if(SE_Application::getInstance()->SEHomeDebug)
        LOGI("[%s] meshTransfer not found!!!!,mesh id is %s\n",this->getName(),mMeshID.getStr());
        return;
    }

    

    if(mMesh)
    {
        //the object has inflated
        return;
    }

    SE_ResourceManager* resourceManager = SE_Application::getInstance()->getResourceManager();
    

    mMesh = meshTransfer->createMesh(SE_Application::getInstance()->getResourceManager());

    if(mMesh)
    {
        resourceManager->setObjectMesh(mMeshID,mMesh);
        mMesh->setMeshID(mMeshID);


        resourceManager->registerRes(SE_ResourceManager::MESH_RES, &mMeshID);
        resourceManager->registerRes(SE_ResourceManager::OBJECT_MESH_RES, &mMeshID);
        mOwnMesh = OWN;
    }
    else
    {
        return;
    }

    SE_Geometry* sp = (SE_Geometry*)getSpatial();

    if(!sp)
    {
        if(SE_Application::getInstance()->SEHomeDebug)
            LOGI("Error !!!!Mesh simobject not attach to a spatial !!!!!!!!!\n");
    }

    bool mipmap = sp->isNeedGenerateMipMap();
    if(!mipmap)
    {
        int num = this->getMesh()->getSurfaceNum();
        for(int i = 0; i < num; ++i)
        {
            SE_Texture* tx = this->getMesh()->getSurface(i)->getTexture();
            if(!tx)
            {
                continue;
            }
            int txnum = tx->getTexUnitNum();
            for(int j = 0; j < txnum; ++j)
            {
                tx->getTextureUnit(j)->setSampleMin(LINEAR);
            }
        }
    }
    SE_Vector4f spatialData = sp->getEffectData();
    int num = this->getMesh()->getSurfaceNum(); 
    
    for(int i = 0; i < num; ++i)
    {
        this->getMesh()->getSurface(i)->getMaterialData()->shiny = spatialData.x;
        this->getMesh()->getSurface(i)->getMaterialData()->shinessStrength = spatialData.y;

    }

    this->getSpatial()->applyLight();
    
}

bool SE_MeshSimObject::changeImageKey(const char* newKey)
{

#if 0
    mSecondImageKey = newKey;

    SE_ImageDataID id = newKey;

    ////id exist
    //SE_TextureUnit* tu = this->getMesh()->getSurface(0)->getTexture()->getTextureUnit(0);
    //    
    //SE_ImageDataID* imageDataIDArray = new SE_ImageDataID[1];
    //imageDataIDArray[0] = id;            
    //tu->setImageDataID(imageDataIDArray,1);

    SE_ResourceManager* rm = SE_Application::getInstance()->getResourceManager();

    rm->registerRes(SE_ResourceManager::IMAGE_RES, &id);
    
    if(rm->getImageData(id))
    {
        
        return true;
    }

    //id not exist,add it
    rm->setImageData(id,NULL);
#endif
    return false;   
}

SE_SimObject::RenderUnitVector SE_MeshSimObject::createUnitForTarget(SE_RenderTarget* rt)
{
    RenderUnitVector ruv;
    int surfaceNum = this->getSurfaceNum();

    for(int i = 0; i < surfaceNum; ++i)
            {

        SE_Surface* surface = getMesh()->getSurface(i);
        SE_TriSurfaceRenderUnit* tsru = new SE_TriSurfaceRenderUnit(surface);


        std::string indexStr = SE_Util::intToString(i);
        std::string spatialName = this->getSpatial()->getSpatialName();
        std::string renderUnitName = spatialName + "_" + indexStr;

                    tsru->setLayer(*getSpatial()->getWorldLayer());
                    
                    tsru->setEnableCullFace(false);

                    tsru->setName(renderUnitName.c_str());

                    tsru->setPrimitiveType(getPrimitiveType());

                    tsru->setRenderableSpatial(this->getSpatial());

                    if(!isUseWorldMatrix())
                    {
                        tsru->setWorldTransform(getSpatial()->getWorldTransform().mul(getLocalMatrix()));
                    }
                    else
                    {
                        tsru->setWorldTransform(getWorldMatrix());
                    }
                    
                    for(int j = 0 ; j < RENDERSTATE_NUM ; j++)
                    {
                    tsru->setRenderState((RENDER_STATE_TYPE)j, getSpatial()->getRenderState((RENDER_STATE_TYPE)j), NOT_OWN);
                    }

                    //tsru->setBlendState(this->getSpatial()->isSpatialEffectHasAttribute(SE_SpatialAttribute::BLENDABLE));
tsru->setBlendState(true);
        //render fbo no need alpha effect;
        tsru->setAlpha(0.1);   
            tsru->setEnableCullFace(true);

                    //need render mirror
                    tsru->setNeedGenerateMirror(true);
                    tsru->setShaderName(DEFAULT_SHADER);
                    tsru->setRenderName(DEFAULT_RENDERER);            

        //tsru->setRenderToFbo(true);

                        SE_Spatial* mirrorobj = this->getSpatial()->getMirrorObject();
                        if(mirrorobj)
                        {
                            tsru->setMirrorObject(mirrorobj);
                        }

                        tsru->setMirrorPlan(this->getSpatial()->getMirrorPlan());

        tsru->setRenderTarget(rt);

                        ruv.push_back(tsru);
                    }
    return ruv;
}
