#include "SE_DynamicLibType.h"
#include "SE_RenderDefine.h"


SE_RenderDefine::SE_RenderDefine()
{

    std::string default_render("default_renderer");
    std::string simple_render_class("SE_SimpleSurfaceRenderer");
    mRenderIdArray.push_back(default_render);
    mRenderClassName.push_back(simple_render_class);

    std::string fadeinout_render("lightmap_renderer");
    std::string fadeinout_render_class("SE_LightMapRenderer");
    mRenderIdArray.push_back(fadeinout_render);
    mRenderClassName.push_back(fadeinout_render_class);

    std::string skeletalanimation_render("skeletalanimation_renderer");
    std::string skeletalanimation_render_class("SE_SkeletalAnimationRenderer");
    mRenderIdArray.push_back(skeletalanimation_render);
    mRenderClassName.push_back(skeletalanimation_render_class);

    std::string simplelighting_render("simplelighting_renderer");
    std::string simplelighting_render_class("SE_SimpleLightingRenderer");
    mRenderIdArray.push_back(simplelighting_render);
    mRenderClassName.push_back(simplelighting_render_class);

    std::string normalmap_renderer("normalmap_renderer");
    std::string normalmap_render_class("SE_NormalMapRenderer");
    mRenderIdArray.push_back(normalmap_renderer);
    mRenderClassName.push_back(normalmap_render_class);

    std::string colorextract_renderer("colorextract_renderer");
    std::string colorextract_renderer_class("SE_ColorExtractRenderer");
    mRenderIdArray.push_back(colorextract_renderer);
    mRenderClassName.push_back(colorextract_renderer_class);

    std::string lineseg_renderer("lineseg_renderer");
    std::string lineseg_renderer_class("SE_LineSegRenderer");
    mRenderIdArray.push_back(lineseg_renderer);
    mRenderClassName.push_back(lineseg_renderer_class);

    std::string particle_renderer("particle_renderer");
    std::string particle_renderer_class("SE_ParticleRenderer");
    mRenderIdArray.push_back(particle_renderer);
    mRenderClassName.push_back(particle_renderer_class);

    std::string shadowmap_renderer("shadowmap_renderer");
    std::string shadowmap_renderer_class("SE_ShadowMapRenderer");
    mRenderIdArray.push_back(shadowmap_renderer);
    mRenderClassName.push_back(shadowmap_renderer_class);
    
    std::string mirror_renderer("mirror_renderer");
    std::string mirror_renderer_class("SE_MirrorRenderer");
    mRenderIdArray.push_back(mirror_renderer);
    mRenderClassName.push_back(mirror_renderer_class);
    
 
    std::string vsm_renderer("vsm_renderer");
    std::string vsm_renderer_class("SE_DrawVSMRenderer");
    mRenderIdArray.push_back(vsm_renderer);
    mRenderClassName.push_back(vsm_renderer_class);

#if 0
    std::string blurv_renderer("blurv_renderer");
    std::string blurv_renderer_class("SE_BlurVRenderer");
    mRenderIdArray.push_back(blurv_renderer);
    mRenderClassName.push_back(blurv_renderer_class);

    std::string blurh_renderer("blurh_renderer");
    std::string blurh_renderer_class("SE_BlurHRenderer");
    mRenderIdArray.push_back(blurh_renderer);
    mRenderClassName.push_back(blurh_renderer_class);
#endif

    std::string blur_renderer("blur_renderer");
    std::string blur_renderer_class("SE_BlurRenderer");
    mRenderIdArray.push_back(blur_renderer);
    mRenderClassName.push_back(blur_renderer_class);

    std::string alphatest_renderer("alphatest_renderer");
    std::string alphatest_renderer_class("SE_AlphaTestRenderer");
    mRenderIdArray.push_back(alphatest_renderer);
    mRenderClassName.push_back(alphatest_renderer_class);

    std::string defaultnoimg_renderer("defaultnoimg_renderer");
    std::string defaultnoimg_renderer_class("SE_SimpleNoImgRenderer");
    mRenderIdArray.push_back(defaultnoimg_renderer);
    mRenderClassName.push_back(defaultnoimg_renderer_class);

    std::string uvanimation_renderer("uvanimation_renderer");
    std::string uvanimation_renderer_class("SE_SimpleUVAnimationRenderer");
    mRenderIdArray.push_back(uvanimation_renderer);
    mRenderClassName.push_back(uvanimation_renderer_class);

    std::string downsample_renderer("downsample_renderer");
    std::string downsample_renderer_class("SE_DownSampleRenderer");
    mRenderIdArray.push_back(downsample_renderer);
    mRenderClassName.push_back(downsample_renderer_class);

    std::string dofgen_renderer("dofgen_renderer");
    std::string dofgen_renderer_class("SE_DofGenRenderer");
    mRenderIdArray.push_back(dofgen_renderer);
    mRenderClassName.push_back(dofgen_renderer_class);

    std::string doflightgen_renderer("doflightgen_renderer");
    std::string doflightgen_renderer_class("SE_DofLightGenRenderer");
    mRenderIdArray.push_back(doflightgen_renderer);
    mRenderClassName.push_back(doflightgen_renderer_class);

    std::string drawdof_renderer("drawdof_renderer");
    std::string drawdof_renderer_class("SE_DrawDofRenderer");
    mRenderIdArray.push_back(drawdof_renderer);
    mRenderClassName.push_back(drawdof_renderer_class);

    std::string flagwave_renderer("flagwave_renderer");
    std::string flagwave_renderer_class("SE_FlagWaveRenderer");
    mRenderIdArray.push_back(flagwave_renderer);
    mRenderClassName.push_back(flagwave_renderer_class);

    std::string drawline_renderer("drawline_renderer");
    std::string drawline_renderer_class("SE_DrawLineRenderer");
    mRenderIdArray.push_back(drawline_renderer);
    mRenderClassName.push_back(drawline_renderer_class);

    std::string coloreffect_renderer("coloreffect_renderer");
    std::string coloreffect_renderer_class("SE_ColorEffectRenderer");
    mRenderIdArray.push_back(coloreffect_renderer);
    mRenderClassName.push_back(coloreffect_renderer_class);

    std::string drawrendertargettoscreen_renderer("drawrendertargettoscreen_renderer");
    std::string drawrendertargettoscreen_renderer_class("SE_DrawRenderTargetToScreenRenderer");
    mRenderIdArray.push_back(drawrendertargettoscreen_renderer);
    mRenderClassName.push_back(drawrendertargettoscreen_renderer_class);

    std::string cloakflagwave_renderer("cloakflagwave_renderer");
    std::string cloakflagwave_renderer_class("SE_CloakFlagWaveRenderer");
    mRenderIdArray.push_back(cloakflagwave_renderer);
    mRenderClassName.push_back(cloakflagwave_renderer_class);
}

SE_RenderDefine::~SE_RenderDefine()
{}
