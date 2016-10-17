package com.dragon.navigation;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import com.dragon.navigation.Control.Data;
import com.dragon.navigation.use.CubeObject;
import com.dragon.navigation.use.CubeShaders;
import com.dragon.navigation.use.LoadingDialogHandler;
import com.dragon.navigation.use.MeshObject;
import com.dragon.navigation.use.SampleUtils;
import com.dragon.navigation.use.Texture;

import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainRenderer implements GLSurfaceView.Renderer {
    /**
     * Created by EdwardPC on 2016/9/13.
     */
    private static final String LOGTAG = "ImageTargetRenderer";
    private Main mActivity;

    private Vector<Texture> mTextures;

    private int shaderProgramID;
    private int shaderProgramID2;

    private int vertexHandle;

    private int normalHandle;
    public static int vec = 290;
    private int textureCoordHandle;

    private int mvpMatrixHandle;

    private int texSampler2DHandle;

    private CubeObject mCubeObject;

    private float kBuildingScale = 12.0f;

    private GLSurfaceView.Renderer mRenderer;

    boolean mIsActive = false;

    private static final float OBJECT_SCALE_FLOAT = 5.0f;
    private static final float OBJECT_SCALE_Z = 1.0f;
    private static final float OBJECT_SCALE_FLOATUP = 200f;
    private float AnimationZ = -5000.0f;
    private float AnimationTZ = 5400.0f;
    private float AnimationRZ = 0f;
    private float AnimationFZ = -50f;
    boolean TeapotAppear = false;
    private int mViewWidth = 0;
    private int mViewHeight = 0;
    private int picturenum = 0;
    private boolean LeaveTrack = false;

    public MainRenderer(Main activity) {
        Log.i(LOGTAG, "creat a ImageTargetRenderer");
        mActivity = activity;
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl) {
        // Call our function to render content
        renderFrame();
        GLES20.glFinish();
//        if (ImageTargets.ScreenShot==true) {
//            saveScreenShot(0, 0, mViewWidth, mViewHeight, "ImageTarget"+picturenum+".png");
//            ImageTargets.ScreenShot=false;
//        }
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        initRendering();

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        mViewWidth = width;
        mViewHeight = height;
        // Call Vuforia function to handle render surface size changes:
    }

    // Function for initializing the renderer.
    private void initRendering() {
        mCubeObject = new CubeObject();
        Log.i(LOGTAG, "initRendering");

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0f);
        GLES20.glClearColor(0, 0, 0, 0f); //设置屏幕背景色RGBA
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            //这是纹理过滤，MIN,LINEAR缩小线性过滤，线性(使用距离当前渲染像素中心最近的4个纹素加权平均值.)
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            //放大线性过滤,后面的LINEAR可以更换为NEAREST接近滤波
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
            //  SampleUtils.checkGLError("");查看util下的ampleSUtils类，该函数用于检查GLES运行时的错误
            //定义一个二维纹理映射。 glTexImage2D(GLenum target,GLint level,GLint components,
            // GLsizei width, glsizei height,GLint border,GLenum format,
            // GLenum type, const GLvoid *pixels);
            //参数format和type描述了纹理映射的格式和数据类型
            // 篇幅过长避免混乱，链接http://blog.csdn.net/shuaihj/article/details/7244313
        }
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        // glGetAttribLocation方法：获取着色器程序中，指定为attribute类型变量的id
        // // 获取指向着色器中vertexPosition的index
        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");//attribute vec4 vertexPosition
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexNormal");//attribute vec4 vertexNormal
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");//attribute vec2 vertexTexCoord
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");//uniform mat4 modelViewProjectionMatrix
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");//uniform sampler2D texSampler2D
        SampleUtils.checkGLError("");
    }


    // The render function.
    private void renderFrame() {
        //Log.i(LOGTAG,"renderFrame");
        //这个画图功能函数也在不断的调用
        //清除颜色缓冲和深度缓冲
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Set the viewport

        GLES20.glViewport(0, -63, 1080, 1920);

        // handle face culling, we need to detect if we are using reflection
        // to determine the direction of the culling
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        float[] modelViewMatrix= Data.modelViewMatrix.clone();
        modelViewMatrix[13]=Data.getMoveX(Data.degree);
        Matrix.scaleM(modelViewMatrix, 0, 400f,
                200f, 1.0f);
        drawModel(mCubeObject, modelViewMatrix, "lanshou", true);
        Data.modelDrawed=true;
    }




        public void setTextures(Vector<Texture> textures)
        {
            mTextures = textures;

        }




        public void drawModel(MeshObject model, float[] modelViewMatrixonlyread, String TextureName, Boolean leaveTrack){
            float modelViewMatrix[]=modelViewMatrixonlyread.clone();
            int textureIndex=(int) Texture.TextureMap.get(TextureName);
            //Output("draw teapot",modelViewMatrix);
            float[] modelViewProjection = new float[16];
            Matrix.multiplyMM(modelViewProjection, 0,Data.ProjectionMatrix, 0, modelViewMatrix, 0);
            GLES20.glUseProgram(shaderProgramID);
            SampleUtils.checkGLError("glUseProgram");//以上的GLES运行无错
            //楼房的渲染
            // if (!mActivity.isExtendedTrackingActive()) {
            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, model.getVertices());
            //变量类型为vec4(x,y,z,1)，这里是3的缘故，
            // 表示(x,y,z)后面那个比例系数1不用
            SampleUtils.checkGLError("vert");
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 0, model.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, model.getTexCoords());
            SampleUtils.checkGLError("coord");
            //启用或者禁用顶点属性数组，下面为启用
            GLES20.glEnableVertexAttribArray(vertexHandle);
            SampleUtils.checkGLError("vd");
            GLES20.glEnableVertexAttribArray(normalHandle);
            // SampleUtils.checkGLError("nd");//这里出错
            GLES20.glEnableVertexAttribArray(textureCoordHandle);
            // activate texture 0, bind it, and pass to shader
            //选择活动纹理单元。函数原型：
                    /*void glActiveTexture (int texture)
                    参数含义：
                    texture指定哪一个纹理单元被置为活动状态。texture必须是GL_TEXTUREi之一，*//*
                    其中0 <= i < GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS，初始值为GL_TEXTURE0。*/
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            //确定了后续的纹理状态改变影响哪个纹理，纹理单元的数量是依据该纹理单元所被支持的具体实现。
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    mTextures.get(textureIndex).mTextureID[0]);
            GLES20.glUniform1i(texSampler2DHandle, 0);

            // pass the model view matrix to the shader
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjection, 0);
            // finally draw the teapot
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                   model.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    model.getIndices());
            //以上无标记处无错误
            // disable the enabled arrays
            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);
            //   } else {
            SampleUtils.checkGLError("Render Frame");
            GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        }
        public void Output(String word,float MatrixData[]){
            Log.i(LOGTAG,word);
            for (int i = 0; i < 16; i += 4) {
                Log.i(LOGTAG, MatrixData[i] + "   " + MatrixData[i + 1] + "   "
                        + MatrixData[i + 2] + "   " + MatrixData[i + 3] + "   ");
            }
        }
    }

