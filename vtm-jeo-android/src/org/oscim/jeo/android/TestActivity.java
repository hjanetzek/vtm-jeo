package org.oscim.jeo.android;

import java.io.File;
import java.io.IOException;

import org.jeo.android.geopkg.GeoPackage;
import org.jeo.android.geopkg.GeoPkgWorkspace;
import org.jeo.data.Dataset;
import org.jeo.data.Handle;
import org.jeo.data.TileDataset;
import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.layers.JeoTileSource;
import org.oscim.layers.TileGridLayer;
import org.oscim.layers.tile.BitmapTileLayer;
import org.oscim.map.Layers;
import org.oscim.renderer.MapRenderer;
import org.oscim.tiling.source.TileSource;

import android.os.Bundle;
import android.os.Environment;

public class TestActivity extends MapActivity {
	GeoPkgWorkspace geopkg;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		MapView mapView = new MapView(this);
		registerMapView(mapView);
		setContentView(mapView);

		MapRenderer.setBackgroundColor(0xff888888);
		//MapRenderer.setBackgroundColor(0xffffffff);

		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, new StamenToner()));

		String file = Environment.getExternalStorageDirectory().getAbsolutePath();
		Layers layers = mMap.getLayers();
		//layers.add(new JeoMapLayer(mMap,
		//                           //JeoTestData.getMemWorkspace("things"),
		//                           (VectorDataset) JeoTestData.getJsonData(file + "/states.json",
		//                                                                   true),
		//                           JeoTestData.getStyle()));

		geopkg = GeoPackage.open(new File(file, "ne.gpkg"));
		//geopkg = GeoPackage.open(new File(file, "sample.geopackage"));

		for (Handle<Dataset> d : geopkg.list()) {
			//System.out.println(d.getType().toString() + " "+ d.toString());
			//if (TileDataset.class.isAssignableFrom(d.getType())) {
			TileSource ts;
			try {
				ts = new JeoTileSource((TileDataset) d.resolve());
				layers.add(new BitmapTileLayer(mMap, ts));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//}
		}
		mMap.getLayers().add(new TileGridLayer(mMap));

		mMap.setMapPosition(20, -90, 1 << 3);
	}

	@Override
	protected void onStop() {
		super.onStop();
		geopkg.close();
	}
}
