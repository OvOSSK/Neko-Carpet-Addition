package neko.voidwarehouse;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Recipe confirmation uses a player command on Minecraft 1.20.1. */
public final class VoidWarehouseRecipePrompt {

    /**
     * 点击事件的 id：{@code carpet_neko_addition:recipe_confirm}。
     *
     * <p>用本模组的命名空间，和搜索通道、打印机通道同一个前缀风格。id 必须<b>全局唯一</b> ——
     * 别的模组撞上同一个 id 时，两边都会收到对方的点击（服务端按 id 分派，没有"认领"机制）。</p>
     */
    public static final ResourceLocation CLICK_ID =
            new ResourceLocation("carpet_neko_addition", "recipe_confirm");

    /** 一条待确认挂多久（tick）：60 秒。过期点下去只说一句「已处理过」，不会静默无事发生 */
    public static final int TTL_TICKS = 1200;

    /** 每人同时最多挂几条；超了丢最旧的 */
    public static final int MAX_PENDING = 3;

    /** 最多跟踪多少个玩家的状态；超了按 LRU 淘汰（代价只是重登一次似的再问一遍） */
    private static final int MAX_TRACKED = 512;

    private static final Map<UUID, State> STATE =
            new LinkedHashMap<UUID, State>(64, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<UUID, State> eldest) {
                    return size() > MAX_TRACKED;
                }
            };

    private VoidWarehouseRecipePrompt() {
    }

    /** 一条待确认 */
    private static final class Pending {
        final VoidWarehouseRecipes.Recipe recipe;
        final List<VoidWarehouseRecipes.Recipe> othersBefore;
        final boolean en;
        final long expireTick;

        Pending(VoidWarehouseRecipes.Recipe recipe, List<VoidWarehouseRecipes.Recipe> othersBefore,
                boolean en, long expireTick) {
            this.recipe = recipe;
            this.othersBefore = othersBefore;
            this.en = en;
            this.expireTick = expireTick;
        }
    }

    /** 每玩家的开关与待确认 */
    private static final class State {
        /** 令牌 → 待确认（插入序 = 先后序，丢最旧时取第一个） */
        final Map<String, Pending> byToken = new LinkedHashMap<>();
        /** recipeId → 令牌：同一条配方不重复提问 */
        final Map<String, String> tokenByRecipe = new HashMap<>();
        /** 本局点过「不要」的配方 */
        final Set<String> refused = new HashSet<>();
        /** 本局「总是记住」 */
        boolean auto;
        /** 令牌计数器（只是为了让令牌每次不同，不做安全用途） */
        long serial;
    }

    private static State state(UUID who) {
        return STATE.computeIfAbsent(who, key -> new State());
    }

    /**
     * 造一个点击事件。
     *
     * @param decision {@code y} 记住 / {@code n} 不要 / {@code a} 总是记住
     */
    public static ClickEvent click(String token, String decision) {
        CompoundTag tag = new CompoundTag();
        tag.putString("t", token);
        tag.putString("d", decision);
        return new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/neko_recipe_confirm " + token + " " + decision);
    }

    /**
     * 这次合成要不要<b>问一句</b>（{@code false} = 走老路，立即记住）。
     *
     * <p>四种情况不问，直接按老路走：</p>
     * <ol>
     *   <li>这条已经在配方本里了 → 老路会给出「早就记过」那一条（每玩家一次）；</li>
     *   <li>配方本满了 → 老路会给出「已满」；此时问也没用，玩家点「记住」照样记不进去；</li>
     *   <li>玩家本局选过「总是记住」；</li>
     *   <li>玩家本局对这条点过「不要」。</li>
     * </ol>
     * <p>另外「已经挂着同一条」时也返回 {@code false}，但<b>不发任何消息</b> ——
     * 见 {@link #isPending} 与 {@link #ask} 的分工：那种情况既不该问，也不该报「已记住」。</p>
     */
    public static boolean shouldAsk(ServerPlayer player, VoidWarehouseData data,
                                    VoidWarehouseRecipes.Recipe recipe, int max) {
        if (player == null || data == null || recipe == null) {
            return false;
        }
        if (data.knows(recipe)) {
            return false;
        }
        if (data.recipeBookUsed() >= max) {
            return false;
        }
        State st = state(player.getUUID());
        return !st.auto && !st.refused.contains(recipe.recipeId());
    }

    /** 这条配方是否已经挂着一条待确认（shift 连合时用来判断「别再问一遍」） */
    public static boolean isPending(ServerPlayer player, VoidWarehouseRecipes.Recipe recipe) {
        if (player == null || recipe == null) {
            return false;
        }
        return state(player.getUUID()).tokenByRecipe.containsKey(recipe.recipeId());
    }

    /**
     * 挂一条待确认，返回<b>要发出去的那一行</b>（三个按钮）。
     *
     * <p>调用前必须已经问过 {@link #shouldAsk} 与 {@link #isPending}。为防误用，
     * 这里再判一次：已经挂着就返回 {@code null}（不发第二遍）。</p>
     */
    public static Component ask(ServerPlayer player, boolean en, VoidWarehouseData data,
                                VoidWarehouseRecipes.Recipe recipe, long nowTick) {
        if (player == null || data == null || recipe == null) {
            return null;
        }
        State st = state(player.getUUID());
        if (st.tokenByRecipe.containsKey(recipe.recipeId())) {
            return null;
        }
        // 超上限：先丢最旧的那一条（它的令牌随即作废，点下去就是「已处理过」）
        while (st.byToken.size() >= MAX_PENDING) {
            String oldest = st.byToken.keySet().iterator().next();
            Pending dropped = st.byToken.remove(oldest);
            if (dropped != null) {
                st.tokenByRecipe.remove(dropped.recipe.recipeId());
            }
        }
        String token = Long.toString(++st.serial, 36) + Integer.toString(player.getUUID().hashCode(), 36);
        List<VoidWarehouseRecipes.Recipe> others = data.recipesFor(recipe.output());
        st.byToken.put(token, new Pending(recipe, others, en, nowTick + TTL_TICKS));
        st.tokenByRecipe.put(recipe.recipeId(), token);
        return VoidWarehouseRecipeNotice.askLine(en, recipe, token);
    }

    /**
     * 处理一次点击（由 {@code CustomClickMixin} 调）。
     *
     * @return 这条点击事件是不是我们的（{@code false} 时调用方原样放行给原版）
     */
    public static boolean handleClick(ServerPlayer player, ResourceLocation id, Optional<Tag> payload) {
        if (player == null || id == null || !CLICK_ID.equals(id)) {
            return false;
        }
        boolean en = VoidWarehouseText.isEnglish(player);
        String token = "";
        String decision = "";
        if (payload.isPresent() && payload.get() instanceof CompoundTag tag) {
            token = tag.getString("t");
            decision = tag.getString("d");
        }
        State st = state(player.getUUID());
        Pending pending = token.isEmpty() ? null : st.byToken.remove(token);
        if (pending != null) {
            st.tokenByRecipe.remove(pending.recipe.recipeId());
        }
        long now = serverTickOf(player);
        // 令牌认不出来 / 已经用掉 / 已经过期 —— 三种都归到同一句话。
        // 不静默：玩家点了却什么都不发生，会以为界面坏了（「什么都没说 = 坏掉」的老教训）。
        if (pending == null || now > pending.expireTick) {
            player.sendSystemMessage(VoidWarehouseRecipeNotice.alreadyHandledLine(en));
            return true;
        }
        if ("n".equals(decision)) {
            st.refused.add(pending.recipe.recipeId());
            player.sendSystemMessage(VoidWarehouseRecipeNotice.refusedLine(en));
            return true;
        }
        boolean always = "a".equals(decision);
        if (always) {
            // 先切开关再记：万一下面 learn 抛异常，玩家的「别再问了」也照样生效
            st.auto = true;
        }
        VoidWarehouseData data = VoidWarehouse.data(player.level().getServer());
        if (data == null) {
            player.sendSystemMessage(VoidWarehouseRecipeNotice.alreadyHandledLine(en));
            return true;
        }
        // 重新学一次并按<b>真实结局</b>出文案：这 60 秒里可能别人也记了同一条（DUPLICATE）、
        // 也可能配方本刚好被填满（FULL）。文案仍然只有 VoidWarehouseRecipeNotice 一份。
        List<Component> out = new ArrayList<>(4);
        VoidWarehouseData.LearnOutcome outcome = data.learnCapturingOthers(pending.recipe);
        out.addAll(VoidWarehouseRecipeNotice.onCrafted(
                player.getUUID(), pending.en, outcome.result(), pending.recipe,
                pending.othersBefore, data.recipeBookUsed(),
                VoidWarehouseRecipeBook.MAX_RECIPES, now));
        if (always) {
            out.add(VoidWarehouseRecipeNotice.autoOnLine(pending.en));
        }
        for (Component line : out) {
            player.sendSystemMessage(line);
        }
        return true;
    }

    /** 本局是否已切到「总是记住」（自检用；产品侧不需要读它） */
    public static boolean isAuto(UUID who) {
        return who != null && state(who).auto;
    }

    /** 本局是否对这条点过「不要」（自检用） */
    public static boolean isRefused(UUID who, VoidWarehouseRecipes.Recipe recipe) {
        return who != null && recipe != null && state(who).refused.contains(recipe.recipeId());
    }

    /**
     * 这位玩家此刻挂着几条待确认（自检用；产品侧不需要读它）。
     *
     * <p>自检要咬「shift 连合不会刷屏」这条，而刷屏的<b>真实形态</b>就是这里变大 ——
     * 数「发了几条消息」需要拦 {@code sendSystemMessage}（假连接下更脆），
     * 数待确认条数才是直接量。</p>
     */
    public static int pendingCount(UUID who) {
        return who == null ? 0 : state(who).byToken.size();
    }

    /** 清掉一位玩家的全部状态（玩家退出时调，避免 LRU 里挂着已经下线的 UUID） */
    public static void forget(UUID who) {
        if (who != null) {
            STATE.remove(who);
        }
    }

    /**
     * 在模组初始化时调一次：<b>玩家一下线就清掉他的本局开关与待确认</b>。
     *
     * <p>🔴 这不只是「省点内存」。{@code [不要]} / {@code [总是记住]} 的契约是
     * <b>「本次登录内」</b>（见类注释「为什么只活在本局」）—— 不在这条路上清，
     * 玩家退出再进来，{@code auto} 还在，就变成「永远不再问」，
     * 而玩家没有任何界面入口能把这件事改回来（没有指令、没有按钮）。
     * 换句话说：不接这个钩子，那句「重新登录后恢复」就是假的。</p>
     *
     * <p>顺带把待确认一起丢掉：令牌本来就只活 60 秒，重登后旧按钮点下去会回
     * 「已经处理过了」，而不是莫名其妙记进一条配方。</p>
     */
    public static void init() {
        INIT_CALLED = true;
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) ->
            dispatcher.register(net.minecraft.commands.Commands.literal("neko_recipe_confirm")
                .then(net.minecraft.commands.Commands.argument("token", com.mojang.brigadier.arguments.StringArgumentType.word())
                    .then(net.minecraft.commands.Commands.argument("decision", com.mojang.brigadier.arguments.StringArgumentType.word())
                        .executes(context -> {
                            String decision = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "decision");
                            if (!java.util.Set.of("y", "n", "a").contains(decision)) return 0;
                            CompoundTag payload = new CompoundTag();
                            payload.putString("t", com.mojang.brigadier.arguments.StringArgumentType.getString(context, "token"));
                            payload.putString("d", decision);
                            return handleClick(context.getSource().getPlayerOrException(), CLICK_ID, Optional.of(payload)) ? 1 : 0;
                        })))));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            if (player != null) {
                forget(player.getUUID());
            }
        });
    }

    /**
     * {@link #init()} 到底被调过没有（自检用）。
     *
     * <p>为什么要这么一个字段：这条钩子是<b>接线</b>，没有任何玩家能直接看见它 ——
     * 漏了它，「重新登录恢复」会静默失效，而所有别的断言照样全绿
     * （效果要等到「退出再进来」才显形，自检跑不到那一步）。
     * 存一个布尔，自检就能咬住「模组初始化时真的调过」。</p>
     */
    public static boolean isInitCalled() {
        return INIT_CALLED;
    }

    private static volatile boolean INIT_CALLED = false;

    /** 自检专用：把状态清空，免得同一次开服里第二套断言被上一套的开关污染 */
    public static void resetForTest() {
        STATE.clear();
    }

    private static long serverTickOf(ServerPlayer player) {
        var server = player.level().getServer();
        return server == null ? 0L : server.getTickCount();
    }
}
