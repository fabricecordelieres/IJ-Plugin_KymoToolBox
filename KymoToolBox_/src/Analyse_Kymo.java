/**
 *
 *  Analyse_Kymo v1, 18 oct. 2008 
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
import ij.measure.*;
import ij.plugin.*;
import ij.plugin.frame.*;

import Utilities.kymograph.*;

public class Analyse_Kymo implements PlugIn{
    ImagePlus ip=null;
    RoiManager rm;
    Roi[] rois;
    int count=0;
    Calibration cal;
    String[] dir={"From left to right", "From right to left"};
    int dirSelect=(int) Prefs.get("AnalyseKymo_dirSelect.double", 0);
    double minSpeed=Prefs.get("AnalyseKymo_minSpeed.double", 0);
    int lineWidth=(int) Prefs.get("AnalyseKymo_lineWidth.double", 2);
    boolean logFullData=Prefs.get("AnalyseKymo_logFullData.boolean", true);
    boolean logCoord=Prefs.get("AnalyseKymo_logCoord.boolean", true);
    boolean colorKymo=Prefs.get("AnalyseKymo_colorKymo.boolean", true);
    boolean reportOnStack=Prefs.get("AnalyseKymo_reportOnStack.boolean", true);
    int dotSize=(int) Prefs.get("AnalyseKymo_dotSize.double", 5);
    int selectedStack;
    String[] stacks;
    
    public void run(String arg){
        if (WindowManager.getImageCount()!=0) ip=WindowManager.getCurrentImage();
        rm=RoiManager.getInstance();

        if (IJ.versionLessThan("1.42k")) return;
        
        if (rm==null){
            IJ.error("Analyse Kymo requieres the RoiManager to be opened.");
            return;
        }
        
        rois=rm.getRoisAsArray();
        
        for (int i=0; i<rm.getCount(); i++){
            int type=rois[i].getType();
            if (type==Roi.LINE || type==Roi.POLYLINE) count++;
        }
        
        if (count==0){
            IJ.error("Analyse Kymo requieres the RoiManager to contain at least one line/freeline selection.");
            return;
        }
        
        if (ip==null){
            IJ.error("Analyse Kymo expects an image to be opened");
            return;
        }
        
        cal=ip.getCalibration();
        
        if (cal.getUnit().equals("pixel") || cal.frameInterval==0){
            IJ.error("Analyse Kymo requieres the image to be spatially and timely calibrated (go to Image/Properties...)");
            IJ.run("Properties...");
            return;
        }

        int[] idFullList=WindowManager.getIDList();
        int nbStacks=0;

        for (int i=0; i<idFullList.length; i++){
            if (WindowManager.getImage(idFullList[i]).getNSlices()!=1) nbStacks++;
        }

        stacks=new String[nbStacks];
        int[] idList=new int[nbStacks];
        nbStacks=0;
        for (int i=0; i<idFullList.length; i++){
            if (WindowManager.getImage(idFullList[i]).getNSlices()!=1){
                idList[nbStacks]=idFullList[i];
                stacks[nbStacks++]=WindowManager.getImage(idFullList[i]).getTitle();
            }
        }
        
        GenericDialog gd=new GenericDialog("Analyse Kymo");
        gd.addChoice("Outward is...", dir, dir[dirSelect]);
        gd.addNumericField("Lim. speed ("+ip.getCalibration().getUnit()+"/"+ip.getCalibration().getTimeUnit()+")", minSpeed, 2);
        gd.addNumericField("Line width", lineWidth, 0);
        gd.addCheckbox("Log_all_data", logFullData);
        gd.addCheckbox("Log_extrapolated_coordinates", logCoord);
        gd.addCheckbox("Show colored kymo", colorKymo);
        if (stacks.length!=0){
            gd.addCheckbox("Report coordinates on original stack", reportOnStack);
            gd.addChoice("Original stack", stacks, stacks[0]);
            gd.addNumericField("Dot size", dotSize, 0);
        }

        gd.showDialog();

        if (gd.wasCanceled()) return;
        
        dirSelect=gd.getNextChoiceIndex();
        minSpeed=gd.getNextNumber();
        lineWidth=(int) gd.getNextNumber();
        logFullData=gd.getNextBoolean();
        logCoord=gd.getNextBoolean();
        colorKymo=gd.getNextBoolean();
        if (stacks.length!=0){
            reportOnStack=gd.getNextBoolean();
            selectedStack=gd.getNextChoiceIndex();
            dotSize=(int) gd.getNextNumber();
        }
        
        Prefs.set("AnalyseKymo_dirSelect.double", dirSelect);
        Prefs.set("AnalyseKymo_minSpeed.double", minSpeed);
        Prefs.set("AnalyseKymo_lineWidth.double", lineWidth);
        Prefs.set("AnalyseKymo_logFullData.boolean", logFullData);
        Prefs.set("AnalyseKymo_logCoord.boolean", logCoord);
        Prefs.set("AnalyseKymo_colorKymo.boolean", colorKymo);
        Prefs.set("AnalyseKymo_reportOnStack.boolean", reportOnStack);
        Prefs.set("AnalyseKymo_dotSize.double", dotSize);

        String infoPath=(String) ip.getProperty("Info");
        boolean infoPathNotNull=infoPath!=null?true:false;
        boolean kymoPathExists=false;
        if (infoPathNotNull)if (infoPath.indexOf("<KymoPathInfo>")!=-1) kymoPathExists=true;

        analyseKymo ak=new analyseKymo(ip, rois, dirSelect==1, minSpeed, logFullData);
        ak.logResults(logFullData);
        if(colorKymo){
            ImagePlus cKymo=ak.getImage(lineWidth);
            if (cKymo!=null) cKymo.show();
        }

        
        if(kymoPathExists){
            if(logCoord) ak.logCoord();
            if(stacks.length!=0 && reportOnStack){
                ImagePlus cStack=ak.reportOnStack(WindowManager.getImage(idList[selectedStack]), dotSize);
                if (cStack!=null) cStack.show();
            }
        }else{
            IJ.error("KymoPathInfo missing", "Can't log coordinates and/or report tracks on stack:\nKymoPathInfo is missing from the image \"Info\" field");
        }
        
        
    }


}