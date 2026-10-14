package codechicken.lib.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import codechicken.lib.vec.Vector3;

public class CCRenderStateThreadingTest {

    private static int[] attribIndices() {
        return new int[] { CCRenderState.normalAttrib().attributeIndex, CCRenderState.colourAttrib().attributeIndex,
                CCRenderState.lightingAttrib().attributeIndex, CCRenderState.sideAttrib().attributeIndex,
                CCRenderState.lightCoordAttrib().attributeIndex };
    }

    @Test
    public void attributeIndicesStableAcrossThreads() throws Exception {
        int[] main = attribIndices();
        assertEquals(5, Arrays.stream(main).distinct().count());

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<int[]>> futures = new ArrayList<>();
            for (int i = 0; i < 8; i++) futures.add(pool.submit(CCRenderStateThreadingTest::attribIndices));
            for (Future<int[]> f : futures) assertArrayEquals(main, f.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void modelBakedOnOneThreadIsReadableOnAnother() throws Exception {
        ExecutorService bakeThread = Executors.newSingleThreadExecutor();
        ExecutorService renderThread = Executors.newSingleThreadExecutor();
        try {
            CCModel model = bakeThread.submit(() -> {
                CCModel m = CCModel.quadModel(4);
                m.verts[0] = new Vertex5(0, 1, 0, 0, 0);
                m.verts[1] = new Vertex5(0, 1, 1, 0, 1);
                m.verts[2] = new Vertex5(1, 1, 1, 1, 1);
                m.verts[3] = new Vertex5(1, 1, 0, 1, 0);
                return m.computeNormals();
            }).get(10, TimeUnit.SECONDS);

            int side = renderThread.submit(() -> {
                assertTrue(
                        model.hasAttribute(CCRenderState.normalAttrib()),
                        "normals baked on another thread not visible under this thread's attributeIndex");
                Vector3[] normals = model.getAttributes(CCRenderState.normalAttrib());
                assertNotNull(normals);
                return CCModel.findSide(normals[0]);
            }).get(10, TimeUnit.SECONDS);

            assertEquals(1, side);
            assertEquals(1, CCModel.findSide(model.getAttributes(CCRenderState.normalAttrib())[0]));
        } finally {
            bakeThread.shutdownNow();
            renderThread.shutdownNow();
        }
    }

    @Test
    public void concurrentFirstUseKeepsIndicesConsistent() throws Exception {
        int threads = 16;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<int[]>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) futures.add(pool.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return attribIndices();
            }));
            int[] expected = attribIndices();
            for (Future<int[]> f : futures) assertArrayEquals(expected, f.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void getAttributeReturnsCanonicalForIndex() {
        int normalIndex = CCRenderState.normalAttrib().attributeIndex;
        CCRenderState.VertexAttribute<?> canonical = CCRenderState.getAttribute(normalIndex);
        assertNotNull(canonical);
        assertEquals(normalIndex, canonical.attributeIndex);
        assertSame(canonical, CCRenderState.getAttribute(normalIndex));
    }
}
