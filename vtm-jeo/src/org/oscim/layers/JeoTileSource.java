package org.oscim.layers;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.jeo.data.Tile;
import org.jeo.data.TileDataset;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.layers.tile.MapTile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JeoTileSource extends TileSource {
	final static Logger log = LoggerFactory.getLogger(JeoTileSource.class);

	final TileDataset mTileDataset;

	public JeoTileSource(TileDataset tileDataset) {
		log.debug("load tileset {}", tileDataset.getName());
		mTileDataset = tileDataset;
		//mTileDataset.pyramid().
		mZoomMax = 1;
		mZoomMin = 0;
	}

	@Override
	public ITileDataSource getDataSource() {
		return new ITileDataSource() {

			@Override
			public QueryResult executeQuery(MapTile tile, ITileDataSink sink) {
				log.debug("query {}", tile);
				try {
					Tile t = mTileDataset.read(tile.zoomLevel, tile.tileX,
					                           // flip Y axis
					                           (1 << tile.zoomLevel) - 1 - tile.tileY);
					if (t == null) {
						log.debug("not found {}", tile);
						return QueryResult.TILE_NOT_FOUND;
					}
					Bitmap b = CanvasAdapter.g.decodeBitmap(new ByteArrayInputStream(t.getData()));
					sink.setTileImage(b);
					log.debug("success {}", tile);

					return QueryResult.SUCCESS;

				} catch (IOException e) {
					e.printStackTrace();
				}
				log.debug("fail {}", tile);

				return QueryResult.FAILED;
			}

			@Override
			public void destroy() {

			}
		};
	}

	int mRefs;

	@Override
	public OpenResult open() {
		mRefs++;
		return OpenResult.SUCCESS;
	}

	@Override
	public void close() {
		if (--mRefs == 0)
			mTileDataset.close();
	}

}
