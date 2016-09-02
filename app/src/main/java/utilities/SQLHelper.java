package utilities;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by Ankush on 27-08-2016.
 */
public class SQLHelper extends SQLiteOpenHelper{
    private static String NAME="NOTICE_BOARD";
    private static int VERSION=1;
    private static String TABLE_NOTICES="NOTICES";
    private static String ROW_SUBJECT="SUBJECT";
    private static String ROW_EXPIRY_DATE="EXP_DATE";
    private static String ROW_REFERENCE="REFERENCE";
    private static String ROW_CONTENT="CONTENT";
    private static String ROW_DATETIME="DATETIME";
    private static String ROW_UPLOADER="UPLOADER";
    private static String ROW_STATUS="STATUS";
    private static String CREATE_TABLE_NOTICES=
            "CREATE TABLE "+TABLE_NOTICES+" ( "+
                    ROW_SUBJECT+" VARCHAR(100),"+
                    ROW_REFERENCE+" VARCHAR(100),"+
                    ROW_EXPIRY_DATE+" DATETIME,"+
                    ROW_CONTENT+" TEXT,"+
                    ROW_UPLOADER+" VARCHAR(100),"+
                    ROW_DATETIME+" DATETIME,"+
                    ROW_STATUS+" BOOLEAN"+
                    ");";


    public SQLHelper(Context context) {
        super(context, NAME, null, VERSION);
        SQLiteDatabase db=this.getWritableDatabase();
        db.execSQL(CREATE_TABLE_NOTICES);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
