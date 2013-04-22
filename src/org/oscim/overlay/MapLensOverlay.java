/*
 * Copyright 2013 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.oscim.overlay;

import org.oscim.core.GeoPoint;
import org.oscim.renderer.overlays.CircleOverlay;
import org.oscim.renderer.overlays.TileOverlay;
import org.oscim.view.MapView;

import android.util.Log;
import android.view.MotionEvent;

public class MapLensOverlay extends Overlay {
	private final TileOverlay mTileLayer;

	//private final TileOverlay mTilerLayer2;
float lensZoomLevel;

 float  lastX1;
 float  lastY1;
 float lastX2;
 float lastY2;
	public float getLastX1() {
	return lastX1;
}

public void setLastX1(float lastX1) {
	this.lastX1 = lastX1;
}

public float getLastY1() {
	return lastY1;
}

public void setLastY1(float lastY1) {
	this.lastY1 = lastY1;
}

public float getLastX2() {
	return lastX2;
}

public void setLastX2(float lastX2) {
	this.lastX2 = lastX2;
}

public float getLastY2() {
	return lastY2;
}

public void setLastY2(float lastY2) {
	this.lastY2 = lastY2;
}

	public float getLensZoomLevel() {
	return mTileLayer.getmOverlayScale();
}

public void setLensZoomLevel(float lensZoomLevel) {
	mTileLayer.setmOverlayScale( lensZoomLevel);
}

	int fingerIndex ;
public static   GeoPoint start ;
public static   GeoPoint end ;
	public MapLensOverlay(MapView mapView , int finger ) {
		super(mapView);

		fingerIndex = finger ;
		mLayer = mTileLayer = new TileOverlay(mapView);
		//mTilerLayer2 = new TileOverlay ( mapView);

		if(finger==0)
		mMapView.getOverlays().add(2,ov1);
		if(finger==1)
		mMapView.getOverlays().add(3,ov2);
	}

	@Override
	public boolean onLongPress(MotionEvent e) {
		// TODO Auto-generated method stub
		return super.onLongPress(e);
	}


	boolean action1;
	boolean action2;
	boolean removed;
	@Override
	public boolean onTouchEvent(MotionEvent e) {
		int action = e.getAction() & MotionEvent.ACTION_MASK;



		if(action == MotionEvent.ACTION_UP){

			action1=false;
			action2=false;
			mMapView.getOverlays().remove(ov1);

  mMapView.getOverlays().remove(ov2);


		}
		//if (action == MotionEvent.ACTION_MOVE){
		//		if (action == MotionEvent.ACTION_DOWN){
		//			mTileLayer.setPointer(e.getX(), e.getY());
		//			mMapView.render();
		//		} else
		//if (action == MotionEvent.ACTION_MOVE && e.getPointerCount() >=2) {


		//TileRenderer.stopZoom = true;
	//if ( e.getPointerCount()>fingerIndex)

		//	mMapView.setClickable(false);
//if ( e.getPointerCount()  == 1  && !removed  ){
	//mMapView.getOverlays().remove(2);
//removed =true;
//}

//if (  e.ACTION_CANCEL == MotionEvent.ACTION_POINTER_1_UP)
	//mMapView.getOverlays().remove(1);



		//	if ( action == MotionEvent.ACTION_POINTER_UP){
				//Log.v("move", "finger2");
			//	e.getActionIndex();
		int index = (e.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
	    int pointId = e.getPointerId(index);


				if (pointId ==1 ){
					action1=true;
					Log.v("move", String.valueOf(pointId));

				}
				if (action == MotionEvent.ACTION_POINTER_1_DOWN )
					action1=false;
		//	}

				if(e.getPointerCount() ==2 ){


				action1=false;

				}


				if (e.getPointerCount() == 1  && action1)
				{
					lastX2= e.getX(0);
					lastY2=e.getY(0);

					ov2.setLat(e.getX(0));
					ov2.setLog(e.getY(0));
					//ov1.setLat(preX1);
					//ov1.setLog(preY1);
				}

				else


				{
					lastX1= e.getX(0);
					lastY1=e.getY(0);
					ov1.setLat(e.getX(0));
					ov1.setLog(e.getY(0));


				}
//if(action1){

	//ov1.setLat(e.getX(0));
	//ov1.setLog(e.getY(0));
//	ov2.setLat(preX2);
	//ov2.setLog(preY2);
//}
//else
	//if (action2){
	//	ov2.setLat(e.getX(0));
		//ov2.setLog(e.getY(0));
		//ov1.setLat(preX1);
		//ov1.setLog(preY1);
	//}

		if(e.getPointerCount() == 2){


		mTileLayer.setPointer(e.getX(fingerIndex) , e.getY(fingerIndex) );


	//	GeoPoint pTemp = mMapView.getMapViewPosition().fromScreenPixels(e.getX(fingerIndex), e.getY(fingerIndex));


		if(fingerIndex ==0){

			preX1=e.getX(fingerIndex);
			preY1= e.getY(fingerIndex);
			ov1.setLat(e.getX(fingerIndex));
			ov1.setLog(e.getY(fingerIndex));
			lastX1= e.getX(fingerIndex);
			lastY1=e.getY(fingerIndex);

			start = mMapView.getMapViewPosition().fromScreenPixels(e.getX(0), e.getY(0));
		}


		if(fingerIndex ==1){

			preX2=e.getX(fingerIndex);
			preY2= e.getY(fingerIndex);
			ov2.setLat(e.getX(fingerIndex));
			ov2.setLog(e.getY(fingerIndex));

			lastX2= e.getX(fingerIndex);
			lastY2=e.getY(fingerIndex);

			end = mMapView.getMapViewPosition().fromScreenPixels(e.getX(1), e.getY(1));
		}
		}


	//	 mMapView.getOverlays().remove(0);
			//if(e.)
	///	mTilerLayer2.setPointer(e.getX(1) , e.getY(1));
			mMapView.render();


		//}

		//else
			//TileRenderer.stopZoom = false;

		//mMap.getOverlayManager().add(0,new EventsOverlay(mMap));

		return false;
	}

	float preX1,preX2, preY1,preY2;
	CircleOverlay ov1 = new CircleOverlay (mMapView,50);
	CircleOverlay ov2 = new CircleOverlay (mMapView,50);
}
