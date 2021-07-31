package badgamesinc.hypnotic.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Streams;

import badgamesinc.hypnotic.config.friends.FriendManager;
import badgamesinc.hypnotic.module.ModuleManager;
import badgamesinc.hypnotic.module.player.Scaffold;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FluidBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.EndermanEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * @author Tigermouthbear 9/26/20
 */
public class WorldUtils {
	public static MinecraftClient mc = MinecraftClient.getInstance();
    public static boolean placeBlockMainHand(BlockPos pos) {
        return placeBlockMainHand(pos, true);
    }
    public static BlockPos getForwardBlock(double length) {
		MinecraftClient mc = MinecraftClient.getInstance();
        final double yaw = Math.toRadians(mc.player.getYaw());
        BlockPos fPos = new BlockPos(mc.player.getX() + (-Math.sin(yaw) * length), mc.player.getY(), mc.player.getZ() + (Math.cos(yaw) * length));
        return fPos;
	}
    public static boolean placeBlockMainHand(BlockPos pos, Boolean rotate) {
        return placeBlockMainHand(pos, rotate, true);
    }
    public static boolean placeBlockMainHand(BlockPos pos, Boolean rotate, Boolean airPlace) {
        return placeBlockMainHand(pos, rotate, airPlace, false);
    }
    public static boolean placeBlockMainHand(BlockPos pos, Boolean rotate, Boolean airPlace, Boolean ignoreEntity) {
        return placeBlockMainHand(pos, rotate, airPlace, ignoreEntity, null);
    }
    public static boolean placeBlockMainHand(BlockPos pos, Boolean rotate, Boolean airPlace, Boolean ignoreEntity, Direction overrideSide) {
        return placeBlock(Hand.MAIN_HAND, pos, rotate, airPlace, ignoreEntity, overrideSide);
    }
    public static boolean placeBlockNoRotate(Hand hand, BlockPos pos) {
        return placeBlock(hand, pos, false, true, false);
    }

    public static boolean placeBlock(Hand hand, BlockPos pos) {
        placeBlock(hand, pos, true, false);
        return true;
    }
    public static boolean placeBlock(Hand hand, BlockPos pos, Boolean rotate) {
        placeBlock(hand, pos, rotate, false);
        return true;
    }
    public static boolean placeBlock(Hand hand, BlockPos pos, Boolean rotate, Boolean airPlace) {
        placeBlock(hand, pos, rotate, airPlace, false);
        return true;
    }
    public static boolean placeBlock(Hand hand, BlockPos pos, Boolean rotate, Boolean airPlace, Boolean ignoreEntity) {
        placeBlock(hand, pos, rotate, airPlace, ignoreEntity, null);
        return true;
    }

    public static boolean placeBlock(Hand hand, BlockPos pos, Boolean rotate, Boolean airPlace, Boolean ignoreEntity, Direction overrideSide) {
        // make sure place is empty if ignoreEntity is not true
        if(ignoreEntity) {
            if (!mc.world.getBlockState(pos).getMaterial().isReplaceable())
                return false;
        } else if(!mc.world.getBlockState(pos).getMaterial().isReplaceable() || !mc.world.canPlace(Blocks.OBSIDIAN.getDefaultState(), pos, ShapeContext.absent()))
            return false;

        Vec3d eyesPos = new Vec3d(mc.player.getX(),
                mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()),
                mc.player.getZ());

        Vec3d hitVec = null;
        BlockPos neighbor = null;
        Direction side2 = null;

        if(overrideSide != null) {
            neighbor = pos.offset(overrideSide.getOpposite());
            side2 = overrideSide;
        }

        for(Direction side: Direction.values()) {
            if(overrideSide == null) {
                neighbor = pos.offset(side);
                side2 = side.getOpposite();

                // check if neighbor can be right clicked aka it isnt air
                if(mc.world.getBlockState(neighbor).isAir() || mc.world.getBlockState(neighbor).getBlock() instanceof FluidBlock) {
                    neighbor = null;
                    side2 = null;
                    continue;
                }
            }

            hitVec = new Vec3d(neighbor.getX(), neighbor.getY(), neighbor.getZ()).add(0.5, 0.5, 0.5).add(new Vec3d(side2.getUnitVector()).multiply(0.5));
            break;
        }

        // Air place if no neighbour was found
        if(airPlace) {
            if (hitVec == null) hitVec = new Vec3d(pos.getX(), pos.getY(), pos.getZ());
            if (neighbor == null) neighbor = pos;
            if (side2 == null) side2 = Direction.UP;
        } else if(hitVec == null || neighbor == null || side2 == null) {
            return false;
        }

        // place block
        double diffX = hitVec.x - eyesPos.x;
        double diffY = hitVec.y - eyesPos.y;
        double diffZ = hitVec.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));
        
        float[] rotations = {
                mc.player.getYaw()
                        + MathHelper.wrapDegrees(yaw - mc.player.getYaw()),
                mc.player.getPitch() + MathHelper
                        .wrapDegrees(pitch - mc.player.getPitch())};

        if(rotate) {
        	if (ModuleManager.INSTANCE.getModule(Scaffold.class).extend.getValue() > 1) {
	        	RotationUtils.setSilentYaw(rotations[0]);
	        	RotationUtils.setSilentPitch(rotations[1]);
        	}
        	mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(rotations[0], rotations[1], mc.player.isOnGround()));
        } else {
        	RotationUtils.resetPitch();
        }

        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        mc.interactionManager.interactBlock(mc.player, mc.world, hand, new BlockHitResult(hitVec, side2, neighbor, false));
        mc.player.swingHand(hand);
        mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));

        return true;
    }

    public static final List<Block> NONSOLID_BLOCKS = Arrays.asList(
            Blocks.AIR, Blocks.LAVA, Blocks.WATER, Blocks.GRASS,
            Blocks.VINE, Blocks.SEAGRASS, Blocks.TALL_SEAGRASS,
            Blocks.SNOW, Blocks.TALL_GRASS, Blocks.FIRE, Blocks.VOID_AIR);

    public static boolean canReplace(BlockPos pos) {
        return NONSOLID_BLOCKS.contains(mc.world.getBlockState(pos).getBlock()) && mc.world.getOtherEntities(null, new Box(pos)).stream().noneMatch(Entity::collides);
    }

    public static void moveEntityWithSpeed(Entity entity, double speed, boolean shouldMoveY) {
        float yaw = (float) Math.toRadians(mc.player.getYaw());

        double motionX = 0;
        double motionY = 0;
        double motionZ = 0;

        if(mc.player.input.pressingForward) {
            motionX = -(MathHelper.sin(yaw) * speed);
            motionZ = MathHelper.cos(yaw) * speed;
        } else if(mc.player.input.pressingBack) {
            motionX = MathHelper.sin(yaw) * speed;
            motionZ = -(MathHelper.cos(yaw) * speed);
        }

        if(mc.player.input.pressingLeft) {
            motionZ = MathHelper.sin(yaw) * speed;
            motionX = MathHelper.cos(yaw) * speed;
        } else if(mc.player.input.pressingRight) {
            motionZ = -(MathHelper.sin(yaw) * speed);
            motionX = -(MathHelper.cos(yaw) * speed);
        }

        if(shouldMoveY) {
            if(mc.player.input.jumping) {
                motionY = speed;
            } else if(mc.player.input.sneaking) {
                motionY = -speed;
            }
        }

        //strafe
        if(mc.player.input.pressingForward && mc.player.input.pressingLeft) {
            motionX = (MathHelper.cos(yaw) * speed) - (MathHelper.sin(yaw) * speed);
            motionZ = (MathHelper.cos(yaw) * speed) + (MathHelper.sin(yaw) * speed);
        } else if(mc.player.input.pressingLeft && mc.player.input.pressingBack) {
            motionX = (MathHelper.cos(yaw) * speed) + (MathHelper.sin(yaw) * speed);
            motionZ = -(MathHelper.cos(yaw) * speed) + (MathHelper.sin(yaw) * speed);
        } else if(mc.player.input.pressingBack && mc.player.input.pressingRight) {
            motionX = -(MathHelper.cos(yaw) * speed) + (MathHelper.sin(yaw) * speed);
            motionZ = -(MathHelper.cos(yaw) * speed) - (MathHelper.sin(yaw) * speed);
        } else if(mc.player.input.pressingRight && mc.player.input.pressingForward) {
            motionX = -(MathHelper.cos(yaw) * speed) - (MathHelper.sin(yaw) * speed);
            motionZ = (MathHelper.cos(yaw) * speed) - (MathHelper.sin(yaw) * speed);
        }

        entity.setVelocity(motionX, motionY, motionZ);
    }

    public static List<BlockPos> getAllInBox(final int x1, final int y1, final int z1, final int x2, final int y2, final int z2) {
        List<BlockPos> list = new ArrayList<>();
        // wanted to see how inline I could make this XD, good luck any future readers
        for(int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) for(int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) for(int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) list.add(new BlockPos(x, y, z));
        return list;
    }

    public static List<BlockPos> getBlocksInReachDistance() {
        List<BlockPos> cube = new ArrayList<>();
        for(int x = -4; x <= 4; x++)
            for(int y = -4; y <= 4; y++)
                for(int z = -4; z <= 4; z++)
                    cube.add(mc.player.getBlockPos().add(x, y, z));

        return cube.stream().filter(pos -> mc.player.squaredDistanceTo(new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ())) <= 18.0625).collect(Collectors.toList());
    }

    //Credit to KAMI for code below
    public static double[] calculateLookAt(double px, double py, double pz, PlayerEntity me) {
        double dirx = me.getX() - px;
        double diry = me.getY() + me.getEyeHeight(me.getPose()) - py;
        double dirz = me.getZ() - pz;

        double len = Math.sqrt(dirx * dirx + diry * diry + dirz * dirz);

        dirx /= len;
        diry /= len;
        dirz /= len;

        double pitch = Math.asin(diry);
        double yaw = Math.atan2(dirz, dirx);

        //to degree
        pitch = pitch * 180.0d / Math.PI;
        yaw = yaw * 180.0d / Math.PI;

        yaw += 90f;

        return new double[]{yaw, pitch};
    }
    //End credit to Kami

    public static void rotate(float yaw, float pitch) {
        mc.player.setYaw(yaw);
        mc.player.setPitch(pitch);
    }

    public static void rotate(double[] rotations) {
        mc.player.setYaw((float) rotations[0]);
        mc.player.setPitch((float) rotations[1]);
    }

    public static void lookAtBlock(BlockPos blockToLookAt) {
        rotate(calculateLookAt(blockToLookAt.getX(), blockToLookAt.getY(), blockToLookAt.getZ(), mc.player));
    }

    public static String vectorToString(Vec3d vector, boolean... includeY) {
        boolean reallyIncludeY = includeY.length <= 0 || includeY[0];
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append((int) Math.floor(vector.x));
        builder.append(", ");
        if(reallyIncludeY) {
            builder.append((int) Math.floor(vector.y));
            builder.append(", ");
        }
        builder.append((int) Math.floor(vector.z));
        builder.append(")");
        return builder.toString();
    }

    public static List<Entity> getTargets(boolean players, boolean friends, boolean teammates, boolean passive, boolean hostile, boolean nametagged, boolean bots) {
        return StreamSupport.stream(mc.world.getEntities().spliterator(), false).filter(entity -> isTarget(entity, players, friends, teammates, passive, hostile, nametagged, bots)).collect(Collectors.toList());
    }

    public static boolean isTarget(Entity entity, boolean players, boolean friends, boolean teammates, boolean passive, boolean hostile, boolean nametagged, boolean bots) {
        if(!(entity instanceof LivingEntity) || entity == mc.player) return false;

        if(players && entity instanceof PlayerEntity) return true;
        if(friends && entity instanceof PlayerEntity && FriendManager.INSTANCE.isFriend(((PlayerEntity) entity).getGameProfile().getName())) return true;
        if(teammates && entity.getScoreboardTeam() == mc.player.getScoreboardTeam() && mc.player.getScoreboardTeam() != null) return true;
        if(passive && isPassive(entity)) return true;
        if(hostile && isHostile(entity)) return true;
        if(nametagged && entity.hasCustomName()) return true;
        if(bots && isBot(entity)) return true;

        return false;
    }

    public static List<Entity> getPlayerTargets() {
        return getPlayerTargets(-1, false);
    }
    public static List<Entity> getPlayerTargets(double withinDistance) {
        return getPlayerTargets(withinDistance, true);
    }
    public static List<Entity> getPlayerTargets(double withinDistance, boolean doDistance) {
        List<Entity> targets = new ArrayList<>();

        targets.addAll(Streams.stream(mc.world.getEntities()).filter(entity -> isValidTarget(entity, withinDistance, doDistance)).collect(Collectors.toList()));
        targets.sort(Comparators.entityDistance);

        return targets;
    }

    public static boolean isValidTarget(Entity entity) {
        return isValidTarget(entity, -1, false);
    }
    public static boolean isValidTarget(Entity entity, double distance) {
        return isValidTarget(entity, distance, true);
    }
    public static boolean isValidTarget(Entity entity, double distance, boolean doDistance) {
        return (entity instanceof PlayerEntity || entity instanceof OtherClientPlayerEntity)
                && !friendCheck(entity)
                && !entity.isRemoved()
                && !hasZeroHealth(entity)
                && !shouldDistance(entity, distance, doDistance)
                && entity != mc.player;
    }

    private static boolean shouldDistance(Entity entity, double distance, boolean doDistance) {
        if(doDistance) return mc.player.distanceTo(entity) > distance;
        else return false;
    }

    public static boolean hasZeroHealth(PlayerEntity playerEntity) {
        return hasZeroHealth((Entity) playerEntity);
    }
    public static boolean hasZeroHealth(Entity entity) {
        if(entity instanceof PlayerEntity) {
            return (((PlayerEntity) entity).getHealth() <= 0);
        } else return false;
    }

    public static boolean friendCheck(PlayerEntity playerEntity) {
        return friendCheck((Entity) playerEntity);
    }
    public static boolean friendCheck(Entity entity) {
        if(entity instanceof PlayerEntity) {
            return FriendManager.INSTANCE.isFriend(((PlayerEntity) entity).getGameProfile().getName());
        } else return false;
    }

    public static boolean isPassive(Entity entity) {
        if(entity instanceof IronGolemEntity && ((IronGolemEntity) entity).getAngryAt() == null) return true;
        else if(entity instanceof WolfEntity && (!((WolfEntity) entity).isAttacking() || ((WolfEntity) entity).getOwner() == mc.player)) return true;
        else return entity instanceof AmbientEntity || entity instanceof PassiveEntity || entity instanceof SquidEntity;
    }

    public static boolean isHostile(Entity entity) {
        if(entity instanceof IronGolemEntity) return ((IronGolemEntity) entity).getAngryAt() == mc.player.getUuid() && ((IronGolemEntity) entity).getAngryAt() != null;
        else if(entity instanceof WolfEntity) return ((WolfEntity) entity).isAttacking() && ((WolfEntity) entity).getOwner() != mc.player;
        else if(entity instanceof PiglinEntity) return ((PiglinEntity) entity).isAngryAt(mc.player);
        else if(entity instanceof EndermanEntity) return ((EndermanEntity) entity).isAngry();
        return entity.getType().getSpawnGroup() == SpawnGroup.MONSTER;
    }

    public static boolean isBot(Entity entity) {
        return entity instanceof PlayerEntity && entity.isInvisibleTo(mc.player) && !entity.isOnGround() && !entity.collides();
    }

    public static void fakeJump() {
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.40, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 0.75, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.01, mc.player.getZ(), true));
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY() + 1.15, mc.player.getZ(), true));
    }

    public static BlockPos roundBlockPos(Vec3d vec) {
        return new BlockPos(vec.x, (int) Math.round(vec.y), vec.z);
    }

    public static void snapPlayer() {
        BlockPos lastPos = mc.player.isOnGround() ? WorldUtils.roundBlockPos(mc.player.getPos()) : mc.player.getBlockPos();
        snapPlayer(lastPos);
    }
    public static void snapPlayer(BlockPos lastPos) {
        double xPos = mc.player.getPos().x;
        double zPos = mc.player.getPos().z;

        if(Math.abs((lastPos.getX() + 0.5) - mc.player.getPos().x) >= 0.2) {
            int xDir = (lastPos.getX() + 0.5) - mc.player.getPos().x > 0 ? 1 : -1;
            xPos += 0.3 * xDir;
        }

        if(Math.abs((lastPos.getZ() + 0.5) - mc.player.getPos().z) >= 0.2) {
            int zDir = (lastPos.getZ() + 0.5) - mc.player.getPos().z > 0 ? 1 : -1;
            zPos += 0.3 * zDir;
        }

        mc.player.setVelocity(0, 0, 0);
        mc.player.updatePosition(xPos, mc.player.getY(), zPos);
        mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.isOnGround()));
    }
}
