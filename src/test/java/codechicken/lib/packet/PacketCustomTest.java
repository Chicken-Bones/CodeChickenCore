package codechicken.lib.packet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.zip.Deflater;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketCustomTest {

    private static final int MAX_DECOMPRESSED_SIZE = 2 * 1024 * 1024;

    @Test
    public void rejectsLargeDeclarationWithoutCompressedData() {
        assertRejected(Unpooled.buffer(5).writeByte(0x81).writeInt(8 * 1024 * 1024));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, Integer.MIN_VALUE, MAX_DECOMPRESSED_SIZE + 1, Integer.MAX_VALUE })
    public void rejectsInvalidDeclaredLengths(int length) {
        assertRejected(Unpooled.buffer(5).writeByte(0x81).writeInt(length));
    }

    @Test
    public void rejectsMissingCompressedDataWithinLimit() {
        assertRejected(Unpooled.buffer(5).writeByte(0x81).writeInt(MAX_DECOMPRESSED_SIZE));
    }

    @Test
    public void rejectsCompleteStreamAboveDecodeLimit() {
        assertRejected(compressed(new byte[MAX_DECOMPRESSED_SIZE + 1]));
    }

    @Test
    public void rejectsTruncatedLengthHeader() {
        assertRejected(Unpooled.buffer(4).writeByte(0x81).writeMedium(10));
    }

    @Test
    public void rejectsMismatchedInflatedLengths() {
        byte[] data = new byte[16384];
        assertRejected(compressed(data).setInt(1, data.length - 1));
        assertRejected(compressed(data).setInt(1, data.length + 1));
        assertRejected(compressed(data).setInt(1, 0));
    }

    @Test
    public void rejectsTruncatedStreamEvenWhenAllOutputWasProduced() {
        ByteBuf payload = compressed(new byte[16384]);
        payload.writerIndex(payload.writerIndex() - 1);
        assertRejected(payload);
    }

    @Test
    public void rejectsCorruptStream() {
        ByteBuf payload = compressed(new byte[16384]);
        payload.setByte(payload.writerIndex() - 1, payload.getByte(payload.writerIndex() - 1) ^ 1);
        assertRejected(payload);
    }

    @Test
    public void rejectsDictionaryDependentStream() {
        assertRejected(compressed(new byte[16384], new byte[] { 1, 2, 3 }));
    }

    @Test
    public void rejectsTrailingCompressedData() {
        assertRejected(compressed(new byte[16384]).writeByte(0));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 8192, 8193, MAX_DECOMPRESSED_SIZE })
    public void decodesCompleteStreams(int length) {
        byte[] data = new byte[length];
        Arrays.fill(data, (byte) 42);
        assertDecoded(compressed(data), data);
    }

    @Test
    public void decodesSlicedAndDirectBuffers() {
        byte[] data = new byte[16384];
        Arrays.fill(data, (byte) 42);
        ByteBuf payload = compressed(data);
        ByteBuf prefixed = Unpooled.buffer().writeLong(-1).writeBytes(payload, 0, payload.readableBytes());
        ByteBuf direct = Unpooled.directBuffer().writeBytes(payload);
        payload.release();
        try {
            assertDecoded(prefixed.slice(8, prefixed.readableBytes() - 8).retain(), data);
            assertDecoded(direct, data);
        } finally {
            prefixed.release();
        }
    }

    @Test
    public void preservesUncompressedPackets() {
        ByteBuf payload = Unpooled.buffer().writeByte(7).writeInt(42);
        try {
            PacketCustom packet = new PacketCustom(payload);
            assertEquals(7, packet.getType());
            assertEquals(42, packet.readInt());
            assertEquals(0, packet.getByteBuf().readableBytes());
        } finally {
            payload.release();
        }
    }

    @Test
    public void roundTripsOutgoingCompression() {
        for (int length : new int[] { 16, 64, 40000, MAX_DECOMPRESSED_SIZE }) {
            byte[] data = new byte[length];
            Arrays.fill(data, (byte) 42);
            PacketCustom outgoing = new PacketCustom("PacketTest", 7);
            FMLProxyPacket wire = null;
            try {
                outgoing.writeByteArray(data);
                if (length <= 64) outgoing.compress();
                wire = outgoing.toPacket();
                assertEquals(length == 16 ? 7 : 0x87, wire.payload().getUnsignedByte(0));
                assertDecoded(wire.payload().retain(), data);
            } finally {
                if (wire != null) wire.payload().release();
                outgoing.getByteBuf().release();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void repeatedSerializationPreservesCompressedPayload(boolean forceCompression) {
        byte[] data = new byte[40000];
        Arrays.fill(data, (byte) 42);
        PacketCustom outgoing = new PacketCustom("PacketTest", 7).writeByteArray(data);
        if (forceCompression) outgoing.compress();
        try {
            for (int serialization = 0; serialization < 3; serialization++) {
                FMLProxyPacket wire = outgoing.toPacket();
                try {
                    assertEquals(0x87, wire.payload().getUnsignedByte(0));
                    assertDecoded(wire.payload().retain(), data);
                } finally {
                    wire.payload().release();
                }
            }
        } finally {
            outgoing.getByteBuf().release();
        }
    }

    @Test
    public void rejectsOutgoingPayloadAboveDecodeLimit() {
        PacketCustom outgoing = new PacketCustom("PacketTest", 7);
        try {
            outgoing.getByteBuf().writeZero(MAX_DECOMPRESSED_SIZE + 1);
            assertThrows(IllegalArgumentException.class, outgoing::toPacket);
        } finally {
            outgoing.getByteBuf().release();
        }
    }

    private static ByteBuf compressed(byte[] data) {
        return compressed(data, null);
    }

    private static ByteBuf compressed(byte[] data, byte[] dictionary) {
        Deflater deflater = new Deflater();
        try {
            if (dictionary != null) deflater.setDictionary(dictionary);
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            byte[] scratch = new byte[8192];
            while (!deflater.finished()) {
                int count = deflater.deflate(scratch);
                compressed.write(scratch, 0, count);
            }
            return Unpooled.buffer().writeByte(0x87).writeInt(data.length).writeBytes(compressed.toByteArray());
        } finally {
            deflater.end();
        }
    }

    private static void assertDecoded(ByteBuf payload, byte[] expected) {
        PacketCustom packet = null;
        try {
            packet = new PacketCustom(payload);
            assertEquals(7, packet.getType());
            assertEquals(expected.length, packet.getByteBuf().readableBytes());
            byte[] actual = new byte[expected.length];
            packet.getByteBuf().readBytes(actual);
            assertArrayEquals(expected, actual);
        } finally {
            if (packet != null && packet.getByteBuf() != payload) packet.getByteBuf().release();
            payload.release();
        }
    }

    private static void assertRejected(ByteBuf payload) {
        try {
            assertThrows(IllegalArgumentException.class, () -> {
                PacketCustom packet = new PacketCustom(payload);
                if (packet.getByteBuf() != payload) packet.getByteBuf().release();
            });
        } finally {
            payload.release();
        }
    }
}
