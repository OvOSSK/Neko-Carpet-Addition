package neko.voidwarehouse;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.nbt.*;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UpdateRegressionTest {
    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }
    private static VoidWarehouseRecipes.Recipe recipe() {
        return new VoidWarehouseRecipes.Recipe(null, "minecraft:oak_planks", 4,
                List.of(new VoidWarehouseRecipes.Part(List.of("minecraft:oak_log"), 1)));
    }
    @Test void legacyRecipeBlobMigratesAndSurvivesReload() {
        VoidWarehouseSettings.voidWarehouse = true;
        VoidWarehouseSettings.voidWarehouseCraft = true;
        var original = new VoidWarehouseData();
        assertTrue(original.learn(recipe()));
        var current = original.save(new CompoundTag());
        var lines = current.getList("learned_recipes_lines", Tag.TAG_STRING);
        assertEquals(1, lines.size());
        var legacy = new CompoundTag();
        legacy.putString("learned_recipes", lines.getString(0));
        var loaded = VoidWarehouseData.load(legacy);
        assertEquals(1, loaded.recipeBookUsed());
        var saved = loaded.save(legacy.copy());
        assertFalse(saved.contains("learned_recipes"));
        assertEquals(1, VoidWarehouseData.load(saved).recipeBookUsed());
        assertEquals(recipe(), loaded.recipesFor("minecraft:oak_planks").get(0));
    }
    @Test void filledBoxRequiresBothShellAndAllContents() {
        var data = new VoidWarehouseData();
        data.insert(new ItemStack(Items.SHULKER_BOX, 1));
        data.insert(new ItemStack(Items.STONE, 63));
        var sample = VoidWarehouseShulker.pack(new ItemStack(Items.STONE), 64);
        assertTrue(data.withdraw(sample, BigInteger.ONE).isEmpty());
        assertEquals(BigInteger.ONE, data.amountOf(Items.SHULKER_BOX));
        assertEquals(BigInteger.valueOf(63), data.amountOf(Items.STONE));
        data.insert(new ItemStack(Items.STONE, 1));
        assertFalse(data.withdraw(sample, BigInteger.ONE).isEmpty());
        assertEquals(BigInteger.ZERO, data.amountOf(Items.SHULKER_BOX));
        assertEquals(BigInteger.ZERO, data.amountOf(Items.STONE));
    }
    @Test void decoratedContainerCannotBeRecreatedFromPlainMaterials() {
        var data = new VoidWarehouseData();
        data.insert(new ItemStack(Items.SHULKER_BOX));
        data.insert(new ItemStack(Items.STONE, 64));
        var sample = VoidWarehouseShulker.pack(new ItemStack(Items.STONE), 64);
        sample.setHoverName(net.minecraft.network.chat.Component.literal("Named box"));
        assertTrue(data.withdraw(sample, BigInteger.ONE).isEmpty());
        assertEquals(BigInteger.ONE, data.amountOf(Items.SHULKER_BOX));
        assertEquals(BigInteger.valueOf(64), data.amountOf(Items.STONE));
    }
    @Test void payloadReadPreservesNbtAndMergesRepeatedSlots() {
        var sample = VoidWarehouseShulker.pack(new ItemStack(Items.STONE), 128);
        var payload = VoidWarehouseData.payloadOf(sample);
        assertEquals(1, payload.size());
        assertEquals(128, payload.get(0).getCount());
        payload.get(0).setCount(1);
        assertEquals(128, VoidWarehouseData.payloadOf(sample).get(0).getCount());
    }
}
