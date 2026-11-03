package capsule.compat.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;

/** Client-only networking calls, kept in their own class so a dedicated server never loads them. */
public final class ClientSide {
    private ClientSide() {
    }

    /** The client main thread. */
    @Environment(EnvType.CLIENT)
    public static java.util.concurrent.Executor gameThread() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    @Environment(EnvType.CLIENT)
    public static void send(FriendlyByteBuf buf) {
        ClientPlayNetworking.send(SimpleChannel.CHANNEL_ID, buf);
    }

    @Environment(EnvType.CLIENT)
    public static void registerReceiver(SimpleChannel channel) {
        ClientPlayNetworking.registerGlobalReceiver(SimpleChannel.CHANNEL_ID, (client, handler, buf, responseSender) -> channel.receiveOnClient(buf));
    }
}
