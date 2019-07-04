package com.cjm721.overloaded.block.basic;

import com.cjm721.overloaded.Overloaded;
import com.cjm721.overloaded.block.ModBlock;
import com.cjm721.overloaded.client.render.dynamic.ImageUtil;
import com.cjm721.overloaded.config.OverloadedConfig;
import com.cjm721.overloaded.tile.functional.TileInfiniteWaterSource;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

import static com.cjm721.overloaded.Overloaded.MODID;
import static net.minecraftforge.fluids.capability.CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY;

public class BlockInfiniteWaterSource extends ModBlock {

  public BlockInfiniteWaterSource() {
    super(getDefaultProperties());
    setRegistryName("infinite_water_source");
  }

  @Override
  public boolean hasTileEntity(BlockState state) {
    return true;
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  public void registerModel() {
    ModelResourceLocation location =
        new ModelResourceLocation(new ResourceLocation(MODID, "infinite_water_source"), null);
    //        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, location);

    ImageUtil.registerDynamicTexture(
        new ResourceLocation(MODID, "textures/block/infinite_water_source.png"),
        OverloadedConfig.INSTANCE.textureResolutions.blockResolution);
  }

  @OnlyIn(Dist.CLIENT)
  @Override
  @Nonnull
  public BlockRenderLayer getRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }

  @Override
  @Nonnull
  public TileEntity createTileEntity(BlockState state, IBlockReader world) {
    return new TileInfiniteWaterSource();
  }

  @Override
  public boolean onBlockActivated(
      BlockState state,
      World world,
      BlockPos pos,
      PlayerEntity player,
      Hand hand,
      BlockRayTraceResult rayTraceResult) {
    if (!world.isRemote && hand == Hand.MAIN_HAND) {
      TileEntity te = world.getTileEntity(pos);
      if (te instanceof TileInfiniteWaterSource) {
        LazyOptional<IFluidHandler> opHandler = te.getCapability(FLUID_HANDLER_CAPABILITY);
        if (!opHandler.isPresent()) {
          Overloaded.logger.warn("Water Source has no Fluid Capability? " + pos);
          return false;
        }

        FluidActionResult result =
            FluidUtil.tryFillContainerAndStow(
                player.getHeldItem(hand),
                opHandler.orElseThrow(() -> new RuntimeException("Impossible Condition")),
                null,
                Integer.MAX_VALUE,
                player,
                true);

        if (result.isSuccess()) {
          player.setHeldItem(Hand.MAIN_HAND, result.getResult());
          return true;
        }
      }
    }
    return false;
  }
}
