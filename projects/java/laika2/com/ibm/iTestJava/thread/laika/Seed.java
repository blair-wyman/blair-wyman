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
import java.awt.*;
import java.awt.Color;
import java.awt.image.*;
import javax.imageio.*;

class Seed implements Growable, Serializable {

    public Seed(long dna) {
        _dna = Math.abs(dna);
        _fixedName = String.format("STRAIN_0x%016X", _dna);
        Random rndm = new Random(_dna);
        for (int i=0; i<10; ++i) {
            _coeffs[i] = (_range - (rndm.nextDouble() * (2 * _range))); 
        }
        _jitterBuffer = new LinkedList<Point2D.Double>();
        _jitterRects  = new LinkedList<Rectangle2D.Double>();
    }
    
    public void inoculate() {
    	_inoculated = true;
    }
    
    public Object call() {        
        while (Viable.contains(_state) && (_ambient > _currentTemp))
        {
            warmOneDegree();
        }
        return null;
    }

    // Implementation of Growable
    public boolean isViable()     { return Viable.contains(_state); }
    public long getTemp()         { return (isViable()) ? _currentTemp : _ambient; }
    
    public void poison()          { 
    	if (! (_state == GrowthState.INOCULATED)) { 
    		_state = GrowthState.POISONED; 
    	}   
    }
    public long tolerance()       { return _currentTemp; }
    public GrowthState getState() { return _state; }
    public String strainName()    { return _fixedName; }
    
    // Must implement Growable's super-interfaces
    public void setAmbientTemp(long temp) { _ambient = temp; }
    public boolean isHeating() { return (isViable() && (_ambient > _currentTemp)); }
    
    // Trackable
    public Rectangle2D.Double domain() {
        return (Rectangle2D.Double)_domainRect.clone();  // area occupied over lifetime 
    }
    public Rectangle2D.Double jitter() {  // area of last n iterations
        return (Rectangle2D.Double)_jitterRect.clone();
    }
    public int getAvailableJitterDepth() { return _jitterBuffer.size(); }

    private BufferedImage plotExperimentalViewToBufferedImage(int size, File f) {
 
        // go and get the countview and the tempview at the same resolution
    	//int[][] countView = buildCountViewCumulative(size, size, true);
        //int[][] tempView = buildTempViewCumulative(size, size, false);
        int[][] tempView2 = buildTempViewCumulative(size, size, false);
        /*int maxCount = 1;
        for (int i=0; i<size; ++i) {
            for (int j=0; j<size; ++j) {
                maxCount = Math.max(maxCount, countView[i][j]);
            }
        }
        int maxTemp = 0;
        for (int i=0; i<size; ++i) {
            for (int j=0; j<size; ++j) {
            	maxTemp = Math.max(maxTemp, tempView[i][j]);
            }
        }
        */
        BufferedImage bi = plotViewToBufferedImage(tempView2, true);
        try {  ImageIO.write(bi, "png", f); } catch(IOException ioe) {}
        
        return bi;
      
    }
    
    private BufferedImage plotCountViewToBufferedImage(int[][] countView) // called by PROOF
    {
        return plotViewToBufferedImage(countView, false);
    }
    
    private BufferedImage plotViewToBufferedImage(int[][] view, boolean experimental) 
    {
        // just need one dimension -- better be square view
        int size = view.length;
        
        int border  =  10;
        int frame   =   5;
        int header  =  20;
        int footer  = 100;
        
        int fullwidth = size + 2*(border + frame);
        int fullheight = size + 2*(border + frame) + header + footer;
        int framewidth = size + 2*(frame);
        int frameheight = framewidth;
        
        int originx = border + frame;
        int originy = header + border + frame;
        
        BufferedImage bi = 
        new BufferedImage(fullwidth, fullheight, BufferedImage.TYPE_INT_RGB);        
        Graphics2D grph = bi.createGraphics();
        
        RenderingHints hints = grph.getRenderingHints();
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        grph.addRenderingHints(hints);
        
        // total black then 1-pixel outline then black frame
        grph.setColor(Color.BLACK);
        grph.fillRect(0,0,fullwidth,fullheight);   
        grph.setColor(Color.WHITE);
        grph.fillRect(1,1,fullwidth-2,fullheight-2);        
        grph.setColor(Color.BLACK);       
        grph.fillRect(border, header+border, framewidth, frameheight);

        String msg;
        if (_inoculated) {
        	msg = String.format("%s @ %d degrees %s", 
        			_fixedName, _currentTemp, _state);
        } else {
	        msg = String.format("%s @ %d degrees (jitter=%d) %s", 
	        		_fixedName, _currentTemp, _jitterDepth, _state);
        }
        
        String matrixX = String.format(
        "%016X%016X%016X%016X%016X"
        ,  Double.doubleToLongBits(_coeffs[0])
        ,  Double.doubleToLongBits(_coeffs[1])
        ,  Double.doubleToLongBits(_coeffs[2])
        ,  Double.doubleToLongBits(_coeffs[3])
        ,  Double.doubleToLongBits(_coeffs[4]));
        String matrixY = String.format(
        "%016X%016X%016X%016X%016X"
        ,  Double.doubleToLongBits(_coeffs[5])
        ,  Double.doubleToLongBits(_coeffs[6])
        ,  Double.doubleToLongBits(_coeffs[7])
        ,  Double.doubleToLongBits(_coeffs[8])
        ,  Double.doubleToLongBits(_coeffs[9])
        );
        String decMatrix = String.format(
        "(%4.3f, %4.3f, %4.3f, %4.3f, %4.3f),(%4.3f, %4.3f, %4.3f, %4.3f, %4.3f)",
        _coeffs[0], _coeffs[1], _coeffs[2], _coeffs[3], _coeffs[4],
        _coeffs[5], _coeffs[6], _coeffs[7], _coeffs[8], _coeffs[9]
        );

        grph.setFont(new Font("Monospaced", (size < 500) ? Font.BOLD : Font.PLAIN, (size < 500) ? 8 : 12));
        grph.drawString(msg, 20, fullheight - 20);
        grph.setFont(new Font("Monospaced", Font.PLAIN, (size < 500) ? 8 : 10)); 
        grph.drawString(matrixX, 20, fullheight - 80);
        grph.drawString(matrixY, 20, fullheight - 68);
        grph.drawString(decMatrix, 20, fullheight - 56);
        grph.setColor(Color.WHITE);
        
        // get the max countView
        int maxCount = 1;
        int minCount = Integer.MAX_VALUE;
        int nonZeros = 0;
        for (int i=0; i<size; ++i) {
        	for (int j=0; j<size; ++j) {
        		if (0 != view[i][j]) {
        			++nonZeros;
        			maxCount = Math.max(maxCount, view[i][j]);
        			minCount = Math.min(minCount, view[i][j]);
        		}
        	}
        }
        
        // scale grayscale intensity by maxcount
        float dimmestColor = 0.5f;
        float brightestColor = 0.9f;
        float brightRange = (brightestColor - dimmestColor);
        float scaleFactor = brightRange / (maxCount - minCount);
        
        // fun fun fun
        float rTickle=0.1f; 
        float gTickle=0.1f;
        float bTickle=0.1f;
        
        for (int i=0; i<size; ++i) {
            for (int j=0; j<size; ++j) {
                if (view[j][i] > 0) {
                    int   thisCount = view[j][i];
                    if (experimental) {
                    	if (0 == thisCount % 2) 
                    		{rTickle =  0.1f; gTickle = 0.0f; bTickle = -0.1f;}
                    	else        
                			{rTickle = -0.1f; gTickle = 0.0f; bTickle =  0.1f;}
                    	switch(thisCount % 3) {
                    		case 0: gTickle = -0.5f; break;
                    		case 1: gTickle = -0.2f; break;
                    		case 2: gTickle =  0.1f; break;
                    	}
                    }                  
                    float grayValue = (dimmestColor + (scaleFactor * (thisCount - minCount)));                                       
                    bi.setRGB(j+originx, i+originy, 
                        new Color(grayValue+rTickle,grayValue+gTickle,grayValue+bTickle).getRGB());
                }
            }
        }
        return bi;
    }
    
    public void plotExperimental(int size, String oFmt, File f) {
        BufferedImage bi = plotExperimentalViewToBufferedImage(size,f);
        try {  ImageIO.write(bi, oFmt, f); } catch(IOException ioe) {}
    }
          
    public void plotSquareJitterImage(int size, String oFmt, File f) {
        BufferedImage bi = plotCountViewToBufferedImage(buildCountViewJitter(size, size));
        try {  ImageIO.write(bi, oFmt, f); } catch(IOException ioe) {}
    }
    
    public void plotSquareCumulativeImage(int size, String oFmt, File f) {
        BufferedImage bi = 
            plotCountViewToBufferedImage(buildCountViewCumulative(size, size, false)); 
        try {  ImageIO.write(bi, oFmt, f); } catch(IOException ioe) {}
    }
    
    public void plotSquareCroppedCumulativeImage(int size, String oFmt, File f) {
        BufferedImage bi = 
            plotCountViewToBufferedImage(buildCountViewCumulative(size, size, false)); 
        try { ImageIO.write(bi, oFmt, f); } catch(IOException ioe) {}
    }

    public int[][] buildTempViewCumulative(int prows, int pcols, boolean jitterCrop) 
    {        
        int subJitter = 100;
        
        // must regrow a copy of the seed and track it as we go
        int[][] rslt = new int[prows][pcols];
        XformParms xp = new XformParms(prows, pcols, (jitterCrop) ? jitter() : domain()); 

        Seed wrkSeed = new Seed(this._dna);
        wrkSeed.setJitterDepth(subJitter);
        if (this._inoculated) { wrkSeed.inoculate(); }
        
        long reqTemp = this._currentTemp;
        int nxtTemp = 0;
        while (nxtTemp < reqTemp) {
            nxtTemp += subJitter;
            wrkSeed.setAmbientTemp(Math.min(nxtTemp, reqTemp));
            wrkSeed.call();  // heat without feedback          
            ArrayList<Point2D.Double> wrkBuf = wrkSeed.getJitterPoints();
            int pX;
            int pY;
            int thisNum = 0;
            for (Point2D.Double p : wrkBuf) {
                Point2D.Double dstPt = new Point2D.Double();            
                xp._xlate.transform(p, dstPt);
                xp._scale.transform(dstPt, dstPt); 
                pX = (int) Math.floor(dstPt.getX());
                pY = (int) Math.floor(dstPt.getY());
                if ((! jitterCrop)
                    || ((0 <= pX) && (pX < pcols) && (0 <= pY) && (pY < prows)))
                {              
                    rslt[pX][pY] = ((nxtTemp - subJitter) + ++thisNum);
                }
            }
        }
        return rslt;
    }

    public int[][] buildCountViewCumulative(int prows, int pcols, boolean jitterCrop) {
        int subJitter = 100;
        
        // must regrow a copy of the seed and track it as we go
        int[][] rslt = new int[prows][pcols];
        XformParms xp = new XformParms(prows, pcols, (jitterCrop) ? jitter() : domain());
 
        Seed wrkSeed = new Seed(this._dna);
        wrkSeed.setJitterDepth(subJitter);
        if (this._inoculated) { wrkSeed.inoculate(); }
        
        long reqTemp = this._currentTemp;
        long nxtTemp = subJitter;
        
        for (nxtTemp=subJitter; nxtTemp < (reqTemp + subJitter); nxtTemp += subJitter) {
            wrkSeed.setAmbientTemp(Math.min(nxtTemp, reqTemp));
            wrkSeed.call();            
            ArrayList<Point2D.Double> wrkBuf = wrkSeed.getJitterPoints();
            int pX;
            int pY;           
            for (Point2D.Double p : wrkBuf) {
                Point2D.Double dstPt = new Point2D.Double();            
                xp._xlate.transform(p, dstPt);
                xp._scale.transform(dstPt, dstPt); 
                pX = (int) Math.floor(dstPt.getX());
                pY = (int) Math.floor(dstPt.getY());
                if ((! jitterCrop)
                    || ((0 <= pX) && (pX < pcols) && (0 <= pY) && (pY < prows)))
                {              
                    rslt[pX][pY]++;
                }
            }
        }
        return rslt;
    }
 
    /*
    public static enum ViewEnum { COUNT_CUMUL, TEMP_CUMUL, COUNT_JITTER, TEMP_JITTER };

    EnumMap<ViewEnum, int[][]> buildViews(int prows, int pcols, EnumSet<ViewEnum> which) { 
        EnumMap <ViewEnum,int[][]> rslt = new EnumMap<ViewEnum, int[][]>( ViewEnum.class );        
        for (ViewEnum v : which) {
            switch (v) {
                case COUNT_CUMUL:  
                    rslt.put(v, buildCountViewCumulative(prows, pcols, true)); break;
                case TEMP_CUMUL:   
                    rslt.put(v, buildTempViewCumulative(prows, pcols, true)); break;
                case COUNT_JITTER: 
                    rslt.put(v, buildCountViewJitter(prows, pcols)); break;
                case TEMP_JITTER:  
                    rslt.put(v, buildTempViewJitter(prows, pcols)); break;
            }
        }
        return rslt;
    }
    */
 
    private class XformParms {       
        public AffineTransform _xlate;
        public AffineTransform _scale;
        public double _originX;
        public double _originY;        
        public XformParms(int rows, int cols, Rectangle2D.Double plotRect) {
        	
            double rectW = plotRect.getWidth();
            double rectH = plotRect.getHeight();
            double rectX = plotRect.getX();
            double rectY = plotRect.getY();
            
            double rectFrame = (Math.max(rectW,rectH) * 0.05d);
            
            /* figure out which one needs adjusting to center*/
            double adjuster = (rectW - rectH) / 2.0d;            
            if (adjuster >= 0.0d) { 
            	rectY -= adjuster;
            } else {
            	rectX += adjuster;  /* N.B. sign here... */
            }

            /* Finally, provide a 10% frame around our plotting box */
            rectX -= rectFrame;
            rectY -= rectFrame;
 
            /* add a 10% buffer area around the domain */
            double scaleDiscriminant = (1.10d * Math.max(rectW, rectH));  
            double yScaleFactor = (double) (rows) / scaleDiscriminant;
            double xScaleFactor = (double) (cols) / scaleDiscriminant;
            
            _originX = /*xScaleFactor*/ -rectX;
            _originY = /*yScaleFactor*/ -rectY;
            _xlate = AffineTransform.getTranslateInstance(_originX, _originY);
            _scale = AffineTransform.getScaleInstance(xScaleFactor, yScaleFactor); 
        }            
    }     
        
    public int[][] buildTempViewJitter(int prows, int pcols) {        

        int[][] rslt = new int[pcols][prows];
        XformParms xp = new XformParms(prows, pcols, jitter()); 
        int pX;
        int pY;
        int count = _jitterDepth;
/*       int startTemp = ((int)_currentTemp - _jitterDepth); */
        for (Point2D.Double j : getJitterPoints()) {
            Point2D.Double dstPt = new Point2D.Double();            
            xp._xlate.transform(j, dstPt);
            xp._scale.transform(dstPt, dstPt); 
            pX = (int) Math.floor(dstPt.getX());
            pY = (int) Math.floor(dstPt.getY()); 
            if ((0<=pX) && (pX<pcols) && (0<=pY) && (pY<prows)) {            
                rslt[pX][pY] = ((int)_currentTemp - count);
            }
        }
        return rslt;
    }
      
    public int[][] buildCountViewJitter(int prows, int pcols) 
    {       
        int[][] rslt = new int[pcols][prows];
        XformParms xp = new XformParms(prows, pcols, jitter()); 
        int pX;
        int pY;
        
        for (Point2D.Double j : _jitterBuffer) {
            Point2D.Double dstPt = new Point2D.Double();            
            xp._xlate.transform(j, dstPt);
            xp._scale.transform(dstPt, dstPt); 
            pX = (int) Math.floor(dstPt.getX());
            pY = (int) Math.floor(dstPt.getY());                
            rslt[pX][pY]++;
        }
        return rslt;
    }
    
    // method overloading...  Gory example.
    public void plotStdout()                    { plotStdout(15,30,false); }
    public void plotStdout(int r)               { plotStdout(r, (int)(2.0f * r), false); }
    public void plotStdout(int r, int c)        { plotStdout(r, c, false); }
    public void plotStdout(int r, float a)      { plotStdout(r, (int)(a*r),false); }
    public void plotStdout(int r, float a, boolean f) { plotStdout(r, (int)(a*r),false); }
       
    public char[][] buildMGridJitterView(int rows, int cols) { 
        return buildMGridView0(_jitterRect, rows, cols); 
    }
    public char[][] buildMGridDomainView(int rows, int cols) { 
        return buildMGridView0(_domainRect, rows, cols); 
    }
    
    private char[][] buildMGridView0(Rectangle2D.Double plotRect, int prows, int pcols) 
    {         
        char[][] rslt = new char[pcols][prows];
        for (int col=0; col<pcols; ++col) { 
            for (int row=0; row<prows; ++row) { 
                rslt[col][row] = ' '; 
            } 
        }       
        XformParms xp = new XformParms(prows, pcols, plotRect);       
        ArrayList<Point2D.Double> jitterPts = getJitterPoints();
 
        int pX;
        int pY;
        
        for (Point2D.Double j : jitterPts) {
            Point2D.Double dstPt = new Point2D.Double();            
            xp._xlate.transform(j, dstPt);
            xp._scale.transform(dstPt, dstPt); 
            pX = (int) Math.floor(dstPt.getX());
            pY = (int) Math.floor(dstPt.getY());
            // do bounds check to avoid scaling issues
            if (   ((pX >= 0) && (pX < pcols)) 
                && ((pY >= 0) && (pY < prows)))
            {
                switch(rslt[pX][pY]) {
                    case ' ' : rslt[pX][pY] = '.'; break;
                    case '.' : rslt[pX][pY] = 'o'; break;
                    case 'o' : rslt[pX][pY] = 'O'; break;
                    case 'O' : rslt[pX][pY] = 'X'; break;
                    case 'X' : rslt[pX][pY] = 'M'; break;
                    default: break;
                }
            }
        } // for points in jitterBuffer
        
        // finally, if ORIGIN fits into this rectangle buffer, add it
        Point2D.Double center = new Point2D.Double(0d,0d);
        xp._xlate.transform(center, center);
        xp._scale.transform(center, center);
        
        pX = (int) Math.floor(center.getX());
        pY = (int) Math.floor(center.getY());
        if (   ((pX >= 0) && (pX < pcols)) 
            && ((pY >= 0) && (pY < prows)))
        {
            rslt[pX][pY] = '@';
        }
        
        return rslt;
    }
    
    // method that finally handles all variants of the overloaded method
    public synchronized void plotStdout(int prows, int pcols, boolean forcePlot) 
    {
        if (! (isViable() || forcePlot)) { return; } // do nothing for dull or dead seeds
        
        // build the little character image buffer
        char jitterImg[][] = buildMGridJitterView(prows, pcols);
        char domainImg[][] = buildMGridDomainView(prows, pcols);
        
        Rectangle2D.Double plotRect = jitter();
        
        PrintStream o = System.out;
        o.printf("   %s:%s:%d %n", _fixedName,_state,_currentTemp);
        o.printf("   WxH (%4g x %4g) @ (%4g,%4g)%n", 
        plotRect.getWidth(), plotRect.getHeight(),
        plotRect.getX(), plotRect.getY());
        String hdr = String.format("+%"+pcols+"s+","").replaceAll(" ","=");
        String ftr = String.format("+%"+pcols+"s+","").replaceAll(" ","-");
        
        o.println("   " + hdr + "   " + hdr);
        for (int row=0; row<prows; ++row) {
            o.print("   |");
            for (int col=0; col<pcols; ++col) {
                o.print(jitterImg[col][row]);
            }
            o.print("|   |");
            for (int col=0; col<pcols; ++col) {
                o.print(domainImg[col][row]);
            }
            o.println("|");
        }
        o.println("   " + ftr + "   " + ftr);
        
    } // end of plotStdout
    
    public void setJitterDepth(int jitterDepth) { _jitterDepth = jitterDepth; }
    public int getJitterDepth() { return _jitterDepth; }
    
    public ArrayList<Point2D.Double> getJitterPoints() {
        ArrayList<Point2D.Double> rtnVal = new ArrayList<Point2D.Double>();
        rtnVal.addAll(_jitterBuffer);
        return rtnVal;
    }
    public ArrayList<Rectangle2D.Double> getJitterRects() {
        ArrayList<Rectangle2D.Double> rtnVal = new ArrayList<Rectangle2D.Double>();
        rtnVal.addAll(_jitterRects);
        return rtnVal;
    }       
    
    // utility method to make us prettier
    public String toString()   { 
        return _fixedName + ":" + _state + ":" + _currentTemp; 
    }
    
    volatile private long _ambient = 0;
    volatile private long _currentTemp = 0;
    volatile private GrowthState _state = GrowthState.PLANTED;
    volatile private Rectangle2D.Double _domainRect = new Rectangle2D.Double();        
    volatile private Rectangle2D.Double _jitterRect = new Rectangle2D.Double();
    
    private long _dna;
    private double _range = 1.1d;
    private double[] _coeffs = new double[10];
    private String _fixedName;
	private final static long serialVersionUID = 1957L;
    private boolean _inoculated = false;
        
    public GrowthState state() { return _state; }       
    public long getAmbientTemp() { return _ambient; }
    public long getCurrentTemp()  {
        if (Viable.contains(_state)) { return _currentTemp; } 
        else { return _ambient; }
    }
    
    Point2D.Double _currentLocation = new Point2D.Double(0d,0d);
    int _jitterDepth;
    
    private volatile LinkedList<Point2D.Double> _jitterBuffer;
    private volatile LinkedList<Rectangle2D.Double> _jitterRects;
    
    private synchronized boolean warmOneDegree() {
        
        // only warm things that can respond to it
        if (! isViable()) { return false; }
        
        // indicate we are in a heating state
        GrowthState keepState = _state;
        _state = GrowthState.HEATING;
        
        // do the math corresponding to one iteration
        double oldX = _currentLocation.getX();
        double oldY = _currentLocation.getY();
        double newX = _coeffs[0]*oldX*oldX + _coeffs[1]*oldX + _coeffs[2]*oldY*oldY + _coeffs[3]*oldY + _coeffs[4];
        double newY = _coeffs[5]*oldX*oldX + _coeffs[6]*oldX + _coeffs[7]*oldY*oldY + _coeffs[8]*oldY + _coeffs[9];
        
        // we're here, so bump the temperature
        _currentTemp++;
        
        // check for known diagnoses       
        if ( Double.isInfinite(newX) || Double.isNaN(newX) || 
            Double.isInfinite(newY) || Double.isNaN(newY))
        {
            _state = GrowthState.FRIED;
            return false;
        }
        
        // We survived heating one more degree -- now do
        // the jitter buffer discrimination.
        _currentLocation.setLocation(newX,newY);
        _domainRect.add(_currentLocation);

        Point2D.Double ptRef = null;
        while (_jitterDepth <= _jitterBuffer.size()) {
        	ptRef = _jitterBuffer.removeLast();
        }
        if (null == ptRef) { 
        	ptRef = new Point2D.Double(newX,newY); 
        } else {
        	ptRef.setLocation(newX,newY);
        }
        
        _jitterBuffer.addFirst(ptRef); 

        // similar for jitter buffer -- reuse element
        // from the list of rectangles
        _jitterRect = null;
        while (_jitterDepth <= _jitterRects.size()) {
        	_jitterRect = _jitterRects.removeLast();
        }
        if (null == _jitterRect) {
        	_jitterRect = new Rectangle2D.Double();
        }

        Point2D.Double trail = null;
        boolean inited = true;
        for (Point2D.Double d : _jitterBuffer) {
        	if (! inited) {  // do we need to build first rectangle?
        		if (null != trail) {  // have we seen at least two points?
        			// set the rectangle by giving it its first two entries
        			_jitterRect.setRect(d.getX(), d.getY(), trail.getX(), trail.getY());
        			inited = true;
        		} else {
        			// we have not seen two points, so just set trail and continue
        			trail = d;
        		}
        	} else {
        		_jitterRect.add(d);
        	}
        }
        
        _jitterRects.addFirst(_jitterRect);

        double rectArea = _jitterRect.getHeight() * _jitterRect.getWidth();
        if ((_jitterBuffer.size() == _jitterDepth)
        		&& (rectArea < (1.0E6 * Double.MIN_VALUE))) {
        	_state = GrowthState.SIZZLED;
        	return false;
        }

        // do jitter analysis by plotting into rectangular grid
        int ems = 0;    // count of M (maximum) touch grid entries
        int dots = 0;   // count of single-touch grid entries
        int blnks = 0;  // count of untouched grid entries
        int others = 0; // count of grid entries with 1 < n < M

        if (_jitterDepth <= _jitterRects.size()) {                
        	char[][] look = buildMGridJitterView(20,20);
        	for (int i=0;i<20;++i) {
        		for (int j=0;j<20;++j) {
        			switch(look[j][i]) {
        			case 'M': ems++; break;
        			case '.': dots++; break;
        			case ' ': blnks++; break;
        			default : others++; break;
        			}
        		}
        	}

        	// main discriminating block is here
        	// other checks should be facilitated
        	
        	// should 'density' and 'mfactor' be exposed?
        	float density = (float)(ems + dots + others) / (float)(Math.max(blnks,1));
        	// ratio of non-Ms to Ms -- wanting at least 3 times as many others as ems
        	float mfactor = (float)(dots + (2 * others)) / (float)(Math.max(ems,1));

        	// see if we want to poison this seed based on mfactor or density
        	if ((mfactor < 1.0f) || (density < 0.10f)) {
        		if (! _inoculated) { 
        			_state = GrowthState.POISONED;
        			return false;
        		} else {
        			_state = GrowthState.INOCULATED;
        		}
        	}
        }
        if (keepState == GrowthState.PLANTED) {
        	// first successful warming -- call it HEATED
        	_state = GrowthState.HEATED;
        } else {
        	_state = keepState;
        }
        return true;      
    } // end of warmOneDegree    
} // end of class Seed

