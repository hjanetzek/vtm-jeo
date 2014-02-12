package org.oscim.jeo.android;

import org.oscim.android.MapActivity;
import org.oscim.android.MapView;
import org.oscim.test.JeoTest;

import android.os.Bundle;
import android.os.Environment;

public class TestActivity extends MapActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(new MapView(this));
		String file = Environment.getExternalStorageDirectory().getAbsolutePath();

		JeoTest.indoorSketch(mMap, file + "/osmindoor.json");
		mMap.setMapPosition(49.417, 8.673, 1 << 17);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
}
