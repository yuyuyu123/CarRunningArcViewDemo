package com.yulin.carrunningarcviewdemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by YuLin on 2017/3/20 0020.
 * An android custom widget called CarRunningArcView,which has the effect that a car(maybe other icon)
 * can run on an arc forward and back.And you can transform it to a count down timer view or a loading
 * view with arc shape and etc.
 */
public class CarRunningArcView extends View {
    private static final String TAG = CarRunningArcView.class.getSimpleName();

    private static final float DEFAULT_STROKE = 15;
    private static final float DEFAULT_START_ANGLE = 150;
    private static final float DEFAULT_SWIPE_ANGLE = 240;
    private static final int DEFAULT_SLEEP_TIME = 20;

    private Paint mPaintInside;
    private Paint mPaintOutside;

    private int mWidth;
    private int mHeight;

    private int mCenterX;
    private int mCenterY;

    private float mRadius;
    private RectF mRectOval;
    private int mArcInsideColor = Color.WHITE;
    private float mArcInsideStroke = DEFAULT_STROKE;
    private float mArcOutSideStroke = DEFAULT_STROKE + 5;
    private float mStartAngle = DEFAULT_START_ANGLE;
    private float mSwipeAngle = DEFAULT_SWIPE_ANGLE;
    private int mSleepTime = DEFAULT_SLEEP_TIME;
    private int mProgress;
    private double mRatio;

    private Bitmap mBitmap;
    private Matrix mMatrix = null;

    private Path mPath;
    private PathMeasure mPathMeasure;
    private float mBitmapDegrees = 0f;

    private float mCurrentValue = 0;     // 用于纪录当前的位置,取值范围[0,1]映射Path的整个长度
    private float[] mPos;                // 当前点的实际位置
    private float[] mTan;                // 当前点的tangent值,用于计算图片所需旋转的角度

    private float mPositions[];
    private SweepGradient mSweepGradient;
    private int[] mArcInsideColors;

    /**
     * Degree offset for rotating clockwise.
     */
    private int mDegreeClockwise = 0;
    /**
     * Degree offset for rotating anticlockwise.
     */
    private int mDegreeAnticlockwise = 180;

    private boolean isReInitialing = false;

    private boolean exit = false;

    private static Handler mHandler = new Handler();

    private CarState mCarState = CarState.MOVED_CLOCKWISE;

    /**
     * The icon's state.
     */
    public enum CarState {
        //Moving state.
        MOVING_CLOCKWISE, MOVED_CLOCKWISE, MOVING_ANTICLOCKWISE, MOVED_ANTICLOCKWISE,
         //Rotation state.
        ROTATING_CLOCKWISE, ROTATED_CLOCKWISE, ROTATING_ANTICLOCKWISE, ROTATED_ANTICLOCKWISE
    }

    public CarRunningArcView(Context context) {
        this(context, null);
    }

    public CarRunningArcView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarRunningArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CarRunningArcView);
        int count = a.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.CarRunningArcView_arcInsideColor:
                    mArcInsideColor = a.getColor(attr, Color.GRAY);
                    break;
                case R.styleable.CarRunningArcView_arcInsideStroke:
                    mArcInsideStroke = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.CarRunningArcView_arcOutSideStroke:
                    mArcOutSideStroke = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.CarRunningArcView_radius:
                    mRadius = a.getDimensionPixelSize(attr, (int) TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, 0, getResources().getDisplayMetrics()));
                    break;
                case R.styleable.CarRunningArcView_sleepTime:
                    mSleepTime = a.getInt(attr, DEFAULT_SLEEP_TIME);
                    break;
                case R.styleable.CarRunningArcView_ratio:
                    mRatio = a.getFloat(attr, 0);
                    break;
                default:
                    break;
            }
        }
        a.recycle();

        initPaint();

        initMipmap();

        initOther();

        setRatio(mRatio,mCarState);
    }

    private void initPaint() {
        mPaintInside = getPaint(false);
        mPaintOutside = getPaint(true);
    }

    private Paint getPaint(boolean isOutside) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        if(isOutside) {
            paint.setStrokeWidth(mArcOutSideStroke);
        } else {
            paint.setStrokeWidth(mArcInsideStroke);
            paint.setColor(mArcInsideColor);
            paint.setAlpha(20);
        }
        return paint;
    }

    private void initOther() {
        mPath = new Path();
        mPathMeasure = new PathMeasure();
        mMatrix = new Matrix();
        mPos = new float[2];
        mTan = new float[2];
    }

    public void setColors(int[] colors) {
        if(null == colors || 0 == colors.length) {
            throw new NullPointerException("the color array that you defined must not be null.");
        }
        if(colors.length < 2) {
            throw new IllegalArgumentException("you must specify at least two kinds of colors.");
        }
        mArcInsideColors = colors;
    }

    private void initMipmap() {
        mBitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.mipmap.icon_big_car);
    }

    @Override protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mWidth = w;
        mHeight = h;
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        if (mRadius + mArcOutSideStroke > Math.min(mWidth / 2, mHeight / 2) || mRadius <= 0) {
            mRadius = Math.min(mWidth / 2, mHeight / 2) - mArcOutSideStroke;
        }
        mRectOval = new RectF(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        mSweepGradient = new SweepGradient(mCenterX, mCenterX, mArcInsideColors, mPositions);

        mPositions = new float[]{0f,0.5f,0.8f,0.9f};
        final float rotationDegrees = mSwipeAngle * 1.0f / 6;
        mMatrix.setRotate(rotationDegrees, mCenterX, mCenterX);
        mSweepGradient.setLocalMatrix(mMatrix);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArcInside(canvas);
        drawArcOutside(canvas);
    }

    /**
     * Draw the arc inside as the progress' background.
     *
     * @param canvas
     */
    private void drawArcInside(Canvas canvas) {
        mPath.reset();
        mPath.addArc(mRectOval, mStartAngle, mSwipeAngle);
        canvas.drawPath(mPath, mPaintInside);
    }

    public CarState getCarState() {
        return mCarState;
    }

    public void setCarState(CarState mCarState) {
        this.mCarState = mCarState;
    }

    /**
     * Set ratio which makes influenced on the arc outside's swipe angle.
     *
     * @param ratio
     */
    public void setRatio(double ratio, CarState state) {
        this.mRatio = ratio;
        setCarState(state);
        setExit(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                running();
            }
        }).start();
    }

    /**
     * 如果只是进度条，只要设置进度就可以了，只要开启一个线程
     */

    /**
     * Draw the arc outside for current progress.
     *
     * @param canvas
     */
    private void drawArcOutside(Canvas canvas) {
        mPath.reset();
        mPath.addArc(mRectOval, mStartAngle, mProgress == 0 ? 0.01f : mProgress);
        mPathMeasure.setPath(mPath,false);
        mPathMeasure.getPosTan(mPathMeasure.getLength() * mCurrentValue, mPos, mTan);
        mBitmapDegrees = (float) (Math.atan2(mTan[1], mTan[0]) * 180.0 / Math.PI);

        Log.e("JP", "mBitmapDegrees--->" + mBitmapDegrees);
        if(mCarState == CarState.ROTATING_CLOCKWISE) {
            mBitmapDegrees += mDegreeClockwise;
        }

        if (mCarState ==CarState.MOVING_ANTICLOCKWISE) {
            mBitmapDegrees += 180;
        }

        if(mCarState == CarState.ROTATING_ANTICLOCKWISE) {
            mBitmapDegrees += mDegreeAnticlockwise;
        }

        mMatrix.reset();
        mMatrix.postRotate(mBitmapDegrees, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);
        mMatrix.postTranslate(mPos[0] - mBitmap.getWidth() / 2, mPos[1] - mBitmap.getHeight() / 2);   // 将图片绘制中心调整到与当前点重合

        mPaintOutside.setShader(mSweepGradient);
        canvas.drawPath(mPath, mPaintOutside);
        canvas.drawBitmap(mBitmap, mMatrix, mPaintOutside);
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    private void running() {
        while (!exit) {
            if(mCarState == CarState.MOVING_CLOCKWISE) {//正在顺时针跑
                if (mProgress < mSwipeAngle * mRatio) {
                    movingClockwise();
                } else {
                   movedClockwise();
                }
            }

            if(mCarState == CarState.ROTATING_CLOCKWISE) {//正在顺时针转
                if(mDegreeClockwise < 180) {
                    rotatingClockwise();
                } else {//顺时针转完毕
                    rotatedClockwise();
                }
            }

            if(mCarState == CarState.MOVING_ANTICLOCKWISE) {//正在逆时针跑
                if (mProgress > mSwipeAngle * mRatio) {
                    movingAnticlockwise();
                } else {//逆时针跑完,如果跑到了0点，则要逆时针转一圈
                    if(mProgress > 0) {
                        movedAnticlockwise1();
                        break;
                    } else {
                       movedAnticlockwise2();
                    }
                }
            }

            if(mCarState == CarState.ROTATING_ANTICLOCKWISE) {//正在逆时针转
                if(mDegreeAnticlockwise > 0) {
                    rotatingAnticlockwise();
                } else {//逆时针旋转完毕
                    rotatedAnticlockwise();
                }
            }

        }
    }

    private void rotatingAnticlockwise() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCarState = CarState.ROTATING_ANTICLOCKWISE;
        mDegreeAnticlockwise--;
        postInvalidate();
    }

    private void rotatingClockwise() {
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCarState = CarState.ROTATING_CLOCKWISE;
        mDegreeClockwise++;
        postInvalidate();
    }

    private void movingClockwise() {
        try {
            Thread.sleep(mSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mCarState = CarState.MOVING_CLOCKWISE;
        mProgress++;
        setCurrentValue();
        postInvalidate();
    }

    private void movedClockwise() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mRatio > 0) {
                    mCarState = CarState.MOVED_CLOCKWISE;//顺时针跑完毕
                    mCarState = CarState.ROTATING_CLOCKWISE;
                    setReInitialing(false);
                } else {
                    mCarState = null;
                }
            }
        },50);
    }

    private void movingAnticlockwise() {
        try {
            Thread.sleep(mSleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mProgress--;
        setCurrentValue();
        postInvalidate();
    }

    private void movedAnticlockwise1() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("JP", "movedAnticlockwise1");
                mCarState = CarState.MOVED_ANTICLOCKWISE;
            }
        },50);
    }

    private void movedAnticlockwise2() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.e("JP", "movedAnticlockwise2");
                mCarState = CarState.MOVED_ANTICLOCKWISE;
                mCarState = CarState.ROTATING_ANTICLOCKWISE;
            }
        },50);
    }

    private void rotatedClockwise() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mCarState = CarState.ROTATED_CLOCKWISE;
                mDegreeClockwise = 0;
                setExit(true);
            }
        },50);
    }

    public boolean isReInitialing() {
        return isReInitialing;
    }

    public void setReInitialing(boolean reInitialing) {
        isReInitialing = reInitialing;
    }

    private void rotatedAnticlockwise() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(!isReInitialing) {
                    mCarState = CarState.ROTATED_ANTICLOCKWISE;
                    setExit(true);
                } else {
                    mCarState = CarState.MOVING_CLOCKWISE;
                }
                mDegreeAnticlockwise = 180;
            }
        },50);
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Set current value([0,1])
     */
    private  void setCurrentValue() {
        mCurrentValue += mProgress * 1.0 / mSwipeAngle;
        if (mCurrentValue >= 1) {
            mCurrentValue = 1;
        }
    }

    /**
     * Get he ratio.
     *
     * @return
     */
    public double getRatio() {
        return mRatio;
    }

    /**
     * Saved state for arc's progress.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        float bitmapDegrees;
        float currentValue;
        double ratio;
        float[] pos = new float[2];
        float[] tan = new float[2];

        /**
         * Constructor called from {@link CarRunningArcView#onSaveInstanceState()}
         */
        SavedState(Parcelable superState) {
            super(superState);
        }

        /**
         * Constructor called from {@link #CREATOR}
         */
        private SavedState(Parcel in) {
            super(in);
            progress = in.readInt();
            bitmapDegrees = in.readFloat();
            currentValue = in.readFloat();
            ratio = in.readDouble();
            in.readFloatArray(pos);
            in.readFloatArray(tan);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeFloat(bitmapDegrees);
            out.writeFloat(currentValue);
            out.writeDouble(ratio);
            out.writeFloatArray(pos);
            out.writeFloatArray(tan);
        }

        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    @Override public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState ss = new SavedState(superState);
        ss.progress = mProgress;
        ss.bitmapDegrees = mBitmapDegrees;
        ss.currentValue = mCurrentValue;
        ss.ratio = mRatio;
        ss.pos = mPos;
        ss.tan = mTan;
        return ss;
    }

    @Override public void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setProgress(ss.progress);
        mBitmapDegrees = ss.bitmapDegrees;
        mCurrentValue = ss.currentValue;
        mRatio = ss.ratio;
        mPos = ss.pos;
        mTan = ss.tan;
    }

    public void setProgress(int progress) {
        this.mProgress = progress;
    }

    /**
     * Get sleep time which makes influenced on arc outside's drawing speed.
     *
     * @return
     */
    public int getSleepTime() {
        return mSleepTime;
    }

    /**
     * Set sleep time which makes influence on the arc's drawing speed.
     *
     * @param sleepTime
     */
    public void setSleepTime(int sleepTime) {
        this.mSleepTime = sleepTime;
    }

    /**
     * Get arc inside color.
     *
     * @return
     */
    public int getArcInsideColor() {
        return mArcInsideColor;
    }

    /**
     * Set arc inside color.
     *
     * @param arcInsideColor
     */
    public void setArcInsideColor(int arcInsideColor) {
        this.mArcInsideColor = arcInsideColor;
        invalidate();
    }

    /**
     * Get arc inside stroke.
     *
     * @return
     */
    public float getArcInsideStroke() {
        return mArcInsideStroke;
    }

    /**
     * Set arc inside stroke which makes influence on the arc's width.
     *
     * @param arcInsideStroke
     */
    public void setArcInsideStroke(float arcInsideStroke) {
        this.mArcInsideStroke = arcInsideStroke;
        invalidate();
    }

    /**
     * Get arc outside's stroke.
     *
     * @return
     */
    public float getArcOutSideStroke() {
        return mArcOutSideStroke;
    }

    /**
     * Set arc outside's stroke which makes influenced on arc's width.
     *
     * @param arcOutSideStroke
     */
    public void setArcOutSideStroke(float arcOutSideStroke) {
        this.mArcOutSideStroke = arcOutSideStroke;
        invalidate();
    }

    /**
     * Get arc's radius.
     *
     * @return
     */
    public float getRadius() {
        return mRadius;
    }

    /**
     * Set arc's radius which makes influenced on arc's width and height.
     *
     * @param radius
     */
    public void setRadius(float radius) {
        this.mRadius = radius;
    }

    /**
     * Get the bitmap using for moving forward or back.
     * @return
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * Set the bitmap using for moving forward or back.
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        if(null == bitmap) {
            throw new NullPointerException("the bitmap that you specified must not be null.");
        }
        this.mBitmap = bitmap;
    }

}
