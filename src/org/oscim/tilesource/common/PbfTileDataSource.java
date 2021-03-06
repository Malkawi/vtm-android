/*
 * Copyright 2012 Hannes Janetzek
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
package org.oscim.tilesource.common;

import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import org.oscim.layers.tile.MapTile;
import org.oscim.tilesource.ITileDataSink;
import org.oscim.tilesource.ITileDataSource;

import android.util.Log;

/**
 *
 *
 */
public abstract class PbfTileDataSource implements ITileDataSource {
	private static final String TAG = PbfTileDataSource.class.getName();

	protected LwHttp mConn;
	protected final PbfDecoder mTileDecoder;


	public PbfTileDataSource(PbfDecoder tileDecoder) {
		mTileDecoder = tileDecoder;
	}

	@Override
	public QueryResult executeQuery(MapTile tile, ITileDataSink sink) {
		QueryResult result = QueryResult.SUCCESS;

		try {
			InputStream is;
			if (!mConn.sendRequest(tile)) {
				Log.d(TAG, tile + " Request Failed");
				result = QueryResult.FAILED;
			} else if ((is = mConn.readHeader()) != null) {
				boolean win = mTileDecoder.decode(tile, sink, is, mConn.getContentLength());
				if (!win)
					Log.d(TAG, tile + " failed");
			} else {
				Log.d(TAG, tile + " Network Error");
				result = QueryResult.FAILED;
			}
		} catch (SocketException e) {
			Log.d(TAG, tile + " Socket exception: " + e.getMessage());
			result = QueryResult.FAILED;
		} catch (SocketTimeoutException e) {
			Log.d(TAG, tile + " Socket Timeout");
			result = QueryResult.FAILED;
		} catch (UnknownHostException e) {
			Log.d(TAG, tile + " No Network");
			result = QueryResult.FAILED;
		} catch (Exception e) {
			e.printStackTrace();
			result = QueryResult.FAILED;
		}

		mConn.requestCompleted();

		if (result != QueryResult.SUCCESS)
			mConn.close();

		return result;
	}

	@Override
	public void destroy() {
		mConn.close();
	}
}
