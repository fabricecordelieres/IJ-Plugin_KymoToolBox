/**
 *
 *  pathInfo v1, 21 oct. 2008 
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

import java.util.ArrayList;

import ij.IJ;
import ij.gui.*;
import ij.measure.*;

/**
 * pathInfo handles full analysis of a track from a kymograph
 * @author Fabrice P. Cordelieres
 */
public class pathInfo {
    /**Code for pause**/
    public static final int PAUSE=0;
    /**Code for inward movement**/
    public static final int IN=1;
    /**Code for outward movement**/
    public static final int OUT=2;
    
    /**Is outward left to right on the image ?**/
    public static final boolean outIsL2R=true;
    /**Is outward right to left on the image ?**/
    public static final boolean outIsR2L=false;
    
    /**The kymograph calibration**/
    Calibration cal;
    /**Number of points in the Roi (nb of inflexion points) or on the interpolated Roi**/
    int nbPoints, nbPointsFullPath;
    /**Coordinates of the inflexion points on the Roi**/
    int[] x, y;
    /**Coordinates of all points on the interpolated Roi**/
    double[] xFullPath, yFullPath;
    /**Direction "reverser": 1 for outward is left to right, -1 otherwise**/
    int dir;
    /**Threshold speed below which the object is set to non mobile**/
    double minSpeed;
    /**Stores distances for each segment of the original Roi**/
    double[] dist;
    /**Stores the flight time either for each segment of the Roi or for all points of the interpolated Roi**/
    double[] time, timeFullPath;
    /**Stores speeds for each segment of the original Roi**/
    double[] speed;
    /**Stores the direction of movements either for each segment of
    the Roi or for all points of the interpolated Roi, using the following code. Pause=0, out=1, in=2**/
    int[] mvt, mvtFullPath;
    /**Cumulated travelled distances**/
    double cumDist;
    /**Cumulated inward travelled distances**/
    double cumDistIn;
    /**Cumulated outward travelled distances**/
    double cumDistOut;
    /**Average speed**/
    double meanSpeed;
    /**Average inward speed**/
    double meanSpeedIn;
    /**Average outward speed**/
    double meanSpeedOut;
    /**Total time of flight**/
    double totalTime;
    /**Total time of inward flight**/
    double timeIn;
    /**Total time of outward flight**/
    double timeOut;
    /**Total pausing time**/
    double timePause;
    /**Straight distance between the first and last points of the track**/
    double minDist;
    /**Persistence=cumDist/minDist**/
    double persistence;
    /**Frequency of inward to outward transitions**/
    double freqIn2Out;
    /**Frequency of inward to pause transitions**/
    double freqIn2Pause;
    /**Frequency of outward to inward transitions**/
    double freqOut2In;
    /**Frequency of outward to pause transitions**/
    double freqOut2Pause;
    /**Frequency of pause to inward transitions**/
    double freqPause2In;
    /**Frequency of pause to outward transitions**/
    double freqPause2Out;
    /**true if a trajectory was misdrawn ex: the path makes loops in time**/
    boolean back2theFuture=false;
    
    
    /**
     * Creates a new pathInfo object
     * @param roi track from the kymograph to analyse
     * @param direction true if outward is from left to right
     * @param cal calibration from the kymograph
     * @param minSpeed minimum speed above which the object is considered to move
     */
    public pathInfo(Roi roi, boolean direction, Calibration cal, double minSpeed){
        dir=direction?-1:1;
        this.cal=cal;
        this.minSpeed=minSpeed;
        
        x=roi.getPolygon().xpoints;
        y=roi.getPolygon().ypoints;
        nbPoints=roi.getPolygon().npoints;
        

        //Reverse array if ROI was traced from bottom to top
        minDist=Math.abs(x[nbPoints-1]-x[0])*cal.pixelWidth;
        if (y[0]>y[nbPoints-1]){
            int[] tmpX=new int[nbPoints];
            int[] tmpY=new int[nbPoints];
            
            for (int i=0; i<nbPoints; i++){
                tmpX[nbPoints-i-1]=x[i];
                tmpY[nbPoints-i-1]=y[i];
            }
            x=tmpX;
            y=tmpY;
        }

        //Check if the ROI has "back to the future" features
        for (int i=0; i<nbPoints-1; i++) if(y[i]>y[i+1]) back2theFuture=true;

        if (back2theFuture){
            IJ.log("Roi "+roi.getName()+" includes time loops: KymoToolBox is not suitable for analysis of objects travelling back in time...");
        }else{
            //Calculates the distance + speed with directionnalities
            cumDist=0;
            cumDistIn=0;
            cumDistOut=0;
            timeIn=0;
            timeOut=0;
            dist=new double[nbPoints-1];
            time=new double[nbPoints-1];
            speed=new double[nbPoints-1];
            mvt=new int[nbPoints-1];

            for (int i=0;i<nbPoints-1; i++){
                dist[i]=dir*(x[i+1]-x[i])*cal.pixelWidth;
                time[i]=(y[i+1]-y[i])*cal.frameInterval;
                cumDist+=Math.abs(dist[i]);
                speed[i]=dist[i]/time[i];
                if (speed[i]<-minSpeed){
                    mvt[i]=IN;
                    cumDistIn+=dist[i];
                    timeIn+=time[i];
                }else if (speed[i]>minSpeed){
                    mvt[i]=OUT;
                    cumDistOut+=dist[i];
                    timeOut+=time[i];
                }else{
                    mvt[i]=PAUSE;
                    timePause+=time[i];
                }
            }

            totalTime=(y[nbPoints-1]-y[0])*cal.frameInterval;
            persistence=cumDist/minDist;
            meanSpeedIn=cumDistIn/timeIn;
            meanSpeedOut=cumDistOut/timeOut;
            meanSpeed=cumDist/totalTime;
            timeIn/=totalTime;
            timeOut/=totalTime;
            timePause/=totalTime;

            //Transition frequencies
            freqIn2Out=0;
            freqIn2Pause=0;
            freqOut2In=0;
            freqOut2Pause=0;
            freqPause2In=0;
            freqPause2Out=0;
            for (int i=1; i<nbPoints-1; i++){
                if (mvt[i]!=mvt[i-1]){
                    if (mvt[i-1]==IN && mvt[i]==OUT) freqIn2Out++;
                    if (mvt[i-1]==IN && mvt[i]==PAUSE) freqIn2Pause++;
                    if (mvt[i-1]==OUT && mvt[i]==IN) freqOut2In++;
                    if (mvt[i-1]==OUT && mvt[i]==PAUSE) freqOut2Pause++;
                    if (mvt[i-1]==PAUSE && mvt[i]==IN) freqPause2In++;
                    if (mvt[i-1]==PAUSE && mvt[i]==OUT) freqPause2Out++;
                }
            }
            freqIn2Out/=totalTime;
            freqIn2Pause/=totalTime;
            freqOut2In/=totalTime;
            freqOut2Pause/=totalTime;
            freqPause2In/=totalTime;
            freqPause2Out/=totalTime;

            //Calculated full path coordinates and fills the mvtFullPath array
            nbPointsFullPath=y[y.length-1]-y[0]+1;
            xFullPath=new double[nbPointsFullPath];
            yFullPath=new double[nbPointsFullPath];
            mvtFullPath=new int[nbPointsFullPath];
            timeFullPath=new double[nbPointsFullPath];
            double a=0;
            double b=0;
            int index=0;


            for (int i=0; i<nbPoints-1; i++){
                a=(x[i+1]-x[i])==0?Double.NaN:((double) y[i+1]-y[i])/((double) x[i+1]-x[i]);
                b=y[i]-a*x[i];
                for (int j=y[i]; j<y[i+1]; j++){
                    xFullPath[index]=(Double.isNaN(a))?x[i]:((j-b)/a+.5);
                    yFullPath[index]=j;
                    mvtFullPath[index]=mvt[i];
                    timeFullPath[index++]=j*cal.frameInterval;
                }
            }
            xFullPath[index]=x[nbPoints-1];
            yFullPath[index]=y[nbPoints-1];
            mvtFullPath[index]=mvtFullPath[index-1];
            timeFullPath[index]=y[nbPoints-1]*cal.frameInterval;
        }
    }

    /**
     * Logs either a summary or the full informations about the track into the Results table
     * @param kymoNb track number to be logged into the table
     * @param rowLabel name of the path to be logged into the table
     * @param logFullData true if all informations are to be logged. If false, only summary informations will be logged
     */
    public void logData(int kymoNb, String rowLabel, boolean logFullData){
        if (!back2theFuture){
            ResultsTable rt=ResultsTable.getResultsTable();
            if (rt==null) rt=new ResultsTable();
            int row=0;

            if (logFullData){
                for (int i=0; i<nbPoints-1; i++){
                    rt.incrementCounter();
                    row=rt.getCounter()-1;
                    rt.setLabel(rowLabel, row);
                    rt.setValue("Kymo_nb", row, kymoNb);
                    rt.setValue("Ttl_Time_("+cal.getTimeUnit()+")", row, time[i]);
                    rt.setValue("Cum_Dist_("+cal.getUnit()+")", row, dist[i]);
                    rt.setValue("Mean_Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", row, speed[i]);
                }
            }

            //----------------------------Min data----------------------------
            rt.incrementCounter();
            row=rt.getCounter()-1;

            rt.setLabel(logFullData?">>>>Summary for "+rowLabel+"<<<<":rowLabel, row);
            rt.setValue("Kymo_nb", row, kymoNb);

            rt.setValue("Mean_Speed_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", row, meanSpeed);
            rt.setValue("Mean_Speed_In_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", row, meanSpeedIn);
            rt.setValue("Mean_Speed_Out_("+cal.getUnit()+"_per_"+cal.getTimeUnit()+")", row, meanSpeedOut);

            rt.setValue("Cum_Dist_("+cal.getUnit()+")", row, cumDist);
            rt.setValue("Cum_Dist_In_("+cal.getUnit()+")", row, cumDistIn);
            rt.setValue("Cum_Dist_Out_("+cal.getUnit()+")", row, cumDistOut);
            rt.setValue("Min_Dist_Start-End_("+cal.getUnit()+")", row, minDist);
            rt.setValue("Persistence", row, persistence);

            rt.setValue("Freq_In>Out_("+cal.getTimeUnit()+"-1)", row, freqIn2Out);
            rt.setValue("Freq_In>Pause_("+cal.getTimeUnit()+"-1)", row, freqIn2Pause);
            rt.setValue("Freq_Out>In_("+cal.getTimeUnit()+"-1)", row, freqOut2In);
            rt.setValue("Freq_Out>Pause_("+cal.getTimeUnit()+"-1)", row, freqOut2Pause);
            rt.setValue("Freq_Pause>In_("+cal.getTimeUnit()+"-1)", row, freqPause2In);
            rt.setValue("Freq_Pause>Out_("+cal.getTimeUnit()+"-1)", row, freqPause2Out);

            rt.setValue("Ttl_Time_("+cal.getTimeUnit()+")", row, totalTime);
            rt.setValue("%_Time_In_", row, timeIn);
            rt.setValue("%_Time_Out_", row, timeOut);
            rt.setValue("%_Time_Pause_", row, timePause);
            //----------------------------Min data----------------------------
            if (rt.getCounter()!=0) rt.show("Results");
        }
    }
    
    /**
     * Logs either a summary or the full informations about the track into the Results table
     * @param kymoNb track number to be logged into the table
     * @param rowLabel name of the path to be logged into the table
     * @param logFullData true if all informations are to be logged. If false, only summary informations will be logged
     */
    public ArrayList<String[]> getData(int kymoNb, String rowLabel, boolean logFullData){
        if (!back2theFuture){
        	ArrayList<String[]> out=new ArrayList<String[]>();
        	
            if (logFullData) for (int i=0; i<nbPoints-1; i++) out.add(new String[]{rowLabel, kymoNb+"", time[i]+"", dist[i]+"", speed[i]+""});
            

            //----------------------------Min data----------------------------
            out.add(	new String[]{logFullData?">>>>Summary for "+rowLabel+"<<<<":rowLabel, kymoNb+"", totalTime+"", cumDist+"", meanSpeed+"", meanSpeedIn+"", meanSpeedOut+"",
            			cumDist+"", cumDistIn+"", cumDistOut+"", minDist+"", persistence+"",
            			freqIn2Out+"", freqIn2Pause+"", freqOut2In+"", freqOut2Pause+"", freqPause2In+"", freqPause2Out+"",
            			totalTime+"", timeIn+"", timeOut+"", timePause+""});
            return out;
        }else{
        	return null;
        }
    }

}