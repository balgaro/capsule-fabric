package capsule.plugins.claims;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * Open Parties and Claims protects chunk columns. Its server API answers by player id: the chunk owner, their party
 * and allies have access.
 * <p>
 * The API is called by reflection: its method names changed between versions of the mod (older versions check access
 * with the claim's player config, newer ones by dimension and chunk), and Capsule must work with either without
 * compiling against one of them.
 */
class OpenPartiesAndClaimsAdapter implements ClaimAdapter {
    private static final String API = "xaero.pac.common.server.api.OpenPACServerAPI";

    private final Method apiGet;
    private final Method getServerClaimsManager;
    private final Method getChunkProtection;

    OpenPartiesAndClaimsAdapter() {
        try {
            Class<?> api = Class.forName(API);
            apiGet = api.getMethod("get", MinecraftServer.class);
            getServerClaimsManager = api.getMethod("getServerClaimsManager");
            getChunkProtection = api.getMethod("getChunkProtection");
        } catch (ReflectiveOperationException e) {
            throw new LinkageError("Open Parties and Claims API not found: " + e);
        }
    }

    @Override
    public String name() {
        return "Open Parties and Claims";
    }

    @Override
    public List<Claim> claims(ServerLevel level, BoundingBox box, ServerPlayer player) {
        try {
            Object api = apiGet.invoke(null, level.getServer());
            Object claims = getServerClaimsManager.invoke(api);
            Object protection = getChunkProtection.invoke(api);
            Method claimAt = claims.getClass().getMethod("get", ResourceLocation.class, int.class, int.class);
            claimAt.setAccessible(true);
            Access access = access(protection);
            ResourceLocation dimension = level.dimension().location();
            UUID id = player.getUUID();
            return ClaimAdapter.perChunk(level, box, (chunkX, chunkZ) -> {
                try {
                    Object claim = claimAt.invoke(claims, dimension, chunkX, chunkZ);
                    return claim == null ? null : access.hasAccess(protection, claim, id, dimension, chunkX, chunkZ);
                } catch (ReflectiveOperationException e) {
                    throw unwrap(e);
                }
            });
        } catch (ReflectiveOperationException e) {
            throw unwrap(e);
        }
    }

    private interface Access {
        boolean hasAccess(Object protection, Object claim, UUID player, ResourceLocation dimension, int chunkX, int chunkZ) throws ReflectiveOperationException;
    }

    /**
     * Newer API: hasChunkAccess(UUID, dimension, chunkX, chunkZ). Older API: hasChunkAccess(config of the claim, UUID).
     */
    private static Access access(Object protection) throws NoSuchMethodException {
        for (Method m : protection.getClass().getMethods()) {
            if (m.getName().equals("hasChunkAccess") && m.getParameterCount() == 4 && m.getParameterTypes()[0] == UUID.class) {
                m.setAccessible(true);
                return (p, claim, player, dimension, x, z) -> (boolean) m.invoke(p, player, dimension, x, z);
            }
        }
        Method getConfig = null;
        Method hasAccess = null;
        for (Method m : protection.getClass().getMethods()) {
            if (m.getName().equals("getConfig") && m.getParameterCount() == 1) getConfig = m;
            if (m.getName().equals("hasChunkAccess") && m.getParameterCount() == 2 && m.getParameterTypes()[1] == UUID.class) hasAccess = m;
        }
        if (getConfig == null || hasAccess == null) throw new NoSuchMethodException("hasChunkAccess");
        Method config = getConfig;
        Method access = hasAccess;
        config.setAccessible(true);
        access.setAccessible(true);
        return (p, claim, player, dimension, x, z) -> (boolean) access.invoke(p, config.invoke(p, claim), player);
    }

    private static RuntimeException unwrap(ReflectiveOperationException e) {
        Throwable cause = e instanceof InvocationTargetException ite && ite.getCause() != null ? ite.getCause() : e;
        return cause instanceof RuntimeException re ? re : new IllegalStateException(cause);
    }
}
