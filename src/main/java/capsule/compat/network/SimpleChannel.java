package capsule.compat.network;

import capsule.CapsuleMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A tiny re-implementation of Forge's SimpleChannel on top of Fabric networking.
 * Every message travels on one channel, prefixed by the numeric id it was registered with.
 */
public class SimpleChannel {
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation("capsule", "capsule_channel");

    private static class Entry<M> {
        final BiConsumer<M, FriendlyByteBuf> encoder;
        final Function<FriendlyByteBuf, M> decoder;
        final BiConsumer<M, Supplier<NetworkEvent.Context>> handler;

        Entry(BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
            this.encoder = encoder;
            this.decoder = decoder;
            this.handler = handler;
        }

        void receive(FriendlyByteBuf buf, NetworkEvent.Context ctx) {
            M message = decoder.apply(buf);
            handler.accept(message, () -> ctx);
        }
    }

    private final Map<Integer, Entry<?>> byId = new HashMap<>();
    private final Map<Class<?>, Integer> idByClass = new HashMap<>();

    public <M> void registerMessage(int id, Class<M> type, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder, BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        byId.put(id, new Entry<>(encoder, decoder, handler));
        idByClass.put(type, id);
    }

    /** Messages sent by clients are handled here (on the server's main thread). */
    public void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(CHANNEL_ID, (server, player, handler, buf, responseSender) -> {
            Entry<?> entry = byId.get((int) buf.readUnsignedByte());
            if (entry != null) entry.receive(buf, new NetworkEvent.Context(player, server));
        });
    }

    /** Messages sent by the server arrive here on the client. */
    public void receiveOnClient(FriendlyByteBuf buf) {
        Entry<?> entry = byId.get((int) buf.readUnsignedByte());
        if (entry != null) entry.receive(buf, new NetworkEvent.Context(null, ClientSide.gameThread()));
    }

    @SuppressWarnings("unchecked")
    private <M> FriendlyByteBuf encode(M message) {
        int id = idByClass.get(message.getClass());
        Entry<M> entry = (Entry<M>) byId.get(id);
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeByte(id);
        entry.encoder.accept(message, buf);
        return buf;
    }

    /** Client to server. */
    public <M> void sendToServer(M message) {
        ClientSide.send(encode(message));
    }

    /** Server to one player. */
    public <M> void sendTo(M message, ServerPlayer player) {
        ServerPlayNetworking.send(player, CHANNEL_ID, encode(message));
    }

    /** Answer the player who sent the message being handled. */
    public <M> void reply(M message, NetworkEvent.Context ctx) {
        if (ctx.getSender() != null) sendTo(message, ctx.getSender());
    }

    /** Server to every player near a point. */
    public <M> void send(PacketDistributor.PacketTarget target, M message) {
        MinecraftServer server = CapsuleMod.server;
        if (server == null) return;
        ServerLevel level = server.getLevel(target.point.dimension);
        if (level == null) return;
        double r2 = target.point.radius * target.point.radius;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(target.point.x, target.point.y, target.point.z) <= r2) {
                sendTo(message, player);
            }
        }
    }
}
