package org.oscim.jeo.test;

import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.JeoMapLayer;
import org.oscim.layers.JeoTestData;
import org.oscim.renderer.GridRenderer;
import org.oscim.renderer.MapRenderer;

public class LayerTest extends GdxMap {

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff505050);
		//		TileSource tileSource = null;
		//		tileSource = new OSciMap4TileSource();
		//		initDefaultLayers(tileSource, false, false, false);

		mMap.getLayers().add(new JeoMapLayer(mMap,
		                                     JeoTestData.getJsonData("states.json", true),
		                                     //JeoTestData.getMemWorkspace("things"),
		                                     JeoTestData.getStyle()));

		mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LayerTest(), null, 256);
	}
}
