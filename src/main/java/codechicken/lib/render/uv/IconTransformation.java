package codechicken.lib.render.uv;

import net.minecraft.util.IIcon;

import codechicken.lib.vec.IrreversibleTransformationException;

public class IconTransformation extends UVTransformation {

    public IIcon icon;

    public IconTransformation(IIcon icon) {
        this.icon = icon;
    }

    @Override
    public void apply(UV uv) {
        uv.u = icon.getInterpolatedU(uv.u * 16);
        uv.v = icon.getInterpolatedV(uv.v * 16);
    }

    @Override
    public UVTransformation inverse() {
        throw new IrreversibleTransformationException(this);
    }
}
