package capsule.compat.network;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

/** Stand-in for Forge's PacketDistributor, with just the "players near a point" target. */
public class PacketDistributor {
    public static final Near NEAR = new Near();

    public static class TargetPoint {
        public final double x, y, z, radius;
        public final ResourceKey<Level> dimension;

        public TargetPoint(double x, double y, double z, double radius, ResourceKey<Level> dimension) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.dimension = dimension;
        }
    }

    public static class PacketTarget {
        public final TargetPoint point;

        PacketTarget(TargetPoint point) {
            this.point = point;
        }
    }

    public static class Near {
        public PacketTarget with(Supplier<TargetPoint> supplier) {
            return new PacketTarget(supplier.get());
        }
    }
}
