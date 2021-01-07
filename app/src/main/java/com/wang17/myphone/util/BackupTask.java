package com.wang17.myphone.util;

import android.content.Context;
import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.wang17.myphone.model.DateTime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import static com.wang17.myphone.util._Utils.e;

/**
 * Created by 阿弥陀佛 on 2016/10/26.
 */

public class BackupTask extends AsyncTask<String, Void, Integer> {
    public static final String COMMAND_BACKUP = "backupDatabase";
    public static final String COMMAND_RESTORE = "restroeDatabase";
    public static final int BACKUP_SUCCESS = 1;
    public static final int RESTORE_SUCCESS = 2;
    public static final int BACKUP_ERROR = 3;
    public static final int RESTORE_ERROR = 4;
    public static final int ERROR = 5;
    private Context mContext;
    private OnFinishedListener onFinishedListener;

    public BackupTask(Context context) {
        e("backup task 构造函数");
        this.mContext = context;
        if (context instanceof OnFinishedListener)
            this.onFinishedListener = (OnFinishedListener) context;
    }

    @Override
    protected Integer doInBackground(String... params) {
        //  Auto-generated method stub
        try {
            File dbFile = mContext.getDatabasePath("mp.db");  // 获得数据库路径，默认是/data/data/(包名)org.dlion/databases/
            File backup = new File(_Session.BACKUP_DIR, "mp"+ DateTime.getToday().getDayStr()+".db");
            String command = params[0];
            if (command.equals(COMMAND_BACKUP)) {
                try {
                    backup.createNewFile();
                    fileCopy(dbFile, backup);
                    return BACKUP_SUCCESS;
                } catch (Exception e) {
                    return BACKUP_ERROR;
                }
            } else if (command.equals(COMMAND_RESTORE)) {
                try {
                    e("backup task restore start copy....");
                    fileCopy(backup, dbFile);
                    e("backup task restore copy over....");
                    return RESTORE_SUCCESS;
                } catch (Exception e) {
                    e.printStackTrace();
                    return RESTORE_ERROR;
                }
            } else {
                return ERROR;
            }
        } catch (Exception e) {
            _Utils.printException(mContext, e);
            return RESTORE_SUCCESS;
        }
    }

    private void fileCopy(File dbFile, File backup) throws IOException {
        //  Auto-generated method stub
        FileChannel inChannel = new FileInputStream(dbFile).getChannel();
        FileChannel outChannel = new FileOutputStream(backup).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            _Utils.printException(mContext, e);
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    protected void onPostExecute(Integer result) {
        //  Auto-generated method stub
        super.onPostExecute(result);
        switch (result) {
            case BACKUP_SUCCESS:
                if (onFinishedListener != null)
                    onFinishedListener.onFinished(BACKUP_SUCCESS);
                Log.d("backup", "ok");
                break;
            case BACKUP_ERROR:
                if (onFinishedListener != null)
                    onFinishedListener.onFinished(BACKUP_ERROR);
                Log.d("backup", "fail");
                break;
            case RESTORE_SUCCESS:
                if (onFinishedListener != null)
                    onFinishedListener.onFinished(RESTORE_SUCCESS);
                Log.d("restore", "success");
                break;
            case RESTORE_ERROR:
                if (onFinishedListener != null)
                    onFinishedListener.onFinished(RESTORE_ERROR);
                Log.d("restore", "fail");
                break;
            default:
                break;
        }
    }

    public interface OnFinishedListener {
        void onFinished(int Result);
    }

    public static void Finished(int result, View view) {
        switch (result) {
            case BackupTask.BACKUP_SUCCESS:
                Snackbar.make(view, "数据库已备份。", Snackbar.LENGTH_LONG).show();
                break;
            case BackupTask.BACKUP_ERROR:
                Snackbar.make(view, "数据库备份出错。", Snackbar.LENGTH_INDEFINITE).setAction("知道了", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setVisibility(View.INVISIBLE);
                    }
                }).show();
                break;
            case BackupTask.RESTORE_SUCCESS:
                Snackbar.make(view, "数据库已恢复。", Snackbar.LENGTH_LONG).show();
                break;
            case BackupTask.RESTORE_ERROR:
                Snackbar.make(view, "数据库恢复出错。", Snackbar.LENGTH_INDEFINITE).setAction("知道了", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setVisibility(View.INVISIBLE);
                    }
                }).show();
                break;
            default:
                Snackbar.make(view, "运行任务时出错。", Snackbar.LENGTH_INDEFINITE).setAction("知道了", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setVisibility(View.INVISIBLE);
                    }
                }).show();
                break;
        }
    }

    private class GameScore {
    }
}