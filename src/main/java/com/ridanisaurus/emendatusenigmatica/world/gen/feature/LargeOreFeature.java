package com.ridanisaurus.emendatusenigmatica.world.gen.feature;

import com.mojang.serialization.Codec;
import com.ridanisaurus.emendatusenigmatica.loader.EELoader;
import com.ridanisaurus.emendatusenigmatica.loader.deposit.model.common.CommonBlockDefinitionModel;
import com.ridanisaurus.emendatusenigmatica.loader.deposit.model.custom.LargeDepositModel;
import com.ridanisaurus.emendatusenigmatica.loader.parser.model.StrataModel;
import com.ridanisaurus.emendatusenigmatica.registries.EERegistrar;
import com.ridanisaurus.emendatusenigmatica.util.WorldGenHelper;
import com.ridanisaurus.emendatusenigmatica.world.gen.feature.config.LargeOreFeatureConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.noise.module.source.Spheres;

import java.util.ArrayList;
import java.util.Random;

public class LargeOreFeature extends Feature<LargeOreFeatureConfig> {
    private final Spheres noise;
    private boolean hasSeeded = false;
    private LargeDepositModel model;
    private ArrayList<CommonBlockDefinitionModel> blocks;

    public LargeOreFeature(Codec<LargeOreFeatureConfig> codec, LargeDepositModel model) {
        super(codec);
        this.model = model;
        noise = new Spheres();
        blocks = new ArrayList<>();
        for (CommonBlockDefinitionModel block : model.getConfig().getBlocks()) {
            NonNullList<CommonBlockDefinitionModel> filled = NonNullList.withSize(block.getWeight(), block);
            blocks.addAll(filled);
        }
    }

    @Override
    public boolean generate(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos pos, LargeOreFeatureConfig config) {
        if (!hasSeeded) {
            hasSeeded = true;
        }
        int yTop = model.getConfig().getMaxYLevel();
        int yBottom = model.getConfig().getMinYLevel();

        if (!model.getDimensions().contains(WorldGenHelper.getDimensionAsString(reader.getWorld()))) {
            return false;
        }

        if (rand.nextInt(100) > model.getConfig().getChance()) {
            return false;
        }

        int radius = model.getConfig().getRadius();

        radius += 0.5;
        radius += 0.5;
        radius += 0.5;

        final double invRadiusX = 1d / radius;
        final double invRadiusY = 1d / radius;
        final double invRadiusZ = 1d / radius;

        final int ceilRadiusX = (int) Math.ceil(radius);
        final int ceilRadiusY = (int) Math.ceil(radius);
        final int ceilRadiusZ = (int) Math.ceil(radius);

        double nextXn = 0;
        forX:
        for (int x = 0; x <= ceilRadiusX; ++x) {
            final double xn = nextXn;
            nextXn = (x + 1) * invRadiusX;
            double nextYn = 0;
            forY:
            for (int y = 0; y <= ceilRadiusY; ++y) {
                final double yn = nextYn;
                nextYn = (y + 1) * invRadiusY;
                double nextZn = 0;
                forZ:
                for (int z = 0; z <= ceilRadiusZ; ++z) {
                    final double zn = nextZn;
                    nextZn = (z + 1) * invRadiusZ;

                    double distanceSq = lengthSq(xn, yn, zn);
                    if (distanceSq > 1) {
                        if (z == 0) {
                            if (y == 0) {
                                break forX;
                            }
                            break forY;
                        }
                        break forZ;
                    }
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ x, pos.getY()+ y, pos.getZ() + z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ -x, pos.getY()+ y, pos.getZ() + z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ x, pos.getY()+ -y, pos.getZ() + z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ x, pos.getY()+ y, pos.getZ() + -z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ -x, pos.getY()+ -y, pos.getZ() + z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ x, pos.getY()+ -y, pos.getZ() + -z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ -x, pos.getY()+ y, pos.getZ() + -z), config);
                    placeBlock(reader, generator, rand, new BlockPos(pos.getX()+ -x, pos.getY()+ -y, pos.getZ() + -z), config);
                }
            }
        }
        return true;
    }

    private void placeBlock(ISeedReader reader, ChunkGenerator generator, Random rand, BlockPos
            pos, LargeOreFeatureConfig config) {
        if (!config.target.test(reader.getBlockState(pos), rand)) {
            return;
        }
        if (rand.nextInt(100) > model.getConfig().getDensity()) {
            return;
        }


        int index = rand.nextInt(blocks.size());
        CommonBlockDefinitionModel commonBlockDefinitionModel = blocks.get(index);
        if (commonBlockDefinitionModel.getBlock() != null) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(commonBlockDefinitionModel.getBlock()));
            reader.setBlockState(pos, block.getDefaultState(), 2);
        } else if (commonBlockDefinitionModel.getTag() != null) {
            ITag<Block> blockITag = BlockTags.getCollection().get(new ResourceLocation(commonBlockDefinitionModel.getTag()));
            Block block = blockITag.getRandomElement(rand);
            reader.setBlockState(pos, block.getDefaultState(), 2);
        } else if (commonBlockDefinitionModel.getMaterial() != null) {
            BlockState currentFiller = reader.getBlockState(pos);
            String fillerId = currentFiller.getBlock().getRegistryName().toString();
            Integer strataIndex = EELoader.STRATA_INDEX_BY_FILLER.getOrDefault(fillerId, null);
            if (strataIndex != null) {
                StrataModel stratum = EELoader.STRATA.get(strataIndex);
                Block block = EERegistrar.oreBlockTable.get(stratum.getId(), commonBlockDefinitionModel.getMaterial()).get();
                reader.setBlockState(pos, block.getDefaultState(), 2);
            }
        }
    }

    private static double lengthSq(double x, double y, double z) {
        return (x * x) + (y * y) + (z * z);
    }
}
