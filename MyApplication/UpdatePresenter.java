package conn.worker.yi_qizhuang.presenter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import conn.worker.yi_qizhuang.module.User;
import conn.worker.yi_qizhuang.presenter.Presenter;
import conn.worker.yi_qizhuang.activity.UpdatePageView;
import conn.worker.yi_qizhuang.api.YiQiZhuangApi;
import conn.worker.yi_qizhuang.bean.AppVersion;
import conn.worker.yi_qizhuang.bean.UpdataInfo;
import conn.worker.yi_qizhuang.util.Constant;
import conn.worker.yi_qizhuang.util.FileUtil;
import conn.worker.yi_qizhuang.util.FileUtils;
import conn.worker.yi_qizhuang.util.HttpUtil;
import conn.worker.yi_qizhuang.util.MD5;
import conn.worker.yi_qizhuang.util.SPUtils;
import conn.worker.yi_qizhuang.util.StringUtil;
import conn.worker.yi_qizhuang.util.ZipUtils;

/**
 * Created by Administrator on 2016/7/18.
 */
public class UpdatePresenter implements Presenter {
    private static final int PROGRESS_VALUE_DOWNLOAD_OK = 70;
    private static final int PROGRESS_VALUE_VALIDATE_OK = 75;
    private static final int PROGRESS_VALUE_UNZIP_OK = 90;
    private static final int PROGRESS_VALUE_REPLACE_OK = 100;

    private boolean isUpdateOK;
    private UpdataInfo updataInfo;
    private boolean needUpdateH5;
    private UpdatePageView mUpdatePageView;
    private Context mContext;
    private HandlerThread workerThread;
    private WorkerHandler workerHandler;
    private UIHandler uiHandler;

    private String strH5ZipDownloadedPath;  //h5的zip包文件全路径
    private String strH5UnZipDirect;        //h5zip将要被解压到的文件夹路径
    private String strH5UnZipFolder;        //h5zip解压后得到的文件夹的路径
    private String strCurrentH5Folder;      //旧版本的H5文件夹路径
    private String strCurrentH5BackFolder;  //在替换时要先备份，这个就是备份文件夹路径
   private Context context;

    private AppVersion mAppVersion = new AppVersion(new AppVersion.IVersionResult(){
        @Override
        public void OnGetVersionResult(UpdataInfo info) {
            updataInfo = info;
            processUpdateInfo(info);
        }

        @Override
        public void OnError() {
            uiHandler.sendEmptyMessageDelayed(UIHandler.MSG_REQUEST_UPDATE_INFO,4*1000);
        }
    });

    private void processUpdateInfo(UpdataInfo info){
        switch (info.getStatus()){
            case AppVersion.CODE_FORCE_UPDATE:
            case AppVersion.CODE_UPDATE_APP:
                mUpdatePageView.showForceUpdateDialog();
                break;
            case AppVersion.CODE_UPDATE_NOTHING:
                mUpdatePageView.setProgressValue(PROGRESS_VALUE_REPLACE_OK);
                gotoNextPage();
                break;
            case AppVersion.CODE_UPDATE_H5:
                updateH5();
                break;
            case AppVersion.CODE_CAN_UPDATE_APP:
                String versonSaved = YiQiZhuangApi.getNewAppVersionCode(mContext);
                if(TextUtils.isEmpty(versonSaved) || StringUtil.isNewVersion(versonSaved,info.getAppMaxV())){     //新的更高的版本
                    YiQiZhuangApi.saveNewAppVersion(mContext,info.getAppMaxV());
                    mUpdatePageView.showPromotionUpdateDialog();
                    needUpdateH5 = false;
                }else{
                    gotoNextPage();
                }
                break;
            case AppVersion.CODE_CAN_UPDATE_APP_H5:
                mUpdatePageView.showPromotionUpdateDialog();
                needUpdateH5 = true;
                break;
        }
    }

    public void setView(UpdatePageView view){
        mUpdatePageView = view;
        mContext = (Context)view;

        workerThread = new HandlerThread("updateThread");
        workerThread.start();
        workerHandler = new WorkerHandler(workerThread.getLooper());

        uiHandler = new UIHandler();
        if(mContext!=null){
            strH5ZipDownloadedPath = YiQiZhuangApi.getH5ZipDownloadDirect(mContext)
                    + File.separator + YiQiZhuangApi.getH5ZipFileName();
            strH5UnZipDirect = YiQiZhuangApi.getH5UnZipDirectPath(mContext);
            strH5UnZipFolder = YiQiZhuangApi.getH5UnZipFolderPath(mContext);
            strCurrentH5Folder = YiQiZhuangApi.getCurrentH5FolderPath(mContext);
            strCurrentH5BackFolder = YiQiZhuangApi.getBackH5FolderPath(mContext);
        }
    }

    //检查是否是第一次运行，如果是就把H5从assets文件夹中复制到/data/data/包名/file/路径
    public void checkH5RuntimeEvirenment(){
        File curH5Folder = new File(strCurrentH5Folder);
        if(curH5Folder.exists()){              //已经拷贝过了
            requestUpdateInfo();
        }else{
            workerHandler.sendEmptyMessage(WorkerHandler.MSG_INIT_H5);
        }
    }

    public void requestUpdateInfo(){
        mAppVersion.getAppUpdateInfo(YiQiZhuangApi.getAppVersion(mContext),YiQiZhuangApi.getH5Version(mContext),
                Constant.PL_USER,Constant.PL_PLATFORM);
    }

    @Override
    public void resume() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void destroy() {
        workerThread.getLooper().quit();
        uiHandler.removeCallbacksAndMessages(null);
    }

    //点击取消升级app后，再判断是否要升级H5
    public void checkH5Update(){
        if(needUpdateH5){
            updateH5();
        }else{
           gotoNextPage();
        }
    }
    //检查下载H5索要的空间
    public boolean checkDownloadCondition() {
        return true;
    }

    //开始H5的升级过程
    private void updateH5(){
        if(checkDownloadCondition()){
            mUpdatePageView.showDownLoadView();
            workerHandler.sendEmptyMessage(WorkerHandler.MSG_CLEAR_ENVIREMENT);
        }else{

        }
    }
    private void gotoNextPage(){
        if(User.getInstance().isLogin()){
            mUpdatePageView.gotoMainPage();
        }else{
            mUpdatePageView.gotoLoginPage();
        }
    }

    //清理升级过程中的各种中间文件。
    private void clearEnvirenment(boolean isUpdateSuccessfull){
        try{
            File h5zip = new File(strH5ZipDownloadedPath);      //如果zip包还在就删除掉
            if(h5zip.exists()){
                h5zip.delete();
            }
            FileUtil.delFolder(strH5UnZipFolder);              //删除解压出来的h5包
            FileUtil.delFolder(strCurrentH5BackFolder);        //删除备份包
        }catch (Exception e){

        }
    }

    //运行于非ui线程,后台校验，解压，和替换
    private class WorkerHandler extends Handler {
        public static final int MSG_INIT_H5 = 87;
        public static final int MSG_CHECK_PACKAGE = 88;
        public static final int MSG_UNZIP_PACKAGE = 89;
        public static final int MSG_REPLACE_PACKAGE = 90;
        public static final int MSG_CLEAR_ENVIREMENT = 91;

        public WorkerHandler(Looper looper){
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data;
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_INIT_H5:                   //第一次运行，从Assets拷贝到file文件夹下。
                    try{
                        FileUtil.mkdir(strCurrentH5Folder);
                        FileUtil.copyAssetsDir(mContext.getAssets(),"h5_library",strCurrentH5Folder);
                        FileUtil.copyAssetsDir(mContext.getAssets(),"merchant",strCurrentH5Folder);
                        uiHandler.sendEmptyMessage(UIHandler.MSG_REQUEST_UPDATE_INFO);
                    }catch (Exception e){
                        uiHandler.sendEmptyMessage(UIHandler.MSG_REQUEST_UPDATE_INFO);
                    }
                    break;
                case MSG_CHECK_PACKAGE:               //zip包的md5值校验
                    String md5Code = MD5.md5sum(strH5ZipDownloadedPath);//"ac830d6a21d6c0fd34a91ad05ba457d8"
                    if(TextUtils.equals(md5Code,updataInfo.getMd5Code())){//updataInfo.getMd5Code()
                        uiHandler.sendEmptyMessage(UIHandler.MSG_CHECK_PACKAGE_OK);
                    }else{
                        File h5zip = new File(strH5ZipDownloadedPath);      //如果zip包还在就删除掉
                        if(h5zip.exists()){
                            h5zip.delete();
                        }
                        uiHandler.sendEmptyMessage(UIHandler.MSG_CHECK_PACKAGE_ERROR);
                    }
                    break;
                case MSG_UNZIP_PACKAGE:
                    try {
                        FileUtils.mkdir(strH5UnZipDirect + File.separator + "app");
                        ZipUtils.UnZipFolder(strH5ZipDownloadedPath,strH5UnZipDirect + File.separator + "app");
                        uiHandler.sendEmptyMessage(UIHandler.MSG_UNZIP_PACKAGE_OK);
                    } catch (Exception e) {
                        e.printStackTrace();
                        File unzipFolder = new File(strH5UnZipFolder);      //解压H5后的文件夹，
                        if(unzipFolder.exists()){
                            unzipFolder.delete();
                        }
                        uiHandler.sendEmptyMessage(UIHandler.MSG_UNZIP_PACKAGE_ERROR);
                    }
                    break;
                case MSG_REPLACE_PACKAGE:
                    try{
                        File curH5Folder = new File(strCurrentH5Folder);
                        if(curH5Folder.exists()){                           //如果存在老的H5文件夹就先备份
                            FileUtils.copyFolder(strCurrentH5Folder,strCurrentH5BackFolder);
                        }

                        if(TextUtils.equals("Y",updataInfo.getIsAll())){        //全量升级
                            if(curH5Folder.exists()){
                                FileUtils.delFolder(strCurrentH5Folder);
                            }
                        }
                        FileUtils.copyFolder(strH5UnZipFolder,strCurrentH5Folder);  //覆盖原有H5包
                        clearEnvirenment(true);
                        uiHandler.sendEmptyMessage(UIHandler.MSG_REPLACE_PACKAGE_OK);
                    }catch (Exception e){       //替换出错就回滚
                        try{
                            File backFolder = new File(strCurrentH5BackFolder);
                            if(backFolder.exists()){
                                FileUtils.delFolder(strCurrentH5Folder);
                                backFolder.renameTo(new File(strCurrentH5Folder));
                            }
                        }catch (Exception e1){

                        }
                        uiHandler.sendEmptyMessage(UIHandler.MSG_REPLACE_PACKAGE_ERROR);
                    }
                    break;
                case MSG_CLEAR_ENVIREMENT:
                    clearEnvirenment(isUpdateOK);
                    uiHandler.sendEmptyMessage(UIHandler.MSG_ClEAR_ENVIREMENT_OK);
                    break;
            }
        }
    }

    private class UIHandler extends Handler{
        public static final int MSG_REQUEST_UPDATE_INFO = 87;
        public static final int MSG_CHECK_PACKAGE_OK = 88;
        public static final int MSG_CHECK_PACKAGE_ERROR = 89;
        public static final int MSG_UNZIP_PACKAGE_OK = 90;
        public static final int MSG_UNZIP_PACKAGE_ERROR = 91;
        public static final int MSG_REPLACE_PACKAGE_OK = 92;
        public static final int MSG_REPLACE_PACKAGE_ERROR = 93;
        public static final int MSG_ClEAR_ENVIREMENT_OK = 94;
        public static final int MSG_DOWNLOAD_H5ZIP = 95;
        public static final int MSG_DOWNLOAD_H5ZIP_OK = 97;
        public static final int MSG_DOWNLOAD_H5ZIP_ERROR = 98;
        public static final int MSG_UPDATE_RETRY = 96;
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_REQUEST_UPDATE_INFO:
                    requestUpdateInfo();
                    break;
                case MSG_CHECK_PACKAGE_OK:
                    mUpdatePageView.setProgressValue(PROGRESS_VALUE_VALIDATE_OK);
                    workerHandler.sendEmptyMessage(WorkerHandler.MSG_UNZIP_PACKAGE);
                    break;
                case MSG_CHECK_PACKAGE_ERROR:
                    this.sendEmptyMessage(MSG_UPDATE_RETRY);
                    break;
                case MSG_UNZIP_PACKAGE_OK:
                    mUpdatePageView.setProgressValue(PROGRESS_VALUE_UNZIP_OK);
                    workerHandler.sendEmptyMessage(WorkerHandler.MSG_REPLACE_PACKAGE);
                    break;
                case MSG_UNZIP_PACKAGE_ERROR:
                    this.sendEmptyMessage(MSG_UPDATE_RETRY);
                    break;
                case MSG_REPLACE_PACKAGE_OK:
                    isUpdateOK = true;
                    gotoNextPage();
                    break;
                case MSG_REPLACE_PACKAGE_ERROR:
                    this.sendEmptyMessage(MSG_UPDATE_RETRY);
                    break;
                case MSG_ClEAR_ENVIREMENT_OK:
                    this.sendEmptyMessage(MSG_DOWNLOAD_H5ZIP);
                    break;
                case MSG_DOWNLOAD_H5ZIP:
                    isUpdateOK = false;
                    downLoadH5Zip(updataInfo.getH5Url());
                    break;
                case MSG_DOWNLOAD_H5ZIP_OK:
                    mUpdatePageView.setProgressValue(PROGRESS_VALUE_DOWNLOAD_OK);
                    workerHandler.sendEmptyMessage(WorkerHandler.MSG_CHECK_PACKAGE);
                    break;
                case MSG_DOWNLOAD_H5ZIP_ERROR:
                    mUpdatePageView.setProgressValue(0);
                    this.sendEmptyMessage(MSG_UPDATE_RETRY);
                    break;
                case MSG_UPDATE_RETRY:
                    mUpdatePageView.setProgressValue(0);
                    updateH5();
                    break;
            }
        }
    }

    private void downLoadH5Zip(String url){
        String urlNew = url.replace("https://", "http://");
        HttpUtil.get(urlNew, new FileAsyncHttpResponseHandler(new File(strH5ZipDownloadedPath)) {
            @Override
            public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, Throwable throwable, File file) {
                uiHandler.sendEmptyMessage(UIHandler.MSG_DOWNLOAD_H5ZIP_ERROR);
            }

            @Override
            public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, File file) {
                uiHandler.sendEmptyMessage(UIHandler.MSG_DOWNLOAD_H5ZIP_OK);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                super.onProgress(bytesWritten, totalSize);
                if(totalSize <= 0){
                    return;
                }
                long value = bytesWritten*PROGRESS_VALUE_DOWNLOAD_OK/totalSize;
                mUpdatePageView.setProgressValue((int)value);
            }
        });
    }
}
