package net.minecraft.client.renderer.entity;

import com.google.common.collect.Lists;

import java.awt.*;
import java.nio.FloatBuffer;
import java.util.List;

import kevin.KevinClient;
import kevin.module.modules.render.Chams;
import kevin.module.modules.render.ESP;
import kevin.module.modules.render.TrueSight;
import kevin.utils.ChatUtils;
import kevin.utils.EntityUtils;
import kevin.utils.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.Team;
import net.minecraft.src.Config;
import net.minecraft.src.Reflector;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.optifine.entity.model.CustomEntityModels;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.EXTPackedDepthStencil;
import org.lwjgl.opengl.GL11;
import shadersmod.client.Shaders;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL11.glColor4d;

public abstract class RenderLivingBase<T extends EntityLivingBase> extends Render<T>
{
    private static final Logger LOGGER = LogManager.getLogger();
    private static final DynamicTexture TEXTURE_BRIGHTNESS = new DynamicTexture(16, 16);
    public ModelBase mainModel;
    protected FloatBuffer brightnessBuffer = GLAllocation.createDirectFloatBuffer(4);
    protected List<LayerRenderer<T>> layerRenderers = Lists.<LayerRenderer<T>>newArrayList();
    protected boolean renderMarker;
    public static float NAME_TAG_RANGE = 64.0F;
    public static float NAME_TAG_RANGE_SNEAK = 32.0F;
    public float renderLimbSwing;
    public float renderLimbSwingAmount;
    public float renderAgeInTicks;
    public float renderHeadYaw;
    public float renderHeadPitch;
    public float renderScaleFactor;
    public static final boolean animateModelLiving = Boolean.getBoolean("animate.model.living");

    public RenderLivingBase(RenderManager renderManagerIn, ModelBase modelBaseIn, float shadowSizeIn)
    {
        super(renderManagerIn);
        this.mainModel = modelBaseIn;
        this.shadowSize = shadowSizeIn;
    }

    public <V extends EntityLivingBase, U extends LayerRenderer<V>> boolean addLayer(U layer)
    {
        return this.layerRenderers.add((LayerRenderer<T>) layer);
    }

    public ModelBase getMainModel()
    {
        return this.mainModel;
    }

    /**
     * Returns a rotation angle that is inbetween two other rotation angles. par1 and par2 are the angles between which
     * to interpolate, par3 is probably a float between 0.0 and 1.0 that tells us where "between" the two angles we are.
     * Example: par1 = 30, par2 = 50, par3 = 0.5, then return = 40
     */
    protected float interpolateRotation(float prevYawOffset, float yawOffset, float partialTicks)
    {
        float f;

        for (f = yawOffset - prevYawOffset; f < -180.0F; f += 360.0F)
        {
            ;
        }

        while (f >= 180.0F)
        {
            f -= 360.0F;
        }

        return prevYawOffset + partialTicks * f;
    }

    public void transformHeldFull3DItemLayer()
    {
    }

    /**
     * Renders the desired {@code T} type Entity.
     */
    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks)
    {
        final Chams chams = (Chams) KevinClient.moduleManager.getModule("Chams");

        if (chams.getState() && chams.getTargetsValue().get() && EntityUtils.isSelected(entity, false)) {
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(1.0F, -1000000F);
        }

        if (!Reflector.RenderLivingEvent_Pre_Constructor.exists() || !Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Pre_Constructor, entity, this, x, y, z))
        {
            if (animateModelLiving)
            {
                entity.limbSwingAmount = 1.0F;
            }

            GlStateManager.pushMatrix();
            GlStateManager.disableCull();
            this.mainModel.swingProgress = this.getSwingProgress(entity, partialTicks);
            this.mainModel.isRiding = entity.isRiding();

            if (Reflector.ForgeEntity_shouldRiderSit.exists())
            {
                this.mainModel.isRiding = entity.isRiding() && entity.getRidingEntity() != null && Reflector.callBoolean(entity.getRidingEntity(), Reflector.ForgeEntity_shouldRiderSit);
            }

            this.mainModel.isChild = entity.isChild();

            try
            {
                float f = this.interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partialTicks);
                float f1 = this.interpolateRotation(entity.prevRotationYawHead, entity.rotationYawHead, partialTicks);
                float f2 = f1 - f;

                if (this.mainModel.isRiding && entity.getRidingEntity() instanceof EntityLivingBase)
                {
                    EntityLivingBase entitylivingbase = (EntityLivingBase)entity.getRidingEntity();
                    f = this.interpolateRotation(entitylivingbase.prevRenderYawOffset, entitylivingbase.renderYawOffset, partialTicks);
                    f2 = f1 - f;
                    float f3 = MathHelper.wrapDegrees(f2);

                    if (f3 < -85.0F)
                    {
                        f3 = -85.0F;
                    }

                    if (f3 >= 85.0F)
                    {
                        f3 = 85.0F;
                    }

                    f = f1 - f3;

                    if (f3 * f3 > 2500.0F)
                    {
                        f += f3 * 0.2F;
                    }

                    f2 = f1 - f;
                }

                float f7 = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
                this.renderLivingAt(entity, x, y, z);
                float f8 = this.handleRotationFloat(entity, partialTicks);
                this.applyRotations(entity, f8, f, partialTicks);
                float f4 = this.prepareScale(entity, partialTicks);
                float f5 = 0.0F;
                float f6 = 0.0F;

                if (!entity.isRiding())
                {
                    f5 = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
                    f6 = entity.limbSwing - entity.limbSwingAmount * (1.0F - partialTicks);

                    if (entity.isChild())
                    {
                        f6 *= 3.0F;
                    }

                    if (f5 > 1.0F)
                    {
                        f5 = 1.0F;
                    }
                }

                GlStateManager.enableAlpha();
                this.mainModel.setLivingAnimations(entity, f6, f5, partialTicks);
                this.mainModel.setRotationAngles(f6, f5, f8, f2, f7, f4, entity);

                if (CustomEntityModels.isActive())
                {
                    this.renderLimbSwing = f6;
                    this.renderLimbSwingAmount = f5;
                    this.renderAgeInTicks = f8;
                    this.renderHeadYaw = f2;
                    this.renderHeadPitch = f7;
                    this.renderScaleFactor = f4;
                }

                if (this.renderOutlines)
                {
                    boolean flag1 = this.setScoreTeamColor(entity);
                    GlStateManager.enableColorMaterial();
                    GlStateManager.enableOutlineMode(this.getTeamColor(entity));

                    if (!this.renderMarker)
                    {
                        this.renderModel(entity, f6, f5, f8, f2, f7, f4);
                    }

                    if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator())
                    {
                        this.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                    }

                    GlStateManager.disableOutlineMode();
                    GlStateManager.disableColorMaterial();

                    if (flag1)
                    {
                        this.unsetScoreTeamColor();
                    }
                }
                else
                {
                    boolean flag = this.setDoRenderBrightness(entity, partialTicks);
                    this.renderModel(entity, f6, f5, f8, f2, f7, f4);

                    if (flag)
                    {
                        this.unsetBrightness();
                    }

                    GlStateManager.depthMask(true);

                    if (!(entity instanceof EntityPlayer) || !((EntityPlayer)entity).isSpectator())
                    {
                        this.renderLayers(entity, f6, f5, partialTicks, f8, f2, f7, f4);
                    }
                }

                GlStateManager.disableRescaleNormal();
            }
            catch (Exception exception1)
            {
                LOGGER.error("Couldn't render entity", (Throwable)exception1);
            }

            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableCull();
            GlStateManager.popMatrix();
            super.doRender(entity, x, y, z, entityYaw, partialTicks);

            if (Reflector.RenderLivingEvent_Post_Constructor.exists())
            {
                Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Post_Constructor, entity, this, x, y, z);
            }
        }

        if (chams.getState() && chams.getTargetsValue().get() && EntityUtils.isSelected(entity, false)) {
            GL11.glPolygonOffset(1.0F, 1000000F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    public float prepareScale(T entitylivingbaseIn, float partialTicks)
    {
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        this.preRenderCallback(entitylivingbaseIn, partialTicks);
        float f = 0.0625F;
        GlStateManager.translate(0.0F, -1.501F, 0.0F);
        return 0.0625F;
    }

    protected boolean setScoreTeamColor(T entityLivingBaseIn)
    {
        GlStateManager.disableLighting();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        return true;
    }

    protected void unsetScoreTeamColor()
    {
        GlStateManager.enableLighting();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    /**
     * Renders the model in RenderLiving
     */
    protected void renderModel(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scaleFactor)
    {
        boolean flag = this.isVisible(entitylivingbaseIn);
        final TrueSight trueSight = (TrueSight) KevinClient.moduleManager.getModule("TrueSight");
        boolean flag1 = !flag && (!entitylivingbaseIn.isInvisibleToPlayer(Minecraft.getMinecraft().player) || (trueSight.getState() && trueSight.getEntitiesValue().get()));

        if (flag || flag1)
        {
            if (!this.bindEntityTexture(entitylivingbaseIn))
            {
                return;
            }

            if (flag1)
            {
                GlStateManager.enableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
            }

            final ESP esp = (ESP) KevinClient.moduleManager.getModule("ESP");
            if(esp.getState() && EntityUtils.isSelected(entitylivingbaseIn, false)) {
                Minecraft mc = Minecraft.getMinecraft();
                boolean fancyGraphics = mc.gameSettings.fancyGraphics;
                mc.gameSettings.fancyGraphics = false;
                float gamma = mc.gameSettings.gammaSetting;
                mc.gameSettings.gammaSetting = 100000F;
                switch(esp.modeValue.get().toLowerCase()) {
                    case "wireframe":
                        GL11.glPushMatrix();
                        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
                        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
                        GL11.glDisable(GL11.GL_TEXTURE_2D);
                        GL11.glDisable(GL11.GL_LIGHTING);
                        GL11.glDisable(GL11.GL_DEPTH_TEST);
                        GL11.glEnable(GL11.GL_LINE_SMOOTH);
                        GL11.glEnable(GL11.GL_BLEND);
                        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        RenderUtils.glColor(esp.getColor(entitylivingbaseIn));
                        Float width;
                        if (esp.getAutoWidth().get()){
                            width = esp.wireframeWidth.get() / mc.player.getDistanceToEntity(entitylivingbaseIn) * 4;
                        }else width = esp.wireframeWidth.get();
                        GL11.glLineWidth(width);
                        this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
                        GL11.glPopAttrib();
                        GL11.glPopMatrix();
                        break;
                    case "outline":
                        if (mc.gameSettings.ofFastRender) {
                            ChatUtils.INSTANCE.messageWithPrefix("§cPlease Turn OFF Fast Render!");
                            esp.setState(false);
                            return;
                        }
                        GlStateManager.resetColor();
                        final Color color = esp.getColor(entitylivingbaseIn);
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        //RenderOutline One
                        final Framebuffer fbo = Minecraft.getMinecraft().getFramebuffer();
                        if(fbo != null) {
                            if(fbo.depthBuffer > -1) {
                                EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
                                final int stencil_depth_buffer_ID = EXTFramebufferObject.glGenRenderbuffersEXT();
                                EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencil_depth_buffer_ID);
                                EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight);
                                EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencil_depth_buffer_ID);
                                EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencil_depth_buffer_ID);
                                fbo.depthBuffer = -1;
                            }
                        }
                        glPushAttrib(GL_ALL_ATTRIB_BITS);
                        glDisable(GL_ALPHA_TEST);
                        glDisable(GL_TEXTURE_2D);
                        glDisable(GL_LIGHTING);
                        glEnable(GL_BLEND);
                        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
                        Float outWidth;
                        if (esp.getAutoWidth().get()){
                            outWidth = esp.outlineWidth.get() / mc.player.getDistanceToEntity(entitylivingbaseIn) * 4.5F;
                        }else outWidth = esp.outlineWidth.get();
                        glLineWidth(outWidth);
                        glEnable(GL_LINE_SMOOTH);
                        glEnable(GL_STENCIL_TEST);
                        glClear(GL_STENCIL_BUFFER_BIT);
                        glClearStencil(0xF);
                        glStencilFunc(GL_NEVER, 1, 0xF);
                        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        //End One
                        this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        //RenderOutline Two
                        glStencilFunc(GL_NEVER, 0, 0xF);
                        glStencilOp(GL_REPLACE, GL_REPLACE, GL_REPLACE);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
                        //End Two
                        this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        //RenderOutline Three
                        glStencilFunc(GL_EQUAL, 1, 0xF);
                        glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
                        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
                        //End Three
                        this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        //RenderOutline Four
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        glDepthMask(false);
                        glDisable(GL_DEPTH_TEST);
                        glEnable(GL_POLYGON_OFFSET_LINE);
                        glPolygonOffset(1.0F, -2000000F);
                        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
                        //End Four
                        this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);
                        glColor4d(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
                        //RenderOutline Five
                        glPolygonOffset(1.0F, 2000000F);
                        glDisable(GL_POLYGON_OFFSET_LINE);
                        glEnable(GL_DEPTH_TEST);
                        glDepthMask(true);
                        glDisable(GL_STENCIL_TEST);
                        glDisable(GL_LINE_SMOOTH);
                        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE);
                        glEnable(GL_BLEND);
                        glEnable(GL_LIGHTING);
                        glEnable(GL_TEXTURE_2D);
                        glEnable(GL_ALPHA_TEST);
                        glPopAttrib();
                        //End Five
                        glColor4d(1D, 1D, 1D, 1D);
                }
                mc.gameSettings.fancyGraphics = fancyGraphics;
                mc.gameSettings.gammaSetting = gamma;
            }

            this.mainModel.render(entitylivingbaseIn, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor);

            if (flag1)
            {
                GlStateManager.disableBlendProfile(GlStateManager.Profile.TRANSPARENT_MODEL);
            }
        }
    }

    protected boolean isVisible(T p_193115_1_)
    {
        return !p_193115_1_.isInvisible() || this.renderOutlines;
    }

    protected boolean setDoRenderBrightness(T entityLivingBaseIn, float partialTicks)
    {
        return this.setBrightness(entityLivingBaseIn, partialTicks, true);
    }

    protected boolean setBrightness(T entitylivingbaseIn, float partialTicks, boolean combineTextures)
    {
        float f = entitylivingbaseIn.getBrightness();
        int i = this.getColorMultiplier(entitylivingbaseIn, f, partialTicks);
        boolean flag = (i >> 24 & 255) > 0;
        boolean flag1 = entitylivingbaseIn.hurtTime > 0 || entitylivingbaseIn.deathTime > 0;

        if (!flag && !flag1)
        {
            return false;
        }
        else if (!flag && !combineTextures)
        {
            return false;
        }
        else
        {
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GlStateManager.enableTexture2D();
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, OpenGlHelper.GL_INTERPOLATE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_CONSTANT);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE2_RGB, OpenGlHelper.GL_CONSTANT);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND2_RGB, 770);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            this.brightnessBuffer.position(0);

            if (flag1)
            {
                this.brightnessBuffer.put(1.0F);
                this.brightnessBuffer.put(0.0F);
                this.brightnessBuffer.put(0.0F);
                this.brightnessBuffer.put(0.3F);

                if (Config.isShaders())
                {
                    Shaders.setEntityColor(1.0F, 0.0F, 0.0F, 0.3F);
                }
            }
            else
            {
                float f1 = (float)(i >> 24 & 255) / 255.0F;
                float f2 = (float)(i >> 16 & 255) / 255.0F;
                float f3 = (float)(i >> 8 & 255) / 255.0F;
                float f4 = (float)(i & 255) / 255.0F;
                this.brightnessBuffer.put(f2);
                this.brightnessBuffer.put(f3);
                this.brightnessBuffer.put(f4);
                this.brightnessBuffer.put(1.0F - f1);

                if (Config.isShaders())
                {
                    Shaders.setEntityColor(f2, f3, f4, 1.0F - f1);
                }
            }

            this.brightnessBuffer.flip();
            GlStateManager.glTexEnv(8960, 8705, this.brightnessBuffer);
            GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
            GlStateManager.enableTexture2D();
            GlStateManager.bindTexture(TEXTURE_BRIGHTNESS.getGlTextureId());
            GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.lightmapTexUnit);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 7681);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.GL_PREVIOUS);
            GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
            GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
            return true;
        }
    }

    protected void unsetBrightness()
    {
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableTexture2D();
        GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, OpenGlHelper.defaultTexUnit);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PRIMARY_COLOR);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, OpenGlHelper.defaultTexUnit);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_ALPHA, OpenGlHelper.GL_PRIMARY_COLOR);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_ALPHA, 770);
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, 5890);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, 5890);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.setActiveTexture(OpenGlHelper.GL_TEXTURE2);
        GlStateManager.disableTexture2D();
        GlStateManager.bindTexture(0);
        GlStateManager.glTexEnvi(8960, 8704, OpenGlHelper.GL_COMBINE);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_RGB, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND1_RGB, 768);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_RGB, 5890);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE1_RGB, OpenGlHelper.GL_PREVIOUS);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_COMBINE_ALPHA, 8448);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_OPERAND0_ALPHA, 770);
        GlStateManager.glTexEnvi(8960, OpenGlHelper.GL_SOURCE0_ALPHA, 5890);
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);

        if (Config.isShaders())
        {
            Shaders.setEntityColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }

    /**
     * Sets a simple glTranslate on a LivingEntity.
     */
    protected void renderLivingAt(T entityLivingBaseIn, double x, double y, double z)
    {
        GlStateManager.translate((float)x, (float)y, (float)z);
    }

    protected void applyRotations(T entityLiving, float p_77043_2_, float rotationYaw, float partialTicks)
    {
        GlStateManager.rotate(180.0F - rotationYaw, 0.0F, 1.0F, 0.0F);

        if (entityLiving.deathTime > 0)
        {
            float f = ((float)entityLiving.deathTime + partialTicks - 1.0F) / 20.0F * 1.6F;
            f = MathHelper.sqrt(f);

            if (f > 1.0F)
            {
                f = 1.0F;
            }

            GlStateManager.rotate(f * this.getDeathMaxRotation(entityLiving), 0.0F, 0.0F, 1.0F);
        }
        else
        {
            String s = TextFormatting.getTextWithoutFormattingCodes(entityLiving.getName());

            if (s != null && ("Dinnerbone".equals(s) || "Grumm".equals(s)) && (!(entityLiving instanceof EntityPlayer) || ((EntityPlayer)entityLiving).isWearing(EnumPlayerModelParts.CAPE)))
            {
                GlStateManager.translate(0.0F, entityLiving.height + 0.1F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    /**
     * Returns where in the swing animation the living entity is (from 0 to 1).  Args : entity, partialTickTime
     */
    protected float getSwingProgress(T livingBase, float partialTickTime)
    {
        return livingBase.getSwingProgress(partialTickTime);
    }

    /**
     * Defines what float the third param in setRotationAngles of ModelBase is
     */
    protected float handleRotationFloat(T livingBase, float partialTicks)
    {
        return (float)livingBase.ticksExisted + partialTicks;
    }

    protected void renderLayers(T entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scaleIn)
    {
        for (LayerRenderer<T> layerrenderer : this.layerRenderers)
        {
            boolean flag = this.setBrightness(entitylivingbaseIn, partialTicks, layerrenderer.shouldCombineTextures());
            layerrenderer.doRenderLayer(entitylivingbaseIn, limbSwing, limbSwingAmount, partialTicks, ageInTicks, netHeadYaw, headPitch, scaleIn);

            if (flag)
            {
                this.unsetBrightness();
            }
        }
    }

    protected float getDeathMaxRotation(T entityLivingBaseIn)
    {
        return 90.0F;
    }

    /**
     * Gets an RGBA int color multiplier to apply.
     */
    protected int getColorMultiplier(T entitylivingbaseIn, float lightBrightness, float partialTickTime)
    {
        return 0;
    }

    /**
     * Allows the render to do state modifications necessary before the model is rendered.
     */
    protected void preRenderCallback(T entitylivingbaseIn, float partialTickTime)
    {
    }

    public void renderName(T entity, double x, double y, double z)
    {
        if (!Reflector.RenderLivingEvent_Specials_Pre_Constructor.exists() || !Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Specials_Pre_Constructor, entity, this, x, y, z))
        {
            if (this.canRenderName(entity))
            {
                double d0 = entity.getDistanceSqToEntity(this.renderManager.renderViewEntity);
                float f = entity.isSneaking() ? NAME_TAG_RANGE_SNEAK : NAME_TAG_RANGE;

                if (d0 < (double)(f * f))
                {
                    String s = entity.getDisplayName().getFormattedText();
                    GlStateManager.alphaFunc(516, 0.1F);
                    this.renderEntityName(entity, x, y, z, s, d0);
                }
            }

            if (Reflector.RenderLivingEvent_Specials_Post_Constructor.exists())
            {
                Reflector.postForgeBusEvent(Reflector.RenderLivingEvent_Specials_Post_Constructor, entity, this, x, y, z);
            }
        }
    }

    protected boolean canRenderName(T entity)
    {
        if (!ESP.renderNameTags || (KevinClient.moduleManager.getModule("NameTags").getState() && EntityUtils.isSelected(entity, false))) return false;

        EntityPlayerSP entityplayersp = Minecraft.getMinecraft().player;
        boolean flag = !entity.isInvisibleToPlayer(entityplayersp);

        if (entity != entityplayersp)
        {
            Team team = entity.getTeam();
            Team team1 = entityplayersp.getTeam();

            if (team != null)
            {
                Team.EnumVisible team$enumvisible = team.getNameTagVisibility();

                switch (team$enumvisible)
                {
                    case ALWAYS:
                        return flag;

                    case NEVER:
                        return false;

                    case HIDE_FOR_OTHER_TEAMS:
                        return team1 == null ? flag : team.isSameTeam(team1) && (team.getSeeFriendlyInvisiblesEnabled() || flag);

                    case HIDE_FOR_OWN_TEAM:
                        return team1 == null ? flag : !team.isSameTeam(team1) && flag;

                    default:
                        return true;
                }
            }
        }

        return Minecraft.isGuiEnabled() && entity != this.renderManager.renderViewEntity && flag && !entity.isBeingRidden();
    }

    public List<LayerRenderer<T>> getLayerRenderers()
    {
        return this.layerRenderers;
    }

    static
    {
        int[] aint = TEXTURE_BRIGHTNESS.getTextureData();

        for (int i = 0; i < 256; ++i)
        {
            aint[i] = -1;
        }

        TEXTURE_BRIGHTNESS.updateDynamicTexture();
    }
}
