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
 * Created by YuLin on 2017/3/20 0020.
 * An android custom widget called CarRunningArcView,which has the effect that a car(maybe other icon)
 * can run on an arc forward and back.And you can transform it to a count down timer view or a loading
 * with arc shape and etc.
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
    private int mArcInsideColor = Color.GRAY;
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

    private SweepGradient mSweepGradient;
    private int[] mArcInsideColors;

    private boolean[] mIsFinished = new boolean[1];
    private boolean[] mIsReverse = new boolean[1];

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

        setRatio(mRatio, null == super.onSaveInstanceState() ? new boolean[] {false} : new boolean[] {true});
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
        mBitmap = BitmapFactory.decodeResource(this.getContext().getResources(), R.mipmap.icon_car);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        mWidth = getWidth();
        mHeight = getHeight();
        mCenterX = mWidth / 2;
        mCenterY = mHeight / 2;
        if (mRadius + mArcOutSideStroke > Math.min(mWidth / 2, mHeight / 2) || mRadius <= 0) {
            mRadius = Math.min(mWidth / 2, mHeight / 2) - mArcOutSideStroke;
        }
        mRectOval = new RectF(mCenterX - mRadius, mCenterY - mRadius, mCenterX + mRadius, mCenterY + mRadius);
        mSweepGradient = new SweepGradient(mCenterX, mCenterX, mArcInsideColors, null);
        final float rotationDegrees = mSwipeAngle * 1.0f / 2;
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
        if (mIsFinished[0] || mIsReverse[0]) {
            mBitmapDegrees += 180;
        }
        mMatrix.reset();
        mMatrix.postRotate(mBitmapDegrees, mBitmap.getWidth() / 2, mBitmap.getHeight() / 2);
        mMatrix.postTranslate(mPos[0] - mBitmap.getWidth() / 2, mPos[1] - mBitmap.getHeight() / 2);   // 将图片绘制中心调整到与当前点重合

        mPaintOutside.setShader(mSweepGradient);
        canvas.drawPath(mPath, mPaintOutside);
        canvas.drawBitmap(mBitmap, mMatrix, mPaintOutside);
    }

    /**
     * Get ratio.
     *
     * @return
     */
    public  double getRatio() {
        return mRatio;
    }

    /**
     * Set ratio which makes influenced on the arc outside's swipe angle.
     *
     * @param ratio
     */
    public  void setRatio(double ratio, final boolean[] isInverse) {
        this.mRatio = ratio;
        this.mIsReverse = isInverse;
        new Thread(new Runnable() {
            @Override
            public void run() {
                startRunning();
            }
        }).start();
    }

    /**
     * Let the icon(here is a red car) run around the arc.
     */
    private void startRunning() {
        while (true) {
            if (!mIsReverse[0]) {//move clockwise
                if (mProgress < mSwipeAngle * mRatio) {
                    mProgress++;
                    setCurrentValue();
                    postInvalidate();
                    try {
                        Thread.sleep(mSleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    mIsFinished[0] = true;
                    postInvalidate();
                    break;
                }
            } else {//move anticlockwise
                if (mProgress >= mSwipeAngle * mRatio) {
                    mProgress--;
                    setCurrentValue();
                    postInvalidate();
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "mProgress,mCurrentValue:" + mProgress + "," + mProgress * 1.0 / mSwipeAngle);
                } else {
                    break;
                }
            }
        }
    }

    /**
     * Set current value([0,1])
     */
    private void setCurrentValue() {
        mCurrentValue += mProgress * 1.0 / mSwipeAngle;
        if (mCurrentValue >= 1) {
            mCurrentValue = 1;
        }
    }

    /**
     * Saved state for arc's progress.
     */
    private static class SavedState extends BaseSavedState {
        int progress;
        float bitmapDegrees;
        float currentValue;
        boolean[] isReverse = new boolean[1];
        boolean[] isFinished = new boolean[1];
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
            in.readBooleanArray(isReverse);
            in.readBooleanArray(isFinished);
            in.readFloatArray(pos);
            in.readFloatArray(tan);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(progress);
            out.writeFloat(bitmapDegrees);
            out.writeFloat(currentValue);
            out.writeBooleanArray(isReverse);
            out.writeBooleanArray(isFinished);
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
        ss.isReverse = mIsReverse;
        ss.isFinished = mIsFinished;
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
        mIsReverse = ss.isReverse;
        mIsFinished = ss.isFinished;
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

    public Bitmap getBitmap() {
        return mBitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        if(null ==bitmap) {
            throw new NullPointerException("the bitmap that you specified must not be null.");
        }
        this.mBitmap = bitmap;
    }
}
