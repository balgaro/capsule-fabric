package capsule.compat.network;

import net.minecraft.server.level.ServerPlayer;

/** Stand-in for Forge's NetworkEvent: only the message context this mod needs. */
public class NetworkEvent {
    public static class Context {
        private final ServerPlayer sender;
        private final java.util.concurrent.Executor gameThread;

        /** gameThread runs work on the server or client main thread: Fabric hands messages over on the network thread. */
        public Context(ServerPlayer sender, java.util.concurrent.Executor gameThread) {
            this.sender = sender;
            this.gameThread = gameThread;
        }

        /** The player who sent the message, or null on the client. */
        public ServerPlayer getSender() {
            return sender;
        }

        /** Runs the work on the game thread (blocks, block entities and entities may only be touched there). */
        public void enqueueWork(Runnable work) {
            gameThread.execute(work);
        }

        public void setPacketHandled(boolean handled) {
        }
    }
}
