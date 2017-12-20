package Utilities.kymograph;


import ij.process.LUT;

/**
 *
 *  buildLUT v1, 9 mars 2009
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

/**
 * This class is used to build and return custom LUTs
 * @author Fabrice P. Cordelieres
 */
public class buildLUT {
    /** Default LUT: gray**/
    public static final int GRAY=0;
    /**KymoLUT: only 4 colors: black, blue, red, green**/
    public static final int KYMO=1;

    /**Number of nuances within the LUT**/
    int mapSize = 256;

    /**Stores the reds within a byte array**/
    byte[] reds=null;
    /**Stores the greens within a byte array**/
    byte[] greens=null;
    /**Stores the blues within a byte array**/
    byte[] blues=null;

    /**
     * Used to return LUT
     * @param LUTtype defines the type of LUT to return
     * @return a LUT object
     */
    public LUT getLUT(int LUTtype){
        switch (LUTtype){
            case GRAY: mapSize=256; break;
            case KYMO: mapSize=256; break;
        }

        reds = new byte[mapSize];
        greens = new byte[mapSize];
        blues = new byte[mapSize];

        switch (LUTtype){
            case GRAY: grays(); break;
            case KYMO: kymo(); break;
        }
        return new LUT(reds, greens, blues);
    }

    /**
     * Prepares the grays LUT
     * @return 256
     */
    int grays() {
        for (int i=0; i<256; i++) {
            reds[i] = (byte)i;
            greens[i] = (byte)i;
            blues[i] = (byte)i;
        }
        return 256;
    }

    /**
     * Prepares the kymoLUT
     * @return 256
     */
    int kymo() {
        mapSize=256;
        for (int i=64; i<128; i++) blues[i] = (byte)255;
        for (int i=128; i<192; i++) reds[i] = (byte)255;
        for (int i=192; i<256; i++) greens[i] = (byte)255;
        return 256;
    }

}
