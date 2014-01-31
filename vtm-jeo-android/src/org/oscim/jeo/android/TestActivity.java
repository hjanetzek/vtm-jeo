package org.oscim.jeo.android;

import org.jeo.android.geopkg.GeoPkgWorkspace;
import org.jeo.data.VectorDataset;
import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint.Cap;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.JeoTestData;
import org.oscim.layers.JeoVectorLayer;
import org.oscim.map.Layers;
import org.oscim.renderer.GridRenderer;
import org.oscim.renderer.MapRenderer;
import org.oscim.theme.styles.Line;

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

		MapRenderer.setBackgroundColor(0xff777777);

		mMap.getLayers().add(new GenericLayer(mMap, new GridRenderer(8, new Line(Color.GRAY,
		                                                                         1.8f,
		                                                                         Cap.BUTT), null)));

		//mMap.setBackgroundMap(new BitmapTileLayer(mMap, new StamenToner()));

		String file = Environment.getExternalStorageDirectory().getAbsolutePath();
		Layers layers = mMap.getLayers();
		layers.add(new JeoVectorLayer(mMap,
		                              //JeoTestData.getMemWorkspace("things"),
		                              (VectorDataset) JeoTestData.getJsonData(file + "/states.json",
		                                                                      true),
		                              JeoTestData.getStyle()));

		//		geopkg = GeoPackage.open(new File(file, "ne.gpkg"));
		//		//geopkg = GeoPackage.open(new File(file, "sample.geopackage"));
		//
		//		for (Handle<Dataset> d : geopkg.list()) {
		//			//System.out.println(d.getType().toString() + " "+ d.toString());
		//			//if (TileDataset.class.isAssignableFrom(d.getType())) {
		//			TileSource ts;
		//			try {
		//				ts = new JeoTileSource((TileDataset) d.resolve());
		//				layers.add(new BitmapTileLayer(mMap, ts));
		//
		//			} catch (IOException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//			//}
		//		}

		mMap.setMapPosition(20, -90, 1 << 3);
	}

	@Override
	protected void onStop() {
		super.onStop();
		//geopkg.close();
	}
}
