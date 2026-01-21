package com.example.teacherliuwriting;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.Log;

import java.util.List;

/**
 * Utility class to compare different skeleton generation methods
 * Helps evaluate quality and performance of:
 * 1. Simple sampling (current genSkeleton)
 * 2. BitmapThinAlgo (Zhang-Suen)
 * 3. LightThinAlgo (simple center detection)
 */
public class ThinningComparison {
    
    private static final String TAG = "ThinningComparison";
    
    /**
     * Compare all three methods and log results
     */
    public static void compareAllMethods(Cons cons) {
        Log.d(TAG, "=== Comparing Skeleton Generation Methods ===");
        Log.d(TAG, "Stroke: " + cons.getName());
        Log.d(TAG, "Contour points: " + cons.size());
        
        // Method 1: Simple sampling (current)
        long start1 = System.currentTimeMillis();
        List<ConPoint> skeleton1 = LightImgProc.genSkeleton(cons);
        long time1 = System.currentTimeMillis() - start1;
        Log.d(TAG, "Method 1 (Simple Sampling): " + skeleton1.size() + " points in " + time1 + "ms");
        
        // Method 2: BitmapThinAlgo (Zhang-Suen)
        long start2 = System.currentTimeMillis();
        Bitmap contourBitmap = createBitmapFromContour(cons);
        Bitmap thinned = BitmapThinAlgo.thinning(contourBitmap);
        List<Point> skeleton2 = BitmapThinAlgo.extractSkeletonPoints(thinned);
        long time2 = System.currentTimeMillis() - start2;
        Log.d(TAG, "Method 2 (Zhang-Suen): " + skeleton2.size() + " points in " + time2 + "ms");
        
        // Method 3: LightThinAlgo (simple center)
        long start3 = System.currentTimeMillis();
        Bitmap thinned3 = LightThinAlgo.thinning(contourBitmap);
        List<Point> skeleton3 = extractPoints(thinned3);
        long time3 = System.currentTimeMillis() - start3;
        Log.d(TAG, "Method 3 (Simple Center): " + skeleton3.size() + " points in " + time3 + "ms");
        
        // Analysis
        Log.d(TAG, "Point density comparison:");
        Log.d(TAG, "  Simple Sampling: " + String.format("%.2f", (double)skeleton1.size() / cons.size() * 100) + "% of contour");
        Log.d(TAG, "  Zhang-Suen: " + String.format("%.2f", (double)skeleton2.size() / cons.size() * 100) + "% of contour");
        Log.d(TAG, "  Simple Center: " + String.format("%.2f", (double)skeleton3.size() / cons.size() * 100) + "% of contour");
        
        Log.d(TAG, "Speed comparison (baseline = Simple Sampling):");
        Log.d(TAG, "  Simple Sampling: 1.0x");
        Log.d(TAG, "  Zhang-Suen: " + String.format("%.2f", (double)time2 / time1) + "x");
        Log.d(TAG, "  Simple Center: " + String.format("%.2f", (double)time3 / time1) + "x");
        
        Log.d(TAG, "===========================================");
    }
    
    /**
     * Create a bitmap from contour for thinning
     */
    private static Bitmap createBitmapFromContour(Cons cons) {
        ConRect rect = cons.minContourRect();
        int width = (int)(rect.right - rect.left) + 20;
        int height = (int)(rect.bottom - rect.top) + 20;
        int offsetX = (int)rect.left - 10;
        int offsetY = (int)rect.top - 10;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        
        // Black background
        canvas.drawColor(Color.BLACK);
        
        // White filled contour
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(false); // Sharp edges for thinning
        
        Path path = new Path();
        if (cons.size() > 0) {
            ConPoint first = cons.get(0);
            path.moveTo(first.dPt().x - offsetX, first.dPt().y - offsetY);
            
            for (int i = 1; i < cons.size(); i++) {
                ConPoint pt = cons.get(i);
                path.lineTo(pt.dPt().x - offsetX, pt.dPt().y - offsetY);
            }
            path.close();
        }
        
        canvas.drawPath(path, paint);
        
        return bitmap;
    }
    
    /**
     * Extract white pixel points from bitmap
     */
    private static List<Point> extractPoints(Bitmap bitmap) {
        List<Point> points = new java.util.ArrayList<>();
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int brightness = getBrightness(pixel);
                if (brightness > 128) {
                    points.add(new Point(x, y));
                }
            }
        }
        
        return points;
    }
    
    /**
     * Calculate pixel brightness
     */
    private static int getBrightness(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        return (int)(0.299 * r + 0.587 * g + 0.114 * b);
    }
    
    /**
     * Visual comparison - save all three results as bitmaps
     */
    public static void saveComparisonImages(Cons cons, android.content.Context context) {
        // Method 1: Simple sampling
        List<ConPoint> skeleton1 = LightImgProc.genSkeleton(cons);
        Bitmap result1 = visualizeSkeleton(skeleton1, cons);
        LightImgProc.saveBitmap(context, result1, "skeleton_simple", "ThinningComparison");
        
        // Method 2: Zhang-Suen
        Bitmap contourBitmap = createBitmapFromContour(cons);
        Bitmap thinned2 = BitmapThinAlgo.thinning(contourBitmap);
        LightImgProc.saveBitmap(context, thinned2, "skeleton_zhangsuen", "ThinningComparison");
        
        // Method 3: Simple center
        Bitmap thinned3 = LightThinAlgo.thinning(contourBitmap);
        LightImgProc.saveBitmap(context, thinned3, "skeleton_center", "ThinningComparison");
        
        // Original contour
        LightImgProc.saveBitmap(context, contourBitmap, "contour_original", "ThinningComparison");
        
        Log.d(TAG, "Comparison images saved to ThinningComparison folder");
    }
    
    /**
     * Visualize skeleton points as bitmap
     */
    private static Bitmap visualizeSkeleton(List<ConPoint> skeleton, Cons cons) {
        ConRect rect = cons.minContourRect();
        int width = (int)(rect.right - rect.left) + 20;
        int height = (int)(rect.bottom - rect.top) + 20;
        int offsetX = (int)rect.left - 10;
        int offsetY = (int)rect.top - 10;
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.BLACK);
        
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(1);
        
        for (ConPoint pt : skeleton) {
            int x = (int)(pt.dPt().x - offsetX);
            int y = (int)(pt.dPt().y - offsetY);
            canvas.drawPoint(x, y, paint);
        }
        
        return bitmap;
    }
    
    /**
     * Quick test method - call this from your activity
     */
    public static void runQuickTest(Cons cons, android.content.Context context) {
        Log.d(TAG, "Running quick thinning test...");
        
        // Performance test
        compareAllMethods(cons);
        
        // Visual test
        saveComparisonImages(cons, context);
        
        Log.d(TAG, "Test complete! Check logs and saved images.");
    }
}
