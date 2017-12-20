/**
 *
 *  Improve_Kymo v1, 15 oct. 2008 
    Fabrice P Cordelieres, fabrice.cordelieres at gmail.com
    
    Copyright (C) 2008 Fabrice P. Cordelieres
  
    License:
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;

import java.awt.*;

import Utilities.kymograph.*;

public class Improve_Kymo implements ExtendedPlugInFilter, DialogListener{
    public static int start=1;
    public static int stop=15;
    @SuppressWarnings("unused")
	private int nPasses=1;
    private int flags = DOES_8G|DOES_16|DOES_32;
    private ImagePlus imp;
    
    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;

        if (IJ.versionLessThan("1.42k")) return DONE;
        
        return flags;
    }
    
    public int showDialog(ImagePlus imp, String command, PlugInFilterRunner pfr) {
        imp.setSlice(imp.getNSlices()/2);
        GenericDialog gd = new GenericDialog(command);
        gd.addNumericField("Start", start, 0);
        gd.addNumericField("Stop", stop, 0);
        gd.addPreviewCheckbox(pfr);
        gd.addDialogListener(this);
        gd.showDialog();                    // input by the user (or macro) happens here
        if (gd.wasCanceled()) return DONE;
        IJ.register(this.getClass());       // protect static class variables (parameters) from garbage collection
        return IJ.setupDialog(imp, flags);  // ask whether to process all slices of stack (if a stack)
    }
    
    public boolean dialogItemChanged(GenericDialog gd, AWTEvent e) {
        start = (int) gd.getNextNumber();
        stop = (int) gd.getNextNumber();
        if (start>stop || start<1 || gd.invalidNumber())
            return false;
        
        return true;
    }
    
    public void setNPasses(int nPasses) {
        this.nPasses = nPasses;
    }
    
    public void run(ImageProcessor ip) {
        ImageProcessor result=ip.createProcessor(ip.getWidth(), ip.getHeight());
        for (int i=start; i<=stop; i++){
            ImageProcessor plane1=ip.duplicate();
            ImageProcessor plane2=ip.duplicate();
            (new GaussianBlur()).blurGaussian(plane1, i-1, i-1, 0.00001);
            (new GaussianBlur()).blurGaussian(plane2, i, i, 0.00001);
            result=new ImageProcessorCalculator().add(result, new ImageProcessorCalculator().substract(plane1, plane2));
        }
        switch (this.imp.getBitDepth()){
            case 8: ip.setIntArray(result.convertToShort(true).convertToByte(true).getIntArray()); break;
            case 16: ip.setIntArray(result.convertToShort(true).getIntArray()); break;
            case 32: ip.setFloatArray(result.getFloatArray()); break;
        }
        this.imp.resetDisplayRange();
    }
}