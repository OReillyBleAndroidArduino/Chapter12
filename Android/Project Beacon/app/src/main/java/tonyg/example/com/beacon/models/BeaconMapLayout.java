package tonyg.example.com.beacon.models;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.LinearLayout;

import java.util.ArrayList;

import tonyg.example.com.beacon.R;
import tonyg.example.com.beacon.ble.BleBeacon;

/**
 * This class represents a the visual Beacon Map
 *
 * @author Tony Gaitatzis backupbrain@gmail.com
 * @date 2016-03-06
 */
public class BeaconMapLayout extends LinearLayout {
    private static final String TAG = BeaconMapLayout.class.getSimpleName();

    /** Graphic properties **/
    private static final int BITMAP_WIDTH = 1000;
    private static final int BITMAP_HEIGHT = 800;
    private static final String PAINT_COLOR = "#CD5C5C";
    private static final int STROKE_COLOR = 5;

    private static final int X_OFFSET = 50;
    private static final int Y_OFFSET = 50;
    private static final int M_PX_MULTIPLIER = 400;

    private Bitmap mBeaconIcon, mCentralIcon; // icons
    private Canvas mCanvas = new Canvas();
    private Paint mPaint = new Paint(); // paint properties
    private Bitmap mMapBitmap; // rendered map

    private ArrayList<BleBeacon> mBeaconList = new ArrayList<BleBeacon>(); // list of beacons
    private boolean mIsCentralPositionSet = false;
    private double[] mCentralPosition; // central position

    /**
     * Create a new BeaconMapLayout
     *
     * @param context the Activity context
     * @param attrs
     * @param defStyle
     */
    public BeaconMapLayout(Context context) {
        super(context);
        initialize();
    }
    /**
     * Create a new BeaconMapLayout
     *
     * @param context the Activity context
     * @param attrs
     * @param defStyle
     */
    public BeaconMapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    /**
     * Create a new BeaconMapLayout
     *
     * @param context the Activity context
     * @param attrs
     * @param defStyle
     */
    public BeaconMapLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    /**
     * Initialize the map
     */
    public void initialize() {
        mBeaconIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.peripheral);

        mCentralIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.central);


        mPaint.setColor(Color.parseColor(PAINT_COLOR));
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(STROKE_COLOR);
        mMapBitmap = Bitmap.createBitmap(BITMAP_WIDTH, BITMAP_HEIGHT, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mMapBitmap);
    }

    /**
     * Add a new Beacon
     *
     * @param beacon
     */
    public void addBeacon(BleBeacon beacon) {
        mBeaconList.add(beacon);
    }

    /**
     * Position the central
     *
     * @param x x location
     * @param y y location
     */
    public void setCentralPosition(double x, double y) {
        mIsCentralPositionSet = true;
        mCentralPosition = new double[]{x, y};
    }

    /**
     * Draw the Beacon Position
     *
     * @param beacon
     */
    private void drawBeaconPosition(BleBeacon beacon) {
        Log.d(TAG, "Drawing point: " + beacon);
        Rect sourceRect = new Rect(0, 0, mBeaconIcon.getWidth(), mBeaconIcon.getHeight());
        Rect destRect = new Rect((int) (beacon.getXLocation() * M_PX_MULTIPLIER - 50 + X_OFFSET), (int) (beacon.getYLocation() * M_PX_MULTIPLIER - 50 + Y_OFFSET), (int) (beacon.getXLocation() * M_PX_MULTIPLIER + 50 + X_OFFSET), (int) (beacon.getYLocation() * M_PX_MULTIPLIER + 50 + Y_OFFSET));
        mCanvas.drawBitmap(mBeaconIcon, sourceRect, destRect, null);
        mCanvas.drawCircle((float) beacon.getXLocation() * M_PX_MULTIPLIER + X_OFFSET, (float) beacon.getYLocation() * M_PX_MULTIPLIER + Y_OFFSET, (float) beacon.getDistance() * M_PX_MULTIPLIER, mPaint);


    }

    /**
     * Draw the Central onscreen
     *
     * @param location
     */
    public void drawCentralPosition(double[] location) {
        Rect sourceRect = new Rect(0, 0, mCentralIcon.getWidth(), mCentralIcon.getHeight());
        Rect destRect = new Rect((int) (location[0] * M_PX_MULTIPLIER - 36 + X_OFFSET), (int) (location[1] * M_PX_MULTIPLIER - 71 + Y_OFFSET), (int) (location[0] * M_PX_MULTIPLIER + 37 + X_OFFSET), (int) (location[1] * M_PX_MULTIPLIER + 72 + Y_OFFSET));
        mCanvas.drawBitmap(mCentralIcon, sourceRect, destRect, null);
    }

    /**
     * Draw the frame
     */
    public void draw() {
        // clear canvas
        mCanvas.drawColor(Color.WHITE);
        // draw each beacon
        for (BleBeacon beacon : mBeaconList) {
            drawBeaconPosition(beacon);
        }
        // draw central position
        if (mIsCentralPositionSet) {
            drawCentralPosition(mCentralPosition);
        }
        this.setBackgroundDrawable(new BitmapDrawable(mMapBitmap));
    }

}

