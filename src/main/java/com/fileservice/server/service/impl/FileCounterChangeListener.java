package com.fileservice.server.service.impl;

import java.util.Set;

import com.fileservice.server.cache.FCCache;
import com.fileservice.server.cache.FNCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.stereotype.Component;

@Component
public class FileCounterChangeListener implements FileChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCounterChangeListener.class);

    @Autowired
    private FCCache FCCache;

    @Autowired
    private FNCache FNCache;

    @Override
    public void onChange(Set<ChangedFiles> changeSet) {
        for (ChangedFiles cfiles : changeSet) {
            for (ChangedFile cfile : cfiles.getFiles()) {
                LOGGER.debug("Changed file: " + cfile.getRelativeName());
                if ((cfile.getType().equals(ChangedFile.Type.ADD))) {
                    FCCache.incrementFolderFileCount();
                    FNCache.addFileName(cfile.getRelativeName());
                } else if ((cfile.getType().equals(ChangedFile.Type.DELETE))) {
                    FCCache.decrementFolderFileCount();
                    FNCache.removeFileName(cfile.getRelativeName());
                } else if ((cfile.getType().equals(ChangedFile.Type.MODIFY))){
                    
                }
            }
        }
    }
}
