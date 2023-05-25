package com.wen.w_arcore;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.CameraConfig;
import com.google.ar.core.CameraConfigFilter;
import com.google.ar.core.Config;
import com.google.ar.core.Pose;
import com.google.ar.core.RecordingConfig;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.RecordingFailedException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.wen.w_arcore.ar.support.CameraPermissionHelper;

import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.security.Permission;
import java.util.Collection;

public class MainActivity extends AppCompatActivity {
    private String TAG ="hank";
    private Session session;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ArCoreApk.Availability availability =  ArCoreApk.getInstance().checkAvailability(this);
        try {
//            checkCameraPermission();
//            checkCameraPermissions();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.v(TAG,"availability -> :"+ availability);

//        try {
//            testARCore();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.v(TAG,"testARCore- > e:" + e.toString());
//        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //1.確認相機權限
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }

        checkDeviceSupportAr();
    }


    private boolean mUserRequestedInstall = false;
    //3.確認Ar及裝置是否支援
    private void checkDeviceSupportAr(){
        String msg ="";
        ArCoreApk.Availability availability =  ArCoreApk.getInstance().checkAvailability(this);
        switch (availability){
            case UNKNOWN_ERROR://未知錯誤
                msg = "未知錯誤";
                Log.v(TAG,"checkDeviceSupportAr() UNKNOWN_ERROR");
                break;
            case UNKNOWN_CHECKING://ARCore 未安裝,請確認是否支持AR
                msg = "ARCore 未安裝,請確認是否支持AR";
                Log.v(TAG,"checkDeviceSupportAr() UNKNOWN_CHECKING");
                break;
            case UNKNOWN_TIMED_OUT://ARCore未安裝,設備可能處於離線狀態
                msg = "ARCore未安裝,設備可能處於離線狀態";
                Log.v(TAG,"checkDeviceSupportAr() UNKNOWN_TIMED_OUT");
                break;
            case UNSUPPORTED_DEVICE_NOT_CAPABLE://ARCore不支持此裝備
                msg = "ARCore不支持此裝備";
                Log.v(TAG,"checkDeviceSupportAr() UNSUPPORTED_DEVICE_NOT_CAPABLE");
                break;

            case SUPPORTED_NOT_INSTALLED://支持設備和Android版本，但未安裝ARCore APK。
                msg = "支持設備和Android版本，但未安裝ARCore APK";
                Log.v(TAG,"checkDeviceSupportAr() SUPPORTED_NOT_INSTALLED");
            case SUPPORTED_APK_TOO_OLD://支持設備和安卓版本，安裝一個版本的ARCore APK， 但 ARCore APK 版本太舊了。
                msg = "支持設備和安卓版本，安裝一個版本的ARCore APK， 但 ARCore APK 版本太舊了";
                Log.v(TAG,"checkDeviceSupportAr() SUPPORTED_APK_TOO_OLD");

                try {
                    ArCoreApk.InstallStatus installStatus = ArCoreApk.getInstance().requestInstall(this, true);
                    if(installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED){
                        msg = "重新引導安裝apk";
                    }else {
                        msg = "已更新安裝apk";
                    }
                } catch (UnavailableDeviceNotCompatibleException e) {
                    e.printStackTrace();
                } catch (UnavailableUserDeclinedInstallationException e) {
                    e.printStackTrace();
                }

                break;

            case SUPPORTED_INSTALLED://ARCore 受支持、已安裝並可供使用
                msg = "ARCore 受支持、已安裝並可供使用";
                mUserRequestedInstall = true;
                Log.v(TAG,"checkDeviceSupportAr() SUPPORTED_INSTALLED");
                break;
        }
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();

        if(mUserRequestedInstall){
//            testARCore();
            createSession();
        }else {
            //不支援AR
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //5.ArSession會用大量記憶體,關閉程式記得關閉
        closeSession();
    }

    private void closeSession(){
        if(session != null) session.close();
    }

    //4.創建ArSession
    private void createSession() {
        try {
            // Create a new ARCore session.
             session = new Session(this);

            // Create a session config.
            Config config = new Config(session);

            // Do feature-specific operations here, such as enabling depth or turning on
            // support for Augmented Faces.

            // Configure the session.
            session.configure(config);
            Toast.makeText(this,"createSession Success",Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving supported camera configs: " + e.getMessage());
        }
    }

    //2.當使用者操作是否同意相機權限時
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            //如果沒有權限,跳出提示Toast
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG).show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
                Log.v(TAG,"onRequestPermissionsResult -> !CameraPermissionHelper.shouldShowRequestPermissionRationale() -> launchPermissionSettings()");
            }
            //沒權限結束
            Log.v(TAG,"onRequestPermissionsResult() 沒有權限,跳出提示Toast -> finish()");
            finish();
        }
    }

    private void testARCore()  {
        try {
        Session arSession = new Session(this);

        // Create a CameraConfigFilter and set the desired facing direction
        CameraConfigFilter cameraConfigFilter = new CameraConfigFilter(arSession);
        cameraConfigFilter.setFacingDirection(CameraConfig.FacingDirection.BACK);

        // Get the list of supported camera configs based on the filter

            for (CameraConfig cameraConfig : arSession.getSupportedCameraConfigs(cameraConfigFilter)) {
                // Log the details of each supported camera config
                Log.d(TAG, "Supported Camera Config: " + cameraConfig);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving supported camera configs: " + e.getMessage());
        }

    }
}