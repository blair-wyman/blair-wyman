package com.ibm.iTestJava.thread.laika;
/*--------------------------------------------------------------------*/
/*     IBM grants you a nonexclusive license to use this as an        */
/*     example from which you can generate similar function           */
/*     tailored to your own specific needs.                           */
/*                                                                    */
/*     This sample code is provided by IBM for illustrative           */
/*     purposes only. These examples have not been thoroughly         */
/*     tested under all conditions. IBM, therefore, cannot            */
/*     guarantee or imply reliability, serviceability, or function    */
/*     of these programs.                                             */
/*                                                                    */
/*     All programs contained herein are provided to you "AS IS"      */
/*     without any warranties of any kind. The implied warranties     */
/*     of merchantability and fitness for a particular purpose are    */
/*     expressly disclaimed.                                          */
/*--------------------------------------------------------------------*/

import java.util.*;
import java.awt.geom.*;
import java.io.*;

public interface Trackable {
    
    // these interfaces let us track the history of our Growable 2D
    Rectangle2D.Double domain();  // area occupied over seed's lifetime    
    Rectangle2D.Double jitter();  // area of last n iterations of seed   
    void setJitterDepth(int jitterDepth);
    int getAvailableJitterDepth();
    ArrayList<Point2D.Double> getJitterPoints(); // brief history of jitter
    ArrayList<Rectangle2D.Double> getJitterRects(); // history of jitter rects
    void plotStdout();  // draw a simple 30x15 diagram of the jitter buffer
    void plotStdout(int rows, int cols); // alternate sizes of output
    void plotStdout(int rows, int cols, boolean evenIfDead);
    char[][] buildMGridJitterView(int rows, int cols); // get an M view of the jitter
    char[][] buildMGridDomainView(int rows, int cols); // get an M view of the domain
    int[][] buildCountViewCumulative(int rows, int cols, boolean crop); // cropped 
    int[][] buildTempViewCumulative(int rows, int cols, boolean crop); // cropped
    int[][] buildCountViewJitter(int rows, int cols); // just jitter counts
    int[][] buildTempViewJitter(int rows, int cols); // just jitter temps
    void plotSquareJitterImage(int size, String fmt, File f); // plot jitter to a file
    void plotSquareCumulativeImage(int size, String ofmt, File f); // plot full "life" 
    void plotSquareCroppedCumulativeImage(int size, String ofmt, File f); // plot full 
    void plotExperimental(int size, String ofmt, File f); // plot to file -- experiment    
}    
