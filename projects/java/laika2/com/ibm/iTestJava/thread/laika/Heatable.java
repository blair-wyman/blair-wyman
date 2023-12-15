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
/** 
Implemented by objects that can be "heated" to 
a numeric value.  Heating an object causes it to
be advised of changes to its ambient temperature.

Calls to setAmbientTemp may either choose to block
until the Heatable is at ambient temp, or may return
immediately, allowing objects to gradually acquire
ambient temperature.

Calls to <code>isHeating</code> on a Heatable
object must return <code>true</code> if the object
is not at ambient temperature (or above).
*/
interface Heatable { 
	/**
	 * Set the ambient temperature of the object, which 
	 * will cause it to heat until it reaches this temperature
	 * or until it dies or is poisoned.
	 */
	void setAmbientTemp(long temp);
	/**
	 * Determine if the object is currently "heating".
	 * @return <code>true</code> if the object being
	 * heated is below ambient temperature.
	 */
	boolean isHeating();
}

