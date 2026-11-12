package neko.voidwarehouse;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import neko.voidwarehouse.search.SearchApplyPayload;
import neko.voidwarehouse.search.SearchService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SearchFilterTest {
    @BeforeAll static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test void unmatchedSearchHidesInventoryAndClearingRestoresIt() {
        VoidWarehouseData data = new VoidWarehouseData();
        data.insert(new ItemStack(Items.STONE, 64));
        var page = new VoidWarehousePageContainer(data, new VoidWarehouseViews.View(), false);
        assertFalse(page.slotAt(0).isEmpty());
        var none = roundTrip(SearchApplyPayload.forQuery("不存在的物品", List.of()));
        assertFalse(none.clearFilter());
        page.setSearchFilter(none.clearFilter() ? null : SearchService.resolveFilter(none.itemIds()));
        assertTrue(page.hasSearchFilter());
        for (int i = 0; i < 45; i++) assertTrue(page.slotAt(i).isEmpty());
        assertEquals(1, page.pageCount());
        var clear = roundTrip(SearchApplyPayload.forQuery("  ", List.of()));
        assertTrue(clear.clearFilter());
        page.setSearchFilter(clear.clearFilter() ? null : SearchService.resolveFilter(clear.itemIds()));
        assertFalse(page.hasSearchFilter());
        assertFalse(page.slotAt(0).isEmpty());
        assertEquals(java.math.BigInteger.valueOf(64), data.amountOf(Items.STONE));
    }

    @Test void matchingSearchOnlyShowsSelectedItems() {
        VoidWarehouseData data = new VoidWarehouseData();
        data.insert(new ItemStack(Items.STONE, 64));
        data.insert(new ItemStack(Items.DIAMOND, 2));
        var page = new VoidWarehousePageContainer(data, new VoidWarehouseViews.View(), false);
        var packet = roundTrip(SearchApplyPayload.forQuery("diamond", List.of("minecraft:diamond")));
        page.setSearchFilter(SearchService.resolveFilter(packet.itemIds()));
        assertFalse(page.slotAt(0).isEmpty());
        assertTrue(page.getItem(0).is(Items.DIAMOND));
        assertTrue(page.slotAt(1).isEmpty());
    }

    @Test void legacyEmptyPacketStillClears() {
        var buf = PacketByteBufs.create();
        try {
            buf.writeVarInt(0);
            assertTrue(SearchApplyPayload.decode(buf).clearFilter());
        } finally { buf.release(); }
    }

    private static SearchApplyPayload roundTrip(SearchApplyPayload packet) {
        var buf = packet.encode();
        try { return SearchApplyPayload.decode(buf); }
        finally { buf.release(); }
    }
}
