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
import java.util.EnumSet;
import java.util.concurrent.Callable;
    
public interface Growable extends Heatable, Trackable, Callable<Object> {
    
	public static enum GrowthState {
		PLANTED,    // has DNA value established -- unheated
		HEATING,    // is in process of being heated
		HEATED,     // has been heated and not yet killed
		FRIED,      // value spun out of control and died (diverged) 
		SIZZLED,    // value spun down to a point (converged)
		POISONED,   // value had been killed at its current temp
		INOCULATED  // value would have been killed, but was single strain
	};    

	public final static EnumSet<GrowthState> Viable = EnumSet.of (
			GrowthState.PLANTED, 
			GrowthState.HEATING, 
			GrowthState.HEATED,
			GrowthState.INOCULATED
	);   

	// methods -- implicitly abstract in an interface
	/** 
	 * Inoculate this Seed so that it cannot be poisoned. An attempt
	 * to poison a seed that has been inoculated will set its state
	 * to "INOCULATED" and allow it to continue to be heated.
	 */
	void inoculate();
	/**
	 * Test this <code>Growable</code> for viability, which
	 * is defined as the ability to react to heating.
	 * @return <code>true</code> if viable
	 */
	boolean isViable();     // still able to react to heat?
	/**
	 * Obtain the Growable object's current temperature.
	 * @return the long value representing temperature.
	 */
	long getTemp();         // Dead things always at ambient  
	/**
	 * Mark the Growable as no longer viable by setting its
	 * state to <code>POISONED</code>.
	 * Will mark the Growable as INOCULATED (which allows it
	 * to continue heating until it FRIES or SIZZLES) if it
	 * has had its 'inoculate' method called.
	 */
    void poison();   	// freezes temp at current temp
    /**
     * Test the highest temperature this Growable has ever survived.
     * @return the highest temperature ever applied to the
     * Growable that did not kill it or cause it to be 
     * poisoned.
     */
    long tolerance();       // max temp ever applied without death
    /**
     * 
     * @return the <code>GrowthState</code> of this Growable.
     */
    GrowthState getState(); // returns state of this Growable 
    /**
     * Get the name of the strain, which always starts with
     * "STRAIN_0x" followed by 16 hexadecimal digits -- the 
     * digits that are the "dna" value of this Growable.
     * @return a <code>String</code> that contains the
     * unique name of the strain, i.e. 'STRAIN_0x123456789ABCDEF0'
     */
    String strainName();    // returns unique ID of this seed
   
}

