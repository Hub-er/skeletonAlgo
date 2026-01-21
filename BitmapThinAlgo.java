package com.example.teacherliuwriting;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.Arrays;

/**
 * Pure Android Bitmap implementation of Zhang-Suen thinning algorithm
 * Replaces OpenCV-based thin.java with no external dependencies
 * 
 * This implements the same morphological thinning algorithm as the OpenCV version
 * but uses Android Bitmap and int[] arrays instead of Mat objects.
 */
public class BitmapThinAlgo {
    
    private static final String TAG = "BitmapThinAlgo";
    
    /**
     * Main thinning function - implements Zhang-Suen thinning algorithm
     * 
     * @param src Source bitmap (should be binary: black background, white foreground)
     * @return Thinned bitmap (skeleton)
     */
    public static Bitmap thinning(Bitmap src) {
        if (src == null) {
            Log.e(TAG, "Source bitmap is null");
            return null;
        }
        
        int width = src.getWidth();
        int height = src.getHeight();
        
        Log.d(TAG, "Starting thinning on " + width + "x" + height + " bitmap");
        
        // Convert bitmap to binary array (0 or 1)
        int[] pixels = bitmapToBinary(src);
        int[] prev = new int[pixels.length];
        
        int iteration = 0;
        int diffCount;
        
        // Iterate until convergence (no more changes)
        do {
            // Save previous state
            System.arraycopy(pixels, 0, prev, 0, pixels.length);
            
            // Two-pass thinning (even and odd iterations)
            thinningIteration(pixels, width, height, 0);
            thinningIteration(pixels, width, height, 1);
            
            // Count differences
            diffCount = countDifferences(pixels, prev);
            iteration++;
            
            Log.d(TAG, "Iteration " + iteration + ": " + diffCount + " pixels changed");
            
        } while (diffCount > 0 && iteration < 1000); // Safety limit
        
        Log.d(TAG, "Thinning completed after " + iteration + " iterations");
        
        // Convert binary array back to bitmap
        return binaryToBitmap(pixels, width, height);
    }
    
    /**
     * Perform one thinning iteration (Zhang-Suen algorithm)
     * 
     * @param pixels Binary pixel array (0 or 1)
     * @param width Image width
     * @param height Image height
     * @param iter Iteration type: 0=even, 1=odd
     */
    private static void thinningIteration(int[] pixels, int width, int height, int iter) {
        // Create marker array to track pixels to be deleted
        int[] marker = new int[pixels.length];
        
        // Process each pixel (excluding border)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                
                // Skip if pixel is already black (background)
                if (pixels[index] == 0) continue;
                
                // Get 8 neighbors in order: nw, no, ne, we, ea, sw, so, se
                int nw = pixels[(y-1) * width + (x-1)];
                int no = pixels[(y-1) * width + x];
                int ne = pixels[(y-1) * width + (x+1)];
                int we = pixels[y * width + (x-1)];
                int ea = pixels[y * width + (x+1)];
                int sw = pixels[(y+1) * width + (x-1)];
                int so = pixels[(y+1) * width + x];
                int se = pixels[(y+1) * width + (x+1)];
                
                // Calculate A: number of 0→1 transitions in circular order
                int A = ((no == 0 && ne == 1) ? 1 : 0) +
                        ((ne == 0 && ea == 1) ? 1 : 0) +
                        ((ea == 0 && se == 1) ? 1 : 0) +
                        ((se == 0 && so == 1) ? 1 : 0) +
                        ((so == 0 && sw == 1) ? 1 : 0) +
                        ((sw == 0 && we == 1) ? 1 : 0) +
                        ((we == 0 && nw == 1) ? 1 : 0) +
                        ((nw == 0 && no == 1) ? 1 : 0);
                
                // Calculate B: number of non-zero neighbors
                int B = no + ne + ea + se + so + sw + we + nw;
                
                // Calculate connectivity conditions (differ for even/odd iterations)
                int m1 = (iter == 0) ? (no * ea * so) : (no * ea * we);
                int m2 = (iter == 0) ? (ea * so * we) : (no * so * we);
                
                // Mark pixel for deletion if conditions are met
                if (A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0) {
                    marker[index] = 1;
                }
            }
        }
        
        // Apply marker: remove marked pixels
        for (int i = 0; i < pixels.length; i++) {
            if (marker[i] == 1) {
                pixels[i] = 0;
            }
        }
    }
    
    /**
     * Convert bitmap to binary array (0 or 1)
     * White pixels (foreground) → 1
     * Black pixels (background) → 0
     */
    private static int[] bitmapToBinary(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        int[] binary = new int[width * height];
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        
        for (int i = 0; i < pixels.length; i++) {
            // Consider white or light pixels as foreground (1)
            int brightness = getBrightness(pixels[i]);
            binary[i] = (brightness > 128) ? 1 : 0;
        }
        
        return binary;
    }
    
    /**
     * Convert binary array back to bitmap
     * 1 → White pixel
     * 0 → Black pixel
     */
    private static Bitmap binaryToBitmap(int[] binary, int width, int height) {
        int[] pixels = new int[binary.length];
        
        for (int i = 0; i < binary.length; i++) {
            pixels[i] = (binary[i] == 1) ? Color.WHITE : Color.BLACK;
        }
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        
        return bitmap;
    }
    
    /**
     * Calculate pixel brightness using standard luminance formula
     */
    private static int getBrightness(int pixel) {
        int r = (pixel >> 16) & 0xFF;
        int g = (pixel >> 8) & 0xFF;
        int b = pixel & 0xFF;
        return (int)(0.299 * r + 0.587 * g + 0.114 * b);
    }
    
    /**
     * Count number of different pixels between two arrays
     */
    private static int countDifferences(int[] current, int[] previous) {
        int count = 0;
        for (int i = 0; i < current.length; i++) {
            if (current[i] != previous[i]) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Helper method to visualize skeleton points
     * Extracts white pixel coordinates from thinned bitmap
     */
    public static java.util.List<android.graphics.Point> extractSkeletonPoints(Bitmap thinned) {
        java.util.List<android.graphics.Point> points = new java.util.ArrayList<>();
        
        int width = thinned.getWidth();
        int height = thinned.getHeight();
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = thinned.getPixel(x, y);
                if (getBrightness(pixel) > 128) {
                    points.add(new android.graphics.Point(x, y));
                }
            }
        }
        
        Log.d(TAG, "Extracted " + points.size() + " skeleton points");
        return points;
    }
}
