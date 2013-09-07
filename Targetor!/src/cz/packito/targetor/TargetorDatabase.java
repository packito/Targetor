package cz.packito.targetor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * A data access object for the database. For accesing tha database, create an
 * instance of this class and {@link #open()} it. Then you can add stuff to db.
 * Don't forget to {@link #close()} after using.
 * 
 * 
 * @author packito
 * 
 */
public class TargetorDatabase {
	// Database fields
	private SQLiteDatabase database;
	private TargetorDbOpenHelper dbHelper;

	public TargetorDatabase(Context context) {
		dbHelper = new TargetorDbOpenHelper(context);
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public boolean insertHistoryMultiplayer(int score, int targetsShot,
			int misses, int opponentScore, String opponentAddress) {
		ContentValues values = new ContentValues();
		values.put(HistoryTable.COLUMN_DATE, getCurrentSqliteDate());
		values.put(HistoryTable.COLUMN_SCORE, score);
		values.put(HistoryTable.COLUMN_TARGETS_SHOT, targetsShot);
		values.put(HistoryTable.COLUMN_MISSES, misses);
		values.put(HistoryTable.COLUMN_OPPONENT_SCORE, opponentScore);
		values.put(HistoryTable.COLUMN_OPPONENT_ADDRESS, opponentAddress);

		long insertId = database.insert(HistoryTable.TABLENAME, null, values);

		return insertId != -1;
	}

	public boolean insertHistorySingleplayer(int score, int targetsShot,
			int misses, int levelId) {
		ContentValues values = new ContentValues();
		values.put(HistoryTable.COLUMN_DATE, getCurrentSqliteDate());
		values.put(HistoryTable.COLUMN_SCORE, score);
		values.put(HistoryTable.COLUMN_TARGETS_SHOT, targetsShot);
		values.put(HistoryTable.COLUMN_MISSES, misses);
		values.put(HistoryTable.COLUMN_LEVEL_ID, levelId);

		long insertId = database.insert(HistoryTable.TABLENAME, null, values);

		return insertId != -1;
	}

	public int getWins(String opponentAddress) {
		Cursor c = database.rawQuery("SELECT * FROM " + HistoryTable.TABLENAME
				+ " WHERE " + HistoryTable.COLUMN_OPPONENT_ADDRESS + "='"
				+ opponentAddress + "' AND " + HistoryTable.COLUMN_SCORE + ">"
				+ HistoryTable.COLUMN_OPPONENT_SCORE, null);
		return c.getCount();
	}

	/**
	 * TODO change this to respect target scores in each level
	 * 
	 * @return the current level in singleplayer
	 */
	public int getLevel() {
		int lvl = 1;
		while (true) {
			Cursor c = database.rawQuery(
					"SELECT * FROM " + HistoryTable.TABLENAME + " WHERE "
							+ HistoryTable.COLUMN_LEVEL_ID + "=" + lvl
							+ " AND " + HistoryTable.COLUMN_SCORE + ">="
							+ TargetorApplication.calcScore(lvl), null);
			if (c == null || c.getCount() < 1
					|| lvl == TargetorApplication.LEVELS) {
				break;
			} else
				lvl++;
		}
		return lvl;
	}

	public int getLoses(String opponentAddress) {
		Cursor c = database.rawQuery("SELECT * FROM " + HistoryTable.TABLENAME
				+ " WHERE " + HistoryTable.COLUMN_OPPONENT_ADDRESS + "='"
				+ opponentAddress + "' AND " + HistoryTable.COLUMN_SCORE + "<"
				+ HistoryTable.COLUMN_OPPONENT_SCORE, null);
		return c.getCount();
	}

	@SuppressLint("SimpleDateFormat")
	private String getCurrentSqliteDate() {
		DateFormat dateFormatISO8601 = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String crntDate = dateFormatISO8601.format(new Date());
		return crntDate;
	}

	public int getHighScore(int lvl) {
		int score = Integer.MIN_VALUE;
		Cursor c = database.rawQuery("SELECT MAX(" + HistoryTable.COLUMN_SCORE
				+ ") FROM " + HistoryTable.TABLENAME + " WHERE "
				+ HistoryTable.COLUMN_LEVEL_ID + "=" + lvl, null);
		if (c != null && c.moveToFirst() && !c.isNull(0)) {
			score = c.getInt(0);
		}
		return score;
	}

}

/**
 * a {@linkplain SQLiteOpenHelper} for the Targetor database
 * 
 * @author cesa
 * 
 */
class TargetorDbOpenHelper extends SQLiteOpenHelper {

	public static final String DATABASE_NAME = "targetor.sqlite";
	public static final int DATABASE_VERSION = 2;

	public TargetorDbOpenHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		HistoryTable.onCreate(db);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		HistoryTable.onUpgrade(db, oldVersion, newVersion);
	}

}

/**
 * Class representing the history table in the database
 * 
 * @author packito
 * 
 */
class HistoryTable {
	public static final String TABLENAME = "history";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_DATE = "date";
	public static final String COLUMN_SCORE = "score";
	public static final String COLUMN_TARGETS_SHOT = "targets_shot";
	public static final String COLUMN_MISSES = "misses";
	public static final String COLUMN_LEVEL_ID = "level_id";
	public static final String COLUMN_OPPONENT_SCORE = "opponent_score";
	public static final String COLUMN_OPPONENT_ADDRESS = "opponent_address";

	/** Table creation SQL statement */
	private static final String CREATE_TABLE = "CREATE TABLE " + TABLENAME
			+ "(" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
			+ COLUMN_DATE + " TEXT NOT NULL, " + COLUMN_SCORE
			+ " INTEGER NOT NULL," + COLUMN_TARGETS_SHOT
			+ " INTEGER NOT NULL, " + COLUMN_MISSES + " INTEGER NOT NULL, "
			+ COLUMN_LEVEL_ID + " INTEGER, " + COLUMN_OPPONENT_SCORE
			+ " INTEGER, " + COLUMN_OPPONENT_ADDRESS + " TEXT" + ");";

	public static void onCreate(SQLiteDatabase database) {
		database.execSQL(CREATE_TABLE);
	}

	public static void onUpgrade(SQLiteDatabase database, int oldVersion,
			int newVersion) {
		Log.w(HistoryTable.class.getName(), "Upgrading database from version "
				+ oldVersion + " to " + newVersion
				+ ", which will destroy all old data");
		database.execSQL("DROP TABLE IF EXISTS " + TABLENAME + ";");
		onCreate(database);
	}

}