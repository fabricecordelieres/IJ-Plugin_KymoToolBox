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

public class Draw_Kymo implements PlugIn{
    ImagePlus ip=null;
    Roi roi=null;
    int width=(int) Prefs.get("DrawKymo_width.double", 10);    
    Boolean doKymo=Prefs.get("DrawKymo_doKymo.boolean", true);    
    Boolean doKymoStack=Prefs.get("DrawKymo_doKymoStack.boolean", true);
    Boolean doKymoMontage=Prefs.get("DrawKymo_doKymoMontage.boolean", true);
    
    public void run(String arg){
        if (WindowManager.getImageCount()!=0){
            ip=WindowManager.getCurrentImage();
            roi=ip.getRoi();
        }
        
        if (IJ.versionLessThan("1.42k")) return;
        
        if (ip==null){
            IJ.error("Draw Kymo expects an image to be opened");
            return;
        }

        if (roi==null){
            IJ.error("Draw Kymo expects a ROI to be drawn on the image");
            return;
        }
        
        int type=roi.getType();
        
        if (type!=Roi.LINE  && type!=Roi.FREELINE && type!=Roi.POLYLINE){
            IJ.error("Draw Kymo expects a line, a polyline or a freehand ROI to be drawn on the image");
            return;
        }
        
        if (ip.getBitDepth()==24){
            IJ.error("Draw Kymo doesn't work on RGB images");
            return;
        }
        
        if (ip.getNSlices()==1){
            IJ.error("Draw Kymo requieres a stack");
            return;
        }

        if (ip.getGlobalCalibration()!=null){
            GenericDialog gd=new GenericDialog("Error: global calibration");
            gd.addMessage("Draw Kymo is not compatible with global calibration:\n" +
                    "the kymograph won't be correctly calibrated.\n" +
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
        
        GenericDialog gd=new GenericDialog("Draw Kymo");
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
        
        kymograph kymo=new kymograph(ip);
        
        if (doKymo) kymo.getKymograph(width).show();
        if (doKymoStack) kymo.getKymoStack(width).show();
        if (doKymoMontage) kymo.getKymoMontage(width).show();
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