package neko;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.DyeColor;
import neko.voidwarehouse.*;
import neko.voidwarehouse.search.SearchApplyPayload;
import neko.voidwarehouse.restock.PrinterTakePayload;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.math.BigInteger;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PortTest {
    @Test void terminalTitleDoesNotUseUnrelatedHeldItem() {
        assertEquals("虚空仓库", VoidWarehouseTrigger.titleFor(ItemStack.EMPTY, false).getString());
        assertEquals("Void Warehouse", VoidWarehouseTrigger.titleFor(ItemStack.EMPTY, true).getString());
        assertEquals("虚空仓库", VoidWarehouseTrigger.titleFor(new ItemStack(Items.STONE), false).getString());
        ItemStack trigger = new ItemStack(Items.CRAFTING_TABLE);
        trigger.setHoverName(net.minecraft.network.chat.Component.literal("void warehouse"));
        assertEquals("void warehouse", VoidWarehouseTrigger.titleFor(trigger, false).getString());
    }

    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void warehouseNbtRoundTripAndWithdrawal() {
        VoidWarehouseData data = new VoidWarehouseData();
        ItemStack stack = new ItemStack(Items.STONE, 64);
        assertTrue(data.insert(stack));
        assertTrue(stack.isEmpty());
        assertEquals(BigInteger.valueOf(64), data.amountOf(Items.STONE));
        VoidWarehouseData loaded = VoidWarehouseData.load(data.save(new CompoundTag()));
        assertEquals(BigInteger.valueOf(64), loaded.amountOf(Items.STONE));
        assertTrue(loaded.takeItem(Items.STONE, 17));
        assertEquals(BigInteger.valueOf(47), loaded.amountOf(Items.STONE));
        assertFalse(loaded.takeItem(Items.STONE, 48));
        assertEquals(BigInteger.valueOf(47), loaded.amountOf(Items.STONE));
    }

    @Test void shulkerNbtRetainsAllItems() {
        ItemStack box = VoidWarehouseShulker.pack(new ItemStack(Items.STONE), 1728);
        var contents = VoidWarehouseShulker.contentsOf(box);
        assertEquals(27, contents.size());
        assertEquals(1728, contents.stream().mapToInt(ItemStack::getCount).sum());
        contents.get(0).setCount(1);
        assertEquals(64, VoidWarehouseShulker.contentsOf(box).get(0).getCount());
        assertFalse(VoidWarehouseShulker.isBoxable(box));
    }

    @Test void allSixteenCookingRecipesExist() {
        neko.cooking.CookingSettings.concretePowderToGlass = true;
        for (DyeColor color : DyeColor.values()) {
            assertTrue(neko.cooking.ConcretePowderToGlass.findRecipeById(
                    neko.cooking.ConcretePowderToGlass.recipeId(color)).isPresent());
        }
        neko.cooking.CookingSettings.concretePowderToGlass = false;
        assertTrue(neko.cooking.ConcretePowderToGlass.findRecipeById(
                neko.cooking.ConcretePowderToGlass.recipeId(DyeColor.WHITE)).isEmpty());
    }

    @Test void searchPacketRoundTripAndBounds() {
        var packet = new SearchApplyPayload(List.of("minecraft:stone", "minecraft:iron_ingot"));
        var buf = packet.encode();
        try { assertEquals(packet, SearchApplyPayload.decode(buf)); }
        finally { buf.release(); }
        var bad = PacketByteBufs.create();
        try {
            bad.writeVarInt(4097);
            assertThrows(IllegalArgumentException.class, () -> SearchApplyPayload.decode(bad));
        } finally { bad.release(); }
    }

    @Test void printerPacketRoundTrip() {
        var packet = new PrinterTakePayload(123, List.of("minecraft:stone"));
        var buf = PacketByteBufs.create();
        try {
            PrinterTakePayload.CODEC.encode(buf, packet);
            assertEquals(packet, PrinterTakePayload.CODEC.decode(buf));
        } finally { buf.release(); }
    }
}
