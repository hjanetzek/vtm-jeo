package org.oscim.layers;

import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.map.Map;
import org.oscim.tiling.MapTile;
import org.oscim.tiling.TileLoader;
import org.oscim.tiling.TileManager;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

public class JeoTileLayer extends BitmapTileLayer {

	public JeoTileLayer(Map map, BitmapTileSource tileSource) {
		super(map, tileSource);
	}

	@Override
	protected TileLoader createLoader(TileManager tm) {
		return new TileLoader(tm) {

			@Override
			public void cleanup() {
				// TODO Auto-generated method stub

			}

			@Override
			protected boolean executeJob(MapTile tile) {
				// TODO Auto-generated method stub
				return false;
			}

		};
	}

}
