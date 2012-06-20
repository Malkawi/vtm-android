/*
 * Copyright 2012 Hannes Janetzek
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General License for more details.
 *
 * You should have received a copy of the GNU Lesser General License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.mapsforge.android.glrenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import android.util.SparseArray;

class LineLayers {
	private static int NUM_VERTEX_FLOATS = 4;

	private SparseArray<LineLayer> layers;

	LineLayer[] array = null;
	int size = 0;

	LineLayers() {
		layers = new SparseArray<LineLayer>(10);
	}

	LineLayer getLayer(int layer, int color, boolean outline, boolean fixed) {
		LineLayer l = layers.get(layer);
		if (l != null) {
			return l;
		}

		l = new LineLayer(layer, color, outline, fixed);
		layers.put(layer, l);

		return l;
	}

	FloatBuffer compileLayerData(FloatBuffer buf) {
		FloatBuffer fbuf = buf;

		array = new LineLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			LineLayer l = layers.valueAt(i);
			array[i] = l;
			size += l.verticesCnt * NUM_VERTEX_FLOATS;
		}

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 4).order(
					ByteOrder.nativeOrder());
			fbuf = bbuf.asFloatBuffer();
		} else {
			fbuf.position(0);
		}
		int pos = 0;

		for (int i = 0, n = array.length; i < n; i++) {
			LineLayer l = array[i];
			if (l.isOutline)
				continue;

			for (PoolItem item : l.pool) {
				fbuf.put(item.vertices, 0, item.used);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		fbuf.position(0);

		// not needed for drawing
		layers = null;

		return fbuf;
	}

	ShortBuffer compileLayerData(ShortBuffer buf) {
		ShortBuffer sbuf = buf;

		array = new LineLayer[layers.size()];

		for (int i = 0, n = layers.size(); i < n; i++) {
			LineLayer l = layers.valueAt(i);
			array[i] = l;
			size += l.verticesCnt * NUM_VERTEX_FLOATS;
		}

		if (buf == null || buf.capacity() < size) {
			ByteBuffer bbuf = ByteBuffer.allocateDirect(size * 2).order(
					ByteOrder.nativeOrder());
			sbuf = bbuf.asShortBuffer();
		} else {
			sbuf.position(0);
		}
		int pos = 0;

		short[] data = new short[PoolItem.SIZE];

		for (int i = 0, n = array.length; i < n; i++) {
			LineLayer l = array[i];
			if (l.isOutline)
				continue;

			for (int k = 0, m = l.pool.size(); k < m; k++) {
				PoolItem item = l.pool.get(k);
				PoolItem.toHalfFloat(item, data);
				sbuf.put(data, 0, item.used);
			}

			l.offset = pos;
			pos += l.verticesCnt;

			LayerPool.add(l.pool);
			l.pool = null;
		}

		sbuf.position(0);

		// not needed for drawing
		layers = null;

		return sbuf;
	}
}
