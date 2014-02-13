package org.oscim.jeo.test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import org.oscim.gdx.GdxMap;
import org.oscim.layers.JeoVectorLayer;
import org.oscim.test.JeoTest;

public class LayerTest extends GdxMap {

	JeoVectorLayer mLayer;

	static String HOST = "http://overpass-api.de/api/interpreter?data=";
	static String QUERY = "[out:json][timeout:25];"
	        + "(relation[type=level]"
	        + "(53.57001404104646,9.859843254089355,53.57146983805207,9.864558577537537)"
	        + ";);out body;>;out skel qt;";

	@Override
	public void createLayers() {
		JeoTest.indoorSketch(mMap, "osmindoor.json");
		mMap.setMapPosition(49.417, 8.673, 1 << 17);
	}

	public static void main(String[] args) {

		try {
			String query = URLEncoder.encode(QUERY, "utf-8");
			URL url = new URL(HOST + query);
			System.out.println(url.toString());

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();

		}
		//GdxMapApp.init();
		//GdxMapApp.run(new LayerTest(), null, 256);
	}
}
