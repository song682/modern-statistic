package decok.dfcdvadstf.modernstatistic.render;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

/**
 * Entity model renderer — draws living entity (animal &amp; monster) models onto the GUI.
 * <p>Mirrors vanilla {@code GuiInventory.drawEntityOnScreen} but works with any
 * {@link EntityLivingBase} subclass instead of only the player, e.g. mob previews
 * in the statistics screen.</p>
 * <p>Current scope is limited to living entities (animals &amp; monsters); tile entities
 * are out of scope for now.</p>
 */
public final class EntityModelRenderer {

    /** Cached living entity instances, keyed by entity id. 按实体 id 缓存的生物实例。 */
    private static final Map<Integer, EntityLivingBase> ENTITY_CACHE = new HashMap<>();

    private EntityModelRenderer() {}

    /**
     * Get (and cache) a living entity instance for the given entity id.
     * <p>Only {@link EntityLivingBase} subclasses (animals &amp; monsters) are accepted;
     * other entity kinds return null.</p>
     * <p>The instance is created off-world and reset to a neutral static stance, so it
     * can be handed to {@link #renderEntity} repeatedly without affecting the real world.</p>
     *
     * @param entityId entity id, e.g. the key of {@link EntityList#entityEggs}
     * @return a cached living entity instance, or null if the id is unknown or the
     *         entity is not a living entity
     */
    public static EntityLivingBase getEntity(int entityId) {
        EntityLivingBase cached = ENTITY_CACHE.get(entityId);
        if (cached != null) {
            return cached;
        }

        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) {
            return null;
        }

        Entity entity = EntityList.createEntityByID(entityId, world);
        if (!(entity instanceof EntityLivingBase)) {
            return null;
        }

        EntityLivingBase living = (EntityLivingBase) entity;
        // Reset the pose to a neutral static stance so the model renders upright
        living.ticksExisted = 1;
        living.onGround = true;
        living.setPosition(0.0D, 0.0D, 0.0D);

        ENTITY_CACHE.put(entityId, living);
        return living;
    }

    /**
     * Render the model of a living entity at the given GUI position.
     * <p>The entity's rotation fields are temporarily overridden and restored afterwards,
     * so the same cached instance can be rendered at different angles.</p>
     *
     * @param entity the living entity to render (must be an {@link EntityLivingBase})
     * @param x center X of the model on screen (feet base line)
     * @param y feet base Y of the model on screen
     * @param scale model scale; typical GUI values are 15–40
     * @param yawOffset mouse offset X from the model center, e.g. {@code mouseX - x};
     *                  negated internally (the screen is X-mirrored, so the model turns
     *                  to its own right when the cursor moves left); 0 for a fixed
     *                  front-facing pose
     * @param pitchOffset mouse offset Y from the model center, e.g. {@code mouseY - y};
     *                    negated internally so the model looks down when the cursor
     *                    moves down; 0 for a fixed front-facing pose
     */
    public static void renderEntity(EntityLivingBase entity, int x, int y, int scale,
            float yawOffset, float pitchOffset) {
        if (entity == null) {
            return;
        }

        // Make sure the render engine is available for texture binding
        if (RenderManager.instance.renderEngine == null) {
            RenderManager.instance.renderEngine = Minecraft.getMinecraft().getTextureManager();
        }

        GL11.glEnable(GL11.GL_COLOR_MATERIAL);

        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 50.0F);
        GL11.glScalef(-scale, scale, scale);
        GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);

        // Back up the entity's rotation fields (restored after rendering)
        float prevYawOffset = entity.renderYawOffset;
        float prevYaw = entity.rotationYaw;
        float prevPitch = entity.rotationPitch;
        float prevYawHead = entity.prevRotationYawHead;
        float prevRotationYawHead = entity.rotationYawHead;

        GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        // Reset the material color to white: with GL_COLOR_MATERIAL enabled a stale
        // dark glColor (left over from GUI text) would darken the whole model.
        // RenderBiped.doRender does exactly the same, which is why skeletons/zombies
        // looked normal while other mobs were too dark.
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        // Disable the lightmap texture unit: lightmap coordinates are only valid under
        // the world matrix (set by renderEntityStatic); under the GUI matrix the stale
        // coordinates are transformed into uncontrollable values that darken the model.
        // 禁用 lightmap 纹理单元：lightmap 坐标仅在渲染世界时由 renderEntityStatic 设置，
        // 在 GUI 矩阵下遗留坐标会被变换成不可控值，导致模型发黑。
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(((float) Math.atan(pitchOffset / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        // Negate the yaw offset: the GUI pipeline renders through an X mirror
        // (glScalef(-scale, ...)), so screen left is the model's own right.
        // The model must turn toward its own right when the cursor moves left,
        // i.e. behave as if facing the player, not mirroring the cursor.
        entity.renderYawOffset = -(float) Math.atan(yawOffset / 40.0F) * 20.0F;
        entity.rotationYaw = -(float) Math.atan(yawOffset / 40.0F) * 40.0F;
        // Invert the pitch as well: with the model facing the player, moving the
        // cursor down must tilt the model down toward the cursor, not up.
        entity.rotationPitch = ((float) Math.atan(pitchOffset / 40.0F)) * 20.0F;
        entity.rotationYawHead = entity.rotationYaw;
        entity.prevRotationYawHead = entity.rotationYaw;
        GL11.glTranslatef(0.0F, entity.yOffset, 0.0F);

        // Render from the back so the model faces the viewer
        float prevPlayerViewY = RenderManager.instance.playerViewY;
        RenderManager.instance.playerViewY = 180.0F;
        RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        RenderManager.instance.playerViewY = prevPlayerViewY;

        // Restore the backed-up rotation fields
        entity.renderYawOffset = prevYawOffset;
        entity.rotationYaw = prevYaw;
        entity.rotationPitch = prevPitch;
        entity.prevRotationYawHead = prevYawHead;
        entity.rotationYawHead = prevRotationYawHead;

        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);

        // Restore lightmap state
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }
}
