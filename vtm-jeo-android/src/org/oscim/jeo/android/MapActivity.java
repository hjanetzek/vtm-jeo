package org.oscim.jeo.android;

import org.oscim.android.MapView;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.JeoMapLayer;
import org.oscim.layers.JeoTestData;
import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.renderer.GridRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.bitmap.DefaultSources.StamenToner;

import android.os.Bundle;
import android.os.Environment;

public class MapActivity extends org.oscim.android.MapActivity {

	private MapView mMapView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mMapView = new MapView(this);
		registerMapView(mMapView);
		setContentView(mMapView);

		MapRenderer.setBackgroundColor(0xff888888);

		mMap.setBackgroundMap(new BitmapTileLayer(mMap, new StamenToner()));

		String file = Environment.getExternalStorageDirectory() + "/states.json";

		mMap.getLayers().add(new JeoMapLayer(mMap,
		                                     //JeoTestData.getMemWorkspace("things"),
		                                     JeoTestData.getJsonData(file, true),
		                                     JeoTestData.getStyle()));

		mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer()));

		mMap.setMapPosition(20, -90, 1 << 3);
	}
}
