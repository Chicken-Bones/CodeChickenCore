package codechicken.lib.render;

import java.util.Arrays;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;

public class EntityDigIconFX extends EntityFX {

    /** Tint that leaves the particle colour untouched, for materials that don't colour their texture. */
    public static final int NO_TINT = 0xFFFFFF;

    public EntityDigIconFX(World world, double x, double y, double z, double dx, double dy, double dz, IIcon icon) {
        super(world, x, y, z, dx, dy, dz);
        particleIcon = icon;
        particleGravity = 1;
        particleRed = particleGreen = particleBlue = 0.6F;
        particleScale /= 2.0F;
    }

    /**
     * Multiplies the particle colour by an 0xRRGGBB tint, the equivalent of vanilla
     * EntityDiggingFX.applyColourMultiplier. Materials that draw a greyscale texture and colour it at render time
     * (leaves, grass tops) need this, or their particles come out flat grey.
     */
    public EntityDigIconFX applyColourMultiplier(int colour) {
        particleRed *= (colour >> 16 & 255) / 255F;
        particleGreen *= (colour >> 8 & 255) / 255F;
        particleBlue *= (colour & 255) / 255F;
        return this;
    }

    @Override
    public int getFXLayer() {
        return 1;
    }

    public void setScale(float scale) {
        particleScale = scale;
    }

    public float getScale() {
        return particleScale;
    }

    public void setMaxAge(int age) {
        particleMaxAge = age;
    }

    public int getMaxAge() {
        return particleMaxAge;
    }

    /**
     * copy pasted from EntityDiggingFX
     */
    @Override
    public void renderParticle(Tessellator par1Tessellator, float par2, float par3, float par4, float par5, float par6,
            float par7) {
        float f6 = (particleTextureIndexX + particleTextureJitterX / 4.0F) / 16.0F;
        float f7 = f6 + 0.015609375F;
        float f8 = (particleTextureIndexY + particleTextureJitterY / 4.0F) / 16.0F;
        float f9 = f8 + 0.015609375F;
        float f10 = 0.1F * particleScale;

        if (particleIcon != null) {
            f6 = particleIcon.getInterpolatedU(particleTextureJitterX / 4.0F * 16.0F);
            f7 = particleIcon.getInterpolatedU((particleTextureJitterX + 1.0F) / 4.0F * 16.0F);
            f8 = particleIcon.getInterpolatedV(particleTextureJitterY / 4.0F * 16.0F);
            f9 = particleIcon.getInterpolatedV((particleTextureJitterY + 1.0F) / 4.0F * 16.0F);
        }

        float f11 = (float) (prevPosX + (posX - prevPosX) * par2 - interpPosX);
        float f12 = (float) (prevPosY + (posY - prevPosY) * par2 - interpPosY);
        float f13 = (float) (prevPosZ + (posZ - prevPosZ) * par2 - interpPosZ);
        float f14 = 1.0F;
        par1Tessellator.setColorOpaque_F(f14 * particleRed, f14 * particleGreen, f14 * particleBlue);
        par1Tessellator.addVertexWithUV(
                f11 - par3 * f10 - par6 * f10,
                f12 - par4 * f10,
                f13 - par5 * f10 - par7 * f10,
                f6,
                f9);
        par1Tessellator.addVertexWithUV(
                f11 - par3 * f10 + par6 * f10,
                f12 + par4 * f10,
                f13 - par5 * f10 + par7 * f10,
                f6,
                f8);
        par1Tessellator.addVertexWithUV(
                f11 + par3 * f10 + par6 * f10,
                f12 + par4 * f10,
                f13 + par5 * f10 + par7 * f10,
                f7,
                f8);
        par1Tessellator.addVertexWithUV(
                f11 + par3 * f10 - par6 * f10,
                f12 - par4 * f10,
                f13 + par5 * f10 - par7 * f10,
                f7,
                f9);
    }

    /**
     * @deprecated the particle is never tinted, so a material that colours a greyscale texture at render time (leaves,
     *             grass tops) breaks into grey particles. Use
     *             {@link #addBlockHitEffects(World, Cuboid6, int, IIcon, int, EffectRenderer)}, passing
     *             {@link #NO_TINT} for materials whose texture is already coloured.
     */
    @Deprecated
    public static void addBlockHitEffects(World world, Cuboid6 bounds, int side, IIcon icon,
            EffectRenderer effectRenderer) {
        addBlockHitEffects(world, bounds, side, icon, NO_TINT, effectRenderer);
    }

    /**
     * Spawns a hit particle on side of bounds, tinted by colour (0xRRGGBB, {@link #NO_TINT} for none). The caller
     * resolves the tint for the side it hit, so per-face materials like grass can tint only the faces that need it.
     */
    public static void addBlockHitEffects(World world, Cuboid6 bounds, int side, IIcon icon, int colour,
            EffectRenderer effectRenderer) {
        float border = 0.1F;
        Vector3 diff = bounds.max.copy().subtract(bounds.min).add(-2 * border);
        diff.x *= world.rand.nextDouble();
        diff.y *= world.rand.nextDouble();
        diff.z *= world.rand.nextDouble();
        Vector3 pos = diff.add(bounds.min).add(border);

        if (side == 0) diff.y = bounds.min.y - border;
        if (side == 1) diff.y = bounds.max.y + border;
        if (side == 2) diff.z = bounds.min.z - border;
        if (side == 3) diff.z = bounds.max.z + border;
        if (side == 4) diff.x = bounds.min.x - border;
        if (side == 5) diff.x = bounds.max.x + border;

        EntityDigIconFX fx = new EntityDigIconFX(world, pos.x, pos.y, pos.z, 0, 0, 0, icon);
        effectRenderer.addEffect(fx.applyColourMultiplier(colour).multiplyVelocity(0.2F).multipleParticleScaleBy(0.6F));
    }

    /**
     * @deprecated the particles are never tinted, so a material that colours a greyscale texture at render time
     *             (leaves, grass tops) breaks into grey particles. Use
     *             {@link #addBlockDestroyEffects(World, Cuboid6, IIcon[], int[], EffectRenderer)}, passing
     *             {@link #NO_TINT} for the faces whose texture is already coloured.
     */
    @Deprecated
    public static void addBlockDestroyEffects(World world, Cuboid6 bounds, IIcon[] icons,
            EffectRenderer effectRenderer) {
        int[] colours = new int[icons.length];
        Arrays.fill(colours, NO_TINT);
        addBlockDestroyEffects(world, bounds, icons, colours, effectRenderer);
    }

    /**
     * Spawns break particles filling bounds, each drawn from a random face. colours holds the 0xRRGGBB tint for the
     * matching entry of icons ({@link #NO_TINT} for none) and must be the same length, so a particle is tinted by the
     * face it was taken from.
     */
    public static void addBlockDestroyEffects(World world, Cuboid6 bounds, IIcon[] icons, int[] colours,
            EffectRenderer effectRenderer) {
        if (icons.length != colours.length)
            throw new IllegalArgumentException("icons and colours must have the same length");

        Vector3 diff = bounds.max.copy().subtract(bounds.min);
        Vector3 center = bounds.min.copy().add(bounds.max).multiply(0.5);
        Vector3 density = diff.copy().multiply(4);
        density.x = Math.ceil(density.x);
        density.y = Math.ceil(density.y);
        density.z = Math.ceil(density.z);

        for (int i = 0; i < density.x; ++i) for (int j = 0; j < density.y; ++j) for (int k = 0; k < density.z; ++k) {
            double px = bounds.min.x + (i + 0.5) * diff.x / density.x;
            double py = bounds.min.y + (j + 0.5) * diff.y / density.y;
            double pz = bounds.min.z + (k + 0.5) * diff.z / density.z;
            int iconSide = world.rand.nextInt(icons.length);
            EntityDigIconFX fx = new EntityDigIconFX(
                    world,
                    px,
                    py,
                    pz,
                    px - center.x,
                    py - center.y,
                    pz - center.z,
                    icons[iconSide]);
            effectRenderer.addEffect(fx.applyColourMultiplier(colours[iconSide]));
        }
    }
}
