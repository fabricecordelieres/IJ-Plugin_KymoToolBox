/**
 *
 *  kymograph v1, 15 oct. 2008 
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

package Utilities.kymograph;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.*;
import ij.process.*;

import java.awt.Polygon;
import java.util.*;

public class kymograph {
    /**2D+t stack from which to draw the kymograph**/
    ImagePlus ip;
    /**The path from which to draw the kymograph**/
    Roi path;
    /**Length of the path, in physical dimensions**/
    double length;
    /**Number of pixels composing the path**/
    int nbPix;
    /**String containing the unit of length's name**/
    String unit;
    /**String containing the time unit**/
    String timeUnit;
    /**Double containing the frame intervalle**/
    double timeIntervalle;
    /**Straightener object to be used to build the kymograph**/
    Straightener st=new Straightener();
    /**Stores the calibration to be applied to the kymograph**/
    Calibration cal=new Calibration();
    /**x and y series of interpolated coordinates of the path**/
    int[] x,y;
    /**Contains the "KymoPathInfo" string to be added to the kymograph's "Info" property**/
    String infoString="";
    
    
    /**
     * Creates a new kymograph object from an ImagePlus and a Roi
     * @param ip 2D+t stack from which to draw the kymograph, as an ImagePlus
     * @param roi Roi containing the path along which to draw the kymograph
     */
    public kymograph(ImagePlus ip, Roi roi){
        this.ip=ip;
        path=roi;
        unit=ip.getCalibration().getUnit();
        timeUnit=ip.getCalibration().getTimeUnit();
        timeIntervalle=ip.getCalibration().frameInterval;

        if (path==null) throw new IllegalArgumentException("kymograph expects a ROI to be defined on the image.");
        if (path.getType()==Roi.LINE || path.getType()==Roi.FREELINE){
            Polygon pol=path.getPolygon();
            path=new PolygonRoi(pol.xpoints, pol.ypoints, pol.npoints, Roi.POLYLINE);
        }
        int type=path.getType();
        if (type!=Roi.POLYLINE)throw new IllegalArgumentException("kymograph expect a line, freeline or polyline ROI.");
        if (unit.equals("pixel")) throw new IllegalArgumentException("kymograph expects the ImagePlus to be calibrated.");
        if (ip.getNSlices()==1) throw new IllegalArgumentException("kymograph expects the ImagePlus to be a stack.");
    }

    /**
     * Creates a new kymograph object from an ImagePlus and a Roi
     * @param ip 2D+t stack from which to draw the kymograph, as an ImagePlus
     */
    public kymograph(ImagePlus ip){
        this(ip, ip.getRoi());
    }
    
    /**
     * Builds the image of the kymograph
     * @param width width of the path (used for the image straightening)
     * @return the kymograph as an ImagePlus
     */
    public ImagePlus getKymograph(int width){
        if(width<1) width=1;
        calcCalibAndGetKymoPathCoord(width);
    	
        float[][] imgArray=new float[nbPix][ip.getNSlices()];
        ImagePlus result=NewImage.createImage("Kymograph from "+ip.getTitle(), nbPix, ip.getNSlices(), 1, 32, 1);

        for (int i=1; i<=ip.getNSlices(); i++){
            ip.setSlice(i);
            ip.setRoi(path);
            float[] tmp=yProj(st.straighten(ip, path, width));
            for (int j=0; j<Math.min(nbPix, tmp.length); j++) imgArray[j][i-1]=tmp[j];
        }
        
        
        result.getProcessor().setFloatArray(imgArray);
        finalStep(result);
        return result;
    }
    
    /**
     * Builds the kymoStack, ie a stack where each slice is a image of the straightened
     * path and of a predefined width
     * @param width width of the path (used for the image straightening)
     * @return the kymoStack as an ImagePlus
     */
    public ImagePlus getKymoStack(int width){
    	if(width<1) width=1;
    	 calcCalibAndGetKymoPathCoord(width);
        
        ImagePlus result=NewImage.createImage("KymoStack from "+ip.getTitle(), nbPix, width, ip.getNSlices(), 32, 1);
        
        for (int i=1; i<=ip.getNSlices(); i++){
            ip.setSlice(i);
            ip.setRoi(path);
            result.setSlice(i);
            result.setProcessor("", st.straighten(ip, path, width));
        }
        
        result.setTitle("KymoStack from "+ip.getTitle());
        finalStep(result);
        return result;
    }
    
    /**
     * Builds the kymoMontage, ie an image composed of all the images of the straightened
     * path and of a predefined width, along time
     * @param width width of the path (used for the image straightening)
     * @return the kymoMontage as an ImagePlus
     */
    public ImagePlus getKymoMontage(int width){
    	if(width<1) width=1;
    	calcCalibAndGetKymoPathCoord(width);
        
        ImagePlus result=NewImage.createImage("KymoMontage from "+ip.getTitle(), nbPix, width*ip.getNSlices(), 1, 32, 1);
        
        for (int i=1; i<=ip.getNSlices(); i++){
            ip.setSlice(i);
            ip.setRoi(path);
            new ImagePlus("", st.straighten(ip, path, width)).copy(false);
            result.setRoi(new Roi(0, (i-1)*width, nbPix, width));
            result.paste();
            result.killRoi();
        }
        finalStep(result);
        return result;
    }

    /**
     * Calculates the calibration of the kymograph, based on the 2D+t stack's calibration
     * @param width width of the path (used for the image straightening)
     */
     private void calcCalibAndGetKymoPathCoord(int width){
        ip.setRoi(path);
        st.straighten(ip, path, width);
        path=ip.getRoi();
        Polygon pRoi=path.getPolygon();
        x=pRoi.xpoints;
        y=pRoi.ypoints;

        length=path.getLength();
        nbPix=pRoi.npoints;
        
        cal=new Calibration();
        cal.setXUnit(unit);
        cal.pixelWidth=length/nbPix;
        cal.setYUnit(timeUnit);
        cal.pixelHeight=timeIntervalle;
        cal.setTimeUnit(timeUnit);
        cal.frameInterval=timeIntervalle;
        
        
        infoString="<KymoPathInfo>\n<x>";
        for (int i=0; i<x.length; i++) infoString+=x[i]+"\t";
        infoString+="</x>\n<y>";
        for (int i=0; i<y.length; i++) infoString+=y[i]+"\t";
        infoString+="</y>\n</KymoPathInfo>";
    }
    
    /**
     * Doees the maximum intensity projection of an ImageProcessor along the y-axis
     * @param in input ImageProcessor
     * @return a float array containing maximum intensty values
     */
     private float[] yProj(ImageProcessor in){
        float[] out=new float[in.getWidth()];
        float[][] inArray=in.getFloatArray();
        
        for (int i=0; i<out.length; i++){
            float[] tmp=inArray[i];
            Arrays.sort(tmp);
            out[i]=tmp[tmp.length-1];
        }
        
        return out;
    }

    /**
     * Kills Rois, reset display, set calibration and "Info" property on the input ImagePlus
     * @param result input ImagePlus
     */
     private void finalStep(ImagePlus result){
        result.killRoi();
        result.resetDisplayRange();
        result.setCalibration(cal);
        result.setProperty("Info", infoString);
    }


}