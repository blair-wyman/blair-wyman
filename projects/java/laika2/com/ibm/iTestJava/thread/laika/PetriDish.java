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
import java.util.concurrent.*;
import java.io.*;

class PetriDish implements Heatable {
    
    // constructors
    PetriDish(int jitterDepth, int heatingWidth, long timeIndex, PrintStream out) {
        _width = heatingWidth;
        _dishJitter = jitterDepth;
        _contents = new ArrayList<Growable>();
        _startWait = timeIndex;
        _out = out;
    }
    PetriDish() { this(100, 16, System.currentTimeMillis(), System.out); };
    PetriDish(int jitterDepth) 
     { this(jitterDepth, 16, System.currentTimeMillis(), System.out); }
    
    // Heatable methods
    public synchronized void setAmbientTemp(long temp) {
        
        if (null != _burner) {
            _out.println("  ERROR: already burning?");
       }
        
        // set the temperature and run the seeds through a new burner
        _dishTemp = temp;
        _burner = Executors.newFixedThreadPool(_width);
        
        for (Growable g : _contents) {
            g.setAmbientTemp(_dishTemp);
           _burner.submit(g); // do the actual warming call
        }

        // tell the burner to shut down when done
        _burner.shutdown();
    }
    
    public synchronized void waitForQuiesce() { 
        if (! isHeating()) return;
        try {
            long tmpTime;
            while (! _burner.awaitTermination(5, TimeUnit.SECONDS)) {
                tmpTime = System.currentTimeMillis() - _startWait;
                _out.printf("[%8.3f] dish temp: %10d\n", 
                              0.001f * tmpTime, getCurrentTemp());
            }
            tmpTime = System.currentTimeMillis() - _startWait;
            _out.printf("[%8.3f] BURN DONE: %10d\n", 
                          0.001f * tmpTime, getCurrentTemp());
        } catch(InterruptedException ie) {
            _out.println("Took an interrupt exception");
        } finally {
            _burner = null;
        }
    }
    
    public boolean isHeating() { 
        return (null != _burner);
    }            
    
    // methods to put one or more growable things into this dish
    void add(Growable g) {
        _contents.add(g);
        g.setJitterDepth(_dishJitter);
        g.setAmbientTemp(_dishTemp);
    }
    
    void add(Collection<Growable> seeds) {
        _contents.addAll(seeds);
        for (Growable g : _contents) { 
            g.setJitterDepth(_dishJitter);
        }
        setAmbientTemp(_dishTemp);
    }
        
    public synchronized long getCurrentTemp() {
        long accum = 0;
        long count = 0;
        for (Growable g : _contents) { accum += g.getTemp(); count++; }
        return accum / count;
    }
    
    public int size() { return _contents.size(); }
    public synchronized void clear() { _contents.clear(); }  
           
    
    public synchronized int cull() 
    {
        int startSize = _contents.size();
        ListIterator<Growable> iter = _contents.listIterator();

        ArrayList<String> poisoned = new ArrayList<String>();
        ArrayList<String> sizzled  = new ArrayList<String>();
        ArrayList<String> fried    = new ArrayList<String>();
        
        int psum=0, ssum=0, fsum=0, culled=0;
        while (iter.hasNext()) {
            // if (!iter.next().isViable())
            Growable g = iter.next();           
            if (! g.isViable()) {
                culled++;
                long itsTemp = g.tolerance();
                switch(g.getState()) {
                    case POISONED: poisoned.add(g.toString());  psum += itsTemp; break;
                    case SIZZLED:  sizzled.add(g.toString());   ssum += itsTemp; break;
                    case FRIED:    fried.add(g.toString());     fsum += itsTemp; break;
                    default: 
                        System.out.println("ERROR: Unhandled state.");
                        System.exit(1);
                }
                iter.remove();
            }
        }
        float avgPoisonedTemp = (float)(psum) / (float) Math.max(1,poisoned.size());
        float avgSizzleTemp   = (float)(ssum) / (float) Math.max(1,sizzled.size());
        float avgFriedTemp    = (float)(fsum) / (float) Math.max(1,fried.size());
        float avgCullTemp     = (float)(psum+ssum+fsum) / (float)(Math.max(1,culled));

        // build a little 'summary' of our latest culling
        _cullState = String.format(
        "culled total=%d (@%4.1f) " + 
        "poisoned=%d (@%4.1f) " +
        "sizzled=%d (@%4.1f) " +
        "fried=%d (@%4.1f) "
        ,             culled, avgCullTemp
        ,         poisoned.size(), avgPoisonedTemp 
        ,        sizzled.size(), avgSizzleTemp
        ,      fried.size(), avgFriedTemp
        );
                                           
        return startSize - _contents.size();
    }
    
    @SuppressWarnings("unchecked")
	public synchronized ArrayList<Growable> contents() {
        return (ArrayList<Growable>)_contents.clone();
    }
    
    public String getCullDescription() {
        return _cullState;
    }
    
    // private fields -- "innards"
    private String _cullState = "unculled";
    private long _startWait;
    private long _dishTemp = 0; 
    private volatile ExecutorService _burner = null;
    private PrintStream _out;
    private final int _width;
    private final int _dishJitter;
    private ArrayList<Growable> _contents;   
}

