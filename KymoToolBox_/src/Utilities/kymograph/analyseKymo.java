/**
 *
 *  analyseKymo v1, 19 oct. 2008 
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

import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.plugin.frame.RoiManager;
import ij.process.StackConverter;

import java.util.ArrayList;

/**
 * This class allows analysing kymographs and generating output images
 * @author Fabrice P. Cordelieres
 */
public class analyseKymo {
	/**Stores the original kymograph**/
    ImagePlus ip;
    /**Stores the kymograph's calibration**/
    Calibration cal;
    /**Stores the tracks to analyse on the kymograph**/
    Roi[] rois;
    /**True if the outward direction is from left to right**/
    boolean dir;
    /**Minimum speed to be considered for movement**/
    double minSpeed;
    /**True if all data should be logged**/
    boolean logFullData;
    /**Contains one pathInfo object per track to analyse**/
    ArrayList<pathInfo> tracks=null;
    /**x coordinates of the original path (on the 2D+t stack)**/
    int[] xPath=null;
    /**y coordinates of the original path (on the 2D+t stack)**/
    int[] yPath=null;
    
    /**
     * Starts the process of building an analyseKymo object
     * @param ip the input ImagePlus containing the kymograph. The image should be distance and time calibrated
     * @param rois Roi array containing all the tracks to analyse .Rois should be segmented lines in order to be analysed.
     * @param direction true if the outward direction is from left to right
     * @param minSpeed minimum speed to consider the object as moving. This speed is expressed in the same distance and time units as referenced in the image calibration.
     * @param logFullData true if all the data have to be logged (individual distance/speed for each track segment). If false, only a sumamry will be logged.
     */
    public analyseKymo(ImagePlus ip, Roi[] rois, boolean direction, double minSpeed, boolean logFullData){
        this.ip=ip;
        cal=ip.getCalibration();
        this.rois=rois;
        dir=direction;
        this.minSpeed=minSpeed;
        this.logFullData=logFullData;
        fillPathInfo();
    }

    /**
     * Starts the process of building an analyseKymo object from the Rois stored within the RoiManager
     * @param ip the input ImagePlus containing the kymograph. The image should be distance and time calibrated
     * @param direction true if the outward direction is from left to right
     * @param minSpeed minimum speed to consider the object as moving. This speed is expressed in the same distance and time units as referenced in the image calibration.
     * @param logFullData true if all the data have to be logged (individual distance/speed for each track segment). If false, only a sumamry will be logged.
     */
    public analyseKymo(ImagePlus ip, boolean direction, double minSpeed, boolean logFullData){
        this(ip, RoiManager.getInstance().getRoisAsArray(), direction, minSpeed, logFullData);
    }
    
    /**
     * Creates a composite image containing an overlay of the analysed, colored kymograph
     * on the original B&W kymograph. Green segments corresponds to outward movement,
     * red to inward movement and blue to pausing.
     * @param lineWidth line width to be used to generated the colored overlay
     * @return an ImagePlus (2 channels composite image)
     */
    public ImagePlus getImage(int lineWidth){
        CompositeImage out=null;

        if (tracks.size()!=0){
            ImagePlus ori=new ImagePlus(ip.getTitle(), ip.duplicate().getProcessor().convertToByteProcessor());
            
            ImagePlus img=NewImage.createImage("RGB "+ip.getTitle(), ip.getWidth(), ip.getHeight(), 2, 8, NewImage.FILL_BLACK);
            out=new CompositeImage(img, CompositeImage.COMPOSITE);
            out.setDimensions(2, 1, 1);
            out.setChannelLut(new buildLUT().getLUT(buildLUT.KYMO), 1);
              
            out.setPosition(2, 1, 1);
            out.setProcessor("Tracks from "+ip.getTitle(), ori.getProcessor());
            out.setChannelLut(new buildLUT().getLUT(buildLUT.GRAY), 2);
            
            out.setPosition(1, 1, 1);
            out.getChannelProcessor().setColor(0);
            out.getChannelProcessor().setLineWidth(lineWidth);
            for (int i=0; i<tracks.size(); i++){
                pathInfo pi=(pathInfo) tracks.get(i);
                for (int j=0; j<pi.nbPoints-1; j++){
                    switch (pi.mvt[j]){
                        case 0: out.getChannelProcessor().setColor(96); break;
                        case 1: out.getChannelProcessor().setColor(160); break;
                        case 2: out.getChannelProcessor().setColor(224); break;

                    }
                    out.getChannelProcessor().drawLine(pi.x[j], pi.y[j], pi.x[j+1], pi.y[j+1]);
               }
            }
            out.reset();
            out.resetDisplayRanges();
            out.setProperty("Info", ip.getProperty("Info"));
            out.setCalibration(cal);
        }
        return out;
    }

    /**
     * Based on the coordinates of the object saved in the kymograph's properties,
     * and on the analysis of the kymograph, coordiantes on the original 2D+t image
     * might be extrapolated reportOnStack does that job an returns an ImagePlus
     * where each position of the object, for each timepoint, is highlighted in green
     * for outward movement, red for inward movement and blue for pausing.
     * @param stack the input image: might be either the original image or a composite
     * @param dotSize size of the dots to draw on the colored overlay
     * @return an ImagePlus (2 channels, nSlices composite image)
     */
    public ImagePlus reportOnStack(ImagePlus stack, int dotSize){
        Calibration calStack=stack.getCalibration();
        CompositeImage out=null;
        
        if (!tracks.isEmpty()){
            if (!stack.isComposite()){
                ImagePlus stackCopy=new Duplicator().run(stack);
                new StackConverter(stackCopy).convertToGray8();
                ImagePlus tmp=NewImage.createByteImage("RGB "+stack.getTitle(), stack.getWidth(), stack.getHeight(), stack.getNSlices(), NewImage.FILL_BLACK);
                RGBStackMerge rsm= new RGBStackMerge();
                //ImageStack[] is={tmp.getImageStack(), stackCopy.getImageStack(), null};
                //stack=rsm.createComposite(stack.getWidth(), stack.getHeight(), stack.getNSlices(), is, true);
                stack=rsm.mergeHyperstacks(new ImagePlus[]{tmp, stackCopy, null}, true);
                tmp.flush();
                stackCopy.flush();
            }else{
                stack.hide();
            }
            out=new CompositeImage(stack, CompositeImage.COMPOSITE);
            out.setChannelLut(new buildLUT().getLUT(buildLUT.KYMO), 1);
            out.setChannelLut(new buildLUT().getLUT(buildLUT.GRAY), 2);

            out.setPosition(1, 1, 1);
            out.getChannelProcessor().setColor(0);
            out.getChannelProcessor().setLineWidth(dotSize);
            for (int i=0; i<tracks.size(); i++){
                pathInfo pi=(pathInfo) tracks.get(i);
                for (int j=0; j<pi.nbPointsFullPath; j++){
                    switch (pi.mvtFullPath[j]){
                        case 0: out.getChannelProcessor().setColor(96); break;
                        case 1: out.getChannelProcessor().setColor(160); break;
                        case 2: out.getChannelProcessor().setColor(224); break;

                    }
                    out.setPosition(1, (int) pi.yFullPath[j]+1, 1);
                    out.getChannelProcessor().drawDot(xPath[(int) pi.xFullPath[j]], yPath[(int) pi.xFullPath[j]]);
               }
            }
            out.resetDisplayRanges();
            out.setDisplayRange(0, 255, 1);
            out.setProperty("Info", ip.getProperty("Info"));
            out.setCalibration(cal);
            out.setCalibration(calStack);
        }
        return out;
    }

    /**
     * Logs the coordinates of the objects followed, extrapolated from the coordinates
     * of the original timelapse stack and from Rois drawn on the kymograph. As tracks
     * on the original stack have been spline fitted, the coordinates might not be accurate.
     */
    public void logCoord(){
        if (tracks.size()!=0){
            ResultsTable rt=new ResultsTable();
            rt.incrementCounter();
            int row;

            for (int i=0; i<tracks.size(); i++){
                pathInfo pi=(pathInfo) tracks.get(i);
                for (int j=0; j<pi.nbPointsFullPath; j++){
                    if (!(i==0 && j==0)) rt.incrementCounter();
                    row=rt.getCounter()-1;
                    rt.setLabel(ip.getTitle(), row);
                    rt.setValue("Kymo_nb", row, i+1);
                    rt.setValue("Time_("+cal.getTimeUnit()+")", row, pi.timeFullPath[j]);
                    
                    //interpolate x and y
                    double[] coord=interpolate(pi.xFullPath[j]);
                    double x=coord[0];
                    double y=coord[1];
                    
                    double xBefore=j==0?Double.NaN:rt.getValue("x", row-1);
                    double yBefore=j==0?Double.NaN:rt.getValue("y", row-1);
                    
                    rt.setValue("x", row, x);
                    rt.setValue("y", row, y);

                    double dist=j==0?Double.NaN:cal.pixelWidth*Math.sqrt((x-xBefore)*(x-xBefore)+(y-yBefore)*(y-yBefore));
                    if (dist!=0) dist=pi.mvtFullPath[j]==1?-dist:dist;

                    double speed=dist/cal.frameInterval;

                    rt.setValue("Distance_("+cal.getUnit()+")", row, dist);
                    rt.setValue("Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", row, speed);

                }
            }
            if (rt.getCounter()!=0) rt.show("Extrapolated coordinates from "+ip.getTitle());
        }
    }
    
    /**
     * Interpolates path coordinates, weighting based on the distance to the two closest pixels
     * @param xCoord x coordinate on the kymograph
     * @return an array of double containing the extrapolated x, y coordinates
     */
    private double[] interpolate(double xCoord){
    	int ground=(int) Math.round(xCoord-0.5);
    	int roof=(int) Math.round(xCoord+0.5);
    	
    	double distToGround=(xCoord-ground);
    	double distToRoof=(roof-xCoord);
    	
    	double dist=distToGround+distToRoof;
    	
    	int xBefore=xPath[ground];
    	int yBefore=yPath[ground];
    	
    	int xAfter=xPath[roof];
    	int yAfter=yPath[roof];
    	
    	//Weight are inverted to put more weight on the closest !
    	double x=(distToRoof*xBefore+distToGround*xAfter)/dist;
    	double y=(distToRoof*yBefore+distToGround*yAfter)/dist;
    	
    	return new double[]{x, y};
    	
    }
    
    /**
     * Retrieves the headers for the coordinates' table
     * @return the headers as an array of String
     */
    public String[] getCoordHeader(){
    	return new String[]{"Image", "Kymo_nb", "Time_("+cal.getTimeUnit()+")", "x", "y", "Distance_("+cal.getUnit()+")", "Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")"};
    }
    
    /**
     * Returns the coordinates of the objects followed, extrapolated from the coordinates
     * of the original timelapse stack and from Rois drawn on the kymograph. As tracks
     * on the original stack have been spline fitted, the coordinates might not be accurate.
     * @return an ArrayList of String arrays
     */
    public ArrayList<String[]> getCoord(){
    	ArrayList<String[]> out=null;
    	
        if (tracks.size()!=0){
        	out=new ArrayList<String[]>();
            for (int i=0; i<tracks.size(); i++){
                pathInfo pi=(pathInfo) tracks.get(i);
                for (int j=0; j<pi.nbPointsFullPath; j++){
                	double[] coord=interpolate(pi.xFullPath[j]);
                    double x=coord[0];
                    double y=coord[1];
                    
                    
                    double[] coordBefore=j==0?null:interpolate(pi.xFullPath[j-1]);
                    double xBefore=j==0?Double.NaN:coordBefore[0];
                    double yBefore=j==0?Double.NaN:coordBefore[1];
                    
                    double dist=j==0?Double.NaN:cal.pixelWidth*Math.sqrt((x-xBefore)*(x-xBefore)+(y-yBefore)*(y-yBefore));
                	
                	switch(pi.mvtFullPath[j]){
                		case 0: dist=0.0; break;
                		case 1: dist=Math.abs(dist); break;
                		case 2: dist=-Math.abs(dist); break;
                	}
                	
                	System.out.println(dist+"/"+pi.mvtFullPath[j]);
                	
                	//if (dist!=0) dist=pi.mvtFullPath[j]==1?-dist:dist;
                	
                    double speed=dist/cal.frameInterval;
                    out.add(new String[]{ip.getTitle(), (i+1)+"", pi.timeFullPath[j]+"", x+"", y+"", dist+"", speed+""});
                }
            }
        }
        return out;
    }
    
    /**
     * Does the analysis of the kymographs and log the data, either in a summarized or complete way
     * @param logFullData true if all the data have to be logged (individual distance/speed for each track segment). If false, only a sumamry will be logged.
     */
    public void logResults(boolean logFullData){
        tracks=new ArrayList<pathInfo>();
        for (int i=0; i<rois.length; i++){
            if (rois[i].getType()==Roi.LINE) rois[i]=new PolygonRoi(rois[i].getPolygon().xpoints, rois[i].getPolygon().ypoints, 2, Roi.POLYLINE);
            int type=rois[i].getType();
            if ((type==Roi.LINE || type==Roi.POLYLINE) && rois[i].getBounds().getHeight()!=0.0){
                tracks.add(new pathInfo(rois[i], dir, cal, minSpeed));
                if (((pathInfo) tracks.get(tracks.size()-1)).back2theFuture) tracks.remove(tracks.size()-1);
            }else{
                IJ.log("Roi "+rois[i].getName()+" is not a line or a polyline  Roi, or is a horizontal: no analysis will be done on that track.");
            }
        }
        for (int i=0; i<tracks.size(); i++) ((pathInfo) tracks.get(i)).logData(i+1, ip.getTitle(), logFullData);
    }
    
    /**
     * Retrieves the headers for the results' table
     * @return the headers as an array of String
     */
    public String[] getResultsHeader(boolean logFullData){
    	String[] min=new String[]{"Label", "Kymo_nb",
    			"Mean_Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", "Mean_Speed_In_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", "Mean_Speed_Out_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", 
    			"Cum_Dist_("+cal.getUnit()+")", "Cum_Dist_In_("+cal.getUnit()+")", "Cum_Dist_Out_("+cal.getUnit()+")", "Min_Dist_Start-End_("+cal.getUnit()+")", "Persistence", 
    			"Freq_In>Out_("+cal.getTimeUnit()+"-1)", "Freq_In>Pause_("+cal.getTimeUnit()+"-1)", "Freq_Out>In_("+cal.getTimeUnit()+"-1)", "Freq_Out>Pause_("+cal.getTimeUnit()+"-1)", "Freq_Pause>In_("+cal.getTimeUnit()+"-1)", "Freq_Pause>Out_("+cal.getTimeUnit()+"-1)", 
    			"Ttl_Time_("+cal.getTimeUnit()+")", "%_Time_In_", "%_Time_Out_", "%_Time_Pause_"};
    	
    	String[] max=new String[]{"Label", "Kymo_nb", "Ttl_Time_("+cal.getTimeUnit()+")", "Cum_Dist_("+cal.getUnit()+")", "Mean_Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")",
    			"Mean_Speed_In_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", "Mean_Speed_Out_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", 
    			"Cum_Dist_("+cal.getUnit()+")", "Cum_Dist_In_("+cal.getUnit()+")", "Cum_Dist_Out_("+cal.getUnit()+")", "Min_Dist_Start-End_("+cal.getUnit()+")", "Persistence", 
    			"Freq_In>Out_("+cal.getTimeUnit()+"-1)", "Freq_In>Pause_("+cal.getTimeUnit()+"-1)", "Freq_Out>In_("+cal.getTimeUnit()+"-1)", "Freq_Out>Pause_("+cal.getTimeUnit()+"-1)", "Freq_Pause>In_("+cal.getTimeUnit()+"-1)", "Freq_Pause>Out_("+cal.getTimeUnit()+"-1)", 
    			"Ttl_Time_("+cal.getTimeUnit()+")", "%_Time_In_", "%_Time_Out_", "%_Time_Pause_"};		
    	
    	return logFullData?max:min;
    }
    
    /**
     * Does the analysis of the kymographs and returns the data, either in a summarized or complete way
     * @param logFullData true if all the data have to be returned (individual distance/speed for each track segment). If false, only a sumamry will be logged.
     */
    public ArrayList<String[]> getResults(boolean logFullData){
    	ArrayList<String[]> out=new ArrayList<String[]>();
        tracks=new ArrayList<pathInfo>();
        for (int i=0; i<rois.length; i++){
            if (rois[i].getType()==Roi.LINE) rois[i]=new PolygonRoi(rois[i].getPolygon().xpoints, rois[i].getPolygon().ypoints, 2, Roi.POLYLINE);
            int type=rois[i].getType();
            if ((type==Roi.LINE || type==Roi.POLYLINE) && rois[i].getBounds().getHeight()!=0.0){
                tracks.add(new pathInfo(rois[i], dir, cal, minSpeed));
                if (((pathInfo) tracks.get(tracks.size()-1)).back2theFuture) tracks.remove(tracks.size()-1);
            }else{
                IJ.log("Roi "+rois[i].getName()+" is not a line or a polyline  Roi, or is a horizontal: no analysis will be done on that track.");
            }
        }
        for (int i=0; i<tracks.size(); i++){
        	ArrayList<String[]> currResults=((pathInfo) tracks.get(i)).getData(i+1, ip.getTitle(), logFullData);
        	for(int j=0; j<currResults.size(); j++) out.add(currResults.get(j));
        }
        return out;
    }
    
    

    /**
     * Retrieve the original path's coordinates stored in the "Info" property field
     * of the kymograph.
     */
    private void fillPathInfo(){
        String infoPath=(String) ip.getProperty("Info");
        if (infoPath!=null && infoPath.indexOf("<KymoPathInfo>")!=-1){
            String[] x=infoPath.substring(infoPath.lastIndexOf("<x>")+3, infoPath.lastIndexOf("</x>")).split("\t");
            String[] y=infoPath.substring(infoPath.lastIndexOf("<y>")+3, infoPath.lastIndexOf("</y>")).split("\t");
            xPath=new int[x.length];
            yPath=new int[y.length];
            for (int i=0; i<x.length; i++){
                xPath[i]=Integer.parseInt(x[i]);
                yPath[i]=Integer.parseInt(y[i]);
            }
        }
    }
}