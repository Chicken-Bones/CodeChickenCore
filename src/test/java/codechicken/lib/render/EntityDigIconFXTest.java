package codechicken.lib.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.minecraft.util.IIcon;

import org.junit.jupiter.api.Test;

public class EntityDigIconFXTest {

    @Test
    public void rejectsMismatchedIconAndColourCountsBeforeSpawning() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> EntityDigIconFX.addBlockDestroyEffects(null, null, new IIcon[1], new int[0], null));

        assertEquals("icons and colours must have the same length", exception.getMessage());
    }
}
