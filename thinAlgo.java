package com.example;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;

/**
 * Perform one thinning iteration.
 * Normally you wouldn't call this function directly from your code.
 *
 * Parameters:
 * 		im    Binary image with range = [0,1]
 * 		iter  0=even, 1=odd
 */
public class ThinAlgo {
    
    public static Mat thinningIteration(Mat img, int iter){
        if (img.channels() != 1) {
            return img;
        }
        if (img.depth() == 1) {
            return img;
        }
        if (img.rows() <= 3 || img.cols() <= 3) {
            return img;
        }
    
        Mat marker = Mat.zeros(img.size(), CvType.CV_8UC1);

        int nRows = img.rows();
        int nCols = img.cols();

        if (img.isContinuous()) {
            nCols *= nRows;
            nRows = 1;
        }

        int x, y;
        byte nw, no, ne;
        byte we, me, ea;
        byte sw, so, se;

        // uchar *pDst;
        byte[] pDst = new byte[nCols];
        
        // initialize row pointers
        byte[] pCurr = new byte[nCols];
        byte[] pBelow = new byte[nCols];
        byte[] pAbove = new byte[nCols];

        for (y = 1; y < img.rows() - 1; ++y) {
            // shift the rows up by one
            img.get(y-1, 0, pAbove);
            img.get(y, 0, pCurr);
            img.get(y+1, 0, pBelow); 

            marker.get(y, 0, pDst);

            // initialize col pointers
            no = pAbove[0];
            ne = pAbove[1];
            me = pCurr[0];
            ea = pCurr[1];
            so = pBelow[0];
            se = pBelow[1];

            for (x = 1; x < img.cols() - 1; ++x) {
                // shift col pointers left by one (scan left to right)
                nw = no;
                no = ne;
                ne = pAbove[x+1];
                we = me;
                me = ea;
                ea = pCurr[x+1];
                sw = so;
                so = se;
                se = pBelow[x+1];

                int A  =  ((no == 0 && ne == 1) ? 1 : 0) 
                        + ((ne == 0 && ea == 1) ? 1 : 0) 
                        + ((ea == 0 && se == 1) ? 1 : 0) 
                        + ((se == 0 && so == 1) ? 1 : 0) 
                        + ((so == 0 && sw == 1) ? 1 : 0) 
                        + ((sw == 0 && we == 1) ? 1 : 0) 
                        + ((we == 0 && nw == 1) ? 1 : 0) 
                        + ((nw == 0 && no == 1) ? 1 : 0);
                
                int B  = no + ne + ea + se + so + sw + we + nw;
                int m1 = (iter == 0 ? (no * ea * so) : (no * ea * we));
                int m2 = (iter == 0 ? (ea * so * we) : (no * so * we));
    
                if (A == 1 && (B >= 2 && B <= 6) && m1 == 0 && m2 == 0)
                    pDst[x] = 1;
                    
            }
            marker.put(y, 0, pDst);
        }

        // img &= ~marker;
        Core.bitwise_not(marker, marker);
        Core.bitwise_and(img, marker, img);
        return img;
    }


    static Mat thinning(Mat src) {
        Mat dst = src.clone();    
        Core.divide(dst, new Scalar(255), dst);  // convert pixel to ： 0 | 1

        Mat prev = Mat.zeros(dst.size(), CvType.CV_8UC1);
        Mat diff = new Mat();

        do {
            dst = thinningIteration(dst, 0);
            dst = thinningIteration(dst, 1);
            Core.absdiff(dst, prev, diff);
            dst.copyTo(prev);
        } 
        while (Core.countNonZero(diff) > 0);

        Scalar scalar = new Scalar(255); // the specific constant
        Core.multiply(dst, scalar, dst); // mat values are all 255 now

        return dst;
    } 
}
