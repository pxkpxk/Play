package com.dlighttech.camera2;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import com.dlighttech.camera2.camera.CameraController;
import com.dlighttech.camera2.camera.CameraControllerV1;
import com.dlighttech.camera2.camera.CameraControllerV2;
import com.dlighttech.camera2.dimenco3dapi.Factory;
import com.dlighttech.camera2.dimenco3dapi.IEyeTracker;
import com.dlighttech.camera2.view.TestTwoSurfaceView;

public class MainActivity extends Activity {

    private static String TAG = "DualCamera";
    private static int CAMERA_COUNT = Camera.getNumberOfCameras();

    private CameraControllerV2 mCameraControllerLeft;
    private TextureView mTextureViewLeft;
    private SurfaceTexture mSurfaceTextureLeft;

    private CameraControllerV2 mCameraControllerRight;
    private TextureView mTextureViewRight;
    private SurfaceTexture mSurfaceTextureRight;

    private RelativeLayout mMainLayout;
    private TestTwoSurfaceView mSurfaceView;


//    private Camera3DSurfaceView mSurfaceView;
//    private TwoImage3DSurfaceView mSurfaceView;

    private IEyeTracker mEyeTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ViewGroup.LayoutParams vlp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);

        //mSurfaceView = new TestTwoSurfaceView(this, null);
        //mSurfaceView = new Camera3DSurfaceView(this);
//        mSurfaceView = new TwoImage3DSurfaceView(this);
//        mSurfaceView.setLayoutParams(vlp);
//
//        mEyeTracker = Factory.getEyetracker(this);
//
//        mSurfaceView.SetEyeTracker(mEyeTracker);

        //GradientDrawable border = new GradientDrawable();
        //border.setColor(0xFFFFFFFF); //white background
        //border.setStroke(1, 0xFF000000); //black border with full opacity

        //mSurfaceView.setBackground(border);





        mMainLayout = (RelativeLayout)findViewById(R.id.mainLayout);
        mTextureViewLeft = (TextureView) findViewById(R.id.textureViewLeft);
        mTextureViewRight = (TextureView) findViewById(R.id.textureViewRight);

        mSurfaceView = new TestTwoSurfaceView(this,null);



        mSurfaceView.setSurfaceReadyListener(new TestTwoSurfaceView.SurfaceReadyListener() {
            @Override
            public void OnSurfaceReady() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mSurfaceTextureLeft = mSurfaceView.getSurfaceTextureLeft();
                        mSurfaceTextureRight = mSurfaceView.getSurfaceTextureRight();

                        mCameraControllerLeft = new CameraControllerV2();
                        mCameraControllerLeft.init(MainActivity.this);
                       mCameraControllerLeft.setCameraIndex(0);     //
                        mCameraControllerLeft.setSurfaceTexture(mSurfaceTextureLeft);
                       mCameraControllerLeft.setTextureView(mTextureViewLeft);  //
                        mCameraControllerLeft.openCamera(0);

//
                        mCameraControllerRight = new CameraControllerV2();
                        mCameraControllerRight.init(MainActivity.this);
                        mCameraControllerRight.setCameraIndex(1);      //
                        mCameraControllerRight.setTextureView(mTextureViewRight);   //
                        mCameraControllerRight.setSurfaceTexture(mSurfaceTextureRight);
                        mCameraControllerRight.openCamera(1);
                    }
                });
            }
        });
//

        mMainLayout.addView(mSurfaceView, 0);

        ImageButton shootButton = (ImageButton)findViewById(R.id.shootButton);
        shootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraControllerLeft.isReady()) {
                    mCameraControllerLeft.takePicture("test");
                }

                if (mCameraControllerRight.isReady()) {
                    mCameraControllerRight.takePicture("test");
                }
            }
        });

    }
}