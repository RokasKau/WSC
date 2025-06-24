package com.example.music2;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.VideoView;

public class AspectRatioVideoView extends VideoView {

    private int videoWidth;
    private int videoHeight;

    public AspectRatioVideoView(Context context) {
        super(context);
    }

    public AspectRatioVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AspectRatioVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setVideoSize(int width, int height) {
        videoWidth = width;
        videoHeight = height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        int viewHeight = MeasureSpec.getSize(heightMeasureSpec);

        if (videoWidth == 0 || videoHeight == 0) {
            setMeasuredDimension(viewWidth, viewHeight);
            return;
        }

        float viewAspectRatio = (float) viewWidth / viewHeight;
        float videoAspectRatio = (float) videoWidth / videoHeight;

        if (videoAspectRatio > viewAspectRatio) {
            int scaledHeight = viewHeight;
            int scaledWidth = (int) (viewHeight * videoAspectRatio);
            setMeasuredDimension(scaledWidth, scaledHeight);
        } else {
            int scaledWidth = viewWidth;
            int scaledHeight = (int) (viewWidth / videoAspectRatio);
            setMeasuredDimension(scaledWidth, scaledHeight);
        }
    }
}

