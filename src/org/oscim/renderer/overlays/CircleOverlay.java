/*
 * Copyright 2013 Hannes Janetzek.org
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
package org.oscim.renderer.overlays;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import org.oscim.core.MapPosition;
import org.oscim.core.PointD;
import org.oscim.overlay.Overlay;
import org.oscim.renderer.GLRenderer.Matrices;
import org.oscim.renderer.GLState;
import org.oscim.utils.GlUtils;
import org.oscim.utils.Matrix4;
import org.oscim.view.MapView;

import android.opengl.GLES20;

/*
 * This is an example how to integrate custom OpenGL drawing routines as map overlay
 *
 * based on chapter 2 from:
 * https://github.com/dalinaum/opengl-es-book-samples/tree/master/Android
 * */
public class CircleOverlay  extends Overlay{


	  float raduis ;
	  private float lat ; // storing the information related to the circle pos
	  private float log ;
public float getRaduis() {
		return raduis;
	}



	public void setRaduis(float raduis) {
		this.raduis = raduis;
	}



public class CustomOverlay extends RenderOverlay {

	private int mProgramObject;
	private int hVertexPosition;
	private int hMatrixPosition;
float width ;
	int hcolor;
	FloatBuffer colorb;
	float [] colordata ;


	private FloatBuffer mVertices;
	private final  float[] mVerticesData;
	private boolean mInitialized;
private final MapView mMapView ;
	private final float timerTick;
private final  int scale ;
private int mScreenWidth;
private int mScreenHeight;
private float mOverlayOffsetX;
private float mOverlayOffsetY;

private final float mOverlayScale = 1.8f;
private final PointD mScreenPoint = new PointD();
private final Matrix4 mProjMatrix = new Matrix4();
private final Matrix4 mViewProjMatrix = new Matrix4();

	public CustomOverlay(MapView mapView , float  raduis) {
		super(mapView);
mMapView= mapView;
	mVerticesData= new float [] {
				-raduis, -raduis, -1, -1,
				-raduis, raduis, -1, 1,
				raduis, -raduis, 1, -1,
				raduis, raduis,  1, 1

	};


	mScreenWidth = mapView.getWidth();
	mScreenHeight = mapView.getHeight();
 // testing colors on different themes  use U_color insisted
	if(mapView.getRenderTheme().equals("DEFAULT")  )
		colordata = new float [] {1f,1f,1f,1f};
	else

	colordata = new float [] {0.0f,0.0f,0.0f,0.0f};
	//Toast.makeText(mapView.getContext(), mapView.getRenderTheme(), Toast.LENGTH_LONG).show();

	timerTick= System.currentTimeMillis()+10;
	scale=1;
	}
	// ---------- everything below runs in GLRender Thread ----------
	@Override
	public void update(MapPosition curPos, boolean positionChanged, boolean tilesChanged, Matrices matrices) {
		if (!mInitialized) {
			if (!init())
				return;

			mInitialized = true;

			// tell GLRender to call 'compile' when data has changed
			newData = true;

			// fix current MapPosition




			mProjMatrix.copy(matrices.proj);
			// discard z projection from tilt
			mProjMatrix.setValue(10, 0);
			mProjMatrix.setValue(14, 0);
		//	mMapPosition.setFromLatLon(lat, log, mMapPosition.zoomLevel);
			 mMapPosition.copy(curPos);
		}

	// curPos.setPosition(lat, log);


//this.render(curPos, matrices);


	}


	@Override
	public void compile() {
		// modify mVerticesData and put in FloatBuffer

		mVertices.clear();
		mVertices.put(mVerticesData);
		mVertices.flip();

		newData = false;

		// tell GLRender to call 'render'
		isReady = true;
	}

	private final Matrix4 tmpMatrix = new Matrix4();

	@Override
	public void render(MapPosition pos, Matrices m) {

		// Use the program object
		GLES20.glDepthMask(true);
		// set depth buffer to min depth -1
		GLES20.glClearDepthf(-1);
		GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);
		// back to usual
		GLES20.glClearDepthf(1);

		GLES20.glDepthFunc(GLES20.GL_ALWAYS);

		GLState.useProgram(mProgramObject);

		// set depth offset of overlay circle to be greater
		// than those used for tiles (so that their gl_less test
		// evaluates to true inside the circle)
		GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
		GLES20.glPolygonOffset(1, 100);

		GLState.blend(true);
		GLState.test(true, false);

		// unbind previously bound VBOs
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		// Load the vertex data
		GLES20.glVertexAttribPointer(hVertexPosition, 4,
				GLES20.GL_FLOAT, false, 0, mVertices);

		GLState.enableVertexArrays(hVertexPosition, -1);

		mScreenWidth = mMapView.getWidth();
		mScreenHeight = mMapView.getHeight();

		mOverlayOffsetX = lat;
		mOverlayOffsetY = log;

		m.mvp.setTranslation(mOverlayOffsetX - mScreenWidth / 2,
				mOverlayOffsetY - mScreenHeight / 2, 0);

		float ratio = (1f / mScreenWidth) *.7f;
		tmpMatrix.setScale(ratio, ratio, ratio);

		m.mvp.multiplyMM(tmpMatrix, m.mvp);

		m.mvp.multiplyMM(m.proj, m.mvp);
		m.mvp.setAsUniform(hMatrixPosition);

		// Draw the circle
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		// get current tiles
		//mTileSet = GLRenderer.getVisibleTiles(mTileSet);

		//mDrawCnt = 0;
		GLES20.glDepthFunc(GLES20.GL_LESS);

		// scale position passed to Line/PolyRenderer
		// FIXME scale is used for both alpha fading
		// and line width...
		MapPosition scaledPos = mMapPosition;
		scaledPos.copy(pos);

		//FIXME scaledPos.scale *= mOverlayScale;

		// get translation vector from map at screen point to center
		// relative to current scale
		mMapView.getMapViewPosition().getScreenPointOnMap(
				mOverlayOffsetX, mOverlayOffsetY,
				mMapPosition.scale, mScreenPoint);

		mViewProjMatrix.setTranslation(
				-(float)mScreenPoint.x,
				-(float)mScreenPoint.y, 0);

		// rotate around center
		tmpMatrix.setRotation(pos.angle, 0, 0, 1);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		// translate to overlay circle in screen coordinates
		tmpMatrix.setTransScale(
				(mOverlayOffsetX - mScreenWidth / 2),
				(mOverlayOffsetY - mScreenHeight / 2),
				mOverlayScale);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		// normalize coordinates, i guess thats how it's called
		tmpMatrix.setScale(ratio, ratio, ratio);
		mViewProjMatrix.multiplyMM(tmpMatrix, mViewProjMatrix);

		mViewProjMatrix.multiplyMM(mProjMatrix, mViewProjMatrix);
		GLES20.glUniform1f(hcolor, colordata[0]);
		// Draw the triangle
	//	GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);



	/*	mVerticesData= new float [] {
				-raduis+animation, -raduis+animation, -1, -1,
				-raduis+animation, raduis-animation, -1, 1,
				raduis-animation, -raduis+animation, 1, -1,
				raduis-animation, raduis-animation,  1, 1

	};  */


		//mMapView.render();

		GlUtils.checkGlError("...");
	}


	int animation  =  0;
	private boolean init() {
		// Load the vertex/fragment shaders
		int programObject = GlUtils.createProgram(vShaderStr, fShaderStr);

		if (programObject == 0)
			return false;

		// (1 << pos.zoomLevel) * pos.scale
		// Tile.TILE_SIZE

		//  earthRadius / 1 << 0 * 1.0 * Tile.TILE_SIZE


		// Handle for vertex position in shader
		hVertexPosition = GLES20.glGetAttribLocation(programObject, "a_pos");
	//	GLES20.glBindAttribLocation(programObject, 1, "u_color");
		hMatrixPosition = GLES20.glGetUniformLocation(programObject, "u_mvp");


		hcolor = GLES20.glGetUniformLocation(programObject, "radius");



		// Store the program object
		mProgramObject = programObject;
	//	GLES20.glLinkProgram(mProgramObject);


		mVertices = ByteBuffer.allocateDirect(mVerticesData.length * 4)
				.order(ByteOrder.nativeOrder()).asFloatBuffer();

		//this.colorb = ByteBuffer.allocateDirect(colordata.length * 4)
			//	.order(ByteOrder.nativeOrder()).asFloatBuffer();

		//colorb.clear();
		//colorb.put(1).position(0);
	//colorb.flip();


		return true;
	}
	private final static String vShaderStr =
			"precision mediump float;"
					+ "uniform mat4 u_mvp;"
				//	+" attribute vec4 u_color;"
					+ "uniform float radius;"
//					+ "uniform float blur;"
//					+ "uniform vec2 center;"
					+ "attribute vec4 a_pos;"
					+ "varying vec2 tex;"
					+" varying vec4 v_color;   "
					+ "void main()"
					+ "{"
					+ "   gl_Position = u_mvp * vec4(a_pos.xy, 0.0, 1.0);"
					+ "   tex = a_pos.zw;"
                    + "        if (radius > .5) "
					+"        v_color = vec4 ( 0,0,1,0);     "
                    + "                  else "
					+"      v_color = vec4 ( 0,1.0,0,0);      "
					+ "}";

	private final static String fShaderStr =
			"precision mediump float;"
					+ "varying vec2 tex;"
                    + "varying vec4 v_color;"

					+ "void main()"
					+ "{"
					//+ "   float a = 1.0 - step(1.0, length(tex));"
					+ "   float a =.5- smoothstep(.2, 1.0, length(tex));"

					+ "  gl_FragColor = vec4(v_color.x * a , v_color.y * a,v_color.z *a,a);"
					+ "}";
}



public CircleOverlay(MapView mapView , float raduis) {
	super(mapView);

	mLayer = new CustomOverlay(mapView , raduis);

}


public float getLat() {
	return lat;
}
public void setLat(float lat) {
	this.lat = lat;
}
public float getLog() {
	return log;
}
public void setLog(float log) {
	this.log = log;
}
}