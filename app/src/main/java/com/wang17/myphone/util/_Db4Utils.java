package com.wang17.myphone.util;

import android.util.Log;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class _Db4Utils {

    private static final String REMOTE_IP = "db4free.net";
    private static final String URL = "jdbc:mysql://" + REMOTE_IP + "/wangsc";
    private static final String USER = "wangsc";
    private static final String PASSWORD = "sql351489";


    private static final String _TAG = "wangsc";
    public static Connection openConnection(String url, String user, String password) {
        Connection conn = null;
        try {
            final String DRIVER_NAME = "com.mysql.jdbc.Driver";
            Class.forName(DRIVER_NAME);
            conn = DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException e) {
            conn = null;
        } catch (SQLException e) {
            conn = null;
        }

        return conn;
    }

    public static void query(Connection conn, String sql) {

        if (conn == null) {
            return;
        }

        Statement statement = null;
        ResultSet result = null;

        try {
            statement = conn.createStatement();
            result = statement.executeQuery(sql);
            if (result != null && result.first()) {
                int idColumnIndex = result.findColumn("id");
                int userColumnIndex = result.findColumn("user");
                int timeColumnIndex = result.findColumn("time");
                int longitudeColumnIndex = result.findColumn("longitude");
                int latitudeColumnIndex = result.findColumn("latitude");
                int speedColumnIndex = result.findColumn("speed");
                int bearingColumnIndex = result.findColumn("bearing");
                int accuracyColumnIndex = result.findColumn("accuracy");
                int providerColumnIndex = result.findColumn("provider");
                int typeColumnIndex = result.findColumn("type");
                int satellitesColumnIndex = result.findColumn("satellites");
                int poiNameColumnIndex = result.findColumn("poiName");
                int summaryColumnIndex = result.findColumn("summary");
                int addressColumnIndex = result.findColumn("address");
                Log.e(_TAG,"id\t\t" + "user\t\t" + "time\t\t" + "longitude\t\t" + "latitude\t\t" + "speed\t\t" + "bearing\t\t" + "accuracy\t\t"
                        + "provider\t\t"+ "type\t\t"+ "satellites\t\t"+ "poiName\t\t"+ "summary\t\t"+ "address\t\t");
                while (!result.isAfterLast()) {
                    Log.e(_TAG,result.getString(idColumnIndex));
                    Log.e(_TAG,result.getString(userColumnIndex));
                    Log.e(_TAG,result.getString(timeColumnIndex));
                    Log.e(_TAG,result.getString(longitudeColumnIndex));
                    Log.e(_TAG,result.getString(latitudeColumnIndex));
                    Log.e(_TAG,result.getString(speedColumnIndex));
                    Log.e(_TAG,result.getString(bearingColumnIndex));
                    Log.e(_TAG,result.getString(accuracyColumnIndex));
                    Log.e(_TAG,result.getString(providerColumnIndex));
                    Log.e(_TAG,result.getString(typeColumnIndex));
                    Log.e(_TAG,result.getString(satellitesColumnIndex));
                    Log.e(_TAG,result.getString(poiNameColumnIndex));
                    Log.e(_TAG,result.getString(summaryColumnIndex));
                    Log.e(_TAG,result.getString(addressColumnIndex));
                    result.next();
                }
            }
        } catch (SQLException e) {
            Log.e(_TAG,e.getMessage());
        } finally {
            try {
                if (result != null) {
                    result.close();
                    result = null;
                }
                if (statement != null) {
                    statement.close();
                    statement = null;
                }
            } catch (SQLException sqle) {

            }
        }
    }

    public static boolean execSQL(Connection conn, String sql) {
        boolean execResult = false;
        try {
            Log.e(_TAG,"0"+conn);
            if (conn == null) {
                return execResult;
            }
Log.e(_TAG,"1");
            Statement statement = null;

            try {
                statement = conn.createStatement();
                Log.e(_TAG,"2");
                if (statement != null) {
                    execResult = statement.execute(sql);
                    Log.e(_TAG,"3");
                }
            } catch (SQLException e) {
                execResult = false;
            }
        } catch (Exception e) {
            Log.e(_TAG,e.getMessage());
        }

        return execResult;
    }
}
