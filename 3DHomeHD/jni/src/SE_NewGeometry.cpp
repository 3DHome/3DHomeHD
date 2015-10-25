#include "SE_DynamicLibType.h"
#include "SE_NewGeometry.h"
#include "SE_SimObject.h"
#include "SE_Buffer.h"
#include "SE_Camera.h"
#include "SE_Common.h"
#include "SE_RenderUnit.h"
#include "SE_RenderManager.h"
#include "SE_BoundingVolume.h"
#include "SE_SimObjectManager.h"
#include "SE_Application.h"
#include "SE_RenderTargetManager.h"
#include <list>
#include "SE_Mesh.h"
#include "SE_GeometryData.h"
#include "SE_Matrix.h"
#include "SE_Log.h"
#include "SE_MeshSimObject.h"
#include "SE_MemLeakDetector.h"
IMPLEMENT_OBJECT(SE_NewGeometry)
struct SE_NewGeometry::_Impl
{
    typedef std::list<SE_SimObject*> SimObjectList;
    SimObjectList attachObject;
    ~_Impl()
    {
        std::list<SE_SimObject*>::iterator it;
        for(it = attachObject.begin() ; it != attachObject.end() ; it++)
        {
            if(_DEBUG)
            LOGI("SE_NewGeometry : delete sim object name = %s\n", (*it)->getName());
            SE_Application::getInstance()->getSimObjectManager()->remove((*it)->getID());
        }
    }

};
struct SE_NewGeometry::_Implchild
{
    std::list<SE_Spatial*> children;
    ~_Implchild()
    {
        std::list<SE_Spatial*>::iterator it;
        for(it = children.begin() ; it != children.end() ; it++)
        {
            delete *it;
        }
    }

};
///////////////////////////////////////////////
SE_NewGeometry::SE_NewGeometry(SE_Spatial* parent) : SE_Spatial(parent)
{
    mImpl = new SE_NewGeometry::_Impl;
    mImplchild = new SE_NewGeometry::_Implchild;
}
SE_NewGeometry::SE_NewGeometry(SE_SpatialID id, SE_Spatial* parent) : SE_Spatial(id, parent)
{
    mImpl = new SE_NewGeometry::_Impl;
    mImplchild = new SE_NewGeometry::_Implchild;
}
SE_NewGeometry::~SE_NewGeometry()
{
    delete mImpl;
    delete mImplchild;    
}
void SE_NewGeometry::attachSimObject(SE_SimObject* go)
{
    mImpl->attachObject.push_back(go);
    go->setSpatial(this);
    //save current SimObject
    setCurrentAttachedSimObj(go);
}
void SE_NewGeometry::detachSimObject(SE_SimObject* go)
{
    mImpl->attachObject.remove(go);
    go->setSpatial(NULL);
    //clear current SimObject
    setCurrentAttachedSimObj(NULL);
}

void SE_NewGeometry::write(SE_BufferOutput& output)
{
    output.writeString("SE_NewGeometry");
    output.writeInt(mImplchild->children.size());
    output.writeInt(mImpl->attachObject.size());
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        SE_SimObject* obj = *it;
        obj->write(output);
    }
    SE_Spatial::write(output);
}

void SE_NewGeometry::addChild(SE_Spatial* child)
{
    std::list<SE_Spatial*>::iterator it = mImplchild->children.begin();
    for(; it != mImplchild->children.end() ; it++)
    {
        SE_Spatial* s = *it;
        if(s == child)
            return;
    }
    mImplchild->children.push_back(child);
}
void SE_NewGeometry::removeChild(SE_Spatial* child)
{
    mImplchild->children.remove(child);
}

void SE_NewGeometry::read(SE_BufferInput& input)
{
    int attachObjNum = input.readInt();
    SE_SimObjectManager* simObjectManager = SE_Application::getInstance()->getSimObjectManager();
    for(int i = 0 ; i < attachObjNum ; i++)
    {
        std::string str = input.readString();
        SE_SimObject* obj = (SE_SimObject*)SE_ObjectFactory::create(str.c_str());
        obj->read(input);
        attachSimObject(obj);
        SE_SimObjectID id = SE_ID::createSimObjectID();
        if(!simObjectManager->set(id, obj))
        {
            LOGI("obj has exist!!!\n");
            delete obj;
        }
    }
    SE_Spatial::read(input);
}
void SE_NewGeometry::updateRenderState()
{
    SE_Spatial::updateRenderState();
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        SE_SimObject* obj = *it;
        for(int i = 0 ; i < SE_Spatial::RENDERSTATE_NUM ; i++)
        {
            //obj->setRenderState((SE_Spatial::RENDER_STATE_TYPE)i, getRenderState((SE_Spatial::RENDER_STATE_TYPE)i));    
        }
    }
}
void SE_NewGeometry::updateWorldTransform()
{
    SE_Spatial::updateWorldTransform();
    SE_Matrix4f localM;
    localM.set(getLocalRotate().toMatrix3f(), getLocalScale(), getLocalTranslate());
    mWorldTransform = getPrevMatrix().mul(localM).mul(getPostMatrix());
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        (*it)->doTransform(getWorldTransform());
    }
    std::list<SE_Spatial*>::iterator itchild = mImplchild->children.begin();
    for(; itchild != mImplchild->children.end() ; itchild++)
    {
        SE_Spatial* s = *itchild;
        s->updateWorldTransform();
    }
}

void SE_NewGeometry::setAlpha(float alpha)
{
    /*SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        (*it)->setAlpha(alpha);
    }*/
}
void SE_NewGeometry::updateBoundingVolume()
{
    if(mWorldBoundingVolume)
    {
        delete mWorldBoundingVolume;
        mWorldBoundingVolume = NULL;
    }
    switch(getBVType())
    {
    case SE_BoundingVolume::AABB:
        mWorldBoundingVolume = new SE_AABBBV;
        break;
    case SE_BoundingVolume::OBB:
        mWorldBoundingVolume = new SE_OBBBV;
        break;
    case SE_BoundingVolume::SPHERE:
        mWorldBoundingVolume = new SE_SphereBV;
        break;
    }
    if(mWorldBoundingVolume)
    {
        SE_NewGeometry::_Impl::SimObjectList::iterator it;
        for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
        {
            //(*it)->doTransform(getWorldTransform());
            SE_Vector3f* points = (*it)->getVertexArray();
            int num = (*it)->getVertexNum();
            mWorldBoundingVolume->createFromPoints(points, num);
        }
    }
}
int SE_NewGeometry::travel(SE_SpatialTravel* spatialTravel, bool travelAlways)
{
    int r = spatialTravel->visit(this);
    if(r)
        return r;
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        SE_SimObject* so = *it;
        int ret = spatialTravel->visit(so);
        if(ret && !travelAlways)
            break;
    }
    std::list<SE_Spatial*>::iterator itchild = mImplchild->children.begin();
    for(; itchild != mImplchild->children.end() ; itchild++)
    {
        SE_Spatial* s = *itchild;

            int r = s->travel(spatialTravel, travelAlways);
            if(r && !travelAlways)
                break;

    }
    if(travelAlways)
        return 0;
    else
        return r;
}
SE_NewGeometry::SPATIAL_TYPE SE_NewGeometry::getSpatialType()
{
    return GEOMETRY;
}
static SE_RenderUnit* createSelectedFrame(SE_NewGeometry* spatial)
{
    SE_RenderUnit* ru = NULL;
    if(spatial)
    {
        SE_BoundingVolume* bv = spatial->getWorldBoundingVolume();
        if(bv)
        {
            SE_AABBBV* aabbBV = NULL;
            SE_SphereBV* sphereBV = NULL;
            SE_OBBBV* obbBV = NULL; 
            switch(bv->getType())
            {
            case SE_BoundingVolume::AABB:
                {
                    aabbBV = (SE_AABBBV*)bv;
                    SE_AABB aabb = aabbBV->getGeometry();
                    SE_Segment edge[12];
                    aabb.getEdge(edge);
                    ru = new SE_LineSegRenderUnit(edge, 12, SE_Vector3f(0, 1, 0));
                }
                break;
            case SE_BoundingVolume::SPHERE:
                {
                    sphereBV = (SE_SphereBV*)bv;
                }
                break;
            case SE_BoundingVolume::OBB:
                {
                    obbBV = (SE_OBBBV*)bv;
                }
                break;
            }
        }
    }
    return ru;
}
void SE_NewGeometry::renderScene(SE_Camera* camera, SE_RenderManager* renderManager, SE_CULL_TYPE cullType)
{
    if(!isVisible())
        return;
    SE_BoundingVolume* bv = getWorldBoundingVolume();
	SE_CULL_TYPE currCullType = SE_PART_CULL;
    if(bv && cullType == SE_PART_CULL)
    {
        int culled = camera->cullBV(*bv);
        if(culled == SE_FULL_CULL)
            return;
		else
		    currCullType = (SE_CULL_TYPE)culled;
    }
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        SE_SimObject* so = *it;
        SE_SimObject::RenderUnitVector renderUnitVector = so->createRenderUnit();
        SE_SimObject::RenderUnitVector::iterator itRU;
        for(itRU = renderUnitVector.begin() ; itRU!= renderUnitVector.end(); itRU++)
        {
            if(*itRU)
            {
                //transparency object should be draw after normal object.
                const SE_ProgramDataID& spID = (*itRU)->getSurface()->getProgramDataID();
                std::string spid = spID.getStr();
                if(spid.compare("fadeinout_shader") == 0)
                {
                    renderManager->addRenderUnit(*itRU,SE_RenderManager::RQ5);                    
                    
                }
                else if((*itRU)->isDrawStencil())
                {
                    //draw stencil buffer, first
                    renderManager->addRenderUnit(*itRU,SE_RenderManager::RQ2); 
                }
                else if((*itRU)->isMirroredObject())
                {
                    //draw stencil buffer, first
                    renderManager->addRenderUnit(*itRU,SE_RenderManager::RQ3); 
                }
                else
                {
				    renderManager->addRenderUnit(*itRU, SE_RenderManager::RQ4);
            }
        }
    }
    }
    if(isSelected())
    {
        SE_RenderUnit* ru = createSelectedFrame(this);
        if(ru != NULL)
            renderManager->addRenderUnit(ru, SE_RenderManager::RQ7);
        else
        {
            SE_NewGeometry::_Impl::SimObjectList::iterator it;
            for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
            {
                SE_SimObject* so = *it;
                SE_RenderUnit* ru = so->createWireRenderUnit();
                renderManager->addRenderUnit(ru, SE_RenderManager::RQ7);
            }
        }
    }
    std::list<SE_Spatial*>::iterator itchild = mImplchild->children.begin();
    for(; itchild != mImplchild->children.end() ; itchild++)
    {
        SE_Spatial* s = *itchild;
        s->renderScene(camera, renderManager, currCullType);
    }
}

bool SE_NewGeometry::showFrame(int index){
    SE_NewGeometry::_Impl::SimObjectList::iterator it;
    for(it = mImpl->attachObject.begin() ; it != mImpl->attachObject.end() ; it++)
    {
        SE_SimObject* simObject = *it;
        SE_Mesh* mesh = simObject->getMesh();
	mMaxFrame = mesh->mKeyFrames.size();
        std::list<SE_KeyFrame<SE_NewTransform>*>::iterator it;
        for(it = mesh->mKeyFrames.begin() ; it != mesh->mKeyFrames.end() ; it++)
        {
            SE_KeyFrame<SE_NewTransform>* kf = *it;
            if(kf->key == index) {
                if (kf->data.vertexNum > 0) {
                    SE_GeometryData* geometryData = mesh->getGeometryData();
                    SE_Vector3f* oldVertex = geometryData->getVertexArray();
                    int oldVertexNum = geometryData->getVertexNum();
                    for (int i=0; i< oldVertexNum; i++) {
                        oldVertex[i].x = kf->data.vertexArray[i].x;
                        oldVertex[i].y = kf->data.vertexArray[i].y;
                        oldVertex[i].z = kf->data.vertexArray[i].z;
                     }
                     SE_Surface* surface = mesh->getSurface(0);
                     surface->upDateFaceVertex();
                 }
                 mWorldTransform = getPrevMatrix().mul(kf->data.matrix).mul(getPostMatrix());
                 break;
            }
        }
    }
    std::list<SE_Spatial*>::iterator itchild = mImplchild->children.begin();
    for(; itchild != mImplchild->children.end() ; itchild++)
    {
        SE_NewGeometry* s = (SE_NewGeometry*)*itchild;
        s->showFrame(index);
    }
    return index < mMaxFrame -1;
}

