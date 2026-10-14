package codechicken.lib.render.uv;

import net.minecraft.util.IIcon;

import codechicken.lib.vec.IrreversibleTransformationException;

public class MultiIconTransformation extends UVTransformation {

    public IIcon[] icons;

    public MultiIconTransformation(IIcon... icons) {
        this.icons = icons;
    }

    @Override
    public void apply(UV uv) {
        IIcon icon = icons[uv.tex % icons.length];
        uv.u = icon.getInterpolatedU(uv.u * 16);
        uv.v = icon.getInterpolatedV(uv.v * 16);
    }

    @Override
    public UVTransformation inverse() {
        throw new IrreversibleTransformationException(this);
    }
}
