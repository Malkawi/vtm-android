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
package org.mapsforge.database.postgis;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;

import org.mapsforge.core.BoundingBox;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.core.Tag;
import org.mapsforge.core.Tile;
import org.mapsforge.core.WebMercator;
import org.mapsforge.database.FileOpenResult;
import org.mapsforge.database.IMapDatabase;
import org.mapsforge.database.IMapDatabaseCallback;
import org.mapsforge.database.MapFileInfo;
import org.mapsforge.database.QueryResult;
import org.postgresql.PGConnection;

import android.util.Log;

/**
 * 
 *
 */
public class MapDatabase implements IMapDatabase {
	private final static String TAG = "MapDatabase";

	private static final String QUERY = "SELECT tags, geom FROM __get_tile(?,?,?)";

	private final float mScale = 1; // 1000000.0f;

	private int mCoordPos = 0;
	private int mIndexPos = 0;
	private float[] mCoords = new float[100000];
	private short[] mIndex = new short[10000];

	private Tag[] mTags;

	private final MapFileInfo mMapInfo =
			new MapFileInfo(new BoundingBox(-180, -85, 180, 85),
					new Byte((byte) 14), new GeoPoint(53.11, 8.85),
					WebMercator.NAME,
					0, 0, 0, "de", "comment", "author");

	private boolean mOpenFile = false;

	private Connection connection = null;
	private static volatile HashMap<Entry<String, String>, Tag> tagHash =
			new HashMap<Entry<String, String>, Tag>(100);
	private PreparedStatement prepQuery = null;

	private boolean connect() {
		Connection conn = null;
		String dburl = "jdbc:postgresql://city.informatik.uni-bremen.de:5432/gis-2.0";

		Properties dbOpts = new Properties();
		dbOpts.setProperty("user", "osm");
		dbOpts.setProperty("password", "osm");
		dbOpts.setProperty("socketTimeout", "50");
		dbOpts.setProperty("tcpKeepAlive", "true");

		try {
			DriverManager.setLoginTimeout(20);
			System.out.println("Creating JDBC connection...");
			Class.forName("org.postgresql.Driver");
			conn = DriverManager.getConnection(dburl, dbOpts);
			connection = conn;
			prepQuery = conn.prepareStatement(QUERY);

			PGConnection pgconn = (PGConnection) conn;

			pgconn.addDataType("hstore", PGHStore.class);

			conn.createStatement().execute("set statement_timeout to 60000");

		} catch (Exception e) {
			System.err.println("Aborted due to error:");
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public QueryResult executeQuery(Tile tile, IMapDatabaseCallback mapDatabaseCallback) {
		if (connection == null) {
			if (!connect())
				return QueryResult.FAILED;
		}

		ResultSet r;

		try {
			prepQuery.setLong(1, tile.tileX * 256);
			prepQuery.setLong(2, tile.tileY * 256);
			prepQuery.setInt(3, tile.zoomLevel);
			Log.d(TAG, "" + prepQuery.toString());
			prepQuery.execute();
			r = prepQuery.getResultSet();
		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return QueryResult.FAILED;
		}

		byte[] b = null;
		PGHStore h = null;
		int cnt = 0;
		try {
			while (r != null && r.next()) {
				mIndexPos = 0;
				mCoordPos = 0;
				cnt++;
				try {
					Object obj = r.getObject(1);
					h = null;

					if (obj instanceof PGHStore)
						h = (PGHStore) obj;
					else {
						Log.d(TAG, "no tags: skip way");
						continue;
					}
					b = r.getBytes(2);

				} catch (SQLException e) {
					e.printStackTrace();
					continue;
				}

				if (b == null) {
					// Log.d(TAG, "no geometry: skip way");
					continue;
				}
				mTags = new Tag[h.size()];

				int i = 0;
				for (Entry<String, String> t : h.entrySet()) {
					if (t.getKey() == null) {
						Log.d(TAG, "no KEY !!! ");
						break;
					}
					Tag tag = tagHash.get(t);
					if (tag == null) {
						tag = new Tag(t.getKey(), t.getValue());
						tagHash.put(t, tag);

					}
					mTags[i++] = tag;
				}
				if (i < mTags.length)
					continue;

				boolean polygon = parse(b);
				if (mIndexPos == 0) {
					Log.d(TAG, "no index: skip way");
					continue;
				} else if (mIndexPos == 1) {
					mapDatabaseCallback.renderPointOfInterest((byte) 0, mCoords[1],
							mCoords[0], mTags);
				} else {

					short[] idx = new short[mIndexPos];
					System.arraycopy(mIndex, 0, idx, 0, mIndexPos);
					mapDatabaseCallback.renderWay((byte) 0, mTags, mCoords, idx, polygon);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			connection = null;
			return QueryResult.FAILED;
		}
		// Log.d(TAG, "rows: " + cnt);
		return QueryResult.SUCCESS;
	}

	@Override
	public String getMapProjection() {
		return WebMercator.NAME;
	}

	@Override
	public MapFileInfo getMapFileInfo() {
		return mMapInfo;
	}

	@Override
	public boolean hasOpenFile() {
		return mOpenFile;
	}

	@Override
	public FileOpenResult openFile(File mapFile) {
		mOpenFile = true;
		return new FileOpenResult();
	}

	@Override
	public void closeFile() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			} finally {
				connection = null;
			}
		}
		mOpenFile = false;
	}

	@Override
	public String readString(int position) {
		return null;
	}

	// taken from postgis-java

	private static ValueGetter valueGetterForEndian(byte[] bytes) {
		if (bytes[0] == ValueGetter.XDR.NUMBER) { // XDR
			return new ValueGetter.XDR(bytes);
		} else if (bytes[0] == ValueGetter.NDR.NUMBER) {
			return new ValueGetter.NDR(bytes);
		} else {
			throw new IllegalArgumentException("Unknown Endian type:" + bytes[0]);
		}
	}

	/**
	 * Parse a binary encoded geometry. Is synchronized to protect offset counter. (Unfortunately, Java does not have
	 * neither call by reference nor multiple return values.)
	 * 
	 * @param value
	 *            ...
	 */
	private boolean parse(byte[] value) {
		return parseGeometry(valueGetterForEndian(value));
	}

	private boolean parseGeometry(ValueGetter data) {
		byte endian = data.getByte(); // skip and test endian flag
		if (endian != data.endian) {
			throw new IllegalArgumentException("Endian inconsistency!");
		}
		int typeword = data.getInt();

		int realtype = typeword & 0x1FFFFFFF; // cut off high flag bits

		boolean haveZ = (typeword & 0x80000000) != 0;
		boolean haveM = (typeword & 0x40000000) != 0;
		boolean haveS = (typeword & 0x20000000) != 0;

		// int srid = Geometry.UNKNOWN_SRID;
		boolean polygon = false;
		if (haveS) {
			// srid = Geometry.parseSRID(data.getInt());
			data.getInt();
		}
		switch (realtype) {
			case Geometry.POINT:
				parsePoint(data, haveZ, haveM);
				break;
			case Geometry.LINESTRING:
				parseLineString(data, haveZ, haveM);
				break;
			case Geometry.POLYGON:
				parsePolygon(data, haveZ, haveM);
				polygon = true;
				break;
			case Geometry.MULTIPOINT:
				parseMultiPoint(data);
				break;
			case Geometry.MULTILINESTRING:
				parseMultiLineString(data);
				break;
			case Geometry.MULTIPOLYGON:
				parseMultiPolygon(data);
				polygon = true;
				break;
			case Geometry.GEOMETRYCOLLECTION:
				parseCollection(data);
				break;
			default:
				throw new IllegalArgumentException("Unknown Geometry Type: " + realtype);
		}
		// if (srid != Geometry.UNKNOWN_SRID) {
		// result.setSrid(srid);
		// }
		return polygon;
	}

	private void parsePoint(ValueGetter data, boolean haveZ, boolean haveM) {
		// double X = data.getDouble();
		// double Y = data.getDouble();
		mCoords[0] = (float) (data.getDouble() * mScale);
		mCoords[1] = (float) (data.getDouble() * mScale);
		mIndex[0] = 2;
		mIndexPos = 1;
		if (haveZ)
			data.getDouble();

		if (haveM)
			data.getDouble();

	}

	/**
	 * Parse an Array of "full" Geometries
	 * 
	 * @param data
	 *            ...
	 * @param count
	 *            ...
	 */
	private void parseGeometryArray(ValueGetter data, int count) {
		for (int i = 0; i < count; i++) {
			parseGeometry(data);
			mIndex[mIndexPos++] = 0;
		}
	}

	//
	// private void parsePointArray(ValueGetter data, boolean haveZ, boolean haveM) {
	// int count = data.getInt();
	// for (int i = 0; i < count; i++) {
	// parsePoint(data, haveZ, haveM);
	// }
	// }

	private void parseMultiPoint(ValueGetter data) {
		parseGeometryArray(data, data.getInt());
	}

	private void parseLineString(ValueGetter data, boolean haveZ, boolean haveM) {
		int count = data.getInt();
		for (int i = 0; i < count; i++) {
			mCoords[mCoordPos++] = (float) (data.getDouble()) * mScale;
			mCoords[mCoordPos++] = (float) (data.getDouble()) * mScale;
			if (haveZ)
				data.getDouble();
			if (haveM)
				data.getDouble();
		}
		mIndex[mIndexPos++] = (short) (count * 2);
	}

	private void parsePolygon(ValueGetter data, boolean haveZ, boolean haveM) {
		int count = data.getInt();

		for (int i = 0; i < count; i++) {
			parseLineString(data, haveZ, haveM);
		}
	}

	private void parseMultiLineString(ValueGetter data) {
		int count = data.getInt();
		parseGeometryArray(data, count);
	}

	private void parseMultiPolygon(ValueGetter data) {
		int count = data.getInt();
		parseGeometryArray(data, count);
	}

	private void parseCollection(ValueGetter data) {
		int count = data.getInt();
		parseGeometryArray(data, count);
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub

	}
}