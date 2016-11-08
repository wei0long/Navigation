package com.dragon.navigation;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.help.Inputtips;
import com.amap.api.services.help.InputtipsQuery;
import com.amap.api.services.help.Tip;
import com.dragon.navigation.Control.Control;
import com.dragon.navigation.Control.Data;
import com.dragon.navigation.use.DataSmoother;
import com.dragon.navigation.use.SampleApplicationGLView;
import com.dragon.navigation.use.Texture;
import com.dragon.navigation.util.AMapUtil;
import com.dragon.navigation.util.BlankRender;
import com.dragon.navigation.util.MyTextView;
import com.dragon.navigation.util.NewWidget;
import com.dragon.navigation.util.ToastUtil;
import com.dragon.navigation.util.scrollerlayout;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimerTask;
import java.util.Vector;

/**
 * This file created by dragon on 2016/7/26 19:40,belong to com.dragon.arnav.basicFuction.camera2 .
 */
public class Main extends Activity implements View.OnClickListener, SensorEventListener,
        TextWatcher, Inputtips.InputtipsListener {
    private static final String TAG = "Main";
    private ArPoiSearch mArPoiSearch;
    private Location mLocation;
    private Vector<Texture> mTextures;
    private SampleApplicationGLView mGlView;
    private MainRenderer mRenderer;
    //    private EditText editCity;
    private TextureView textureView;
    //用SparseIntArray来代替hashMap，进行性能优化。
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    private NewWidget mNewWidget;
    private static final int NUM_SMOOTH_SAMPLES = 4;
    private static final DataSmoother.Smoothing SMOOTHING = DataSmoother.Smoothing.AVERAGE;
    private final DataSmoother gravitySmoother = new DataSmoother(
            NUM_SMOOTH_SAMPLES, 3);
    private final DataSmoother magneticFieldSmoother = new DataSmoother(
            NUM_SMOOTH_SAMPLES, 3);

    //    静态初始化块
    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //    以下定义是摄像头相关和摄像头会话相关
    private String cameraId;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession cameraCaptureSessions;
    protected CaptureRequest.Builder captureRequestBuilder;
    private Size imageDimension;
    private ImageReader imageReader;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;
    //    *****************************************************************
//    指南针
    private ImageView imgZnz;
    //    搜素关键字
    private AutoCompleteTextView mSearchText;
    //    当前城市，自定义控件，为了获取焦点
    private MyTextView mCurrentCity;
    // 搜索
    private TextView btnSearch;
    //    商家
    private TextView mMerchant;
    private Button btnMy;
    // 要输入的poi搜索关键字
    private String keyWord = "";

    //****************************************************************
    float currentDegree = 0f;
    SensorManager mSensorManager;
    private Sensor accelerometer;//加速度传感器
    private Sensor magnetic;//地磁传感器



    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] motion = new float[3];
    private float[] gravity = new float[3];//低通滤波后的重力
    private float[] linear_acceleration=new float[3];//不含重力的手机加速度
    //***********新建子线程更新UI**********************
    private static final int UPDATE_TEXT = 1;
    private Handler mUiHandler = new MyUiHandler();
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private TextView mydegree;
    private  scrollerlayout layout_sub_Lin;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_camera2);
        Log.i("dcdv","dvdvd");
        textureView = (TextureView) findViewById(R.id.texture);
      //  ViewStub myviewstub =(ViewStub)findViewById(R.id.lanshouqian);

     initnewview();

        LinearLayout BlankLayout = (LinearLayout) View.inflate(this, R.layout.blanklayout,
                null);
        assert textureView != null;
        textureView.setSurfaceTextureListener(textureListener);
//********************Location***************************
        mCurrentCity = (MyTextView) findViewById(R.id.current_city);
        LatLonPoint lp = new LatLonPoint(0, 0);
        mLocation = new Location(Main.this, savedInstanceState, mCurrentCity);
        mLocation.initLoction();

        LinearLayout happy = (LinearLayout) View.inflate(this, R.layout.succees,
                null);
        mydegree = (TextView)happy.findViewById(R.id.degree);
        mydegree.setText("");


        BlankLayout.setVisibility(View.VISIBLE);
       /* LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(360, 600);
        layoutParams.setMargins(200,250,0,0);*/
        // buttonattack.setLayoutParams(layoutParams);
       // layoutParams.gravity= Gravity.LEFT;
        BlankLayout.addView(happy,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        happy.setPadding(200,100,0,0);
        addContentView(BlankLayout,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        BlankLayout.bringToFront();
//        ToastUtil.show(Main.this,mLocation.getLp());
//********************POI********************************

        mTextures = new Vector<Texture>();
        loadTextures();

//两个按键，搜索和我的监听
        btnMy = (Button) findViewById(R.id.My);
        btnMy.setOnClickListener(this);
        btnSearch = (TextView) findViewById(R.id.btn_search);
        btnSearch.setOnClickListener(this);


//        ToastUtil.show(Main.this,mLocation.getLocationResult().getAddress());
        imgZnz = (ImageView) findViewById(R.id.Compass);
        imgZnz.setAlpha(0.2f);
//        ****************************************************
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
//        Log.e("dragon",deviceSensors+"");
//        实例化加速度传感器
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        实例化地磁传感器
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        calculateOrientation();
//        *******************************************************
        mSearchText = (AutoCompleteTextView) findViewById(R.id.searchText);
        mSearchText.addTextChangedListener(this);// 添加文本输入框监听事件
        mMerchant = (TextView) findViewById(R.id.merchant);
//以下动态加载
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();





        Thread mThread = new Thread(myRunnable);
        mThread.start();
    }
    public void initnewview(){
       /* ViewStub myviewstub =(ViewStub)findViewById(R.id.lanshouqian);
        View wen1=(View) myviewstub.inflate();
        NewWidget wen=(NewWidget)findViewById(R.id.newwidget1);
        // LayoutInflater inflater = LayoutInflater.from(this);
        if(wen==null){
            Log.i("fdf","fdfsdfsdfdsfds");
        }
    //    LinearLayout.LayoutParams LP_wen = new LinearLayout.LayoutParams(200, 150);
        wen.setTitle("gg");
        wen.setContent("bb");
        wen.setTitleBackgroundColor(Color.RED);
        wen.setContentBackgroundColor(Color.GRAY);
        wen.setTextSize(40);
        wen.setTextColor(Color.GREEN);

     //   wen.setLayoutParams(LP_wen);
        wen.smoothScrollBy(-100,-100,20000);*/
        scrollerlayout paper=new scrollerlayout(this);
        RelativeLayout orange = (RelativeLayout) View.inflate(this, R.layout.moveanywhere,
                null);
        ViewStub tess=(ViewStub)orange.findViewById(R.id.wang);
        View wen2=(View) tess.inflate();
        NewWidget wen1=(NewWidget)orange.findViewById(R.id.newwidget1);

        wen1.setTitle("fvfbf");
        wen1.setContent("bb");
        wen1.setTitleBackgroundColor(Color.RED);
        wen1.setContentBackgroundColor(Color.GRAY);
        wen1.setTextSize(40);
        wen1.setTextColor(Color.GREEN);
        LinearLayout.LayoutParams LP_wen = new LinearLayout.LayoutParams(200, 150);
        wen1.setLayoutParams(LP_wen);
        Log.i("hhh",((ViewGroup)wen1.getParent()).toString());
        LinearLayout liang=(LinearLayout)orange.findViewById(R.id.liang);
        liang.removeView(wen1);
       // ((ViewGroup)wen1.getParent()).removeView(wen1);
        paper.addView(wen1);
        addContentView(paper,new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        paper.smoothScrollTo(-1000,-1000,20000);

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.dragon.navigation/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }


    // 定义一个内部类继承自Handler，并且覆盖handleMessage方法用于处理子线程传过来的消息
    class MyUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case UPDATE_TEXT: // 接受到消息之后，对UI控件进行修改
                    mMerchant.setText("商家：11");
                    break;

                default:
                    break;
            }
        }
    }


    //    定义了一个独立的监听类
    TextureView.SurfaceTextureListener textureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            //open your camera here
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            // Transform you image captured size according to the surface width and height
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        }
    };

    @Override
    public void afterTextChanged(Editable s) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String newText = s.toString().trim();
        if (!AMapUtil.IsEmptyOrNullString(newText)) {
            InputtipsQuery inputquery = new InputtipsQuery(newText, "");
            Inputtips inputTips = new Inputtips(Main.this, inputquery);
            inputTips.setInputtipsListener(this);
            inputTips.requestInputtipsAsyn();
        }
    }

    @Override
    public void onGetInputtips(List<Tip> tipList, int rCode) {
        if (rCode == 1000) {// 正确返回
            List<String> listString = new ArrayList<String>();
            for (int i = 0; i < tipList.size(); i++) {
                listString.add(tipList.get(i).getName());
            }
            ArrayAdapter<String> aAdapter = new ArrayAdapter<String>(
                    getApplicationContext(),
                    R.layout.route_inputs, listString);
            mSearchText.setAdapter(aAdapter);
            aAdapter.notifyDataSetChanged();
        } else {
            ToastUtil.showerror(this, rCode);
        }

    }

    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
//        摄像头打开激发该方法
        public void onOpened(CameraDevice camera) {
            Log.e(TAG, "onOpened");
            cameraDevice = camera;
//            开始预览
            try {
                createCameraPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //        摄像头断开连接时的方法
        @Override
        public void onDisconnected(CameraDevice camera) {
            cameraDevice.close();
            Main.this.cameraDevice = null;
        }

        //        打开摄像头出现错误时激发方法
        @Override
        public void onError(CameraDevice camera, int error) {
            cameraDevice.close();
            Main.this.cameraDevice = null;
        }
    };

    protected void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("Camera Background");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    protected void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void initCamera2() {
        if (null == cameraDevice) {
            Log.e(TAG, "cameraDevice is null");
//            如果没打开则返回
            return;
        }
//        摄像头管理器，专门用于检测、打开系统摄像头，并连接CameraDevices.
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.e(TAG, "cameraDevice.getId " + cameraDevice.getId());//查看选中的摄像头ID
//            获取指定摄像头的相关特性（此处摄像头已经打开）
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraDevice.getId());

//            定义图像尺寸
            Size[] jpegSizes;
//                获取摄像头支持的最大尺寸
            jpegSizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP).getOutputSizes(ImageFormat.JPEG);
////            初始化一个尺寸
//            int width = 640;
//            int height = 480;

            int width = jpegSizes[0].getWidth();
            int height = jpegSizes[0].getHeight();

//            创建一个ImageReader对象，用于获得摄像头的图像数据
            ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
//动态数组
            List<Surface> outputSurfaces = new ArrayList<Surface>(2);

            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(textureView.getSurfaceTexture()));
//生成请求对象（TEMPLATE_STILL_CAPTURE此处请求是拍照）
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
//            将ImageReader的surface作为captureBuilder的输出目标
            captureBuilder.addTarget(reader.getSurface());
////设置自动对焦模式
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
////            设置自动曝光模式
//            captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);//推荐采用这种最简单的设置请求模式
            // 获取设备方向
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            根据设置方向设置照片显示的方向
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATIONS.get(rotation));

//            reader.setOnImageAvailableListener(readerListener, mBackgroundHandler);
            //拍照开始或是完成时调用，用来监听CameraCaptureSession的创建过程
            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
//                    Toast.makeText(Main.this, "Saved:" + file, Toast.LENGTH_SHORT).show();
                    createCameraPreview();
                }
            };

            cameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    try {
                        session.capture(captureBuilder.build(), captureListener, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void createCameraPreview() {
        try {
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
//            设置默认的预览大小
            texture.setDefaultBufferSize(imageDimension.getWidth(), imageDimension.getHeight());
            Surface surface = new Surface(texture);
//            请求预览
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            captureRequestBuilder.addTarget(surface);
//            创建cameraCaptureSession,第一个参数是图片集合，封装了所有图片surface,第二个参数用来监听这处创建过程
            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    //The camera is already closed
                    if (null == cameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    cameraCaptureSessions = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(Main.this, "Configuration change", Toast.LENGTH_SHORT).show();
                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void openCamera() {
//        实例化摄像头
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "is camera open");
        try {
//            指定要打开的摄像头
            cameraId = manager.getCameraIdList()[0];
//            获取打开摄像头的属性
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
//The available stream configurations that this camera device supports; also includes the minimum frame durations and the stall durations for each format/size combination.
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            assert map != null;
            imageDimension = map.getOutputSizes(SurfaceTexture.class)[0];
//            打开摄像头，第一个参数代表要打开的摄像头，第二个参数用于监测打开摄像头的当前状态，第三个参数表示执行callback的Handler,
//            如果程序希望在当前线程中执行callback，像下面的设置为null即可。
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, stateCallback, null);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera 1");
    }

    protected void updatePreview() {
        if (null == cameraDevice) {
            Log.e(TAG, "updatePreview error, return");
        }
//        设置模式为自动
        captureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            cameraCaptureSessions.setRepeatingRequest(captureRequestBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        if (null != cameraDevice) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (null != imageReader) {
            imageReader.close();
            imageReader = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //        注册监听事件
        mSensorManager.registerListener(Main.this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(Main.this, magnetic, SensorManager.SENSOR_DELAY_UI);
        mLocation.onResume();
        Log.e(TAG, "onResume");
        startBackgroundThread();
        if (textureView.isAvailable()) {
            Log.e(TAG, "textureViewAvailable");
//            openCamera();
            initCamera2();
        } else {
            textureView.setSurfaceTextureListener(textureListener);
        }

        if (mGlView != null) {


            mGlView.setVisibility(View.VISIBLE);//可見

            mGlView.setVisibility(View.VISIBLE);

            mGlView.onResume();
        }
    }

    @Override
    protected void onPause() {
        Log.e(TAG, "onPause");
        mSensorManager.unregisterListener(this);
        closeCamera();
        stopBackgroundThread();
        super.onPause();

        gravitySmoother.clear();
        magneticFieldSmoother.clear();
        mLocation.onPause();
        mLocation.deactivate();
    }

    @Override
    protected void onStop() {
        mSensorManager.unregisterListener(this);
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.dragon.navigation/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }


    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mLocation.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocation.onDestroy();
    }


    private float calculateOrientation() {
//        SensorManager mSensorMgr = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
//        HandlerThread mHandlerThread = new HandlerThread("sensorThread");
//        mHandlerThread.start();
//        Handler handler = new Handler(mHandlerThread.getLooper());
//        mSensorMgr.registerListener(this, mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
//                SensorManager.SENSOR_DELAY_FASTEST, handler);
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues,
                magneticFieldValues);
        SensorManager.getOrientation(R, values);
        values[0] = (float) Math.toDegrees(values[0]);
        return values[0];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravitySmoother.put(event.values);
            gravitySmoother.getSmoothed(accelerometerValues, SMOOTHING);

            final float alpha=0.8f;

            for(int i=0;i<3;i++){
                gravity[i]=alpha*gravity[i]+(1-alpha)*accelerometerValues[i];
                linear_acceleration[i]=accelerometerValues[i]-gravity[i];
            }



        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldSmoother.put(event.values);
            magneticFieldSmoother.getSmoothed(magneticFieldValues, SMOOTHING);
        }
        int degree = (int)calculateOrientation();

        RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        if (Data.getfirstdegree == false && Data.modelDrawed == true) {
            Data.firstdegree = -degree;
            Data.getfirstdegree = true;
        }
        Data.predegree=(currentDegree-Data.firstdegree);//度数差
        TranslateAnimation ta = new TranslateAnimation(0, Data.predegree*15, 0,0);
       // mNewWidget.scrollTo(Data.predegree*15,0);
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//        imgZnz.setRotate(ORIENTATIONS.get(rotation),50,50);
        ra.setDuration(200);
      //  layout_sub_Lin.startAnimation(ta);

        imgZnz.startAnimation(ra);
        currentDegree = -degree;




    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //    检测搜索关键字是否为空，不为空就开始搜索
    public void searchButton() {
        keyWord = AMapUtil.checkEditText(mSearchText);
        if ("".equals(keyWord)) {
            ToastUtil.show(this, "请输入搜索关键字");
            return;
        } else {
//            默认搜索范围是深圳市，为空是全国
            final LinearLayout lin = (LinearLayout) findViewById(R.id.list_Lin);
            mArPoiSearch = new ArPoiSearch(this, keyWord, "", "深圳市", lin);
            mArPoiSearch.doSearch();
        }
    }


    //    检测所有按键
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search:
//            ToastUtil.show(Main.this,"搜索");
                searchButton();
//            更新UI
                new Thread(new Runnable() {
                    @Override
                    public void run() { //　新建一个线程，并新建一个Message的对象，是用Handler的对象发送这个Message
                        Message msg = new Message();
                        msg.what = UPDATE_TEXT; // 用户自定义的一个值，用于标识不同类型的消息
                        mUiHandler.sendMessage(msg); // 发送消息
                    }
                }).start();
                break;
            case R.id.My:
//            ToastUtil.show(Main.this,"我的");
                Intent intent = new Intent(Main.this, MySetting.class);
                startActivity(intent);
//            Toast.makeText(Main.this, "我的", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private Runnable myRunnable = new Runnable() {
        public void run() {

            while (true) {
                try {
                    Thread.sleep(500);
                }catch (InterruptedException e){

                }
                int degree=(int)calculateOrientation();

                    Data.degree=-degree;
               /* layout_sub_Lin.offsetLeftAndRight(Data.predegree*15);*/
                //layout_sub_Lin.scrollTo(Data.predegree*15,0);

                Message message2 = new Message();
                message2.what = 2;
                handler.sendMessage(message2);

                if (Control.displayPoi == false) {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                }
            }
        }

    };
    TimerTask task = new TimerTask() {

        public void run() {

            //execute the task

        }

    };


    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    Control.displayPoi = true;
                    break;
                case 2:
                    mydegree.setText("predegree="+Data.predegree+"\n"+
                            "gravity[0]="+gravity[0]+"\n"+
                            "gravity[1]"+gravity[1]+"\n"+
                            "gravity[2]"+gravity[2]+"\n"+
                            "accelerometerValues[0]="+accelerometerValues[0]+"\n"+
                            "accelerometerValues[1]"+accelerometerValues[1]+"\n"+
                            "accelerometerValues[2]"+accelerometerValues[2]+"\n"+
                            " linear_acceleration[0]"+ linear_acceleration[0]+"\n"+
                            " linear_acceleration[1]"+ linear_acceleration[1]+"\n"+
                            " linear_acceleration[2]"+ linear_acceleration[2]+"\n"+
                            "mNewWidget X="+mNewWidget.getX()+" Y="+mNewWidget.getY()+"\n"+
                            "Curr X="+mNewWidget.mScroller.getCurrX()+" Y="+mNewWidget.mScroller.getCurrY()+"\n"+
                            "FinalX="+mNewWidget.mScroller.getFinalX()+" Y="+mNewWidget.mScroller.getFinalY()+"\n"+
                            "OFFSET     "+mNewWidget.mScroller.isFinished()
                    );
                    break;
            }
            super.handleMessage(msg);
        }

    };


    private void loadTextures() {


     /*   gg.setVisibility(View.INVISIBLE);*/

    /*    happy.setDrawingCacheEnabled(true);
        happy.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        happy.buildDrawingCache();
        happy.layout(0, 0, happy.getMeasuredWidth(),
                happy.getMeasuredHeight());
        Bitmap bitmap = happy.getDrawingCache();
        Bitmap bitmap2 = Bitmap.createBitmap(bitmap);
        saveScreenShot(bitmap2,"hou"+".png");*/

        mNewWidget = new NewWidget(this);
        layout_sub_Lin = new scrollerlayout(this);
//        layout_sub_Lin.setBackgroundColor(Color.argb(0xff, 0x00, 0xff, 0x00));

        RelativeLayout hh=new RelativeLayout(this);
        LinearLayout.LayoutParams LP_WW = new LinearLayout.LayoutParams(200, 150);
        mNewWidget.setTitle("蓝瘦");
        mNewWidget.setContent("香菇");
        mNewWidget.setTitleBackgroundColor(Color.RED);
        mNewWidget.setContentBackgroundColor(Color.GRAY);
        mNewWidget.setTextSize(40);
        mNewWidget.setTextColor(Color.GREEN);
        mNewWidget.setLayoutParams(LP_WW);
        layout_sub_Lin.setVisibility(View.VISIBLE);
        hh.addView(mNewWidget);
        hh.setPadding(440,885,0,0);

        layout_sub_Lin.addView(hh);
        addContentView(layout_sub_Lin, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        layout_sub_Lin.bringToFront();
      //  mTextures.add(Texture.loadTextureFromView(layout_sub_Lin, "lanshou"));
        layout_sub_Lin.smoothScrollBy(250,1000,50000);



    }


}
