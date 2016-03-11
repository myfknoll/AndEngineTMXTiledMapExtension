package org.andengine.extension.tmx;

import java.util.Arrays;

import org.andengine.engine.camera.Camera;
import org.andengine.extension.tmx.TMXLoader.ITMXTilePropertiesListener;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.region.ITextureRegion;
import org.andengine.opengl.util.GLState;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.color.Color;
import org.andengine.util.exception.AndEngineRuntimeException;
import org.xml.sax.Attributes;

public class MultiTMXLayer extends TMXLayer {

	// ===========================================================
	// Constants
	// ===========================================================

	private static final int MAX_TILESETS = 4;

	// ===========================================================
	// Fields
	// ===========================================================

	private final ITexture mTextures[] = new ITexture[MAX_TILESETS];
	private int usedTextures = 0;

	public MultiTMXLayer(final TMXTiledMap pTMXTiledMap, final Attributes pAttributes,
			final VertexBufferObjectManager pVertexBufferObjectManager) {
		super(pTMXTiledMap, pAttributes, pVertexBufferObjectManager);
	}

	@Override
	protected void addTileByGlobalTileID(final int pGlobalTileID, final ITMXTilePropertiesListener pTMXTilePropertyListener) {
		final TMXTiledMap tmxTiledMap = this.mTMXTiledMap;

		final int tilesHorizontal = this.mTileColumns;

		final int column = this.mTilesAdded % tilesHorizontal;
		final int row = this.mTilesAdded / tilesHorizontal;

		final TMXTile[][] tmxTiles = this.mTMXTiles;

		final ITextureRegion tmxTileTextureRegion;
		if(pGlobalTileID == 0) {
			tmxTileTextureRegion = null;
		} else {
			tmxTileTextureRegion = tmxTiledMap.getTextureRegionFromGlobalTileID(pGlobalTileID);

			ITexture texture = tmxTileTextureRegion.getTexture();

			boolean contains = Arrays.asList(mTextures).contains(texture);
			if (!contains) {
				if (usedTextures < MAX_TILESETS) {
					this.mTextures[usedTextures] = texture;
					if (usedTextures == 0) {
						mTexture = texture;
					}
					super.initBlendFunction(texture);
					usedTextures++;
				} else {
					throw new AndEngineRuntimeException("TMXTileSet limit reached.");
				}
			}
		}

		final int tileHeight = this.mTMXTiledMap.getTileHeight();
		final int tileWidth = this.mTMXTiledMap.getTileWidth();

        final TMXTile tmxTile =
                new TMXTile(pGlobalTileID, column, row, tileWidth, tileHeight, tmxTileTextureRegion);
        tmxTiles[row][column] = tmxTile;

        if (pGlobalTileID != 0) {
            this.setIndex(this.getSpriteBatchIndex(column, row));
            this.drawWithoutChecks(tmxTileTextureRegion, tmxTile.getTileX(), tmxTile.getTileY(), tileWidth,
                    tileHeight, Color.WHITE_ABGR_PACKED_FLOAT);
            this.submit(); // TODO Doesn't need to be called here, but should rather be called in a "init"
// step, when parsing the XML is complete.

            /* Notify the ITMXTilePropertiesListener if it exists. */
            if (pTMXTilePropertyListener != null) {
                final TMXProperties<TMXTileProperty> tmxTileProperties =
                        tmxTiledMap.getTMXTileProperties(pGlobalTileID);
                if (tmxTileProperties != null) {
                    pTMXTilePropertyListener.onTMXTileWithPropertiesCreated(tmxTiledMap, this, tmxTile,
                            tmxTileProperties);
                }
            }
        }

        this.mTilesAdded++;
    }

    @Override
    public void reset() {
        super.reset();

        for (int i = 0; i < usedTextures; i++) {
            this.initBlendFunction(mTextures[i]);
        }
    }

    @Override
    protected void preDraw(final GLState pGLState, final Camera pCamera) {
        super.preDraw(pGLState, pCamera);

        for (int i = 0; i < usedTextures; i++) {
            mTextures[i].bind(pGLState);
        }
    }

    @Override
    public ITexture getTexture() {
        throw new UnsupportedOperationException("use getTextures()");
    }

    public ITexture[] getTextures() {
        return mTextures;
    }
}
