package net.lyof.phantasm.world.feature.custom;

import com.mojang.serialization.Codec;
import net.lyof.phantasm.Phantasm;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.CountConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class BasaltGazeboStructure extends Feature<CountConfig> {
    public static final Feature<CountConfig> INSTANCE = new BasaltGazeboStructure(CountConfig.CODEC);

    public BasaltGazeboStructure(Codec<CountConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(FeatureContext<CountConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();
        
        // Find a suitable ground level
        BlockPos groundPos = findGroundLevel(world, origin);
        if (groundPos == null) return false;
        
        // Generate the gazebo structure
        generateGazebo(world, groundPos, random);
        
        return true;
    }

    private BlockPos findGroundLevel(StructureWorldAccess world, BlockPos start) {
        // Look down from start position to find solid ground
        // Start a bit higher to ensure we catch the surface
        BlockPos searchStart = new BlockPos(start.getX(), Math.min(start.getY() + 20, world.getTopY() - 1), start.getZ());
        
        for (int y = searchStart.getY(); y > world.getBottomY(); y--) {
            BlockPos checkPos = new BlockPos(start.getX(), y, start.getZ());
            if (!world.getBlockState(checkPos).isAir() && 
                world.getBlockState(checkPos.up()).isAir()) {
                return checkPos.up();
            }
        }
        return null;
    }

    private void generateGazebo(StructureWorldAccess world, BlockPos center, Random random) {
        // Generate foundation (3 levels)
        generateFoundation(world, center);
        
        // Generate gazebo floor
        generateFloor(world, center.up(3));
        
        // Generate columns
        generateColumns(world, center.up(3));
        
        // Generate roof
        generateRoof(world, center.up(8));
        
        // Generate portal pool adjacent to the gazebo (overlooking it)
        // Move it slightly further out and lower for better view
        generatePortalPool(world, center.add(0, -2, 9), random);
    }

    private void generateFoundation(StructureWorldAccess world, BlockPos base) {
        // 9x9 foundation with steps
        for (int level = 0; level < 3; level++) {
            int size = 9 - level * 2;
            
            for (int x = -size/2; x <= size/2; x++) {
                for (int z = -size/2; z <= size/2; z++) {
                    BlockPos pos = base.add(x, level, z);
                    
                    // Create stepped foundation
                    if (Math.abs(x) == size/2 || Math.abs(z) == size/2) {
                        this.setBlockState(world, pos, Blocks.BASALT.getDefaultState());
                    } else if (level == 2) {
                        this.setBlockState(world, pos, Blocks.POLISHED_BASALT.getDefaultState());
                    } else {
                        this.setBlockState(world, pos, Blocks.BASALT.getDefaultState());
                    }

                    // Extend foundation downwards to prevent floating
                    if (level == 0) {
                        BlockPos downPos = pos.down();
                        int depth = 0;
                        while ((world.getBlockState(downPos).isAir() || !world.getBlockState(downPos).isOpaque()) && depth < 20) {
                            this.setBlockState(world, downPos, Blocks.BASALT.getDefaultState());
                            downPos = downPos.down();
                            depth++;
                        }
                    }
                }
            }
        }
    }

    private void generateFloor(StructureWorldAccess world, BlockPos base) {
        // 5x5 polished basalt floor
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = base.add(x, 0, z);
                this.setBlockState(world, pos, Blocks.POLISHED_BASALT.getDefaultState());
            }
        }
        
        // Decorative border with slabs (open to the South where the pool is)
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (Math.abs(x) == 2 || Math.abs(z) == 2) {
                    // Skip the South side (z == 2) to make it open
                    if (z != 2) {
                        BlockPos pos = base.add(x, 1, z);
                        this.setBlockState(world, pos, 
                            Blocks.STONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.BOTTOM));
                    }
                }
            }
        }
    }

    private void generateColumns(StructureWorldAccess world, BlockPos base) {
        // 4 corners of the gazebo - pillars
        BlockPos[] columnPositions = {
            base.add(-2, 0, -2), // NW
            base.add(2, 0, -2),  // NE  
            base.add(-2, 0, 2),  // SW
            base.add(2, 0, 2)    // SE
        };
        
        for (BlockPos columnBase : columnPositions) {
            // Generate 4-block high columns
            for (int y = 0; y < 4; y++) {
                this.setBlockState(world, columnBase.up(y), Blocks.BASALT.getDefaultState());
            }
            
            // Add decorative capital
            this.setBlockState(world, columnBase.up(4), 
                Blocks.POLISHED_BLACKSTONE_SLAB.getDefaultState().with(SlabBlock.TYPE, SlabType.TOP));
        }
    }

    private void generateRoof(StructureWorldAccess world, BlockPos base) {
        // Simple pyramidal roof
        for (int level = 0; level < 3; level++) {
            int size = 5 - level * 2;
            
            for (int x = -size/2; x <= size/2; x++) {
                for (int z = -size/2; z <= size/2; z++) {
                    BlockPos pos = base.add(x, level, z);
                    
                    if (level == 2 && x == 0 && z == 0) {
                        // Top of pyramid
                        this.setBlockState(world, pos, Blocks.BASALT.getDefaultState());
                    } else {
                        Direction facing;
                        if (z == -size/2) facing = Direction.SOUTH;
                        else if (z == size/2) facing = Direction.NORTH;
                        else if (x == -size/2) facing = Direction.EAST;
                        else facing = Direction.WEST;

                        this.setBlockState(world, pos, Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState()
                                .with(StairsBlock.FACING, facing));
                    }
                }
            }
        }
    }

    private void generatePortalPool(StructureWorldAccess world, BlockPos center, Random random) {
        // Natural pool shape
        int radius = 4;
        
        for (int x = -radius - 2; x <= radius + 2; x++) {
            for (int z = -radius - 2; z <= radius + 2; z++) {
                BlockPos pos = center.add(x, 0, z);
                double distance = Math.sqrt(x*x + z*z);
                double noise = random.nextDouble() * 1.5; // 0.0 to 1.5 variation
                
                if (distance < radius - 1 + noise) {
                    // Inner pool - Portal
                    this.setBlockState(world, pos, Blocks.END_PORTAL.getDefaultState());
                    
                    // Basin floor
                    this.setBlockState(world, pos.down(), Blocks.OBSIDIAN.getDefaultState());
                    
                    // Clear air above
                    for(int i=1; i<=3; i++) this.setBlockState(world, pos.up(i), Blocks.AIR.getDefaultState());

                } else if (distance < radius + 0.5 + noise) {
                    // Border
                    net.minecraft.block.BlockState borderState = Blocks.POLISHED_BASALT.getDefaultState();
                    if (random.nextFloat() < 0.2f) borderState = Blocks.CRYING_OBSIDIAN.getDefaultState();
                    
                    this.setBlockState(world, pos, borderState);
                    
                    // Extend border down to ground
                    BlockPos downPos = pos.down();
                    int depth = 0;
                    while ((world.getBlockState(downPos).isAir() || !world.getBlockState(downPos).isOpaque()) && depth < 20) {
                        this.setBlockState(world, downPos, Blocks.BASALT.getDefaultState());
                        downPos = downPos.down();
                        depth++;
                    }
                }
            }
        }
    }
}
