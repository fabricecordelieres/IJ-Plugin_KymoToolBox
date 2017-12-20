
import ij.Prefs;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;

/**
 *
 *  Smart_Calib v1, 18 mars 2009
    Fabrice P Cordelieres, fabrice.cordelieres at gmail.com

    Copyright (C) 2009 Fabrice P. Cordelieres

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

public class Smart_Calib implements PlugIn{

    public void run(String arg) {
        double xyCalib=Prefs.get("SmartCalib_xyCalib.double", 0.129);
        double frameInterval=Prefs.get("SmartCalib_frameInterval.double", 2);
        String spaceUnit=Prefs.get("SmartCalib_spaceUnit.String", "µm");
        String timeUnit=Prefs.get("SmartCalib_timeUnit.String", "sec");

        GenericDialog gd=new GenericDialog("Smart Calib'");
        gd.addNumericField("Pixel width/height", xyCalib, 3);
        gd.addStringField("Space unit", spaceUnit);
        gd.addNumericField("Frame interval", frameInterval, 3);
        gd.addStringField("Time unit", timeUnit);
        gd.showDialog();

        if (gd.wasCanceled()) return;

        Prefs.set("SmartCalib_xyCalib.double", gd.getNextNumber());
        Prefs.set("SmartCalib_spaceUnit.String", gd.getNextString());
        Prefs.set("SmartCalib_frameInterval.double", gd.getNextNumber());
        Prefs.set("SmartCalib_timeUnit.String", gd.getNextString());
        Prefs.set("SmartCalib_isSet.boolean", true);
    }

    public Calibration getCalibration(){
        Calibration smartCalib=new Calibration();
        if(!Prefs.get("SmartCalib_isSet.boolean", false)){
            smartCalib=null;
        }else{
            smartCalib.pixelWidth=Prefs.get("SmartCalib_xyCalib.double", 0.129);
            smartCalib.pixelHeight=Prefs.get("SmartCalib_xyCalib.double", 0.129);
            smartCalib.setUnit(Prefs.get("SmartCalib_spaceUnit.String", "µm"));
            smartCalib.frameInterval=Prefs.get("SmartCalib_frameInterval.double", 2);
        }
        return smartCalib;
    }

}
