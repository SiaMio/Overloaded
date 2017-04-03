package com.cjm721.ibhstd.common.block.compressed;

import com.cjm721.ibhstd.ModStart;
import com.google.common.base.Function;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.property.IExtendedBlockState;

import java.util.*;

/**
 * Created by CJ on 4/2/2017.
 */
public class CompressedBaskedModel implements IBakedModel {
    public static final ModelResourceLocation BAKED_MODEL = new ModelResourceLocation(ModStart.MODID + ":bakedmodelblock");
    private final Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter;

    private VertexFormat format;
    private Map<Block,List<BakedQuad>> cache;

    private TextureAtlasSprite defaultSprite;

    public CompressedBaskedModel(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        this.format = format;
        this.bakedTextureGetter = bakedTextureGetter;
        defaultSprite = bakedTextureGetter.apply(new ResourceLocation("minecraft", "blocks/cobblestone"));
        cache = new HashMap<>();
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, Vec3d normal, double x, double y, double z, float u, float v, TextureAtlasSprite sprite) {
        for (int e = 0; e < format.getElementCount(); e++) {
            switch (format.getElement(e).getUsage()) {
                case POSITION:
                    builder.put(e, (float)x, (float)y, (float)z, 1.0f);
                    break;
                case COLOR:
                    builder.put(e, 1.0f, 1.0f, 1.0f, 1.0f);
                    break;
                case UV:
                    if (format.getElement(e).getIndex() == 0) {
                        u = sprite.getInterpolatedU(u);
                        v = sprite.getInterpolatedV(v);
                        builder.put(e, u, v, 0f, 1f);
                        break;
                    }
                case NORMAL:
                    builder.put(e, (float) normal.xCoord, (float) normal.yCoord, (float) normal.zCoord, 0f);
                    break;
                default:
                    builder.put(e);
                    break;
            }
        }
    }

    private BakedQuad createQuad(Vec3d v1, Vec3d v2, Vec3d v3, Vec3d v4, TextureAtlasSprite sprite) {
        Vec3d normal = v3.subtract(v2).crossProduct(v1.subtract(v2)).normalize();

        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(format);
        builder.setTexture(sprite);
        putVertex(builder, normal, v1.xCoord, v1.yCoord, v1.zCoord, 0, 0, sprite);
        putVertex(builder, normal, v2.xCoord, v2.yCoord, v2.zCoord, 0, 16, sprite);
        putVertex(builder, normal, v3.xCoord, v3.yCoord, v3.zCoord, 16, 16, sprite);
        putVertex(builder, normal, v4.xCoord, v4.yCoord, v4.zCoord, 16, 0, sprite);
        return builder.build();
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        if (side != null) {
            return Collections.emptyList();
        }

        Block block = state.getBlock();
        List<BakedQuad> quads = cache.get(block);

        if(quads != null){
            return quads;
        }

        quads = new ArrayList<>();
        CompressedBlock compressedBlock = (CompressedBlock)block;
        ModelResourceLocation location = compressedBlock.getBaseModelLocation();
        TextureAtlasSprite sprite = bakedTextureGetter.apply(new ResourceLocation(location.getResourceDomain(), "blocks/" + location.getResourcePath()));

        int tilesPerRow = 1;
        for(int i = 0; i < compressedBlock.getCompressionAmount(); i++) {
            tilesPerRow *= 2;
        }
        float incAmo = 1F/((float)tilesPerRow);
        //TextureAtlasSprite sprite = defaultSprite;
        // z = 0
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(0 + incAmo * y, 0 + incAmo * x, 0), new Vec3d(0 + incAmo * y, incAmo + incAmo * x, 0),
                        new Vec3d(incAmo  + incAmo * y, incAmo + incAmo * x, 0), new Vec3d(incAmo  + incAmo * y, 0 + incAmo * x, 0), sprite));
            }
        }

        // z = 1
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(0 + incAmo * y, 0 + incAmo * x, 1), new Vec3d(incAmo  + incAmo * y, 0 + incAmo * x, 1),
                        new Vec3d(incAmo  + incAmo * y, incAmo + incAmo * x, 1), new Vec3d(0 + incAmo * y, incAmo + incAmo * x, 1), sprite));
            }
        }

//        // y = 1
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(0 + incAmo * y, 1,0 + incAmo * x), new Vec3d(0 + incAmo * y, 1, incAmo + incAmo * x),
                        new Vec3d(incAmo  + incAmo * y, 1, incAmo + incAmo * x), new Vec3d(incAmo  + incAmo * y, 1,0 + incAmo * x), sprite));
            }
        }
//        // y = 0
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(0 + incAmo * y, 0,0 + incAmo * x), new Vec3d(incAmo  + incAmo * y, 0,0 + incAmo * x),
                        new Vec3d(incAmo  + incAmo * y, 0, incAmo + incAmo * x), new Vec3d(0 + incAmo * y, 0, incAmo + incAmo * x), sprite));
            }
        }

//        // x = 1
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(1,0 + incAmo * y, 0 + incAmo * x), new Vec3d(1, incAmo  + incAmo * y, 0 + incAmo * x),
                        new Vec3d(1, incAmo  + incAmo * y, incAmo + incAmo * x), new Vec3d(1, 0 + incAmo * y, incAmo + incAmo * x), sprite));
            }
        }

//        // x = 0
        for(int x = 0; x < tilesPerRow; x++) {
            for(int y = 0; y < tilesPerRow; y++) {
                quads.add(createQuad(new Vec3d(0,0 + incAmo * y, 0 + incAmo * x),new Vec3d(0, 0 + incAmo * y, incAmo + incAmo * x),
                        new Vec3d(0, incAmo  + incAmo * y, incAmo + incAmo * x), new Vec3d(0, incAmo  + incAmo * y, 0 + incAmo * x), sprite));
            }
        }


        this.cache.put(block, quads);
        return quads;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return null;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return defaultSprite;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }
}
