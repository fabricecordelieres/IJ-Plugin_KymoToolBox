/**
 *
 *  Draw_Kymo v1, 16 oct. 2008 
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
import ij.plugin.*;

import Utilities.kymograph.*;
import ij.measure.Calibration;
import ij.plugin.frame.RoiManager;

public class MultipleDraw_Kymo implements PlugIn{
    ImagePlus ip=null;
    int width=(int) Prefs.get("DrawKymo_width.double", 10);    
    Boolean doKymo=Prefs.get("DrawKymo_doKymo.boolean", true);    
    Boolean doKymoStack=Prefs.get("DrawKymo_doKymoStack.boolean", true);    
    Boolean doKymoMontage=Prefs.get("DrawKymo_doKymoMontage.boolean", true);
    
    public void run(String arg){
        if (WindowManager.getImageCount()!=0) ip=WindowManager.getCurrentImage();
        RoiManager rm=RoiManager.getInstance();

        if (IJ.versionLessThan("1.42k")) return;
        
        if (ip==null){
            IJ.error("Multiple Draw Kymo expects an image to be opened");
            return;
        }

        if (rm==null || rm.getCount()==0){
            IJ.error("Multiple Draw Kymo expects the RoiManager\nto be opened and to contain at least one ROI");
            return;
        }
        
        if (ip.getBitDepth()==24){
            IJ.error("Multiple Draw Kymo doesn't work on RGB images");
            return;
        }
        
        if (ip.getNSlices()==1){
            IJ.error("Multiple Draw Kymo requieres a stack");
            return;
        }

        if (ip.getGlobalCalibration()!=null){
            GenericDialog gd=new GenericDialog("Error: global calibration");
            gd.addMessage("Multiple Draw Kymo is not compatible with global calibration:\n" +
                    "the kymograph(s) won't be correctly calibrated.\n" +
                    "Continue (global calibration will be erased) or cancel ?");
            gd.showDialog();
            if (gd.wasOKed()) ip.setGlobalCalibration(null);
            if (gd.wasCanceled()) return;
        }

        if (ip.getCalibration().getUnit().equals("pixel") || ip.getCalibration().frameInterval==0){
            if (!useSmartCalib()){
                IJ.run("Properties...");
                return;
            }
        }
        
        
        GenericDialog gd=new GenericDialog("Multiple Draw Kymo");
        gd.addNumericField("Width", width, 0);
        gd.addCheckbox("Get_kymo", doKymo);
        gd.addCheckbox("Get_kymoStack", doKymoStack);
        gd.addCheckbox("Get_kymoMontage", doKymoMontage);
        gd.showDialog();
        
        if (gd.wasCanceled()) return;
        
        width=(int) gd.getNextNumber();
        doKymo=gd.getNextBoolean();
        doKymoStack=gd.getNextBoolean();
        doKymoMontage=gd.getNextBoolean();
        
        Prefs.set("DrawKymo_width.double", width);
        Prefs.set("DrawKymo_doKymo.boolean", doKymo);
        Prefs.set("DrawKymo_doKymoStack.boolean", doKymoStack);
        Prefs.set("DrawKymo_doKymoMontage.boolean", doKymoMontage);

        Roi[] roiArray=rm.getRoisAsArray();
        for (int i=0; i<rm.getCount(); i++){
            int type=roiArray[i].getType();
            if (type==Roi.LINE || type==Roi.POLYLINE || type==Roi.FREELINE){
                kymograph kymo=new kymograph(ip, roiArray[i]);
                ImagePlus result=null;
                if (doKymo){
                    result=kymo.getKymograph(width);
                    result.setTitle(i+1+"-Kymograph from "+ip.getTitle());
                    result.show();
                }
                if (doKymoStack){
                    result=kymo.getKymoStack(width);
                    result.setTitle(i+1+"-KymoStack from "+ip.getTitle());
                    result.show();
                }
                if (doKymoMontage){
                    result=kymo.getKymoMontage(width);
                    result.setTitle(i+1+"-KymoMontage from "+ip.getTitle());
                    result.show();
                }
            }else{
                IJ.log("Roi "+roiArray[i].getName()+" is not a line or a polyline Roi: no kymo, kymoStack and/or kymoMontage will be drawn from it.");
            }
        }
    }

    private boolean useSmartCalib(){
        Calibration smartCalib=new Smart_Calib().getCalibration();
        GenericDialog gd=new GenericDialog("Error: no calibration found");
        gd.addMessage("Draw Kymo requieres the image to be time and space calibrated.");
        if (smartCalib!=null){
            gd.addMessage("Use the following Smart Calib' as calibration values ?\n" +
                    "Pixel width/height: "+ smartCalib.pixelWidth+smartCalib.getUnit()+"; Frame interval: "+smartCalib.frameInterval+smartCalib.getTimeUnit());
        }
        gd.showDialog();

        if (gd.wasOKed()){
            if (smartCalib==null){
                return false;
            }else{
                ip.setCalibration(smartCalib);
                return true;
            }
        }

        if (gd.wasCanceled()){
            return false;
        }
        return false;
    }
}