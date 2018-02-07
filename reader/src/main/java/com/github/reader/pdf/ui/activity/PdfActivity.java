package com.github.reader.pdf.ui.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.github.reader.R;
import com.github.reader.app.base.BaseMvpActivity;
import com.github.reader.app.model.entity.BaseAnnotation;
import com.github.reader.app.ui.view.BaseDocView;
import com.github.reader.app.ui.view.CatalogMenu;
import com.github.reader.app.ui.view.IBaseDocView;
import com.github.reader.pdf.model.Hit;
import com.github.reader.pdf.model.MuPDFAlert;
import com.github.reader.pdf.model.MuPDFCore;
import com.github.reader.pdf.model.OutlineActivityData;
import com.github.reader.pdf.model.SearchTaskResult;
import com.github.reader.pdf.presenter.PdfMainPresenter;
import com.github.reader.pdf.ui.adapater.MuPDFPageAdapter;
import com.github.reader.pdf.ui.presentation.PdfPresentation;
import com.github.reader.pdf.ui.view.MuPdfDocView;
import com.github.reader.utils.AppUtils;
import com.github.reader.utils.Constants;
import com.github.reader.utils.LogUtils;
import com.github.reader.utils.ToastUtil;

/**
 * 主屏V的实现
 */
public class PdfActivity extends BaseMvpActivity<PdfMainPresenter>
        implements IPdfMainView, View.OnClickListener {
    private static final String TAG = "PdfActivity";
    private Context mContext;
    private TextView mTvPrePage;
    private TextView mTvNextPage;
    private LinearLayout mAnnotMenu;
    private LinearLayout mTvCopy;
    private LinearLayout mTvHighlight;
    private LinearLayout mTvUnderLine;
    private LinearLayout mTVStrikeOut;
    private FrameLayout mReadWidget;
    private DrawerLayout mDrawerLayout;
    private CatalogMenu mCatalogMenu;
    private ActionBarDrawerToggle mDrawerToggle;
    private ImageButton mCancelAccept;
    private ImageButton mSureAccept;
    private TextView mPageNum;

    //TODO 需重新设定
    enum TopBarMode {
        Main, Search, Annot
    }

    enum AcceptMode {Okdefault, CopyText, Highlight, Underline, StrikeOut, Ink, Delete}

    private final int OUTLINE_REQUEST = 0;

    private MuPDFCore core;
    private String mFileName;
    private MuPdfDocView mDocView;
    private ViewGroup mReaderRootView;
    private boolean mButtonsVisible;
    private EditText mPasswordView;
    private TextView mTitle;
    private ImageView mReturn;
    private SeekBar mPageSlider;
    private ImageView mSearchButton;
    private ImageView mBrowseDirButton;
    private ImageView mBrightnessButton;
    private ImageView mClipButton;
    private TextView mSwitchScreenButton;
    private ImageView mOutlineButton;
    private ImageButton mIvInk;
    private TextView mAnnotTypeText;
    private ImageView mAnnotButton;
    private RelativeLayout mMenuRoot;
    private RelativeLayout mBottomContainer;
    private RelativeLayout mTopMenuContainer;
    private ViewAnimator mTopMenuSwitcher;
    private TopBarMode mTopBarMode = TopBarMode.Main;
    private AcceptMode mAcceptMode = AcceptMode.Okdefault;
    private ImageButton mSearchBack;
    private ImageButton mSearchFwd;
    private EditText mSearchText;
    private ImageView mIvCancelSearch;
    private AlertDialog.Builder mAlertBuilder;

    private boolean isHideButtons;
    private boolean isSearchMode;
    private AlertDialog mAlertDialog;
    private PdfPresentation mPresentation;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        if (core == null) {
            core = (MuPDFCore) getLastNonConfigurationInstance();
            if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
                mFileName = savedInstanceState.getString("FileName");
            }
            if (savedInstanceState != null && savedInstanceState.containsKey("ButtonsHidden")) {
                isHideButtons = savedInstanceState.getBoolean("ButtonsHidden", false);
            }
            if (savedInstanceState != null && savedInstanceState.containsKey("SearchMode")) {
                isSearchMode = savedInstanceState.getBoolean("SearchMode", false);
            }
        }
        if (core == null) {
            mvpPresenter.openDocument(getIntent());
        }
        core = (MuPDFCore) mvpPresenter.getDocManager();
        createUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (core != null) {
            core.startAlerts();
            mvpPresenter.createAlertWaiter();
        }
        mvpPresenter.onResume();
        mTitle.setText(Constants.FILE_NAME);
        if (core != null) {
            mDocView.setDisplayedViewIndex(Constants.CURRENT_DISPLAY_INDEX);
        }
    }

    @Override
    protected PdfMainPresenter setPresenter() {
        return new PdfMainPresenter(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation==Configuration.ORIENTATION_PORTRAIT){
           LogUtils.d("现在是竖屏");
            setButtonEnabled(mBrowseDirButton,false);
            Constants.HORIZONTAL_SCROLLING = true;
            if(mDocView != null){
                mDocView.refresh();
            }

            if(mCatalogMenu != null){
                mCatalogMenu.refactorMenuWith();
            }
        }
        if(newConfig.orientation==Configuration.ORIENTATION_LANDSCAPE){
            LogUtils.d("现在是横屏");
            setButtonEnabled(mBrowseDirButton,true);
            if(mDocView != null){
                mDocView.refresh();
            }

            if(mCatalogMenu != null){
                mCatalogMenu.refactorMenuWith();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mvpPresenter.onPause();
    }

    @Override
    protected void onStop() {
        if (core != null) {
            if (mAlertDialog != null) {
                mAlertDialog.cancel();
                mAlertDialog = null;
            }
            mvpPresenter.destroyAlertWaiter();
            core.stopAlerts();
        }
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (mDocView != null) {
            mDocView.onDestory();
        }
        if (core != null)
            core.onDestroy();
        mvpPresenter.onDestory();
        core = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (core != null && core.hasChanges()) {
            mAlertBuilder = new AlertDialog.Builder(mContext);
            DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (which == AlertDialog.BUTTON_POSITIVE)
                        core.save();

                    finish();
                }
            };
            AlertDialog alert = mAlertBuilder.create();
            alert.setTitle("MuPDF");
            alert.setMessage(getString(R.string.document_has_changes_save_them_));
            alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.yes), listener);
            alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.no), listener);
            alert.show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public MuPdfDocView getDocView() {
        return mDocView;
    }

    @Override
    public EditText getSearchTextView() {
        return mSearchText;
    }

    @Override
    public void createUI() {
        if (core == null)
            return;

        //step 1.First create the document view
        mDocView = new MuPdfDocView(this) {
            @Override
            protected void onMoveToChild(int i) {
                if (core == null)
                    return;
                mPageSlider.setMax((Constants.DOC_PAGE_COUNT - 1) * mvpPresenter.getPageSliderRes());
                mPageSlider.setProgress(i * mvpPresenter.getPageSliderRes());
                super.onMoveToChild(i);
            }

            @Override
            protected void onTapMainDocArea() {
                Log.d(TAG, "onTapMainDocArea: mButtonsVisible=" + mButtonsVisible);
                if (!mButtonsVisible) {
                    showButtons();
                } else {
                    hideButtons();
                }
            }

            /**
             * 滑动文档时,隐藏buttons
             */
            @Override
            protected void onDocMotion() {
                Log.d(TAG, "onDocMotion: ");
                hideButtons();
            }

            @Override
            protected void onHit(Hit item) {
                Log.d(TAG, "onHit: mTopBarMode=" + mTopBarMode.ordinal());
                switch (mTopBarMode) {
                    case Main:
                        if (mButtonsVisible) {
                            //hideButtons();
                            IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
                            if (pageView != null)
                                pageView.deselectAnnotation();
                            return;
                        }
                        Log.d(TAG, "onHit: mButtonsVisible="+mButtonsVisible);
                        if (item == Hit.Annotation) {
                            mTopBarMode = TopBarMode.Annot;
                            mAcceptMode = AcceptMode.Delete;
                            mAnnotTypeText.setText(R.string.delete);
                            mTopMenuSwitcher.setDisplayedChild(mTopBarMode.ordinal());
                            showTopMenuAnim();
                        }
                        break;
                    case Annot:
                        showTopMenuAnim();
                        break;
                    default:
                        /*// Not in annotation editing mode, but the pageview will
                        // still select and highlight hit annotations, so
                        // deselect just in case.
                        mTopBarMode = TopBarMode.Main;
                        mTopMenuSwitcher.setDisplayedChild(0);
                        mAcceptMode = AcceptMode.Okdefault;
                        IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
                        if (pageView != null)
                            pageView.deselectAnnotation();
                        hideButtons();*/
                        break;
                }
            }
        };
        mDocView.setAdapter(new MuPDFPageAdapter(this, core));

        //step 2
        makeButtonsView();
        //step 3
        mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStopTrackingTouch(SeekBar seekBar) {
                mDocView.setDisplayedViewIndex((seekBar.getProgress() + mvpPresenter.getPageSliderRes() / 2) / mvpPresenter.getPageSliderRes());
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                updatePageNumView((progress + mvpPresenter.getPageSliderRes() / 2) / mvpPresenter.getPageSliderRes());
            }
        });

        //step 4
        if (core.fileFormat().startsWith("PDF") && core.isUnencryptedPDF() && !core.wasOpenedFromBuffer()) {
            mAnnotButton.setEnabled(true);
        } else {
            mAnnotButton.setEnabled(false);
        }

        //step 5 Search invoking buttons are disabled while there is no text specified
        mSearchBack.setEnabled(false);
        mSearchFwd.setEnabled(false);
        mSearchBack.setColorFilter(mContext.getResources().getColor(R.color.search_button_enable_color));
        mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

        //step 6  React to interaction with the text widget
        mSearchText.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {
                boolean haveText = s.toString().length() > 0;
                setButtonEnabled(mSearchBack, haveText);
                setButtonEnabled(mSearchFwd, haveText);

                // Remove any previous search results
                if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
                    SearchTaskResult.set(null);
                    mDocView.resetupChildren();
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
            }
        });

        //React to Done button on keyboard
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE)
                    mvpPresenter.search(1);
                return false;
            }
        });

        mSearchText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                    mvpPresenter.search(1);
                return false;
            }
        });

        //目录
        if (core.hasOutline()) {
            mOutlineButton.setEnabled(true);
            if (mCatalogMenu != null) {
                mCatalogMenu.setData(core.getOutline());
            }
        } else {
            setButtonEnabled(mOutlineButton, false);
        }

        //流式/板式区分
        if (!core.isPdfFlow()) {
            setButtonEnabled(mAnnotButton, false);
        }

        if (!Constants.isDoubleScreen) {
            setButtonEnabled(mSwitchScreenButton, false);
        }

        if (!isHideButtons)
            showButtons();

        if (isSearchMode)
            searchModeOn();

        mReadWidget.addView(mDocView);
        setContentView(mReaderRootView);
    }

    private void makeButtonsView() {
        mReaderRootView = (ViewGroup) getLayoutInflater().inflate(R.layout.reader_main_layout, null);
        mDrawerLayout = (DrawerLayout) mReaderRootView.findViewById(R.id.root);
        mCatalogMenu = (CatalogMenu) mReaderRootView.findViewById(R.id.left_menu);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, null, R.string.menu_open, R.string.menu_close) {
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            }
        };
        mDrawerToggle.syncState();
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        mCatalogMenu.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                OutlineActivityData.get().position = position;
                mDrawerLayout.closeDrawer(Gravity.LEFT);
                mDocView.setDisplayedViewIndex(mCatalogMenu.getCurPage(position));
                Log.d(TAG, "onItemClick: position=" + position + " 当前页码:=" + mCatalogMenu.getCurPage(position));
            }
        });

        mReadWidget = (FrameLayout) mReaderRootView.findViewById(R.id.flReadWidget);
        mMenuRoot = (RelativeLayout) mReaderRootView.findViewById(R.id.layout_menu);
        mBottomContainer = (RelativeLayout) mReaderRootView.findViewById(R.id.bottom_container);
        mTopMenuContainer = (RelativeLayout) mReaderRootView.findViewById(R.id.top_menu_container);
        mTopMenuSwitcher = (ViewAnimator) mReaderRootView.findViewById(R.id.top_menu_switcher);
        mTitle = (TextView) mReaderRootView.findViewById(R.id.tv_title);
       /* mTitle.setTextColor(mContext.getResources().getColor(R.color.search_button_disable_color));*/
        mReturn = (ImageView) mReaderRootView.findViewById(R.id.iv_return);
        mReturn.setColorFilter(mContext.getResources().getColor(R.color.common_white));
        mSearchButton = (ImageView) mReaderRootView.findViewById(R.id.iv_search);
        mSearchButton.setColorFilter(mContext.getResources().getColor(R.color.common_white));
        mAnnotTypeText = (TextView) mReaderRootView.findViewById(R.id.annotType);
        mReturn.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);

        mCancelAccept = (ImageButton) mReaderRootView.findViewById(R.id.cancelAcceptButton);
        mSureAccept = (ImageButton) mReaderRootView.findViewById(R.id.acceptButton);
        mCancelAccept.setOnClickListener(this);
        mSureAccept.setOnClickListener(this);

        mPageNum = (TextView) mReaderRootView.findViewById(R.id.progress);

        mIvInk = (ImageButton) mReaderRootView.findViewById(R.id.iv_ink);
        mOutlineButton = (ImageView) mReaderRootView.findViewById(R.id.outline_button);
        mBrowseDirButton = (ImageView) mReaderRootView.findViewById(R.id.browseDirection_button);
        mBrightnessButton = (ImageView) mReaderRootView.findViewById(R.id.brightness_button);
        mClipButton = (ImageView) mReaderRootView.findViewById(R.id.clip_button);
        mSwitchScreenButton = (TextView) mReaderRootView.findViewById(R.id.switchScreen_button);
        mAnnotButton = (ImageView) mReaderRootView.findViewById(R.id.annot_button);
        mIvInk.setOnClickListener(this);
        mOutlineButton.setOnClickListener(this);
        mAnnotButton.setOnClickListener(this);
        mBrowseDirButton.setOnClickListener(this);
        mSwitchScreenButton.setOnClickListener(this);
        mBrightnessButton.setOnClickListener(this);
        mClipButton.setOnClickListener(this);
        setButtonEnabled(mBrightnessButton,true);

        mAnnotMenu = (LinearLayout) mReaderRootView.findViewById(R.id.layout_annot_menu);
        mTvCopy = (LinearLayout) mAnnotMenu.findViewById(R.id.copytext_ly);
        mTvHighlight = (LinearLayout) mAnnotMenu.findViewById(R.id.highlight_ly);
        mTvUnderLine = (LinearLayout) mAnnotMenu.findViewById(R.id.underline_ly);
        mTVStrikeOut = (LinearLayout) mAnnotMenu.findViewById(R.id.strikeOut_ly);
        mTvCopy.setOnClickListener(this);
        mTvHighlight.setOnClickListener(this);
        mTvUnderLine.setOnClickListener(this);
        mTVStrikeOut.setOnClickListener(this);

        mTvPrePage = (TextView) mReaderRootView.findViewById(R.id.tv_pre_page);
        mTvNextPage = (TextView) mReaderRootView.findViewById(R.id.tv_next_page);
        mPageSlider = (SeekBar) mReaderRootView.findViewById(R.id.seekbarQuickRead);
        mTvPrePage.setOnClickListener(this);
        mTvNextPage.setOnClickListener(this);

        mSearchBack = (ImageButton) mReaderRootView.findViewById(R.id.ivSearchBack);
        mSearchFwd = (ImageButton) mReaderRootView.findViewById(R.id.ivSearchForward);
        mSearchText = (EditText) mReaderRootView.findViewById(R.id.searchText);
        mIvCancelSearch = (ImageView) mReaderRootView.findViewById(R.id.ivCancelSearch);
        mSearchBack.setOnClickListener(this);
        mSearchFwd.setOnClickListener(this);
        mIvCancelSearch.setOnClickListener(this);
    }


    @Override
    public void startActivityForResult(Intent intent) {
        startActivityForResult(intent, OUTLINE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case OUTLINE_REQUEST:
                Log.d(TAG, "onActivityResult: resultCode=" + resultCode);
                if (resultCode >= 0) {
                    mDocView.setDisplayedViewIndex(resultCode);
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Object onRetainNonConfigurationInstance() {
        MuPDFCore mycore = core;
        core = null;
        return mycore;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFileName != null && mDocView != null) {
            outState.putString("FileName", mFileName);
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putInt("page" + mFileName, mDocView.getDisplayedViewIndex());
            edit.commit();
        }

        if (!mButtonsVisible)
            outState.putBoolean("ButtonsHidden", true);

        if (mTopBarMode == TopBarMode.Search)
            outState.putBoolean("SearchMode", true);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.browseDirection_button:
                mvpPresenter.switchReadMode();
                break;
            case R.id.switchScreen_button:
                Constants.isShowInMainSceen = !Constants.isShowInMainSceen;
                switchScreen();
                break;
            case R.id.iv_search:
                searchModeOn();
                break;
            case R.id.annot_button:
                onAnnotButtonClick();
                break;
            case R.id.iv_return:
                this.finish();
                break;
            case R.id.outline_button:
                openCatalogMenu();
                break;
            case R.id.tv_pre_page:
                // Activate search invoking buttons
                mSearchBack.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mvpPresenter.search(-1);
                    }
                });

                break;
            case R.id.tv_next_page:
                mSearchFwd.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        mvpPresenter.search(1);
                    }
                });
                break;
            case R.id.copytext_ly:
                onCopyTextButtonClick();
                break;
            case R.id.highlight_ly:
                onHighlightButtonClick();
                break;
            case R.id.underline_ly:
                onUnderlineButtonClick();
                break;
            case R.id.strikeOut_ly:
                onStrikeOutButtonClick();
                break;
            case R.id.ivSearchBack:
                mvpPresenter.search(-1);
                break;
            case R.id.ivSearchForward:
                mvpPresenter.search(1);
                break;
            case R.id.ivCancelSearch:
                ToastUtil.getInstance().showToast("ivCancelSearch");
                searchModeOff();
                break;
            case R.id.cancelAcceptButton:
                onCancelAcceptButtonClick();
                break;
            case R.id.acceptButton:
                onAcceptButtonClick();
                break;
            case R.id.iv_ink:
                onInkButtonClick();
                break;
            case R.id.brightness_button:
                ToastUtil.getInstance().showToast("暂时不支持");
                break;
            case R.id.clip_button:
                ToastUtil.getInstance().showToast("暂时不支持");
                break;
            default:
                break;
        }
    }

    private void openCatalogMenu() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }

    }

    private void setButtonEnabled(View button, boolean enabled) {
        button.setEnabled(enabled);
        if(button instanceof ImageView){
            ((ImageView)button).setColorFilter(enabled ? mContext.getResources().getColor(R.color.search_button_enable_color)
                    : mContext.getResources().getColor(R.color.search_button_disable_color));
        }else if(button instanceof TextView){
            ((TextView)button).setTextColor(enabled ? mContext.getResources().getColor(R.color.search_button_enable_color)
                    : mContext.getResources().getColor(R.color.search_button_disable_color));
        }

    }

    private void showButtons() {
        if (core == null)
            return;
        if (!mButtonsVisible) {
            mButtonsVisible = true;
            // Update page number text and slider
            int index = mDocView.getDisplayedViewIndex();
            updatePageNumView(index);
            mPageSlider.setMax((Constants.DOC_PAGE_COUNT - 1) * mvpPresenter.getPageSliderRes());
            mPageSlider.setProgress(index * mvpPresenter.getPageSliderRes());
            if (mTopBarMode == TopBarMode.Search) {
                mSearchText.requestFocus();
                AppUtils.showKeyboard(mSearchText);
            }
            showTopMenuAnim();
            showBottomAnim();
        }
    }

    private void showTopMenuAnim() {
        if (mTopMenuContainer.getVisibility() != View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, -mTopMenuContainer.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mTopMenuContainer.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mTopMenuContainer.startAnimation(anim);
            mButtonsVisible = true;
        }
    }

    private void showBottomAnim() {
        Log.d(TAG, "showBottomAnim: ",new Throwable());
        if (mBottomContainer.getVisibility() != View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, mBottomContainer.getHeight(), 0);
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mBottomContainer.setVisibility(View.VISIBLE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    // mPageNumberView.setVisibility(View.VISIBLE);
                }
            });
            mBottomContainer.startAnimation(anim);
            mButtonsVisible = true;
        }
    }

    private void showAnnotView() {
        if (mAnnotMenu.getVisibility() != View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, mAnnotMenu.getHeight(), 0);
            anim.setDuration(400);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                    mAnnotMenu.setVisibility(View.VISIBLE);
                  //  mBottomContainer.setVisibility(View.GONE);
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                }
            });
            mAnnotMenu.startAnimation(anim);
            mButtonsVisible = true;
        }
        Log.d(TAG, "showAnnotView: mButtonsVisible="+mButtonsVisible);
    }

    private void hideAnnotView() {
        if (mAnnotMenu.getVisibility() == View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, 0, mAnnotMenu.getHeight());
            anim.setDuration(400);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {

                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mAnnotMenu.setVisibility(View.GONE);
                }
            });
            mAnnotMenu.startAnimation(anim);
        }
    }

    private void hideTopMenuAnim() {
        mTopBarMode = TopBarMode.Main;
        mTopMenuSwitcher.setDisplayedChild(0);
        if (mTopMenuContainer.getVisibility() == View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, 0, -mTopMenuContainer.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    mTopMenuContainer.setVisibility(View.INVISIBLE);
                }
            });
            mTopMenuContainer.startAnimation(anim);
        }

    }

    private void hideBottomAnim() {
        if (mBottomContainer.getVisibility() == View.VISIBLE) {
            Animation anim = new TranslateAnimation(0, 0, 0, mBottomContainer.getHeight());
            anim.setDuration(200);
            anim.setAnimationListener(new Animation.AnimationListener() {
                public void onAnimationStart(Animation animation) {
                }

                public void onAnimationRepeat(Animation animation) {
                }

                public void onAnimationEnd(Animation animation) {
                    Log.d(TAG, "onAnimationEnd: INVISIBLE");
                    mBottomContainer.setVisibility(View.INVISIBLE);
                }
            });
            mBottomContainer.startAnimation(anim);
        }
    }

    private void hideButtons() {
        if (mButtonsVisible) {
            mButtonsVisible = false;
            AppUtils.hideKeyboard(mSearchText);

            Log.d(TAG, "hideButtons: mTopBarMode=" + mTopBarMode.ordinal() + " mAcceptMode=" + mAcceptMode.ordinal());
            if (mTopBarMode == TopBarMode.Main || (mTopBarMode == TopBarMode.Annot && (mAcceptMode == AcceptMode.Okdefault || mAcceptMode == AcceptMode.Delete))) {
                hideTopMenuAnim();
            }
            boolean a = mBottomContainer.getVisibility() == View.VISIBLE;
            Log.d(TAG, "hideButtons: mBottomContainer. isVisible="+a);
            hideBottomAnim();
        }
        hideAnnotView();
    }

    private void searchModeOn() {
        if (mTopBarMode != TopBarMode.Search) {
            mTopBarMode = TopBarMode.Search;
            mSearchText.requestFocus();
            AppUtils.showKeyboard(mSearchText);
            mTopMenuSwitcher.setDisplayedChild(1);
        }
    }

    private void searchModeOff() {
        LogUtils.d(TAG, "searchModeOff: mTopBarMode="+mTopBarMode.ordinal());
        if (mTopBarMode == TopBarMode.Search) {
            mTopBarMode = TopBarMode.Main;
            AppUtils.hideKeyboard(mSearchText);
            mTopMenuSwitcher.setDisplayedChild(0);
            SearchTaskResult.set(null);
            mDocView.resetupChildren();
        }
    }

    public void updatePageNumView(int index) {
        if (core == null || mPageNum == null)
            return;
        mPageNum.setText(index + 1 + "/" + Constants.DOC_PAGE_COUNT);
    }

    private void showInfo(String message) {
        ToastUtil.getInstance().showToast(message);
    }

    //{{start qiaodong
    //add feature switch screen
    public void switchScreen() {
        if (Constants.isShowInMainSceen) {
            if (mPresentation != null && mPresentation.isShowing()) {
                mPresentation.getRootView().removeAllViews();
                mPresentation.dismiss();
                mPresentation = null;

                mDocView.refresh();
                mReaderRootView.setBackground(null);
                mReaderRootView.addView(mReadWidget);
                mReaderRootView.addView(mMenuRoot);
            }
        } else {
            if (mPresentation == null) {
                mPresentation = (PdfPresentation) mvpPresenter.initPresentation();
            }
            mDocView.refresh();
            mReaderRootView.removeAllViews();
            mReaderRootView.setBackgroundColor(getResources().getColor(R.color.common_black));
            mPresentation.getRootView().removeAllViews();
            mPresentation.getRootView().post(new Runnable() {
                @Override
                public void run() {
                    mPresentation.getRootView().addView(mReadWidget);
                    mPresentation.getRootView().addView(mMenuRoot);
                }
            });
        }
    }

    public void onAnnotButtonClick() {
        //mTopBarMode = TopBarMode.Annot;
        hideBottomAnim();
        showAnnotView();
    }

    public void onCopyTextButtonClick() {
        mAcceptMode = AcceptMode.CopyText;
        mAnnotTypeText.setText(getString(R.string.copy_text));
        mDocView.setMode(BaseDocView.Mode.Selecting);
        onAnnotItemClick();
    }

    public void onHighlightButtonClick() {
        mAcceptMode = AcceptMode.Highlight;
        mAnnotTypeText.setText(R.string.highlight);
        mDocView.setMode(BaseDocView.Mode.Selecting);
        onAnnotItemClick();
    }

    public void onUnderlineButtonClick() {
        mAcceptMode = AcceptMode.Underline;
        mAnnotTypeText.setText(R.string.underline);
        mDocView.setMode(BaseDocView.Mode.Selecting);
        onAnnotItemClick();
        hideButtons();
    }

    public void onStrikeOutButtonClick() {
        mAcceptMode = AcceptMode.StrikeOut;
        mAnnotTypeText.setText(R.string.strike_out);
        mDocView.setMode(BaseDocView.Mode.Selecting);
        onAnnotItemClick();
    }

    public void onInkButtonClick() {
        mTopBarMode = TopBarMode.Annot;
        mAcceptMode = AcceptMode.Ink;
        mAnnotTypeText.setText(R.string.ink);
        mDocView.setMode(BaseDocView.Mode.Drawing);
        onAnnotItemClick();
        hideBottomAnim();
    }

    public void onAnnotItemClick() {
        mTopBarMode = TopBarMode.Annot;
        mTopMenuSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        hideAnnotView();
    }

    public void onCancelAcceptButtonClick() {
        IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
        if (pageView != null) {
            pageView.deselectText();
            pageView.deselectAnnotation();
            pageView.cancelDraw();
        }
        mDocView.setMode(BaseDocView.Mode.Viewing);
        mTopBarMode = TopBarMode.Main;
        mAcceptMode = AcceptMode.Okdefault;
        mTopMenuSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        hideButtons();
    }

    public void onAcceptButtonClick() {
        IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
        boolean success = false;
        switch (mAcceptMode) {
            case CopyText: //复制文字
                LogUtils.d(TAG, "OnAcceptButtonClick CopyText");
                if (pageView != null)
                    success = pageView.copySelection();
                mTopBarMode = TopBarMode.Main;
                showInfo(success ? getString(R.string.copied_to_clipboard) : getString(R.string.no_text_selected));
                break;

            case Highlight://高亮
                LogUtils.d(TAG, "OnAcceptButtonClick Highlight");
                if (pageView != null)
                    success = pageView.markupSelection(BaseAnnotation.Type.Highlight);
                mTopBarMode = TopBarMode.Main;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case Underline://下划线
                LogUtils.d(TAG, "OnAcceptButtonClick Underline");
                if (pageView != null)
                    success = pageView.markupSelection(BaseAnnotation.Type.Underline);
                mTopBarMode = TopBarMode.Main;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case StrikeOut: //删除线
                LogUtils.d(TAG, "OnAcceptButtonClick StrikeOut");
                if (pageView != null)
                    success = pageView.markupSelection(BaseAnnotation.Type.StrikeOut);
                mTopBarMode = TopBarMode.Main;
                if (!success)
                    showInfo(getString(R.string.no_text_selected));
                break;

            case Ink://墨迹
                LogUtils.d(TAG, "OnAcceptButtonClick Ink");
                if (pageView != null)
                    success = pageView.saveDraw();
                mTopBarMode = TopBarMode.Main;
                if (!success)
                    showInfo(getString(R.string.nothing_to_save));
                break;
            case Delete:
                onDeleteButtonClick();
                break;
            default:
                break;
        }
        mDocView.setMode(BaseDocView.Mode.Viewing);
        mTopMenuSwitcher.setDisplayedChild(mTopBarMode.ordinal());
        hideButtons();
    }

    //删除已编辑的文本
    public void onDeleteButtonClick() {
        IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deleteSelectedAnnotation();
        mTopBarMode = TopBarMode.Main;
        mAcceptMode = AcceptMode.Okdefault;
        hideButtons();
    }

    public void onCancelDeleteButtonClick(View v) {
        IBaseDocView pageView = (IBaseDocView) mDocView.getDisplayedView();
        if (pageView != null)
            pageView.deselectAnnotation();
        mTopBarMode = TopBarMode.Annot;
        //  mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
    }

    @Override
    public void showOpenErrorDialog(String reason) {
        Resources res = getResources();
        mAlertBuilder = new AlertDialog.Builder(this);
        AlertDialog alert = mAlertBuilder.create();
        alert.setCancelable(false);
        alert.setTitle(String.format(res.getString(R.string.cannot_open_document_Reason), reason));
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        alert.show();
    }

    @Override
    public void requestPassword() {
        mAlertBuilder = new AlertDialog.Builder(this);
        mPasswordView = new EditText(this);
        mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
        mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

        AlertDialog alert = mAlertBuilder.create();
        alert.setTitle(R.string.enter_password);
        alert.setView(mPasswordView);
        alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (core.authenticatePassword(mPasswordView.getText().toString())) {
                            createUI();
                        } else {
                            requestPassword();
                        }
                    }
                });
        alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
        alert.show();
    }

    @Override
    public void showMupdfAlertDialog(final boolean alertsActive, final MuPDFAlert result, final MuPDFAlert.ButtonPressed pressed[]) {
        DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mAlertDialog = null;
                if (alertsActive) {
                    int index = 0;
                    switch (which) {
                        case AlertDialog.BUTTON1:
                            index = 0;
                            break;
                        case AlertDialog.BUTTON2:
                            index = 1;
                            break;
                        case AlertDialog.BUTTON3:
                            index = 2;
                            break;
                    }
                    result.buttonPressed = pressed[index];
                    // Send the user's response to the core, so that it can
                    // continue processing.
                    core.replyToAlert(result);
                    // Create another alert-waiter to pick up the next alert.
                    mvpPresenter.createAlertWaiter();
                }
            }
        };
        mAlertDialog = mAlertBuilder.create();
        mAlertDialog.setTitle(result.title);
        mAlertDialog.setMessage(result.message);
        switch (result.iconType) {
            case Error:
                break;
            case Warning:
                break;
            case Question:
                break;
            case Status:
                break;
        }
        switch (result.buttonGroupType) {
            case OkCancel:
                mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.cancel), listener);
                pressed[1] = MuPDFAlert.ButtonPressed.Cancel;
            case Ok:
                mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.okay), listener);
                pressed[0] = MuPDFAlert.ButtonPressed.Ok;
                break;
            case YesNoCancel:
                mAlertDialog.setButton(AlertDialog.BUTTON3, getString(R.string.cancel), listener);
                pressed[2] = MuPDFAlert.ButtonPressed.Cancel;
            case YesNo:
                mAlertDialog.setButton(AlertDialog.BUTTON1, getString(R.string.yes), listener);
                pressed[0] = MuPDFAlert.ButtonPressed.Yes;
                mAlertDialog.setButton(AlertDialog.BUTTON2, getString(R.string.no), listener);
                pressed[1] = MuPDFAlert.ButtonPressed.No;
                break;
        }
        mAlertDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                mAlertDialog = null;
                if (alertsActive) {
                    result.buttonPressed = MuPDFAlert.ButtonPressed.None;
                    core.replyToAlert(result);
                    mvpPresenter.createAlertWaiter();
                }
            }
        });
        mAlertDialog.show();
    }

}
