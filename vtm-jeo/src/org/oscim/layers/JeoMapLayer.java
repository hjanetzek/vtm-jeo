package org.oscim.layers;

import java.io.IOException;

import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.CoordinatePath;
import org.jeo.geom.Geom;
import org.jeo.map.CartoCSS;
import org.jeo.map.RGB;
import org.jeo.map.Rule;
import org.jeo.map.RuleList;
import org.jeo.map.Style;
import org.oscim.backend.canvas.Color;
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
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.utils.TileClipper;
import org.oscim.utils.async.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class JeoMapLayer extends Layer implements UpdateListener {

	public static final Logger log = LoggerFactory.getLogger(JeoMapLayer.class);

	private final VectorDataset mDataset;
	private final Worker mWorker;

	private final RuleList mRules;

	private final static double UNSCALE_COORD = 8;

	public JeoMapLayer(Map map, VectorDataset data, Style style) {
		super(map);
		mDataset = data;
		mWorker = new Worker(mMap);
		mRules = style.getRules().selectById(data.getName(), true).flatten();
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

	class Task {
		ElementLayers layers = new ElementLayers();
		MapPosition position = new MapPosition();
	}

	class Worker extends SimpleWorker<Task> {
		private final GeometryBuffer mGeom = new GeometryBuffer(128, 4);
		private double mMinX;
		private double mMinY;
		private final TileClipper mClipper = new TileClipper(-1024, -1024, 1024, 1024);

		public Worker(Map map) {
			super(map, 50, new Task(), new Task());
		}

		/* automatically in sync with worker thread */
		@Override
		public void cleanup(Task t) {
			t.layers.clear();
		}

		/* running on worker thread */
		@Override
		public boolean doWork(Task t) {
			Envelope b;
			Viewport v = mMap.getViewport();

			synchronized (v) {
				BoundingBox bbox = v.getViewBox();
				b = new Envelope(bbox.getMinLongitude(), bbox.getMaxLongitude(),
				                 bbox.getMinLatitude(), bbox.getMaxLatitude());

				v.getMapPosition(t.position);
			}

			// reduce lines points min distance
			mMinX = ((b.getMaxX() - b.getMinX()) / mMap.getWidth());
			mMinY = ((b.getMaxY() - b.getMinY()) / mMap.getHeight());
			mMinX *= 0.05;
			mMinY *= 0.05;

			try {
				Query q = new Query().bounds(b);
				log.debug("query {}", b);

				for (Feature f : mDataset.cursor(q)) {

					RuleList rs = mRules.match(f);
					if (rs.isEmpty())
						continue;

					Rule r = rs.collapse();
					if (r == null)
						continue;

					draw(t, f, r);
				}
			} catch (IOException e) {
				log.error("Error querying layer " + mDataset.getName() + e);
			}

			mMap.render();
			return true;
		}

		void draw(Task task, Feature f, Rule rule) {
			//log.debug("draw {}", f.get("STATE_NAME"));

			Geometry g = f.geometry();
			if (g == null)
				return;

			switch (Geom.Type.from(g)) {
				case POINT:
				case MULTIPOINT:
					return;
				case LINESTRING:
				case MULTILINESTRING:
					drawLine(task, f, rule, g);
					return;
				case POLYGON:
					drawPolygon(task, f, rule, g);
					return;

				case MULTIPOLYGON:
					for (int i = 0, n = g.getNumGeometries(); i < n; i++)
						drawPolygon(task, f, rule, g.getGeometryN(i));
					return;
				default:
					break;
			}
		}

		private void drawLine(Task t, Feature f, Rule rule, Geometry g) {
			LineLayer ll = t.layers.getLineLayer(0);

			if (ll.line == null) {
				RGB color = rule.color(f, CartoCSS.LINE_COLOR, RGB.black);
				float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
				ll.line = new Line(0, color(color), width);
				ll.width = width;
			}

			mGeom.clear();
			mGeom.startLine();

			CoordinatePath p = CoordinatePath.create(g);
			transformPath(t.position, mGeom, p);

			//log.debug("add line " + mGeom.pointPos);

			ll.addLine(mGeom);
		}

		private void drawPolygon(Task t, Feature f, Rule rule, Geometry g) {
			LineLayer l2 = t.layers.getLineLayer(2);
			if (l2.line == null) {
				l2.line = new Line(2, Color.BLUE, 1);
				l2.width = 1.5f;
			}
			LineLayer ll = t.layers.getLineLayer(3);

			if (ll.line == null) {
				//RGB color = rule.color(f, CartoCSS.POLYGON_FILL, RGB.red);
				//float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
				//ll.line = new Line(2, color(color), width);
				ll.line = new Line(2, Color.RED, 1);
				ll.width = 1.5f;
			}

			MeshLayer mesh = t.layers.getMeshLayer(1);
			if (mesh.area == null) {
				mesh.area = new Area(Color.fade(Color.DKGRAY, 0.3));
			}

			mGeom.clear();
			mGeom.startPolygon();

			CoordinatePath p = CoordinatePath.create(g).generalize(mMinX, mMinY);
			if (transformPath(t.position, mGeom, p) < 3)
				return;

			if (!mClipper.clip(mGeom))
				return;

			//log.debug("add poly " + mGeom.pointPos);
			l2.addLine(mGeom);
			//simp.simplify(mGeom, mTolerance * t.position.zoomLevel);
			//simp.simplify(mGeom, mTolerance);
			ll.addLine(mGeom);

			mesh.addMesh(mGeom);
		}

		//private final SimplifyVW simp = new SimplifyVW();

		public int color(RGB rgb) {
			return rgb.getAlpha() << 24
			        | rgb.getRed() << 16
			        | rgb.getGreen() << 8
			        | rgb.getBlue();
		}

		private int transformPath(MapPosition pos, GeometryBuffer g, CoordinatePath path) {

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
			layers.baseLayers = t.layers.baseLayers;

			compile();
			//log.debug("is ready " + isReady() + " " + layers.getSize());
		}
	}

	float mTolerance = 4;

	public void changeTolerance(boolean b) {
		mTolerance += b ? 0.1 : -0.1;
		log.debug("" + mTolerance);
	}
}
