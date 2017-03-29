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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * Created by YuLin on 2017/3/28 0028.
 */
public class CarLoadingViewTwo extends View {
    private static final String TAG = CarLoadingViewTwo.class.getSimpleName();

    private static final float DEFAULT_STROKE = 15;
    private static final float DEFAULT_START_ANGLE = 150;
    private static final float DEFAULT_SWIPE_ANGLE = 240;
    private static final int DEFAULT_SLEEP_TIME = 20;

    private Paint mPaintInside;
    private Paint mPaintOutside;
    private Paint mPaintLoading;

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

    private float mCurrentValue = 0;
    private float[] mPos;
    private float[] mTan;

    private float mPositions[];
    private SweepGradient mSweepGradient;
    private int[] mArcInsideColors;

    private int lastProgress = 0;
    private boolean isReset = false;

    public CarLoadingViewTwo(Context context) {
        this(context, null);
    }

    public CarLoadingViewTwo(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarLoadingViewTwo(Context context, AttributeSet attrs, int defStyleAttr) {
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

        setProgress(1);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (mProgress < mSwipeAngle) {
                        while (currentProgress > getProgress()) {
                            currentProgress--;
                            setCurrentValue();
                            postInvalidate();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        while (currentProgress < getProgress()) {
                            isReset = false;
                            currentProgress++;
                            setCurrentValue();
                            postInvalidate();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }).start();

    }

    private boolean isBackFinished = true;

    public boolean isBackFinished() {
        return isBackFinished;
    }

    public void setBackFinished(boolean backFinished) {
        isBackFinished = backFinished;
    }

    private int currentProgress;

    private void initPaint() {
        mPaintInside = getPaint(false);
        mPaintOutside = getPaint(true);
        mPaintLoading = getPaintLoading();
    }

    private Paint getPaint(boolean isOutside) {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        if (isOutside) {
            paint.setStrokeWidth(mArcOutSideStroke);
        } else {
            paint.setStrokeWidth(mArcInsideStroke);
            paint.setColor(mArcInsideColor);
            paint.setAlpha(15);
        }
        return paint;
    }

    private Paint getPaintLoading() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(mArcOutSideStroke - 3);
        paint.setColor(Color.WHITE);
        paint.setAlpha(30);
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
        if (null == colors || 0 == colors.length) {
            throw new NullPointerException("the color array that you defined must not be null.");
        }
        if (colors.length < 2) {
            throw new IllegalArgumentException("you must specify at least two kinds of colors.");
        }
        mArcInsideColors = colors;
    }

    private void initMipmap() {
        mBitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.mipmap.icon_big_car);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        mWidth = w;
        mHeight = h;
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        if (mRadius + mArcOutSideStroke > Math.min(mWidth / 2, mHeight / 2) || mRadius <= 0) {
            mRadius = Math.min(mWidth / 2, mHeight / 2) - mArcOutSideStroke;
        }
        mRectOval = new RectF(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        mPositions = new float[]{0f, 0.35f, 0.55f, 0.65f};
        mSweepGradient = new SweepGradient(mCenterX, mCenterX, mArcInsideColors, mPositions);
        final float rotationDegrees = mSwipeAngle * 1.0f / 2;
        mMatrix.setRotate(rotationDegrees, mCenterX, mCenterX);
        mSweepGradient.setLocalMatrix(mMatrix);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawArcInside(canvas);
        drawArcOutside(canvas);
        drawArcLoading(canvas);

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

    /**
     * Set ratio which makes influenced on the arc outside's swipe angle.
     *
     * @param ratio
     */
    public void setRatio(double ratio) {
        if (ratio < 0) {
            ratio = 0;
        }
        if (ratio > 1) {
            ratio = 1;
        }
        this.mRatio = ratio;
        setProgress((int) (mSwipeAngle * ratio));
    }

    /**
     * Draw the arc outside for current progress.
     *
     * @param canvas
     */
    private void drawArcOutside(Canvas canvas) {
        mPath.reset();
        mPath.addArc(mRectOval, mStartAngle + currentProgress, mSwipeAngle - currentProgress);
        mPathMeasure.setPath(mPath, false);
        mPathMeasure.getPosTan(mPathMeasure.getLength() * mCurrentValue, mPos, mTan);
        mPaintOutside.setShader(mSweepGradient);
        canvas.drawPath(mPath, mPaintOutside);
    }

    /**
     * Draw the arc loading which covers the arc outside(rainbow).
     *
     * @param canvas
     */
    private void drawArcLoading(Canvas canvas) {
        mPath.reset();
        mPath.addArc(mRectOval, mStartAngle, currentProgress == 0 ? 0.01f : currentProgress);
        mPathMeasure.setPath(mPath, false);
        mPathMeasure.getPosTan(mPathMeasure.getLength() * mCurrentValue, mPos, mTan);

        mBitmapDegrees = (float) (Math.atan2(mTan[1], mTan[0]) * 180.0 / Math.PI);

        mMatrix.reset();
        mMatrix.postRotate(mBitmapDegrees, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);
        mMatrix.postTranslate(mPos[0] - mBitmap.getWidth() / 2, mPos[1] - mBitmap.getHeight() / 2);
        mPaintOutside.setShader(mSweepGradient);
        canvas.drawPath(mPath, mPaintLoading);
        canvas.drawBitmap(mBitmap, mMatrix, null);
    }

    public int getProgress() {
        return mProgress;
    }

    /**
     * Set current value([0,1])
     */
    private void setCurrentValue() {
//        mCurrentValue += mProgress * 1.0 / mSwipeAngle;
        mCurrentValue += currentProgress * 1.0 / mSwipeAngle;
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
         * Constructor called from {@link CarLoadingViewTwo#onSaveInstanceState()}
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
                = new Creator<CarLoadingViewTwo.SavedState>() {
            public CarLoadingViewTwo.SavedState createFromParcel(Parcel in) {
                return new CarLoadingViewTwo.SavedState(in);
            }

            public CarLoadingViewTwo.SavedState[] newArray(int size) {
                return new CarLoadingViewTwo.SavedState[size];
            }
        };
    }

    @Override
    public Parcelable onSaveInstanceState() {
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

    @Override
    public void onRestoreInstanceState(Parcelable state) {
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
     *
     * @return
     */
    public Bitmap getBitmap() {
        return mBitmap;
    }

    /**
     * Set the bitmap using for moving forward or back.
     *
     * @param bitmap
     */
    public void setBitmap(Bitmap bitmap) {
        if (null == bitmap) {
            throw new NullPointerException("the bitmap that you specified must not be null.");
        }
        this.mBitmap = bitmap;
    }

}
