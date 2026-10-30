package com.arabatube.app;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarActivityService;

/** Android Auto bu servisi bağlar ve döndürdüğümüz CarActivity'yi araç ekranına çizer. */
public class CarService extends CarActivityService {
    @Override
    public Class<? extends CarActivity> getCarActivity() {
        return TubeCarActivity.class;
    }
}
