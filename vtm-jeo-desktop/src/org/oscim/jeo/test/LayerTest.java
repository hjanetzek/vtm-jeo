package org.oscim.jeo.test;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.JeoVectorLayer;
import org.oscim.test.JeoTest;

public class LayerTest extends GdxMap {

	JeoVectorLayer mLayer;

	@Override
	public void createLayers() {
		JeoTest.indoorSketch(mMap, "osmindoor.json");
		mMap.setMapPosition(49.417, 8.673, 1 << 17);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LayerTest(), null, 256);
	}
}
