package ru.nyansus.mc.ethosstep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class EthosStepPluginTest {
    private static final NamespacedKey GRANTED_KEY = new NamespacedKey("ethos-step", "granted");
    private static final Attribute ATTRIBUTE = Attribute.GENERIC_STEP_HEIGHT;

    private Player player;
    private AttributeInstance instance;
    private PersistentDataContainer data;
    private AtomicReference<Byte> marker;

    @Before
    public void setUp() {
        player = mock(Player.class);
        instance = mock(AttributeInstance.class);
        data = mock(PersistentDataContainer.class);
        marker = new AtomicReference<>();
        when(player.getPersistentDataContainer()).thenReturn(data);
        when(player.getAttribute(ATTRIBUTE)).thenReturn(instance);
        when(instance.getBaseValue()).thenReturn(0.6);
        when(data.get(GRANTED_KEY, PersistentDataType.BYTE)).thenAnswer(invocation -> marker.get());
        Mockito.doAnswer(invocation -> {
            marker.set(invocation.getArgument(2));
            return null;
        }).when(data).set(GRANTED_KEY, PersistentDataType.BYTE, (byte) 1);
    }

    @Test
    public void permissionGrantsHeightAndPersistsUnlock() {
        when(player.hasPermission(EthosStepPlugin.PERMISSION)).thenReturn(true);

        apply(1.0);

        verify(instance).setBaseValue(1.0);
        assertEquals(Byte.valueOf((byte) 1), marker.get());
        verify(instance, never()).addModifier(Mockito.any());
    }

    @Test
    public void playerWithoutPermissionOrUnlockIsUntouched() {
        apply(1.0);

        verify(player, never()).getAttribute(ATTRIBUTE);
        assertNull(marker.get());
    }

    @Test
    public void removalOfPermissionDoesNotRevokeUnlock() {
        marker.set((byte) 1);

        apply(1.0);

        verify(instance).setBaseValue(1.0);
        verify(player, never()).hasPermission(EthosStepPlugin.PERMISSION);
    }

    @Test
    public void previouslyUnlockedPlayerReceivesChangedConfig() {
        marker.set((byte) 1);

        apply(1.5);

        verify(instance).setBaseValue(1.5);
    }

    @Test
    public void respawnRestoresHeightWithoutPermission() {
        when(player.hasPermission(EthosStepPlugin.PERMISSION)).thenReturn(true);
        apply(1.0);
        when(player.hasPermission(EthosStepPlugin.PERMISSION)).thenReturn(false);
        AttributeInstance respawnedInstance = mock(AttributeInstance.class);
        when(player.getAttribute(ATTRIBUTE)).thenReturn(respawnedInstance);

        apply(1.0);

        verify(respawnedInstance).setBaseValue(1.0);
        assertEquals(Byte.valueOf((byte) 1), marker.get());
    }

    @Test
    public void matchingBaseValueStillPersistsFirstUnlock() {
        when(player.hasPermission(EthosStepPlugin.PERMISSION)).thenReturn(true);
        when(instance.getBaseValue()).thenReturn(1.0);

        apply(1.0);
        apply(1.0);

        verify(instance, never()).setBaseValue(anyDouble());
        verify(data).set(GRANTED_KEY, PersistentDataType.BYTE, (byte) 1);
        assertEquals(Byte.valueOf((byte) 1), marker.get());
    }

    @Test
    public void missingPlayerAttributeDoesNotRecordSuccessfulGrant() {
        when(player.hasPermission(EthosStepPlugin.PERMISSION)).thenReturn(true);
        when(player.getAttribute(ATTRIBUTE)).thenReturn(null);

        apply(1.0);

        assertNull(marker.get());
    }

    @Test
    public void falseMarkerDoesNotUnlockHeight() {
        marker.set((byte) 0);

        apply(1.0);

        verify(player, never()).getAttribute(ATTRIBUTE);
    }

    @Test
    public void resolvesModernAttributeWithoutLegacyLookup() {
        List<NamespacedKey> requested = new ArrayList<>();

        Attribute resolved = EthosStepPlugin.resolveAttribute(key -> {
            requested.add(key);
            return ATTRIBUTE;
        });

        assertSame(ATTRIBUTE, resolved);
        assertEquals(List.of(NamespacedKey.minecraft("step_height")), requested);
    }

    @Test
    public void fallsBackToLegacyAttributeName() {
        List<NamespacedKey> requested = new ArrayList<>();

        Attribute resolved = EthosStepPlugin.resolveAttribute(key -> {
            requested.add(key);
            return key.getKey().equals("generic.step_height") ? ATTRIBUTE : null;
        });

        assertSame(ATTRIBUTE, resolved);
        assertEquals(List.of(NamespacedKey.minecraft("step_height"),
                NamespacedKey.minecraft("generic.step_height")), requested);
    }

    @Test
    public void missingRegistryAttributeIsReportedAsUnavailable() {
        assertNull(EthosStepPlugin.resolveAttribute(key -> null));
    }

    @Test
    public void acceptsFiniteNonNegativeNumbers() {
        List<String> warnings = new ArrayList<>();
        for (Number value : List.of(0, 1, 1.5, 10.0)) {
            assertEquals(value.doubleValue(), EthosStepPlugin.readHeight(value, warnings::add), 0.0);
        }
        assertTrue(warnings.isEmpty());
    }

    @Test
    public void invalidValuesFallBackWithWarning() {
        Object[] invalid = {null, "1.0", "invalid", true, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        List<String> warnings = new ArrayList<>();
        for (Object value : invalid) {
            assertEquals(1.0, EthosStepPlugin.readHeight(value, warnings::add), 0.0);
        }
        assertEquals(invalid.length, warnings.size());
    }

    private void apply(double height) {
        EthosStepPlugin.apply(player, ATTRIBUTE, height, GRANTED_KEY);
    }
}
