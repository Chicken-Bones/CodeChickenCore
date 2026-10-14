package codechicken.lib.render;

import java.util.ArrayList;
import java.util.Collections;

import codechicken.lib.render.CCRenderState.IVertexOperation;
import codechicken.lib.render.CCRenderState.VertexAttribute;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class CCRenderPipeline {

    private final CCRenderState renderState;
    private final PipelineBuilder builder;

    public CCRenderPipeline(CCRenderState renderState) {
        this.renderState = renderState;
        builder = new PipelineBuilder(renderState);
    }

    public CCRenderPipeline() {
        this(CCRenderState.instance());
    }

    public class PipelineBuilder {

        private final CCRenderState renderState;

        public PipelineBuilder(CCRenderState renderState) {
            this.renderState = renderState;
        }

        public PipelineBuilder() {
            this(CCRenderState.instance());
        }

        public PipelineBuilder add(IVertexOperation op) {
            ops.add(op);
            return this;
        }

        public PipelineBuilder add(IVertexOperation... ops) {
            Collections.addAll(CCRenderPipeline.this.ops, ops);
            return this;
        }

        public void build() {
            rebuild();
        }

        public void render() {
            rebuild();
            renderState.renderInstance();
        }
    }

    private class PipelineNode {

        public ArrayList<PipelineNode> deps = new ArrayList<>();
        public IVertexOperation op;

        public void add() {
            if (op == null) return;

            for (int i = 0; i < deps.size(); i++) deps.get(i).add();
            deps.clear();
            sorted.add(op);
            op = null;
        }
    }

    private final ArrayList<VertexAttribute> attribs = new ArrayList<>();
    private final ArrayList<IVertexOperation> ops = new ArrayList<>();
    private final ArrayList<PipelineNode> nodes = new ArrayList<>();
    private final ArrayList<IVertexOperation> sorted = new ArrayList<>();
    private PipelineNode loading;

    public void setPipeline(IVertexOperation... ops) {
        this.ops.clear();
        for (int i = 0; i < ops.length; i++) this.ops.add(ops[i]);
        rebuild();
    }

    public void reset() {
        ops.clear();
        unbuild();
    }

    private void unbuild() {
        for (int i = 0; i < attribs.size(); i++) attribs.get(i).active = false;
        attribs.clear();
        sorted.clear();
    }

    public void rebuild() {
        if (ops.isEmpty() || this.renderState.model == null) return;

        // ensure enough nodes for all ops
        while (nodes.size() < this.renderState.operationCount()) nodes.add(new PipelineNode());
        unbuild();

        if (this.renderState.useNormals) addAttribute(this.renderState.normalAttrib);
        if (this.renderState.useColour) addAttribute(this.renderState.colourAttrib);
        if (this.renderState.computeLighting) addAttribute(this.renderState.lightingAttrib);

        for (int i = 0; i < ops.size(); i++) {
            IVertexOperation op = ops.get(i);
            loading = nodes.get(op.operationID());
            boolean loaded = op.load(renderState);
            if (loaded) loading.op = op;

            if (op instanceof VertexAttribute) if (loaded) attribs.add((VertexAttribute) op);
            else((VertexAttribute) op).active = false;
        }

        for (int i = 0; i < nodes.size(); i++) nodes.get(i).add();
    }

    public void addRequirement(int opRef) {
        loading.deps.add(nodes.get(opRef));
    }

    public void addDependency(VertexAttribute attrib) {
        loading.deps.add(nodes.get(attrib.operationID()));
        addAttribute(attrib);
    }

    public void addAttribute(VertexAttribute attrib) {
        if (!attrib.active) {
            ops.add(attrib);
            attrib.active = true;
        }
    }

    public void operate() {
        for (int i = 0; i < sorted.size(); i++) sorted.get(i).operate(renderState);
    }

    public PipelineBuilder builder() {
        ops.clear();
        return builder;
    }
}
