

package com.dlighttech.camera2.dimenco3dapi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.view.Surface;
import android.view.View;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
//import android.widget.SeekBar;

@SuppressLint("ViewConstructor")
final class Video3DSurfaceViewImpl extends GLSurfaceView implements IVideo3DSurfaceView {

    VideoRender mRenderer;
    private MediaPlayer mMediaPlayer = null;
	private Context mContext;
	private boolean cPortrait;
    protected  Video3DSurfaceViewImpl(Context context, MediaPlayer mp) {
        super(context);
        mContext=context;
        
		Log.i("DimencoVideo3dSurfaceViewImpl", "Construct Video3DSurface");

        setEGLContextClientVersion(3);
        mMediaPlayer = mp;
        mRenderer = new VideoRender(context);
        setRenderer(mRenderer);
        
        this.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        
    }

    @Override
    public void onResume() {
        queueEvent(new Runnable(){
                public void run() {
                }});

        super.onResume();
        mRenderer.setMediaPlayer(mMediaPlayer);
        mRenderer.mDimencoEyeTracker.onResume();
    }
    @Override
    public void onPause() {
        super.onPause();
        mRenderer.mDimencoEyeTracker.onPause();
   }


    private final class VideoRender
        implements GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
        private static final String TAG = "VideoRender";
        public IEyeTracker mEyeTracker=null; 
        public IDimencoEyeTracker mDimencoEyeTracker=null; 
        private Program mProg;// = new Program();
        private Program mProgPattern;// = new Program();

        private static final int FLOAT_SIZE_BYTES = 4;
        private static final int TRIANGLE_VERTICES_DATA_STRIDE_BYTES = 5 * FLOAT_SIZE_BYTES;
        private static final int TRIANGLE_VERTICES_DATA_POS_OFFSET = 0;
        private static final int TRIANGLE_VERTICES_DATA_UV_OFFSET = 3;
        private final float[] mTriangleVerticesData = {
            // X, Y, Z, U, V
            -1.0f, -1.0f, 0, 0.f, 0.f,
            1.0f, -1.0f, 0, 1.f, 0.f,
            -1.0f,  1.0f, 0, 0.f, 1.f,
            1.0f,  1.0f, 0, 1.f, 1.f,
        };

        private FloatBuffer mTriangleVertices;



    	private float[] mMVPMatrix = new float[16];
        private float[] mSTMatrix = new float[16];

        private int mProgram;
        private int mProgramPattern;
        private int mTextureID;
        private int muMVPMatrixHandle;
        private int muSTMatrixHandle;
        private int maPositionHandle;
        private int maTextureHandle;
        private int mPitchHandle;
        private int mSlantHandle;
        private int mWidthHandle;
        private int mHeightHandle;
        private int mCenterviewHandle;
        private int mBarsHandle;
		private int mPortraitHandle;
		
        private double mPitch;
        private double mSlant;
        private double mCenterview;
		private double mPattern;
		private int mPortrait;

        private SurfaceTexture mSurface;
        private boolean updateSurface = false;

        private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

        private MediaPlayer mMediaPlayer;
		private DatagramSocket socket;
		
        private class InitShader
        {
        	public Program Init(boolean full)
        	{/*
        		String mVertexShader =
    		    		"#version 100 \n" +
    		            "precision highp float;\n" +
    		            "//uniform mat4 uMVPMatrix;\n" +
    		            "//uniform mat4 uSTMatrix;\n" +
    		            "attribute vec2 Position;\n" +
    		            "//attribute vec2 TexCoord;\n" +
    		            "varying vec2 TexCoord;\n" +
    		            "//out vec4 ShadowCoord;\n" +
    		            
    		            "void main() {\n" +
    		            "  gl_Position = vec4(Position, 0.0, 1.0);//uMVPMatrix * aPosition;\n" +
    		            "  TexCoord = Position;// = (uSTMatrix * aTextureCoord).xy;\n" +
    		            "  //ShadowCoord=gl_Position;\n" +
    		            "}\n";
*/
        		String mVertexShader =
                        "#version 100 \nprecision highp float;\n" +
                        "uniform mat4 uMVPMatrix;\n" +
                        "uniform mat4 uSTMatrix;\n" +
                        "attribute vec4 aPosition;\n" +
                        "attribute vec4 aTextureCoord;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "varying vec3 vViewHelpers;\n" +
                        "uniform  float pitch;\n" +
                        "uniform  float slant;\n" +
                        "uniform  highp float width;\n" +
                        "uniform  highp float height;\n" +
                        "uniform  float centerview;\n" +
                        "void main() {\n" +
                        "  float startView=centerview;\n" +
                        "  gl_Position = uMVPMatrix * aPosition;\n" +
                        "  vTextureCoord = (uSTMatrix * aTextureCoord).xy;\n" +
                		"  float viewx= ( ((1.0+gl_Position.x)*width/2.0-width/2.0)*3.0+(-height/2.0)*slant)*28.0/pitch+startView;\n" +
                		"  float viewy=    (1.0+gl_Position.y)*height/2.0                             *slant*28.0/pitch;\n" +
                		"  //float viewz=    1.0/3.0                                           *slant*28.0/pitch;\n" +
                		"  float viewz=    1.0                                           *28.0/pitch;\n" +
                        "  vViewHelpers.x=viewx; \n" +
                        "  vViewHelpers.y=viewy; \n" +
                        "  vViewHelpers.z=viewz; \n" +
                        "}\n";

        		 
                String mFragmentShader1 =
                        "#version 100 \n#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTextureCoord;\n" +
                        "varying highp vec3 vViewHelpers;\n" +
                        "uniform samplerExternalOES sTexture;\n" +

                        
                        "uniform highp float pitch;\n" +
                        "uniform highp  float slant;\n" +
                        "uniform highp  float centerview;\n" +
                        "uniform highp  float width;\n" +
                        "uniform highp  float height;\n" +
                    "uniform  float bars;\n" +
                    "uniform  int portrait;\n" +
                    
                    "vec4 getBars(vec2 t, highp float view)\n {" +
        			" float off = -960.0/2.0+0.0*25.0+50.0*10.0; \n" +
        			"  highp float v;" +
        			"  if (t.y<0.03 || t.y>1.0-0.03) { v=0.0; } else {\n" +
        			"     v = (((view-14.0)/4.0));//{ \n" +
        			"   }" +
        			"   highp float tx=off+t.x*2.0*960.0; \n" +
        			"   highp float modv = 100.0; \n" +
        			"	//bars at screen depth \n" +
        			"   highp float fa = abs( mod(tx+8.0*v,modv)-modv/2.0); //fabs( mod(off+t.x*960.0,50.0) \n" +
        			"	if ( fa<8.0/2.0 ) \n" +
        			"     return vec4(1.0,1.0,1.0,1.0)*0.8; else return vec4(0.0,0.0,0.0,1.0)+0.2;\n" +
        			"  }\n" +

        			"vec4 getBars3(vec2 t, highp float view,highp float width)\n {" +
        			" float off = -960.0/2.0+25.0+50.0*10.0; \n" +
        			"  highp float v;" +
        			"  if (t.y<0.03 || t.y>1.0-0.03) { v=0.0; } else {\n" +
        			"     v = (((view-14.0)/4.0));//{ \n" +
        			"   }" +
        			"   highp float tx=off+t.x*2.0*960.0; \n" +
        			"   highp float modv = 100.0; \n" +
        			"	//bars at screen depth \n" +
        			"   highp float fa = abs( mod(tx+8.0*v,modv)-modv/2.0); //fabs( mod(off+t.x*960.0,50.0) \n" +
        			"	if ( fa<width ) \n" +
        			"     return vec4(1.0,1.0,1.0,1.0)*0.8; else return vec4(0.0,0.0,0.0,1.0)+0.4*0.0;\n" +
        			"  }\n" +
                    
                    
                    "lowp vec4 interleave1(highp vec4 p, vec4 left, vec4 right, float subpos) {\n"+
/* NEEDED FOR SOME DISPLAYS (early version of phone with a watermark). NO IDEA WHY (this code is not working anymore. due to optimizations
   we moved the calculation for the view into the vertex shader. If this is needed, the pixel shader
   should do all calculations again (or maybe only the part that uses p.y
                    "  if (gl_FragCoord.y<416.5) " +
                    "	  p.y=p.y-1.0; \n"+
                    "  else if (gl_FragCoord.y<1024.0) " +
                    "	  p.y=p.y-0.5; \n"+
*/            		
                    
            		"  highp float view=mod( vViewHelpers.x+vViewHelpers.y+subpos*vViewHelpers.z,28.0);\n" +

            		" vec2 t = vTextureCoord.xy; t.x=t.x/2.0; \n" +
            	
            		"  if (portrait>0) \n" +
        		    "  { vec2 ot=t;\n" +
        		    "    t.x=ot.y/2.0; \n" + //(0.25-ot.y)*2560.0/1600.0;" +
        		    "    t.y=1.0-2.0*ot.x; \n" + //*1600.0/2560.0;" +
        		    "    t.x=t.x+0.5;" +
        		    "  } \n" +
            		
            		"  if (bars<0.5) {\n" +
            		"  ; \n" +
					"";
                
                
                
                String mFragmentShaderPattern =
                		
                		
            		"  } else if (bars>=10.0) { \n" +
            		"    return vec4(1.0,1.0,1.0,1.0)*(bars-10.0)/10.0; \n" +
            		"  } else \n" +
					    /* green cross for user to position himself*/
            		"   if (abs(p.x-width/2.0)<1.5 || abs(p.y-height/2.0)<1.5) return vec4(0.0,1.0,0.0,1.0); else \n" +
/*           		"  if (p.x<10.0 || p.x>width-10.0 || p.y<10.0 || p.y>height-10.0) \n" +
            		"      return vec4(1.0,1.0,1.0,1.0)*0.0; \n" +
            		"  else if (p.x<25.0 || p.x>width-25.0 || p.y<25.0 || p.y>height-25.0) \n" +
            		"      return vec4(1.0,1.0,1.0,1.0); \n" +
            		"  else \n" +
*/            		"  if (bars>4.5) { \n" +
            		"	 left =vec4(1.0,1.0,1.0,1.0)*0.0; \n" +
                	"	 right=vec4(1.0,1.0,1.0,1.0)*0.0; \n" + 
            		"  } \n" +
            		"  else \n" +
            		"  if (bars>3.5) { \n" +
            		"	 left =vec4(1.0,1.0,1.0,1.0)*1.0; \n" +
                	"	 right=vec4(1.0,1.0,1.0,1.0)*0.0; \n" + 
            		"  } else if (bars>2.5) { \n" +
            		"      return vec4(1.0,1.0,1.0,1.0)* ( (view>12.5 && view<15.5)?1.0:0.0); \n" +

            		"  } else if (bars>1.5) { \n" +
            		"  return getBars(t,view);  \n" +
            		"  } else {\n" +
            		"  left = getBars(t,21.0); \n" +
            		"  right = getBars(t,7.0); \n" +
                    "";

      String mFragmentShader2 =
    	    		"  } \n" +
            		
					"  return ( (mod(view+28.0,28.0))<14.0)?left:right; \n" +

                    "  \n" +
                    "}\n" +
                    
                    "void main() {\n" +
                    "  vec2 tc = vTextureCoord;\n" +

            		"  if (portrait>0) \n" +
        		    "  { vec2 ot=tc;\n" +
        		    "    tc.x=ot.y; \n" + //(0.25-ot.y)*2560.0/1600.0;" +
        		    "    tc.y=1.0-ot.x; \n" + //*1600.0/2560.0;" +
        		    "	 tc.y=1.2*(tc.y-0.5)+0.5; \n" +
 	 				"    tc.x=0.8*(tc.x-0.25)+0.25; \n" +

 	 				"    tc.x=1.0-tc.x; \n" +
 	 				"    tc.y=1.0-tc.y; \n" +
        		    "  } \n" +
            		
                    
                    "  tc.x=tc.x/2.0;\n" +
                    "  vec4 left = texture2D(sTexture, tc); \n" +
                    "  tc.x+=0.5;\n" +
                    "  vec4 right = texture2D(sTexture, tc); \n" +
            		"  if (portrait>0) \n" +
        		    "  { \n" +
                    "  	vec4 tmp = left; \n" +
        		    "   left=right; \n" +
        		    "   right=tmp; \n" +
        		    "  } \n" +
                    "  gl_FragColor.r=interleave1(gl_FragCoord, left,right,2.0).r;\n" +
                    "  gl_FragColor.g=interleave1(gl_FragCoord, left,right,1.0).g;\n"+
                    "  gl_FragColor.b=interleave1(gl_FragCoord, left,right,0.0).b;\n"+
                    "}\n";        
                
                
            	Program mProg= new Program();
            	
            	mProg.init();
                mProg.setVertexSrc(mVertexShader);
                //full=true; // some phone GPU's cannot handle multiple shaders, in that case, use full=true/false always
                mProg.setFragmentSrc(mFragmentShader1+ (full?mFragmentShaderPattern:"") + mFragmentShader2);
                return mProg;
        	}
        }
        public VideoRender(Context context) {
            mTriangleVertices = ByteBuffer.allocateDirect(
                    mTriangleVerticesData.length * FLOAT_SIZE_BYTES)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            mTriangleVertices.put(mTriangleVerticesData).position(0);

            Matrix.setIdentityM(mSTMatrix, 0);
        }

        public void setMediaPlayer(MediaPlayer player) {
            mMediaPlayer = player;
        }
        
        @SuppressWarnings("unused")
		@Override
        public void onDrawFrame(GL10 glUnused) {
        	boolean newFrame=false;
        	int counter=0;
        	while (!newFrame)
        	{
	        	synchronized(this) {
	                if (updateSurface) {
	                    mSurface.updateTexImage();
	                    mSurface.getTransformMatrix(mSTMatrix);
	                    updateSurface = false;
	                    newFrame=true;
	                }
	            }
	        	//if(true)break;
	        	// if not playing (paused), still draw frames
	            if (!newFrame && mMediaPlayer!=null && mMediaPlayer.isPlaying())
	            {
	            	//if (mMediaPlayer!=null && mMediaPlayer.isPlaying()) return;
	        		try {
						Thread.sleep(1000 / 60);
					} catch (InterruptedException e) {
						//e.printStackTrace();
					}
	        		counter++;
	        		if (counter>10) break;
	            }
	            else 
	            	break;
        	}
    		InetAddress broadcastIP=null;
    		int calibMode =0;
    		if (true)//mEyeTracker instanceof IDimencoEyeTracker)
            {
            	IDimencoEyeTracker tmp = (IDimencoEyeTracker) mDimencoEyeTracker;
            	broadcastIP=tmp.getClientAddress();
            	calibMode = mDimencoEyeTracker.getCalibMode();
            	calibMode=1;
            	//if (calibMode!=0)
            	{
	            	mPitch = tmp.GetPitch();
	               	mSlant = tmp.GetSlant();
	               	mCenterview = tmp.GetCenterview();
            	}
               	mPattern=tmp.GetPattern();
               	float angles[]=tmp.getFaceOrientation_TB_xyz_ext_angles();
                
               	//if (angles[2]>Math.PI/4) mPortrait=1; else mPortrait=0;
               	mPortrait=cPortrait?1:0;
            }
            
            GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

            if (mPattern<1)
              GLES30.glUseProgram(mProgram);
            else
            	GLES30.glUseProgram(mProgramPattern);
            	
            checkGlError("glUseProgram");

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_POS_OFFSET);
            GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maPosition");
            GLES20.glEnableVertexAttribArray(maPositionHandle);
            checkGlError("glEnableVertexAttribArray maPositionHandle");

            mTriangleVertices.position(TRIANGLE_VERTICES_DATA_UV_OFFSET);
            GLES20.glVertexAttribPointer(maTextureHandle, 2, GLES20.GL_FLOAT, false,
                    TRIANGLE_VERTICES_DATA_STRIDE_BYTES, mTriangleVertices);
            checkGlError("glVertexAttribPointer maTextureHandle");
            GLES20.glEnableVertexAttribArray(maTextureHandle);
            checkGlError("glEnableVertexAttribArray maTextureHandle");

            Matrix.setIdentityM(mMVPMatrix, 0);
            if (muMVPMatrixHandle!=-1) GLES20.glUniformMatrix4fv(muMVPMatrixHandle, 1, false, mMVPMatrix, 0);
            if (muSTMatrixHandle!=-1) GLES20.glUniformMatrix4fv(muSTMatrixHandle, 1, false, mSTMatrix, 0);
            logGlError("glUniformMatrix...");

            if (mPitchHandle!=-1) GLES20.glUniform1f(mPitchHandle, (float) mPitch);
            logGlError("glUniform mPitchHandle");
            if (mSlantHandle!=-1) GLES20.glUniform1f(mSlantHandle, (float) mSlant);
            logGlError("glUniform mSlantHandle");
            if (mWidthHandle!=-1) GLES20.glUniform1f(mWidthHandle, (float) getWidth());
            logGlError("glUniform mWidthHandle");
            if (mHeightHandle!=-1) GLES20.glUniform1f(mHeightHandle, (float) getHeight());
            logGlError("glUniform mHeightHandle");
            if (mCenterviewHandle!=-1) GLES20.glUniform1f(mCenterviewHandle, (float) mCenterview);
            if (mBarsHandle!=-1) GLES20.glUniform1f(mBarsHandle, (float) mPattern);
            logGlError("glUniform mBarsHandle");
            if (mPortraitHandle!=-1) GLES20.glUniform1i(mPortraitHandle, (int) mPortrait);
            logGlError("glUniform mPortraitHandle");
            GLES30.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            checkGlError("glDrawArrays");
            GLES30.glFinish();

        }

        @Override
        public void onSurfaceChanged(GL10 glUnused, int width, int height) {
        	 
        }
        
        @Override
        public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
        	
            
            InitShader m=new InitShader(); mProg=m.Init(false);
            m=new InitShader(); mProgPattern=m.Init(true);
            
            mProgram = mProg.getId(); //createProgram(mVertexShader, mFragmentShader);
            mProgramPattern = mProgPattern.getId();            
            if (mProgram == 0) {
                return;
            }
            //GLES20.glBindAttribLocation(mProg.getId(), 0, "Position");

            mProg.link();
            mProgPattern.link();
            mProg.use();

            
            maPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
            checkGlError("glGetAttribLocation aPosition");
            if (maPositionHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for aPosition");
            }
            maTextureHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
            checkGlError("glGetAttribLocation aTextureCoord");
            if (maTextureHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for aTextureCoord");
            }

            muMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation uMVPMatrix");
            if (muMVPMatrixHandle == -1) {
               // throw new RuntimeException("Could not get attrib location for uMVPMatrix");
            }

            muSTMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uSTMatrix");
            checkGlError("glGetUniformLocation uSTMatrix");
            if (muSTMatrixHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for uSTMatrix");
            }

            mPitchHandle = GLES20.glGetUniformLocation(mProgram, "pitch");
            checkGlError("glGetUniformLocation pitch");
            if (mPitchHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for pitch");
            }

            mSlantHandle = GLES20.glGetUniformLocation(mProgram, "slant");
            checkGlError("glGetUniformLocation pitch");
            if (mSlantHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for slant");
            }

            mWidthHandle = GLES20.glGetUniformLocation(mProgram, "width");
            checkGlError("glGetUniformLocation width");
            mHeightHandle = GLES20.glGetUniformLocation(mProgram, "height");
            checkGlError("glGetUniformLocation height");

            mCenterviewHandle = GLES20.glGetUniformLocation(mProgram, "centerview");
            checkGlError("glGetUniformLocation centerview");
            if (mCenterviewHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for centerview");
            }

            mBarsHandle = GLES20.glGetUniformLocation(mProgram, "bars");
            checkGlError("glGetUniformLocation bars");
            if (mBarsHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for bars");
            }

            mPortraitHandle = GLES20.glGetUniformLocation(mProgram, "portrait");
            checkGlError("glGetUniformLocation portrait");
            if (mPortraitHandle == -1) {
                //throw new RuntimeException("Could not get attrib location for portrait");
            }
            

            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            mTextureID = textures[0];
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mTextureID);
            checkGlError("glBindTexture mTextureID");

            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);

            GLES20.glDisable(GLES20.GL_BLEND);
            
            //
            // Create the SurfaceTexture that will feed this textureID,
            // and pass it to the MediaPlayer
            //
            mSurface = new SurfaceTexture(mTextureID);
            mSurface.setOnFrameAvailableListener(this);

            Surface surface = new Surface(mSurface);
            mMediaPlayer.setSurface(surface);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            surface.release();

            try {
                mMediaPlayer.prepare();
            } catch (IOException | IllegalStateException t) {
                Log.e(TAG, "media player prepare failed");
            }

            synchronized(this) {
                updateSurface = false;
            }
            
            mMediaPlayer.start();
        }

        synchronized public void onFrameAvailable(SurfaceTexture surface) {
            updateSurface = true;
        }

        private void checkGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);
                throw new RuntimeException(op + ": glError " + error);
            }
        }
        private void logGlError(String op) {
            int error;
            while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
                Log.e(TAG, op + ": glError " + error);

            }
        }

       @SuppressWarnings("unused")
        private final String mFragmentShaderCopy =
                "#extension GL_OES_EGL_image_external : require\n" +
                "varying vec2 vTextureCoord;\n" +
                "uniform samplerExternalOES sTexture;\n" +
                "void main() {\n" +
                "  gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                "}\n";
                
    }  // End of class VideoRender.

    //@Override
	public void SetPlayer(MediaPlayer player)
    {
    	mMediaPlayer=player;
    }

    public void setPitchSlant(double pitch, double slant)
    {
    	mRenderer.mPitch=pitch;
    	mRenderer.mSlant=slant;
    }

	@Override
	public GLSurfaceView GetView() {
		return this;
	}

	@Override
	public void SetEyeTracker(IEyeTracker tracker) {
		this.mRenderer.mEyeTracker=tracker;
		if (tracker instanceof IDimencoEyeTracker)
			this.mRenderer.mDimencoEyeTracker=(IDimencoEyeTracker) tracker;
		else
			this.mRenderer.mDimencoEyeTracker=(IDimencoEyeTracker) Factory.getEyetracker(mContext);

	}

}  // End of class VideoSurfaceView.

