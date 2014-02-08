package org.oscim.layers;

import org.jeo.geom.CoordinatePath;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeometryBuffer;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.Viewport;
import org.oscim.renderer.ElementRenderer;
import org.oscim.renderer.MapRenderer.Matrices;
import org.oscim.renderer.elements.ElementLayers;
import org.oscim.utils.async.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public abstract class JtsLayer extends Layer implements UpdateListener {
	public static final Logger log = LoggerFactory.getLogger(JeoVectorLayer.class);

	private final Worker mWorker;

	private final static double UNSCALE_COORD = 8;

	public JtsLayer(Map map) {
		super(map);
		mWorker = new Worker(mMap);
		mRenderer = new Renderer();
	}

	@Override
	public void onDetach() {
		super.onDetach();

		mWorker.cancel(true);
	}

	@Override
	public void onMapUpdate(MapPosition pos, boolean changed, boolean clear) {
		if (changed || clear) {
			mWorker.submit(10);
		}
	}

	abstract protected void drawFeatures(Task t, Envelope b);

	protected int transformPath(MapPosition pos, GeometryBuffer g, CoordinatePath path) {

		double scale = pos.scale * Tile.SIZE / UNSCALE_COORD;
		int cnt = 0;
		O: while (path.hasNext()) {
			Coordinate c = path.next();
			float x = (float) ((MercatorProjection.longitudeToX(c.x) - pos.x) * scale);
			float y = (float) ((MercatorProjection.latitudeToY(c.y) - pos.y) * scale);

			switch (path.getStep()) {
				case MOVE_TO:
					if (g.isPoly())
						g.startPolygon();
					else if (g.isLine())
						g.startLine();

					cnt++;
					g.addPoint(x, y);
					break;
				case LINE_TO:
					cnt++;
					g.addPoint(x, y);
					break;
				case CLOSE:
					//g.addPoint(x, y);
					//if (g.type == GeometryType.POLY)
					break;
				case STOP:
					break O;
			}
		}
		return cnt;
	}

	class Task {
		ElementLayers layers = new ElementLayers();
		MapPosition position = new MapPosition();
	}

	class Worker extends SimpleWorker<Task> {

		public Worker(Map map) {
			super(map, 50, new Task(), new Task());
		}

		/** automatically in sync with worker thread */
		@Override
		public void cleanup(Task t) {
			t.layers.clear();
		}

		/** running on worker thread */
		@Override
		public boolean doWork(Task t) {
			Envelope b;
			Viewport v = mMap.viewport();

			synchronized (v) {
				BoundingBox bbox = v.getBBox();
				b = new Envelope(bbox.getMinLongitude(), bbox.getMaxLongitude(),
				                 bbox.getMinLatitude(), bbox.getMaxLatitude());

				v.getMapPosition(t.position);
			}

			drawFeatures(t, b);

			mMap.render();
			return true;
		}

	}

	class Renderer extends ElementRenderer {
		MapPosition mTmpPos = new MapPosition();

		@Override
		protected void update(MapPosition position, boolean changed,
		        Matrices matrices) {

			Task t = mWorker.poll();

			if (t == null)
				return;

			mMapPosition.copy(t.position);
			mMapPosition.setScale(mMapPosition.scale / UNSCALE_COORD);
			layers.setBaseLayers(t.layers.getBaseLayers());

			compile();
			//log.debug("is ready " + isReady() + " " + layers.getSize());
		}
	}
}
