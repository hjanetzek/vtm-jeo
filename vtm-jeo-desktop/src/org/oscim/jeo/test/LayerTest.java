package org.oscim.jeo.test;

import org.jeo.data.VectorDataset;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GdxMapApp;
import org.oscim.layers.JeoVectorLayer;
import org.oscim.layers.JeoTestData;
import org.oscim.layers.TileGridLayer;
import org.oscim.renderer.MapRenderer;

import com.badlogic.gdx.Input;

public class LayerTest extends GdxMap {

	JeoVectorLayer mLayer;

	@Override
	public void createLayers() {
		MapRenderer.setBackgroundColor(0xff505050);
		//mMap.getLayers().add(new BitmapTileLayer(mMap, new DefaultSources.ImagicoLandcover()));

		mLayer = new JeoVectorLayer(mMap,
		                         (VectorDataset) JeoTestData.getJsonData("states.json",
		                                                                 true),
		                         //JeoTestData.getMemWorkspace("things"),
		                         JeoTestData.getStyle());

		mMap.layers().add(mLayer);
		mMap.layers().add(new TileGridLayer(mMap));
	}

	@Override
	protected boolean onKeyDown(int keycode) {
		if (Input.Keys.NUM_1 == keycode) {
			mLayer.changeTolerance(true);
			mMap.clearMap();
			return true;

		}
		else if (Input.Keys.NUM_2 == keycode) {
			mLayer.changeTolerance(false);
			mMap.clearMap();
			return true;
		}

		return super.onKeyDown(keycode);
	}

	public static void main(String[] args) {
		GdxMapApp.init();
		GdxMapApp.run(new LayerTest(), null, 256);
	}
}
