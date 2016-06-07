package com.android.volley.toolbox;

import android.os.Build;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

/**
 * Created by koplee on 16-5-27.
 */
public class GetDeviceInfo {

    //获取CPU核心数
    public int getNumberOfCores(){
        if(Build.VERSION.SDK_INT >= 17){
            return Runtime.getRuntime().availableProcessors();
        }else{
            return getNumberOfCoresOldPhones();
        }
    }

    public int getNumberOfCoresOldPhones(){
        //private Class to display only CPU devices in the directory listing
        class CPUFilter implements FileFilter{
            @Override
            public boolean accept(File file) {
                //Check if filename is "cpu", followed by a single digit number
                if(Pattern.matches("cpu[0-9]+", file.getName())){
                    return true;
                }
                return false;
            }
        }

        try{
            //Get directory containing CPU info
            File dir = new File("/sys/devices/system/cpu/");
            //Filter to only list the devices we care about
            File[] files = dir.listFiles(new CPUFilter());
            //Return the number of cores(virturl CPU devices)
            return files.length;
        }catch(Exception e){
            //Default
            return 1;
        }
    }
}

