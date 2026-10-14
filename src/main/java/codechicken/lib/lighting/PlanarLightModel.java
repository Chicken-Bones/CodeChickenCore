package codechicken.lib.lighting;

import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.render.CCRenderState;

/**
 * Faster precomputed version of LightModel that only works for axis planar sides
 */
public class PlanarLightModel implements CCRenderState.IVertexOperation {

    public static PlanarLightModel standardLightModel = LightModel.standardLightModel.reducePlanar();

    public int[] colours;

    public PlanarLightModel(int[] colours) {
        this.colours = colours;
    }

    @Override
    public boolean load(CCRenderState state) {
        if (!state.computeLighting) return false;

        state.pipeline.addDependency(CCRenderState.sideAttrib());
        state.pipeline.addDependency(CCRenderState.colourAttrib());
        return true;
    }

    @Override
    public void operate(CCRenderState state) {
        state.setColourInstance(ColourRGBA.multiply(state.colour, colours[state.side]));
    }

    @Override
    public int operationID() {
        return LightModel.operationIndex;
    }
}
