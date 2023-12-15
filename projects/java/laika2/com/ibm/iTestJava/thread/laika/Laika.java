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
import java.io.File;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Class <code>Laika</code> is a simple application that searches for
 * interesting iterated equations, and plots them to JPG or PNG files.
 * It uses and requires features first available in JDK 1.5 or higher. 
 * @author  Blair Wyman
 * @version 0.1, 03/23/2006
 * @version 0.2, 09/01/2007
 */
class Laika {
	
    private static final String _usage_head =     
    "usage: java -jar laika.jar [-settemp=<t>] [-jitter=<j>] [-width=<w>]   \n" +
    "                           [-nseeds=<n> | -strain=<dna>] [-outdir=<d>] \n" +
    "                           [-randseed=<s> | -surprise] [-favorites]    \n" +
    "                           [-h | -help] \n" +
    "\n" +
    "  Args: \n" + 
    "       -nseeds=<n>    number of seeds to search for      [20]   \n" +
    "       -jitter=<j>    jitter buffer xize                [100]   \n" +
    "       -settemp=<t>   max ambient temp                [10000]   \n" +
    "       -width=<w>     number of seeds to heat at once     [8]   \n" +
    "       -randseed=<s>  master random seed    [19570108024300L]   \n" +
    "       -surprise      use pseudorandom master seed    [clock]   \n" +  
    "       -strain=<x>    strain ID   'STRAIN_0xhhhhhhhhhhhhhhhh'   \n" +
    "       -outdir=<d>    output directory for proof file output    \n" +
    "       -imgsize=<i>   size of output square (min=400)  [500]    \n" +
    "       -h | -help     short or long form of help text           \n" +
    "\n";
    
    private static final String _usage_ref = 
    " (use '-help' for more info) \n";
    
    private static final String _usage_body =
    "  \"Exploring quadratic iterations since before the new millenium.\"           \n" +
    "\n" +
    "  Laika builds 'seeds' (objects) that are uniquely defined by their 'dna'      \n" +
    "  (a positive 8-byte integer).  Seeds can react to 'heat' (iteration)          \n" +
    "  by moving around in a two-dimensional plane, occasionally in very            \n" +
    "  interesting ways.  Laika's goal it to find the most interesting seeds        \n" +
    "  and show you the beautiful shapes they can take. \n" +
    "\n" +
    "  Laika has limited skills in crafting interesting DNA, so the vast majority   \n" +
    "  of seeds are doomed to short and uninteresting lives.  Many cannot stand     \n" +
    "  much heat before becoming FRIED or SIZZLED.  Most that survive to higher     \n" +
    "  temperatures degenerate into 'uninteresting' growth behaviors that Laika     \n" +
    "  attempts to detect and POISON. \n" +
    "\n" +
    "  Laika uses a seed's DNA value to to deterministically fill an array of ten   \n" +
    "  double-precision float coefficients (say A-J).  In turn, these coefficients  \n" +
    "  completely determine how a seed 'moves' when it is 'heated' (iterated).      \n" +
    "\n" +
    "  Seeds start at the origin (0,0), and move one step per degree of 'warming,'  \n" +
    "  according to the following simple iteration equation.                        \n" +
    "       x' = A*x + B*x*x + C*y + D*y*y + E;       \n" +
    "       y' = F*x + G*x*x + H*y + I*y*y + J;       \n" +
    " \n" +
    "  After each degree of warming, a seed is in one of these states:              \n" +
    "     HEATED     -- survived the temperature change, remains viable       \n" +
    "     FRIED      -- x or y location went to +/- Infinity                  \n" +
    "     SIZZLED    -- x or y location converges to a point                  \n" +
    "     POISONED   -- Laika decided the growth was uninteresting            \n" +
    "     INOCULATED -- Would have been 'POISONED' but was single strain 	  \n" +
    "\n" +
    "  NOTES: \n" +
    "    * Individual strains (submitted using -strain) are 'inoculated'  		    \n" +
    "    * When a warmed seed is no longer 'viable,' it instantly achieves          \n" +
    "      any ambient temperature it is exposed to without further state change.   \n" +
    "    * Viable seeds retain their highest internal temperature even if stored.   \n" +
    "\n" +
    "  In order to determine when seeds are no longer 'viable', Laika tracks their  \n" +
    "  progress in two detectable ways -- using a 'domain rectangle' and 'jitter'   \n" +
    "  A 'domain rectangle' simply bounds a seed's lifetime movement in the plane.  \n" +
    "  'Jitter' is a bounded 'warming history' of locations and other information.  \n" + 
    "\n" +
    "  When a seed survives initial warming without dying by 'natural causes,' (i.e.\n" +
    "  (FRIED or SIZZLED), Laika looks more closely to see if it is 'interesting.'  \n" +
    "  Laika uses 'jitter' (from Trackable) as a resource to improve this choice.   \n" +
    "\n" +
    "  After each degree of warming, Laika uses this jitter buffer to build a       \n" +
    "  'jitter rectangle' that bounds the region of the jitter at that point.       \n" +
    "  A history of these jitter rectangles is preserved, providing diagnostic      \n" +
    "  feedback that actually then reaches back (2 * jitter_depth) in temp.         \n" +
    "\n";
    
    /** Captured time stamp at class load time. */
    private static final long _loadedMs = System.currentTimeMillis(); 
   
    /** Utility function to return time in milliseconds since class load time stamp */
    private static long elapsedMs() { 
        return System.currentTimeMillis() - _loadedMs; 
    }

    /** Utility wrapper for calls to <code>System.out.println(String s)</code>
      * in a way that reports the relative time of the report since the class
      * was loaded by the VM.
      <pre>
           say("This is a message.");
      </pre>
      * ...results in output
      <pre>
           [  123.456] This is a message.
      </pre>
    */      
    private static void say(String s) { 
        System.out.printf("[%8.3f] %s\n", 0.001f * elapsedMs(), s); 
    }
    
    /** Utility wrapper for <code>System.out.printf(String fmt, Object...)</code> 
     * that provides time stamped output.  @see say
     */
    private static void sayf(String fmt, Object... args) {
        String fmtPfx = String.format("[%8.3f] ", 0.001f * elapsedMs());
        System.out.printf(fmtPfx + fmt + "%n", args); // add a newline
    }      
    
    /**
    * Investigate a single seed value, and plot the results in various formats and 
    * resolutions.
    @param strain       seed value to investigate
    @param jDepth       depth of jitter buffer, in number of elements
    @param reqTemp      requested maximum seed temperature
    @param xysize       height and width of the square output image
    @param outFmt       value that matches an available image writer ("png", "jpg", etc.)
    @param outDir       location to store the STRAIN_* files
    */

    public void initSingle
    (long strain, int jDepth, long reqTemp, int xysize, String outFmt, File outDir) 
    {
        // Single strain constructor -- used to get arbitrarily high-res output 
        // for a given seed value.  Do a bit of 'echo location'
        sayf("LAIKA: investigating a single strain: STRAIN_0x%016X", strain);  
        
        Seed singleSeed = new Seed(strain);
        try {
            singleSeed.inoculate(); 	// prevent poisoning
            
            say("Lighting a match to warm this strain individually...");
            ExecutorService flame = Executors.newSingleThreadExecutor();        
            singleSeed.setJitterDepth(jDepth);           
            singleSeed.setAmbientTemp(reqTemp);        
            flame.submit(singleSeed); 	// calls singleSeed.run() 
            flame.shutdown();  			// immediately shut down the Executor
            
            say("...match is burning, reporting temp every 5 seconds");
            while(! flame.awaitTermination(5, TimeUnit.SECONDS)) {
                sayf("...heated to %,7d degrees", singleSeed.getTemp());
            }
        } catch(InterruptedException ie) {
            sayf("...heating interrupted at %,7d degrees", singleSeed.getTemp());
        }
 
        // plot a very simple side-by-side character view of the seed 
        say("LAIKA: seed " + singleSeed + " after heating...");
    	report(outDir, singleSeed, reqTemp, 0, xysize, outFmt, true);
    }
    
    /**
    * Search repeatedly for a set of interesting seeds, given a temperature and jitter
    * count.  Laika used the <code>initialSeed</code> parameter to generate the 
    * master seed for this "instance".  
    * <p>
    * Note that, in theory, the same seed value should deterministically result 
    * in the same set of candidate seeds being created and heated.  This allows this
    * simple example to serve as a testcase confirming repeatable complex behaviors. 
    * <p>
    * Each interesting seed is plotted into a 500x500 PNG file in the output directory
    * specified by the outDir parameter.
    @param wanted       number of seeds to look for
    @param jDepth       jitter depth of the seeds
    @param maxTemp      maximum temperature to display
    @param amps         degree of parallelism in the 'burn' phase
    @param germ         master random seed for this instance
    @param outFmt       file type to create (e.g. "png" or "jpg")
    @param outDir       output directory into which to put the "ID" PNG file
    */
    public  Laika() {}

    private static void report(File outDir, Growable reportSeed, long maxTemp, int jDepth, int imgsize, String outFmt, boolean immune) 
    {	
    	// write the little side-by-side bling
    	reportSeed.plotStdout(15,30,immune);

    	String tpl = String.format(
    			"%s_%%s_@%dJ%d_%dx%d_.%%s"  //    describes parms that follow
    			,reportSeed.strainName()              // STRAIN_0x...
    			,        maxTemp                 // always use requested temp
    			,           jDepth               // jitter depth -- important parm
    			,               imgsize          // in number of pixels
    			,                  imgsize);     // in number of pixels

    	String proofFileName = String.format(tpl, "proof", "png");
    	String expFileName = String.format(tpl, "test", outFmt);

    	File proofFile = new File(outDir, proofFileName);
    	if (proofFile.exists()) {
    		say("Found proof file " + proofFile + " -- skipping...");
    	} else {
    		say("Building proof file " + proofFile);
    		reportSeed.plotSquareCroppedCumulativeImage(imgsize, "png", proofFile);
    	}

    	say("  ...writing experimental plotfile ");
    	File expFile = new File(outDir, expFileName);
    	if (expFile.exists()) {
    		say("Found test file -- overwriting...");
    	}
    	say("Builing test file" + expFile);
    	reportSeed.plotExperimental(imgsize, outFmt, expFile);
    	say("Laika is done.");

    } 
    
    public void initGroup(int wanted, int jDepth, long maxTemp, int amps, long germ, String outFmt, File outDir, int imgsize) 
    {
        
        // get a "Petri Dish" object, passing along the jitter and amps
        sayf("LAIKA: Getting a Petri dish with %d jitter pulling %d amps... "
        ,                                      jDepth,           amps);
        PetriDish pdish = new PetriDish(jDepth, amps, _loadedMs, System.out); 
        
        // build something to collect our "interesting" growables into
        ArrayList<Growable> coolSeeds = new ArrayList<Growable>();        
        
        sayf("LAIKA looking for %d seeds to survive to %d degrees ", wanted, maxTemp);           
        sayf("Creating new Random object using initial seed 0x%1$016X (%1$,d)", germ);
       
        // build our private random using the germ provided
        Random rndm = new Random(germ);
        
        int MAXLOOPS = 500;  // arbitrary -- a REALLY long time....
        
        sayf("Entering search for %d interesting seeds (%d times)", wanted, MAXLOOPS); 
        int loopCount = 0;
        
        int dishSize;  // use before initialization is guarded against
        
        int MIN_DISH_SIZE =  50; // arbitrary
        int MAX_DISH_SIZE = 500; // arbitrary
        if (wanted < MIN_DISH_SIZE) { 
            sayf("Searching with minimum dish capacity %d", MIN_DISH_SIZE);
            dishSize = MIN_DISH_SIZE;
        } else if (wanted > MAX_DISH_SIZE) {
            sayf("Searching with maximum dish capacity %d", MAX_DISH_SIZE);
            dishSize = MAX_DISH_SIZE;
        } else {
            dishSize = wanted;
        }
        
        while ((loopCount++ < MAXLOOPS) && (wanted > coolSeeds.size())) {
           
            sayf("LAIKA[%d] BEGIN LOOP", loopCount);
            sayf("  Clearing out dish (cap. %d)", dishSize);
            pdish.clear();
            
            sayf("  Filling empty dish with %d new seeds", dishSize);
            for (int i=0; i<dishSize; ++i) {
                pdish.add(new Seed(rndm.nextLong())); // SEEDED from the germ!!!
            }
            
            sayf("  Setting dish temp to %d degrees...", maxTemp);
            pdish.setAmbientTemp(maxTemp); 
            pdish.waitForQuiesce();
            
            say("  Culling dead seeds from dish...");
            int culled = pdish.cull();
            
            float pct = (100.0f * culled) / (float) dishSize;
            sayf("  ...culled %d seeds (%4.1f%%) -- preserving %d survivors "
            ,                 culled,   pct,                   pdish.size());                       
            sayf("LAIKA[%d]...cull report %s%n", loopCount, pdish.getCullDescription());
            
            // add whatever is left into the list of cool seeds
            coolSeeds.addAll(pdish.contents());    
            
            // let the user know 
            int hits = pdish.size();
            if (0 < hits) {                     
                
                sayf("  ...reporting on %d interesting seeds", hits);
                for (Growable g : pdish.contents()) {
 
                	report(outDir, g, maxTemp, jDepth, imgsize, outFmt, false);
                        
                } // end for each surviving Growable
                 
            } else { // no hits
                say("  ...lost the entire dish.");
            }
            sayf("  Current cool count: %d", coolSeeds.size());
            sayf("LAIKA[%d] END OF LOOP", loopCount);           
        }
        
        if (coolSeeds.size() < wanted) {
            sayf("LOOPEXHAUST: Found %d cool after %d loops", coolSeeds.size(), MAXLOOPS);
        }
        
        say("Summary of cool seeds");
        int num=0;
        for (Growable g : coolSeeds) {
            System.out.printf("[%3d] %s%n", ++num, g); // implicitly g.toString()
        } 
        
        say("Laika is done.");        
    }
       
    private static final String _usage_short = _usage_head + _usage_ref;
    private static final String _usage_long = _usage_head + _usage_body;    
    private static void usage() { usage("Laika:"); }  
    private static void usage(String msg) { 
        sayf("[Laika: %s]%n %s %n", msg, _usage_short); 
        System.exit(2);
    }
     
    public static void main(String[] args) {
        
        int  pseeds    = 20;            // default number of seeds
        int  pjitter   = 100;           // default jitter buffer depth
        long psettemp  = 1000;          // default ambient temperature
        int  pwidth    = 8;             // default parallelism of heating
        long prandseed = BLAIR_GERM;    // default germ
        String tmpName = System.getProperty("java.io.tmpdir",".");
        long pstrain   = 0;
        int  pimgsize  = 500;
        
        System.out.println("Laika2: V2");
        if (args.length > 0) {
            for (String a : args) {
                if (a.startsWith("-h")) {
                    if ("-help".equals(a)) {
                        System.out.println(_usage_long);
                        System.exit(1);
                    } else {
                        usage();
                    }
                } else if (a.startsWith("-strain=STRAIN_0x")) {
                    pstrain = Long.parseLong(a.substring(17),16);
                } else if (a.startsWith("-nseeds=")) {
                    pseeds = Integer.parseInt(a.substring(8));
                } else if (a.startsWith("-jitter=")) {
                    pjitter = Integer.parseInt(a.substring(8));
                } else if (a.startsWith("-settemp=")) {
                    psettemp = Long.parseLong(a.substring(9));
                } else if (a.startsWith("-width=")) {
                    pwidth = Integer.parseInt(a.substring(7));
                } else if (a.startsWith("-randseed=")) {
                    prandseed = Long.parseLong(a.substring(10));
                } else if (a.startsWith("-outdir=")) {
                    tmpName = a.substring(8);
                } else if (a.startsWith("-surprise")) {
                    prandseed = System.currentTimeMillis();
                } else if (a.startsWith("-imgsize=")) {
                    pimgsize = Integer.parseInt(a.substring(9));
                } else {
                    usage("Unrecognized option:" + a);
                }
            }
        }
        
        File outDir = new File(tmpName);
        if (! (outDir.exists() || outDir.mkdirs())) {
        	usage("Unable to create -outdir directory:" + tmpName);
        }
         
        if (0 != pstrain) { 
            Laika singleLaika = new Laika();
            singleLaika.initSingle(pstrain, pjitter, psettemp, pimgsize, "png", outDir);
        } else {
            Laika biggerLaika = new Laika();
            biggerLaika.initGroup(pseeds, pjitter, psettemp, pwidth, prandseed, "png", outDir, pimgsize); 
        }
    } // end of main()
    
    // private fields, including default germ and serial version UID
    private final static long BLAIR_GERM = 19570108024300L;    
    
} // end of class Laika

