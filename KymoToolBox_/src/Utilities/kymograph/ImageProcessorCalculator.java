/**
 *
 *  ImageProcessorCalculator v1, 15 oct. 2008 
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

import ij.process.*;

/**
 * ImageProcessorCalculator allows direct mathematical operations between two ImageProcessor
 * @author Fabrice P. Cordelieres
 */
public class ImageProcessorCalculator {
    public static final int ADD=0;
    public static final int SUBSTRACT=1;
    public static final int MULTIPLY=2;
    public static final int DIVIDE=3;
    
    
    /**
     * Creates a new ImageProcessorCalculator
     */
    public ImageProcessorCalculator(){
    }
    
    /**
     * Generates a new ImageProcessor containing the result of ip1+ip2
     * @param ip1 input 1 (ImageProcessor)
     * @param ip2 input 2 (ImageProcessor)
     * @return a new ImageProcessor containing the result of ip1+ip2
     */
    public ImageProcessor add(ImageProcessor ip1, ImageProcessor ip2){
        return this.calculate(ip1, ip2, ADD);
    }
    
     /**
     * Generates a new ImageProcessor containing the result of ip1-ip2
     * @param ip1 input 1 (ImageProcessor)
     * @param ip2 input 2 (ImageProcessor)
     * @return a new ImageProcessor containing the result of ip1-ip2
     */
    public ImageProcessor substract(ImageProcessor ip1, ImageProcessor ip2){
        return this.calculate(ip1, ip2, SUBSTRACT);
    }
    
     /**
     * Generates a new ImageProcessor containing the result of ip1*ip2
     * @param ip1 input 1 (ImageProcessor)
     * @param ip2 input 2 (ImageProcessor)
     * @return a new ImageProcessor containing the result of ip1*ip2
     */
    public ImageProcessor multiply(ImageProcessor ip1, ImageProcessor ip2){
        return this.calculate(ip1, ip2, MULTIPLY);
    }
    
     /**
     * Generates a new ImageProcessor containing the result of ip1/ip2
     * @param ip1 input 1 (ImageProcessor)
     * @param ip2 input 2 (ImageProcessor)
     * @return a new ImageProcessor containing the result of ip1/ip2
     */
    public ImageProcessor divide(ImageProcessor ip1, ImageProcessor ip2){
        return this.calculate(ip1, ip2, DIVIDE);
    }
    
    /**
     * Method used to generate the output by doing the pixelwise operation
     * @param ip1 input 1 (ImageProcessor)
     * @param ip2 input 2 (ImageProcessor)
     * @param operation ADD=0, SUBSTRACT=1, MULTIPLY=2, DIVIDE=3
     * @return a new ImageProcessor containing the result
     */
    private ImageProcessor calculate(ImageProcessor ip1, ImageProcessor ip2, int operation){
        if (ip1.getWidth()!= ip2.getWidth()|| ip1.getHeight()!=ip2.getHeight())throw new IllegalArgumentException("ImageProcessorCalculator expects the two IamgeProcessor to be of the same size.");
        ImageProcessor result=((ip1.duplicate()).convertToFloat());
        float[][] array1=ip1.getFloatArray();
        float[][] array2=ip2.getFloatArray();
        for (int i=0; i<array1.length; i++){
            for (int j=0; j<array1[0].length; j++){
                switch (operation){
                    case ADD: array1[i][j]+=array2[i][j]; break;
                    case SUBSTRACT: array1[i][j]-=array2[i][j]; break;
                    case MULTIPLY: array1[i][j]*=array2[i][j]; break;
                    case DIVIDE: array1[i][j]/=array2[i][j]; break;
                }
            }
        }
        result.setFloatArray(array1);
        return result;
    }
}