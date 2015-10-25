#ifndef SE_NEWGEOMETRY_H
#define SE_NEWGEOMETRY_H
#include "SE_Spatial.h"
class SE_SimObject;
class SE_BufferOutput;
class SE_BufferInput;
class SE_ENTRY SE_NewGeometry : public SE_Spatial
{
    DECLARE_OBJECT(SE_NewGeometry)
public:
    SE_NewGeometry(SE_Spatial* parent = NULL);
    SE_NewGeometry(SE_SpatialID id, SE_Spatial* parent = NULL);
    ~SE_NewGeometry();
    void attachSimObject(SE_SimObject* go);
    void detachSimObject(SE_SimObject* go);
    void updateWorldTransform();
	void updateRenderState();
    void updateBoundingVolume();
    int travel(SE_SpatialTravel* spatialTravel, bool travalAways);
    void renderScene(SE_Camera* camera, SE_RenderManager* renderManager, SE_CULL_TYPE cullType);
    void write(SE_BufferOutput& output);
    void read(SE_BufferInput& input);
	SPATIAL_TYPE getSpatialType();
    void setAlpha(float alpha);
    void addChild(SE_Spatial* child);
    void removeChild(SE_Spatial* child);
    bool showFrame(int index);
   private:
    struct _Impl;
    _Impl* mImpl;
    struct _Implchild;
    _Implchild* mImplchild;
    int mMaxFrame;
};
#endif
