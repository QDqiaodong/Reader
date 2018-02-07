package com.github.reader.app.model.manager;

import android.content.Context;

import com.github.reader.app.model.entity.DaoMaster;
import com.github.reader.app.model.entity.DaoSession;
import com.github.reader.app.model.entity.DocumentBean;
import com.github.reader.utils.Constants;


/**
 * 数据库辅助类
 */

public class DBManager {
    private static final String TAG = DBManager.class.getSimpleName();
    private static DBManager mInstance;
    private DaoMaster.DevOpenHelper mOpenHelper;
    private DaoMaster mDaoMaster;
    private DaoSession mDaoSession;
    private static final String DBName = "DocumentMessage.db";

    private DBManager() {
    }

    public static DBManager getInstance() {
        if (mInstance == null) {
            mInstance = new DBManager();
        }
        return mInstance;
    }

    public void init(Context context) {
        mOpenHelper = new DaoMaster.DevOpenHelper(context,  DBName, null);
        mDaoMaster = new DaoMaster(mOpenHelper.getWritableDb());
        mDaoSession = mDaoMaster.newSession();
    }

    public DaoSession getSession() {
        return mDaoSession;
    }

    public DaoMaster getMaster() {
        return mDaoMaster;
    }

    /**
     * 当前文档对象
     *
     * @return
     */
    public DocumentBean getCurrentDocumentBean() {
        DocumentBean documentBean = new DocumentBean();
        documentBean.setIndex(Constants.CURRENT_DISPLAY_INDEX);
        documentBean.setPath(Constants.DOCUMENT_PATH);
        return documentBean;
    }

}
