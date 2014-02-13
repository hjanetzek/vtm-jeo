package org.oscim.layers;

import java.io.IOException;

import org.jeo.data.Query;
import org.jeo.data.VectorDataset;
import org.jeo.feature.Feature;
import org.jeo.geom.Geom;
import org.jeo.map.Rule;
import org.jeo.map.RuleList;
import org.jeo.map.Style;
import org.oscim.map.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

public abstract class JeoVectorLayer extends JtsLayer {

	public static final Logger log = LoggerFactory.getLogger(JeoVectorLayer.class);

	private final VectorDataset mDataset;
	private final RuleList mRules;

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
	protected void processFeatures(org.oscim.layers.JtsLayer.Task t, Envelope b) {
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

				Geometry g = f.geometry();
				if (g == null)
					continue;

				switch (Geom.Type.from(g)) {
					case POINT:
						addPoint(t, f, r, g);
						break;
					case MULTIPOINT:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addPoint(t, f, r, g.getGeometryN(i));
						break;
					case LINESTRING:
						addLine(t, f, r, g);
						break;
					case MULTILINESTRING:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addLine(t, f, r, g.getGeometryN(i));
						break;
					case POLYGON:
						addPolygon(t, f, r, g);
						break;
					case MULTIPOLYGON:
						for (int i = 0, n = g.getNumGeometries(); i < n; i++)
							addPolygon(t, f, r, g.getGeometryN(i));
						break;
					default:
						break;
				}
			}
		} catch (IOException e) {
			log.error("Error querying layer " + mDataset.getName() + e);
		}
	}

	protected abstract void addLine(Task t, Feature f, Rule rule, Geometry g);

	protected abstract void addPolygon(Task t, Feature f, Rule rule, Geometry g);

	protected abstract void addPoint(Task t, Feature f, Rule rule, Geometry g);
}
