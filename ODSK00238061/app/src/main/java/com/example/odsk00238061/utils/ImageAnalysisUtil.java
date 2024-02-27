package com.example.odsk00238061.utils;

import androidx.camera.core.ImageAnalysis;

public class ImageAnalysisUtil {

    public static ImageAnalysis getODSImageAnalysis(){
        return new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
    }

    public static ImageAnalysis getOCRImageAnalysis(){
        return new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
    }
}
