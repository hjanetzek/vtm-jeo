package org.oscim.layers;

import java.io.IOException;
import java.util.HashMap;

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
import org.oscim.core.GeometryBuffer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.utils.geom.TileClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class JeoVectorLayer extends JtsLayer implements UpdateListener {

	public static final Logger log = LoggerFactory.getLogger(JeoVectorLayer.class);

	private final VectorDataset mDataset;
	private final RuleList mRules;
	private final GeometryBuffer mGeom = new GeometryBuffer(128, 4);
	private final TileClipper mClipper = new TileClipper(-1024, -1024, 1024, 1024);

	private double mMinX;
	private double mMinY;
	protected double mDropPointDistance = 0;

	public JeoVectorLayer(Map map, VectorDataset data, Style style) {
		super(map);
		mDataset = data;

		//mRules = style.getRules().selectById(data.getName(), true).flatten();
		mRules = style.getRules().selectById("way", true).flatten();
		log.debug(mRules.toString());

		mRenderer = new Renderer();
	}

	@Override
	protected void drawFeatures(org.oscim.layers.JtsLayer.Task t, Envelope b) {
		if (mDropPointDistance > 0) {
			/* reduce lines points min distance */
			mMinX = ((b.getMaxX() - b.getMinX()) / mMap.getWidth());
			mMinY = ((b.getMaxY() - b.getMinY()) / mMap.getHeight());
			mMinX *= mDropPointDistance;
			mMinY *= mDropPointDistance;
		}

		try {
			Query q = new Query().bounds(b);
			log.debug("query {}", b);

			for (Feature f : mDataset.cursor(q)) {
				//log.debug("feature {}", f);
				RuleList rs = mRules.match(f);
				if (rs.isEmpty())
					continue;

				Rule r = rs.collapse();
				if (r == null)
					continue;

				//log.debug("draw feature");
				draw(t, f, r);
			}
		} catch (IOException e) {
			log.error("Error querying layer " + mDataset.getName() + e);
		}
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
				if (((LineString) g).isClosed()) {
					drawPolygon(task, f, rule, g);
					break;
				}
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
		ll.setDropDistance(0);
		ll.heightOffset = -4.5f;
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

		int level = 0;

		/* not sure if one could match these geojson properties with cartocss */
		Object o = f.get("@relations");
		if (o instanceof HashMap) {
			@SuppressWarnings("unchecked")
			HashMap<String, Object> tags = (HashMap<String, Object>) o;
			@SuppressWarnings("unchecked")
			HashMap<String, Object> reltags = (HashMap<String, Object>) tags.get("reltags");

			if (reltags != null) {
				o = reltags.get("level");
				if (o instanceof String) {
					//log.debug("got level {}", o);
					level = Integer.parseInt((String) o);
				}
			}
		}

		LineLayer ll = t.layers.getLineLayer(level * 2 + 1);

		if (ll.line == null) {
			float width = rule.number(f, CartoCSS.LINE_WIDTH, 1.2f);
			ll.line = new Line(0, Color.rainbow((level + 1) / 10f), width);
			ll.width = width;
			ll.heightOffset = level * 4;
		}

		MeshLayer mesh = t.layers.getMeshLayer(level * 2);
		if (mesh.area == null) {
			RGB color = rule.color(f, CartoCSS.POLYGON_FILL, RGB.red);
			mesh.area = new Area(color(color));
			//mesh.area = new Area(Color.fade(Color.DKGRAY, 0.1f));
			mesh.heightOffset = level * 4f;
		}

		mGeom.clear();
		mGeom.startPolygon();

		CoordinatePath p = CoordinatePath.create(g);
		if (mMinX > 0 || mMinY > 0)
			p.generalize(mMinX, mMinY);

		if (transformPath(t.position, mGeom, p) < 3)
			return;

		if (!mClipper.clip(mGeom))
			return;

		ll.addLine(mGeom);
		mesh.addMesh(mGeom);
	}

	public static int color(RGB rgb) {
		return rgb.getAlpha() << 24
		        | rgb.getRed() << 16
		        | rgb.getGreen() << 8
		        | rgb.getBlue();
	}
}
