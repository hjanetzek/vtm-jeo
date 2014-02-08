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
import org.oscim.core.GeometryBuffer;
import org.oscim.map.Map;
import org.oscim.map.Map.UpdateListener;
import org.oscim.renderer.elements.LineLayer;
import org.oscim.renderer.elements.MeshLayer;
import org.oscim.theme.styles.Area;
import org.oscim.theme.styles.Line;
import org.oscim.utils.TileClipper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public class JeoVectorLayer extends JtsLayer implements UpdateListener {

	public static final Logger log = LoggerFactory.getLogger(JeoVectorLayer.class);

	private final VectorDataset mDataset;
	private final RuleList mRules;
	private final GeometryBuffer mGeom = new GeometryBuffer(128, 4);
	private final TileClipper mClipper = new TileClipper(-1024, -1024, 1024, 1024);

	private double mMinX;
	private double mMinY;

	public JeoVectorLayer(Map map, VectorDataset data, Style style) {
		super(map);
		mDataset = data;
		mRules = style.getRules().selectById(data.getName(), true).flatten();
		mRenderer = new Renderer();
	}

	//private final SimplifyVW simp = new SimplifyVW();

	@Override
	protected void drawFeatures(org.oscim.layers.JtsLayer.Task t, Envelope b) {
		// reduce lines points min distance
		mMinX = ((b.getMaxX() - b.getMinX()) / mMap.getWidth());
		mMinY = ((b.getMaxY() - b.getMinY()) / mMap.getHeight());
		mMinX *= 0.01;
		mMinY *= 0.01;

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
			mesh.area = new Area(Color.fade(Color.DKGRAY, 0.3f));
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

	float mTolerance = 4;

	public void changeTolerance(boolean b) {
		mTolerance += b ? 0.1 : -0.1;
		log.debug("" + mTolerance);
	}

}
