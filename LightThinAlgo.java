package com.example.teacherliuwriting;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Lightweight replacement for OpenCV thinning algorithm
 * Uses a simplified approach for skeleton generation
 */
public class LightThinAlgo {
    
    /**
     * Simple thinning algorithm replacement
     * Instead of complex morphological thinning, we use a simplified approach
     * that extracts key points from the contour
     */
    public static Bitmap thinning(Bitmap src) {
        if (src == null) return null;
        
        int width = src.getWidth();
        int height = src.getHeight();
        
        // Create result bitmap
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        result.eraseColor(Color.BLACK);
        
        // Simple approach: find center line by averaging positions
        // This is much simpler than true morphological thinning but sufficient for basic use
        
        int[] pixels = new int[width * height];
        src.getPixels(pixels, 0, width, 0, 0, width, height);
        
        // Find white pixels (foreground)
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int index = y * width + x;
                
                if (pixels[index] == Color.WHITE || Color.alpha(pixels[index]) > 128) {
                    // Check if this is a center pixel (simplified)
                    boolean isCenter = isApproximateCenter(pixels, x, y, width, height);
                    if (isCenter) {
                        result.setPixel(x, y, Color.WHITE);
                    }
                }
            }
        }
        
        return result;
    }
    
    /**
     * Simple check to see if a pixel is approximately in the center of a shape
     */
    private static boolean isApproximateCenter(int[] pixels, int x, int y, int width, int height) {
        // Count white pixels in 3x3 neighborhood
        int whiteCount = 0;
        int totalCount = 0;
        
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    int index = ny * width + nx;
                    totalCount++;
                    
                    if (pixels[index] == Color.WHITE || Color.alpha(pixels[index]) > 128) {
                        whiteCount++;
                    }
                }
            }
        }
        
        // Consider it a center if most neighbors are white (part of the shape)
        return whiteCount >= totalCount * 0.6;
    }
}