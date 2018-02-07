package com.github.reader.pdf.presenter;

import android.app.AlertDialog;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.view.Display;
import android.view.WindowManager;

import com.github.reader.app.base.BasePresenter;
import com.github.reader.app.model.entity.DocumentBean;
import com.github.reader.app.model.entity.DocumentBeanDao;
import com.github.reader.app.model.manager.DBManager;
import com.github.reader.app.ui.view.BaseDocView;
import com.github.reader.pdf.model.AsyncTask;
import com.github.reader.pdf.model.MuPDFAlert;
import com.github.reader.pdf.model.MuPDFCore;
import com.github.reader.pdf.model.OutlineActivityData;
import com.github.reader.pdf.model.SearchTask;
import com.github.reader.pdf.model.SearchTaskResult;
import com.github.reader.pdf.ui.activity.IPdfMainView;
import com.github.reader.pdf.ui.presentation.PdfPresentation;
import com.github.reader.utils.AppUtils;
import com.github.reader.utils.Constants;
import com.github.reader.utils.LangUtils;
import com.github.reader.utils.LogUtils;
import com.github.reader.utils.SharedPreferencesUtil;
import com.github.reader.utils.SystemPropertyUtils;
import com.github.reader.utils.ToastUtil;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * 主屏Presenter的实现类
 */

public class PdfMainPresenter extends BasePresenter<IPdfMainView>
        implements IPdfMainPresenter {
    private static final String TAG = "PdfMainPresenter";
    private Context mContext;
    private String mFileName;
//    private IMainPdfContract.IView pdfView;

    private MuPDFCore core;
    private boolean mAlertsActive = false;
    private AsyncTask<Void, Void, MuPDFAlert> mAlertTask;
    private AlertDialog mAlertDialog;
    public int mPageSliderRes;
    private SearchTask mSearchTask;
    private PdfPresentation mPresentation;

    public PdfMainPresenter(IPdfMainView mvpView) {
        super(mvpView);
        mContext = AppUtils.getAppContext();
    }

    @Override
    public void openDocument(Intent intent) {
        byte buffer[] = null;
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();

            if (uri.toString().startsWith("content://")) {
                String reason = null;
                try {
                    InputStream is = mContext.getContentResolver().openInputStream(uri);
                    int len = is.available();
                    buffer = new byte[len];
                    is.read(buffer, 0, len);
                    is.close();
                } catch (OutOfMemoryError e) {
                    LogUtils.d(TAG, "Out of memory during buffer reading");
                    reason = e.toString();
                } catch (Exception e) {
                    LogUtils.d(TAG, "Exception reading from stream: " + e);
                    try {
                        Cursor cursor = mContext.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                        if (cursor.moveToFirst()) {
                            String str = cursor.getString(0);
                            if (str == null) {
                                reason = "Couldn't parse data in intent";
                            } else {
                                uri = Uri.parse(str);
                            }
                        }
                    } catch (Exception e2) {
                        LogUtils.d(TAG, "Exception in Transformer Prime file manager code: " + e2);
                        reason = e2.toString();
                    }
                }
                if (reason != null) {
                    buffer = null;
                    if(mvpView != null)
                        mvpView.showOpenErrorDialog(reason);
                    return;
                }
            }
            if (buffer != null) {
                core = openBuffer(buffer, intent.getType());
            } else {
                String path = Uri.decode(uri.getEncodedPath());
                if (path == null) {
                    path = uri.toString();
                }
                //TODO 根据类型创建不同的Document
                core = openFile(path);
            }
            SearchTaskResult.set(null);
        }
        if (core != null && core.needsPassword()) {
            if(mvpView != null)
                        mvpView.requestPassword();
            LogUtils.d(TAG, "PDF需要密码才能打开文档");
            return;
        }
        if (core != null && core.countPages() == 0) {
            core = null;
        }

        if (core == null) {
            if(mvpView != null)
                        mvpView.showOpenErrorDialog("");
            return;
        }

        initData();
    }

    @Override
    public void initData() {
        Constants.HORIZONTAL_SCROLLING = SharedPreferencesUtil.getInstance().getBoolean(Constants.READ_MODE, true);
        int smax = Math.max(core.countPages() - 1, 1);
        mPageSliderRes = ((10 + smax - 1) / smax) * 2;
    }

    @Override
    public void onResume() {
        if (mSearchTask == null) {
            mSearchTask = new SearchTask(mContext, core) {
                @Override
                protected void onTextFound(SearchTaskResult result) {
                    SearchTaskResult.set(result);
                    // Ask the ReaderView to move to the resulting page
                    if(mvpView != null)
                        mvpView.getDocView().setDisplayedViewIndex(result.pageNumber);
                    // Make the ReaderView act on the change to BaseSearchTaskResult
                    // via overridden onChildSetup method.
                    if(mvpView != null)
                        mvpView.getDocView().resetupChildren();
                }
            };
        }
        performResumeDocProgress();
    }

    public void performResumeDocProgress() {
        if(mvpView == null)
            return;

        BaseDocView basePDFView = mvpView.getDocView();
        if (LangUtils.isEmpty(Constants.DOCUMENT_PATH) || basePDFView == null) {
            return;
        }

        String strQuery = "where " + DocumentBeanDao.Properties.Path.columnName + " = ?";
        List<DocumentBean> documentBeans = DBManager.getInstance().getSession().getDocumentBeanDao()
                .queryRaw(strQuery, Constants.DOCUMENT_PATH);

        DocumentBean documentBean = LangUtils.getFirstObj(documentBeans);
        if (documentBean != null) {
            if (Constants.DOC_PAGE_COUNT < 1
                    || documentBean.getIndex() < 0
                    || documentBean.getPageCount() < 1
                    || documentBean.getIndex() > Constants.DOC_PAGE_COUNT - 1
                    || documentBean.getIndex() == basePDFView.getDisplayedViewIndex()) {
                return;
            } else {
                Constants.CURRENT_DISPLAY_INDEX = documentBean.getIndex();
                LogUtils.d(TAG, "resumePdfProgress..documentBean.getIndex()=" + documentBean.getIndex());
            }
        }
    }

    public void performSaveDocProgress() {
        if(mvpView == null)
            return;

        BaseDocView basePDFView = mvpView.getDocView();

        if (LangUtils.isEmpty(Constants.DOCUMENT_PATH)
                || basePDFView == null
                || basePDFView.getDisplayedViewIndex() < 0
                || Constants.DOC_PAGE_COUNT < 1
                || basePDFView.getDisplayedViewIndex() >= Constants.DOC_PAGE_COUNT) {
            LogUtils.d(TAG + " performSavePdfProgress error");
            return;
        }

        DocumentBean documentBean = new DocumentBean();
        documentBean.setIndex(basePDFView.getDisplayedViewIndex());
        documentBean.setPath(Constants.DOCUMENT_PATH);
        documentBean.setPageCount(Constants.DOC_PAGE_COUNT);
        DBManager.getInstance().getSession().getDocumentBeanDao().insertOrReplace(documentBean);
    }

    @Override
    public void onPause() {
        if(mvpView == null)
            return;

        if (mSearchTask != null)
            mSearchTask.stop();

        if (mFileName != null && mvpView.getDocView() != null) {
            SharedPreferencesUtil.getInstance().putBoolean(Constants.READ_MODE, Constants.HORIZONTAL_SCROLLING);
            performSaveDocProgress();
        }
    }

    @Override
    public void onDestory() {
    }

    @Override
    public void switchReadMode() {
        Constants.HORIZONTAL_SCROLLING = !Constants.HORIZONTAL_SCROLLING;
        LogUtils.d(TAG, "switchReadMode current HORIZONTAL_SCROLLING=" + Constants.HORIZONTAL_SCROLLING);
        if(mvpView != null)
                        mvpView.getDocView().refresh();
        ToastUtil.getInstance().showToast("切换阅读模式");
    }

    @Override
    public Presentation initPresentation() {
        DisplayManager displayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display[] presentationDisplays = displayManager
                .getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
        if (presentationDisplays.length > 0) {
            Display presentationDisplay = presentationDisplays[0];
            mPresentation = new PdfPresentation(mContext, presentationDisplay);
            mPresentation.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

            SystemPropertyUtils.setSystemProperty(mContext, "sys.eink.Appmode", "13");
            mPresentation.show();
            return mPresentation;
        }
        return null;
    }

    @Override
    public Object getDocManager() {
        return core;
    }

    public int getPageSliderRes() {
        return mPageSliderRes;
    }

    @Override
    public void search(int direction) {
        if(mvpView == null)
            return;

        AppUtils.hideKeyboard(mvpView.getSearchTextView());
        int displayPage = mvpView.getDocView().getDisplayedViewIndex();
        SearchTaskResult r = SearchTaskResult.get();
        int searchPage = r != null ? r.pageNumber : -1;
        mSearchTask.go(mvpView.getSearchTextView().getText().toString(), direction, displayPage, searchPage);
    }

    private MuPDFCore openFile(String path) {
        LogUtils.d(TAG, "openFile: path=" + path);
        int lastSlashPos = path.lastIndexOf('/');
        mFileName = new String(lastSlashPos == -1
                ? path
                : path.substring(lastSlashPos + 1));
        Constants.FILE_NAME = mFileName;
        Constants.DOCUMENT_PATH = path;
        try {
            core = new MuPDFCore(mContext, path);
            Constants.DOC_PAGE_COUNT = core.countPages();
            OutlineActivityData.set(null);
        } catch (Exception e) {
            LogUtils.d(TAG, "open file e.toString=" + e.toString());
            return null;
        } catch (OutOfMemoryError e) {
            //  out of memory is not an Exception, so we catch it separately.
            LogUtils.d(TAG, "e.toString=" + e.toString());
            return null;
        }
        return core;
    }

    private MuPDFCore openBuffer(byte buffer[], String magic) {
        LogUtils.d(TAG, "openBuffer: ");
        try {
            core = new MuPDFCore(mContext, buffer, magic);
            OutlineActivityData.set(null);
        } catch (Exception e) {
            System.out.println(e);
            return null;
        }
        return core;
    }

//    @Override
    public void createAlertWaiter() {
        mAlertsActive = true;
        // All mupdf library calls are performed on asynchronous tasks to avoid stalling
        // the UI. Some calls can lead to javascript-invoked requests to display an
        // alert dialog and collect a reply from the user. The task has to be blocked
        // until the user's reply is received. This method creates an asynchronous task,
        // the purpose of which is to wait of these requests and produce the dialog
        // in response, while leaving the core blocked. When the dialog receives the
        // user's response, it is sent to the core via replyToAlert, unblocking it.
        // Another alert-waiting task is then created to pick up the next alert.
        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
        if (mAlertDialog != null) {
            mAlertDialog.cancel();
            mAlertDialog = null;
        }
        mAlertTask = new AsyncTask<Void, Void, MuPDFAlert>() {

            @Override
            protected MuPDFAlert doInBackground(Void... arg0) {
                if (!mAlertsActive)
                    return null;

                return core.waitForAlert();
            }

            @Override
            protected void onPostExecute(final MuPDFAlert result) {
                // core.waitForAlert may return null when shutting down
                if (result == null)
                    return;
                final MuPDFAlert.ButtonPressed pressed[] = new MuPDFAlert.ButtonPressed[3];
                for (int i = 0; i < 3; i++)
                    pressed[i] = MuPDFAlert.ButtonPressed.None;
                if(mvpView != null)
                        mvpView.showMupdfAlertDialog(mAlertsActive, result, pressed);
            }
        };

        mAlertTask.executeOnExecutor(new ThreadPerTaskExecutor());
    }

//    @Override
    public void destroyAlertWaiter() {
        mAlertsActive = false;

        if (mAlertTask != null) {
            mAlertTask.cancel(true);
            mAlertTask = null;
        }
    }

    class ThreadPerTaskExecutor implements Executor {
        public void execute(Runnable r) {
            new Thread(r).start();
        }
    }

}
