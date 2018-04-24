package com.cry.screenop.recorder;

import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.support.v4.app.FragmentActivity;

import com.cry.screenop.MediaProjectionHelper;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.Observable;

import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

/**
 * Created by a2957 on 4/24/2018.
 */

public class MediaCodecPermissionHelper {
    public static Observable<Boolean> requestRecordStorePermissions(FragmentActivity activity) {
        RxPermissions rxPermissions = new RxPermissions(activity);
        return rxPermissions
                .request(WRITE_EXTERNAL_STORAGE, RECORD_AUDIO);
    }

    private static boolean hasRecordStorePermissions(FragmentActivity activity) {
        PackageManager pm = activity.getPackageManager();
        String packageName = activity.getPackageName();
        int granted = pm.checkPermission(RECORD_AUDIO, packageName) | pm.checkPermission(WRITE_EXTERNAL_STORAGE, packageName);
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    public static Observable<MediaProjection> requestMediaProjection(FragmentActivity activity) {
        return Observable
                .just(hasRecordStorePermissions(activity))
                .flatMap(granted -> {
                    if (granted) {
                        return MediaProjectionHelper.requestCapture(activity);
                    } else {
                        return requestRecordStorePermissions(activity)
                                .filter(perGranted -> perGranted)
                                .flatMap(it -> MediaProjectionHelper.requestCapture(activity));
                    }
                });
    }
}
