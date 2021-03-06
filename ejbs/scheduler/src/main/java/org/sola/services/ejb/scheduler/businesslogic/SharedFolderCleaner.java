/**
 * ******************************************************************************************
 * Copyright (C) 2015 - Food and Agriculture Organization of the United Nations (FAO).
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice,this list
 *       of conditions and the following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice,this list
 *       of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *    3. Neither the name of FAO nor the names of its contributors may be used to endorse or
 *       promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,STRICT LIABILITY,OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * *********************************************************************************************
 */
package org.sola.services.ejb.scheduler.businesslogic;

import java.io.File;
import java.util.Calendar;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.*;
import org.sola.common.ConfigConstants;
import org.sola.common.FileMetaData;
import org.sola.common.FileUtility;
import org.sola.common.NetworkFolder;
import org.sola.services.common.logging.LogUtility;
import org.sola.services.ejb.system.businesslogic.SystemEJBLocal;

/**
 *
 * Scheduler class to cleanup shared folder with scanned images. Cleaning event
 * occurs on regular base. Configuration of cleaning period and files lifetime
 * is taken from configuration service.
 */
@Singleton
@Startup
public class SharedFolderCleaner implements SharedFolderCleanerLocal {

    @EJB
    private SystemEJBLocal systemEJB;
    @Resource
    TimerService timerService;
    private NetworkFolder scanFolder;
    private NetworkFolder thumbFolder;
    private NetworkFolder localCacheFolder;
    // The time for the file to live in minutes
    private int fileLifetime;
    // Cleanup period in minutes
    private int period;
    private boolean cleanFolder = false;

    /**
     * Initialization method to setup timer, shared folder path with scanned
     * images and lifetime of files.
     */
    @PostConstruct
    @Override
    public void init() {
        configureService();

        String pollPeriod = systemEJB.getSetting(ConfigConstants.CLEAN_NETWORK_SCAN_FOLDER_POLL_PERIOD, "60");
        period = Integer.valueOf(pollPeriod);

        long periodMs = (long) period * 60 * 1000;
        final TimerConfig timerConfig = new TimerConfig();
        timerConfig.setPersistent(false);
        timerService.createIntervalTimer(periodMs, periodMs, timerConfig);
    }

    /**
     * Configure the service based on the configuration data available in the
     * settings table
     */
    private void configureService() {
        cleanFolder = false;
        String cleanFolderFlag = systemEJB.getSetting(ConfigConstants.CLEAN_NETWORK_SCAN_FOLDER, "N");
        if (cleanFolderFlag.equalsIgnoreCase("Y")) {
            cleanFolder = true;
        }
        if (cleanFolder) {
            String scanFolderLocation = systemEJB.getSetting(ConfigConstants.NETWORK_SCAN_FOLDER,
                    System.getProperty("user.home") + "/sola/scan");

            String domain = systemEJB.getSetting(ConfigConstants.NETWORK_SCAN_FOLDER_DOMAIN, null);
            String shareUser = systemEJB.getSetting(ConfigConstants.NETWORK_SCAN_FOLDER_USER, null);
            String pword = systemEJB.getSetting(ConfigConstants.NETWORK_SCAN_FOLDER_PASSWORD, null);

            if (domain != null || shareUser != null) {
                // Connect to the folder share using the user account identified
                scanFolder = new NetworkFolder(scanFolderLocation, domain, shareUser, pword);
            } else {
                scanFolder = new NetworkFolder(scanFolderLocation);
            }

            localCacheFolder = new NetworkFolder(FileUtility.getCachePath());
            thumbFolder = localCacheFolder.getSubFolder("thumb");

            String fileLifetimeHrs = systemEJB.getSetting(ConfigConstants.SCANNED_FILE_LIFETIME, "720");
            fileLifetime = Integer.valueOf(fileLifetimeHrs) * 60;
        }
    }

    /**
     * Determines the name to use for a thumbnail of the file. 
     * @param thumbName
     * @return 
     */
    private String getThumbName(String thumbName) {
        if (thumbName.contains(".")) {
            thumbName = thumbName.substring(0, thumbName.lastIndexOf("."));
        }
        thumbName += ".jpg";
        return thumbName;
    }

    /**
     * This method is triggered automatically upon timer timeout event.
     *
     * @param timer Timer instance passed to the method automatically by
     * {@link TimerService}
     */
    @Timeout
    public void cleanUpTimeout(Timer timer) {
       // configureService();
        if (cleanFolder) {
            cleanUp();
        } else {
            LogUtility.log("Shared folder cleaning service is disabled - "
                    + Calendar.getInstance().getTime(), Level.INFO);
        }
    }

    /**
     * Cleans shared folder with scanned images, based on configuration
     * parameters.
     */
    public void cleanUp() {
        try {
            LogUtility.log("Start cleaning shared folder at "
                    + Calendar.getInstance().getTime(), Level.INFO);

            int filesDeleted = 0;
            long currentDate = Calendar.getInstance().getTimeInMillis();
            long fileLifetimeMs = (long) fileLifetime * 60 * 1000;

            if (scanFolder != null && scanFolder.exists()) {
                for (FileMetaData file : scanFolder.getAllFiles(null)) {
                    if (file.getModificationDate().getTime() + fileLifetimeMs <= currentDate) {

                        try {
                            scanFolder.deleteFile(file.getName());
                            thumbFolder.deleteFile(getThumbName(file.getName()));
                            localCacheFolder.deleteFile(file.getName());
                            filesDeleted += 1;
                        } catch (Exception ex) {
                            LogUtility.log(ex.getLocalizedMessage(), Level.SEVERE);
                        }
                    }
                }
            }

            LogUtility.log(String.format("Finished folder cleaning at %s. Deleted %s file(s)",
                    Calendar.getInstance().getTime(), filesDeleted), Level.INFO);

        } catch (Exception ex) {
            LogUtility.log(ex.getLocalizedMessage(), Level.SEVERE);
        }
    }
}