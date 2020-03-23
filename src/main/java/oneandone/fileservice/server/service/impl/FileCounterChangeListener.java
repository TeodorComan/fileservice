package oneandone.fileservice.server.service.impl;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.filewatch.FileChangeListener;
import org.springframework.stereotype.Component;

import oneandone.fileservice.server.cache.FileCounterCache;
import oneandone.fileservice.server.cache.FileNamesCache;

@Component
public class FileCounterChangeListener implements FileChangeListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCounterChangeListener.class);

    @Autowired
    private FileCounterCache fileCounterCache;

    @Autowired
    private FileNamesCache fileNamesCache;

    @Override
    public void onChange(Set<ChangedFiles> changeSet) {
        for (ChangedFiles cfiles : changeSet) {
            for (ChangedFile cfile : cfiles.getFiles()) {
                LOGGER.debug("Changed file: " + cfile.getRelativeName());
                if ((cfile.getType().equals(ChangedFile.Type.ADD))) {
                    fileCounterCache.incrementFolderFileCount();
                    fileNamesCache.addFileName(cfile.getRelativeName());
                } else if ((cfile.getType().equals(ChangedFile.Type.DELETE))) {
                    fileCounterCache.decrementFolderFileCount();
                    fileNamesCache.removeFileName(cfile.getRelativeName());
                } else if ((cfile.getType().equals(ChangedFile.Type.MODIFY))){
                    
                }
            }
        }
    }
}
