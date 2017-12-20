/**
 *
 *  ImproveKymo v1, 15 oct. 2008 
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
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.process.*;

/**
 * improveKymo filters an image on a simili wavelet basis.
 * The convolution kernel used is a 2D Gaussian of growing diameter and wavelet planes
 * are obtained by substracting two suggessive so-called wavelet planes.
 * @author Fabrice P. Cordelieres
 */
public class improveKymo {
    /**The input ImagePlus (if any)**/
    ImagePlus ip=null;
    
    /**
     * Starts building a new uninitialised improveKymo object
     */
    public improveKymo(){
    }
    
    /**
     * Starts building a new improveKymo object based on an input ImagePlus
     * @param ip input Imageplus
     */
    public improveKymo(ImagePlus ip){
        this.ip=ip;
    }
    
    /**
     * Generates the n-th wavelet plane ie (input o gauss(diam=n-1)) - (input o gauss(diam=n))
     * @param index n-th wavelet plane index
     * @return an ImageProcessor
     */
    public ImageProcessor getPlane(int index){
        ImageProcessor plane1=ip.getProcessor().duplicate();
        ImageProcessor plane2=ip.getProcessor().duplicate();
        (new GaussianBlur()).blurGaussian(plane1, index-1, index-1, 0.00001);
        (new GaussianBlur()).blurGaussian(plane2, index, index, 0.00001);
        plane1=new ImageProcessorCalculator().substract(plane1, plane2);
        return plane1;
    }
    
    /**
     * Generates all the wavelet planes from index start to stop. NB: the n-th
     * wavelet plane is built as follows: output(n) = (input o gauss(diam=n-1)) - (input o gauss(diam=n))
     * @param start first wavelet plane to return
     * @param stop last wavelet plane to return
     * @return an ImagePlus
     */
    public ImagePlus getPlanes(int start, int stop){
        ImagePlus result=NewImage.createImage("Filtered "+ip.getTitle()+"("+start+"-"+stop+")", ip.getWidth(), ip.getHeight(), stop-start+1, 32, 1);
        for (int i=start; i<=stop; i++){
            result.setSlice(i-start+1);
            ImageProcessor plane1=ip.getProcessor().duplicate();
            ImageProcessor plane2=ip.getProcessor().duplicate();
            (new GaussianBlur()).blurGaussian(plane1, i-1, i-1, 0.00001);
            (new GaussianBlur()).blurGaussian(plane2, i, i, 0.00001);
            result.setProcessor("", new ImageProcessorCalculator().substract(plane1, plane2));
        }
        result.resetDisplayRange();
        return result;
    }
    
    /**
     * Generates an ImagePlus by calculating, then summing the start to end wavelet
     * planes: works on the current slice of the input image
     * @param start first wavelet plane to consider
     * @param stop  last wavelet plane to consider
     * @return an ImagePlus containing the filtered image
     */
    public ImagePlus getFilteredSlice(int start, int stop){
        ImagePlus result=NewImage.createImage("Filtered "+ip.getTitle()+"("+start+"-"+stop+")", ip.getWidth(), ip.getHeight(), 1, 32, 1);
        ZProjector zp=new ZProjector();
        zp.setMethod(ZProjector.SUM_METHOD);
        ImagePlus stack=getPlanes(start, stop);
        zp.setImage(stack);
        zp.doProjection();
        result.setProcessor("", zp.getProjection().getProcessor());
        result.resetDisplayRange();
        return result;
    }
    
    /**
     * Filters the input ImageProcessor by calculating, then summing the start to end wavelet planes
     * @param iproc the input ImageProcessor to filter
     * @param start first wavelet plane to consider
     * @param stop  last wavelet plane to consider
     */
    public void filterSlice(ImageProcessor iproc, int start, int stop){
        this.ip=new ImagePlus("", iproc);
        ZProjector zp=new ZProjector();
        zp.setMethod(ZProjector.SUM_METHOD);
        ImagePlus stack=getPlanes(start, stop);
        zp.setImage(stack);
        zp.doProjection();
        ip.setProcessor("", zp.getProjection().getProcessor());
        ip.resetDisplayRange();
        iproc=ip.getProcessor();
    }
    
     /**
     * Generates an ImagePlus by calculating, then summing the start to end wavelet
     * planes: works on all the slices of the input image
     * @param start first wavelet plane to consider
     * @param stop  last wavelet plane to consider
     * @return an ImagePlus containing the filtered image
     */
    public ImagePlus getFilteredImage(int start, int stop){
        ImagePlus result=NewImage.createImage("Filtered "+ip.getTitle()+"("+start+"-"+stop+")", ip.getWidth(), ip.getHeight(), ip.getNSlices(), 32, 1);
        ZProjector zp=new ZProjector();
        zp.setMethod(ZProjector.SUM_METHOD);
        for (int i=1; i<=ip.getNSlices(); i++){
            result.setSlice(i);
            ip.setSlice(i);
            ImagePlus stack=getPlanes(start, stop);
            zp.setImage(stack);
            zp.doProjection();
            result.setProcessor("", zp.getProjection().getProcessor());
        }
        result.setSlice(1);
        result.resetDisplayRange();
        return result;
    }
    

}