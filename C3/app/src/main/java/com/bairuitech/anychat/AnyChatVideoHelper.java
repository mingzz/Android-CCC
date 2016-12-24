package com.bairuitech.anychat;



import java.nio.ByteBuffer;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;

import com.ljjqdc.app.c3.main.MainActivity;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
//import org.opencv.core.Rect;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;


//AnyChat 视频显示包装类，实现Java层面的视频播放
public class AnyChatVideoHelper {
	private int MAX_VIDEO_NUM = 10;
	VideoRenderer render[];
	
	public AnyChatVideoHelper() {
		render = new VideoRenderer[MAX_VIDEO_NUM];
	}
	
	public int bindVideo(SurfaceHolder holder) {
		for(int i=0; i<MAX_VIDEO_NUM; i++) {
			if(render[i] != null && render[i].GetUserId() == -1) {
				render[i] = null;
			}
		}
		int index = -1;
		for(int i=0; i<MAX_VIDEO_NUM; i++) {
			if(render[i] == null) {
				render[i] = new VideoRenderer(holder);
				index = i;
				break;
			}
		}
		return index;
	}
	
	public void SetVideoUser(int index, int userid) {
		if(index < 0 || index >= MAX_VIDEO_NUM)
			return;
		if(render[index] == null)
			return;
		render[index].SetUserId(userid);
	}
	
	public int SetVideoFmt(int userid, int width, int height) {
		VideoRenderer r = GetRenderByUserId(userid);
		if(r == null)
			return -1;
		r.CreateBitmap(width, height);
		return 0;
	}
	
	/***
	 * 设置最大裁剪图片的比例。比例越大，当需要裁剪的时候丢失的数据越多，surfaceview占的屏幕也更满
	 * @param scale 比例
	 */
	public void setMaxCutScale(int userId,float scale)
	{
		VideoRenderer r = GetRenderByUserId(userId);
		r.setMaxCutScale(scale);
	}
	
	public void ShowVideo(int userid, byte [] mPixel, int rotation, int mirror) {
		VideoRenderer r = GetRenderByUserId(userid);
		if(r == null)
			return;
		r.DrawByteBuffer(mPixel, rotation, mirror);
	}
	
	private VideoRenderer GetRenderByUserId(int userid) {
		for(int i=0; i<MAX_VIDEO_NUM; i++) {
			if(render[i] != null) {
				if(render[i].GetUserId() == userid) {
					return render[i];
				}
			}	
		}
		return null;
	}
	
}



class VideoRenderer implements Callback {
    // the bitmap used for drawing.
    private Bitmap bitmap = null;
    private SurfaceHolder surfaceHolder;
    // Rect of the source bitmap to draw
    private Rect srcRect = new Rect();
    // Rect of the destination canvas to draw to
    private Rect dstRect = new Rect();
    // private float dstTopScale = 0;
    private float dstBottomScale = 1;
    // private float dstLeftScale = 0;
    private float dstRightScale = 1;
    
    private float max_cut_imgscale = 1.0f/3;		//最大能裁剪视频的比例
    
    private int mUserid = -1;
	
	
    public VideoRenderer(SurfaceHolder holder) {
        if(holder == null)
            return;
        mUserid = 0;			// 未知状态
        surfaceHolder = holder;
        holder.addCallback(this);
    }
    
    // 获取当前视频显示单元绑定的用户ID
    public int GetUserId() 				{		return mUserid;			}
    // 设置用户ID
    public void SetUserId(int userid)	{		mUserid = userid;   	}
    // 设置最大裁剪图片的比例
	public void setMaxCutScale(float scale) {
		if(scale>1.0)
			scale=1.0f;
		this.max_cut_imgscale=scale;
	}
    
    // surfaceChanged and surfaceCreated share this function
    private void changeDestRect(int dstWidth, int dstHeight) {
        dstRect.right = (int)(dstRect.left + dstRightScale * dstWidth);
        dstRect.bottom = (int)(dstRect.top + dstBottomScale * dstHeight);
    }
    
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		changeDestRect(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Canvas canvas = holder.lockCanvas();
        if(canvas != null) {
            Rect dst = holder.getSurfaceFrame();
            if(dst != null) {
                changeDestRect(dst.right - dst.left, dst.bottom - dst.top);
            }
            holder.unlockCanvasAndPost(canvas);
        }
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		bitmap = null;
		surfaceHolder = null;
		mUserid = -1;
	}

	public Bitmap CreateBitmap(int width, int height) {
        if (bitmap == null) {
            try {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DISPLAY);
            }
            catch (Exception e) {
            }
        }
        if(bitmap != null && (srcRect.bottom != height || srcRect.right != width))
        	bitmap = null;
        if(bitmap == null) {
	        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
	        srcRect.left = 0;
	        srcRect.top = 0;
	        srcRect.bottom = height;
	        srcRect.right = width;        	
        }
        return bitmap;
    }

    public void SetCoordinates(float left, float top,  float right, float bottom) {
        // dstLeftScale = left;
        // dstTopScale = top;
        dstRightScale = right;
        dstBottomScale = bottom;
    }

    public void DrawByteBuffer(byte [] mPixel, int rotation, int mirror) {
        if(bitmap == null)
            return;
        ByteBuffer byteBuffer = ByteBuffer.wrap(mPixel); // 将 byte 数组包装到缓冲区中
		byteBuffer.rewind();
		bitmap.copyPixelsFromBuffer(byteBuffer);

		Mat mat_bmp = new Mat(bitmap.getHeight() , bitmap.getWidth(), CvType.CV_8UC4);

		if(MainActivity.grey_on) {
			Utils.bitmapToMat(bitmap, mat_bmp);
			//将彩色Mat对象转成单通道的灰度Mat.
			Mat mat_gray = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
			Imgproc.cvtColor(mat_bmp, mat_gray, Imgproc.COLOR_BGRA2GRAY, 1);

			//由于最后将mat转成ARGB_8888型的Bitmap，输入必须是4通道的.
			//因而这里要将单通道转成4通道
//		Mat gray4 = new Mat(mat_gray.rows(), mat_gray.cols(), CvType.CV_8UC4);
//		Imgproc.cvtColor(mat_gray, gray4, Imgproc.COLOR_GRAY2BGRA, 4);
			//将mat对象转成Bitmap显示.
			Bitmap bmp_gray = null;
			bmp_gray = Bitmap.createBitmap(mat_gray.cols(), mat_gray.rows(), Bitmap.Config.RGB_565);
			Utils.matToBitmap(mat_gray, bmp_gray);
			//bitmap=bmp_gray;
			Log.i("getbyte", String.valueOf(bitmap.getByteCount()));
			bitmap = bmp_gray.copy(bmp_gray.getConfig(), true);
			Log.i("getbyte", String.valueOf(bitmap.getByteCount()));
			Log.i("bitmap2:", String.valueOf(bitmap.getWidth()));
		}
		if(MainActivity.face_on) {
			Utils.bitmapToMat(bitmap, mat_bmp);
			MatOfRect faceDetections = new MatOfRect();
			MainActivity.faceDetector.detectMultiScale(mat_bmp, faceDetections);

			Log.i(String.format("Detected %s faces",
					faceDetections.toArray().length), "");

			int facenum = 0;
			// Draw a bounding box around each face.
			org.opencv.core.Rect[] facesArray = faceDetections.toArray();
			for (org.opencv.core.Rect rect : faceDetections.toArray()) {
				Imgproc.rectangle(
						mat_bmp,
						new Point(rect.x, rect.y),
						new Point(rect.x + rect.width, rect.y + rect.height),
						new Scalar(255, 0, 0));
				++facenum;
			}

			// Save the visualized detection.
			// Bitmap bmpdone = Bitmap.createBitmap(bmptest.getWidth(),
			// bmptest.getHeight(), Config.RGB_565);
			Utils.matToBitmap(mat_bmp, bitmap);
		}
		Canvas canvas = surfaceHolder.lockCanvas();
		if (canvas != null) {
			
			Paint paint = new Paint();
			paint.setAntiAlias(true); // 抗锯齿
			Matrix matrix = new Matrix();
			float fScalex = 0;
			float fScaley = 0;
			float transX = 0;
			float transY = 0;
			int c_w = canvas.getWidth();
			int c_h = canvas.getHeight();
			int b_w = bitmap.getWidth();
			int b_h = bitmap.getHeight();
			int temp_b_w = b_w;
			int temp_b_h = b_h;
			if (rotation != 0) {
				matrix.postRotate(rotation, (float)bitmap.getWidth()/2, (float)bitmap.getHeight()/2);
				if (rotation == 90 || rotation == 270) {
					temp_b_w = b_h;
					temp_b_h = b_w;
					matrix.postTranslate((float)(1.0f/2)*(b_h - b_w), (float)(1.0f/2)*(b_w - b_h));
				}
			}
			if (c_h * temp_b_w > c_w * temp_b_h) {
				float cutX=temp_b_w - (float) c_w*temp_b_h/c_h;
				if(cutX>temp_b_w*max_cut_imgscale) {
					cutX=temp_b_w*max_cut_imgscale;
					transY = (c_h - (float) temp_b_h * c_w / (temp_b_w-cutX)) / 2;
				}
				transX=-cutX*c_w/(2*(temp_b_w-cutX));
				fScalex=(float)c_w/(temp_b_w-cutX);
				fScaley=fScalex;
			} else {
				float cutY = temp_b_h - (float) c_h*temp_b_w/c_w;
				if(cutY>temp_b_h*max_cut_imgscale) {
					cutY=(float)temp_b_h*max_cut_imgscale;
					transX = (c_w - (float) temp_b_w * c_h /( temp_b_h-cutY)) / 2;
				}
				transY=-cutY*c_h/(2*(temp_b_h-cutY));
				fScaley = (float) c_h / (temp_b_h-cutY);
				fScalex = fScaley;
			}
			if(mirror != 0) {			
    			matrix.postScale(-fScalex, fScaley);
    			matrix.postTranslate(fScalex*temp_b_w, 0);
    		} else {
    			matrix.postScale(fScalex, fScaley);
    		}
			matrix.postTranslate(transX, transY);
			canvas.drawColor(Color.BLACK);
			canvas.drawBitmap(bitmap, matrix, paint);
			surfaceHolder.unlockCanvasAndPost(canvas);
		} else {
			Log.i("ANYCHAT", "Invalid canvas!");
		}
    }
    
}