package com.fileservice.server.service.impl;

import com.fileservice.server.cache.FCCache;
import com.fileservice.server.cache.FNCache;
import com.fileservice.server.exception.ClientException;
import com.fileservice.server.exception.ClientExceptionMessage;
import com.fileservice.server.exception.ServerException;
import com.fileservice.server.model.File;
import com.fileservice.server.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.fileservice.server.Constants.CHARACTERS;
import static com.fileservice.server.Constants.DATE_FORMAT;


@Service
public class FileServiceImpl implements FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

    @Value("${fpath}")
    private String path;
    @Value("${fname}")
    private String name;
    @Autowired
    private NioFilesWrapper nioFilesWrapper;
    @Autowired
    private FCCache FCCache;
    @Autowired
    private FNCache FNCache;
    private Path fileUPath;
    private Path fileBPath;

    @PostConstruct
    void init( ){
        this.fileUPath =Paths.get(path).toAbsolutePath().normalize();
        this.fileBPath =Paths.get(name).toAbsolutePath().normalize();
    }
    @Override
    public File get(String name, Option... option) {

        Path filePath = fileUPath.resolve(name);

        if(nioFilesWrapper.notExists(filePath)){
            return null;
        }

        File file = new File();
        file.setName(name);
        try {
            file.setLastModified(nioFilesWrapper.getLastModifiedTime(filePath).toMillis());
        } catch (IOException e) {
            throw new ServerException("Couldn't read file lastModifiedTime: " + name,e);
        }
        if(option.equals(Option.ALL)) {



            try {
                file.setContent(nioFilesWrapper.readAllBytes(filePath));
            } catch (IOException e) {
                throw new ServerException("Couldn't read file content: " + filePath, e);
            }
        }






        return file;
    }

    @Override
    public void deleteFile(String name) {
        Path filePath = fileUPath.resolve(name);

        if(nioFilesWrapper.notExists(filePath)){
            throw new ClientException(ClientExceptionMessage.MISSING_FILE,"The file: " + name + " doesn't exist");
        }

        try {
            nioFilesWrapper.delete(filePath);
        }
        catch (IOException e) {
            throw new ServerException("Couldn't delete file: " + filePath.toString(),e);
        }

    }

    @Override
    public void createFile(File file) {

        if(file.getContent()==null || file.getContent().length==0){
            throw new ClientException(ClientExceptionMessage.MISSING_CONTENT, "The file content doesn't exist.");
        }

        String fileName = StringUtils.stripFilenameExtension(file.getName());

        if(!fileName.matches(CHARACTERS)){
            throw new ClientException(ClientExceptionMessage.INVALID_FILENAME, "The filename is invalid.");
        }

        Path filePath = fileUPath.resolve(file.getName());

        Path backupFilePath=null;

        boolean fileBackedUp=false;

        if(nioFilesWrapper.exists(filePath)) {
            LOGGER.debug("File {} already exists, will overwrite.",file);

            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

            String backupFileName = filePath.getFileName()+"_"+sdf.format(new Date());

            backupFilePath = fileBPath.resolve(backupFileName);

            try {
                nioFilesWrapper.copy(filePath,backupFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                LOGGER.debug("Backed up file {}", filePath);
            } catch (IOException e) {
                throw new ServerException("Couldn't backup file " + filePath, e);
            }

            fileBackedUp=true;
        }

        try {
            nioFilesWrapper.copy(new ByteArrayInputStream(file.getContent()),filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Failed to create file {}",filePath,e);
            if(fileBackedUp) {
                try {
                    nioFilesWrapper.copy(backupFilePath,filePath, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException ex) {
                    throw new ServerException("Couldn't restore backup file " + backupFilePath, ex);
                }

                try {
                    nioFilesWrapper.delete(backupFilePath);
                }
                catch (IOException exy) {
                    throw new ServerException("Couldn't delete file: " + filePath.toString(),exy);
                }
            }
            throw new ServerException("Couldn't create file", e);
        }

        if(fileBackedUp) {
            try {
                nioFilesWrapper.delete(fileBPath.resolve(backupFilePath));
            } catch (IOException e) {
                LOGGER.error("Failed to delete backupfile: {}", backupFilePath);
            }
        }

    }

    @Override
    public void update(String fileName, File file) {

        Path currentFilePath = fileUPath.resolve(fileName);

        if(fileName == null || fileName.trim().isEmpty()){
            throw new ClientException(ClientExceptionMessage.INVALID_FILENAME,"The filename is null");
        }

        if(file==null || (file.getName()==null && file.getContent()==null)){
            throw new ClientException(ClientExceptionMessage.INVALID_REQUEST,"The name or the content is mandatory");
        }

        if(file.getName()!=null){
            String diskFileName = StringUtils.stripFilenameExtension(file.getName());
            if(!diskFileName.matches(CHARACTERS)){
                throw new ClientException(ClientExceptionMessage.INVALID_FILENAME, "The filename is invalid.");
            }
        }

        if(file.getLastModified()==0L){
            throw new ClientException(ClientExceptionMessage.INVALID_REQUEST,"Last Modified date is mandatory");
        }

        if(nioFilesWrapper.notExists(currentFilePath)){
            throw new ClientException(ClientExceptionMessage.MISSING_FILE,"The file "+fileName+" doesn't exist");
        }

        FileTime lastModifiedDate;
        try {
            lastModifiedDate = nioFilesWrapper.getLastModifiedTime(currentFilePath);
        } catch (IOException e) {
            throw new ServerException("Couldn't read last modified date of the file",e);
        }

        if(file.getLastModified()!=lastModifiedDate.toMillis()){
            throw new ClientException(ClientExceptionMessage.CONCURRENCY_CONFLICT,"The file "+fileName+" was modified since last read.");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_hh_mm_ss_z");

        String backupFileName = currentFilePath.getFileName()+"_"+sdf.format(new Date());

        Path backupFilePath = fileBPath.resolve(backupFileName);

        try {
            nioFilesWrapper.copy(currentFilePath,backupFilePath, StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException e) {
            throw new ServerException("Couldn't backup file " + currentFilePath, e);
        }

        Path filePathToModify = currentFilePath;

        if(file.getName()!=null){
            filePathToModify = fileUPath.resolve(file.getName());

            if(nioFilesWrapper.exists(filePathToModify)){
                try {
                    nioFilesWrapper.delete(backupFilePath);
                } catch (IOException e) {
                    LOGGER.error("Failed to delete backupfile: {}", backupFilePath);
                }
                throw new ClientException(ClientExceptionMessage.FILENAME_CONFLICT,"File " + filePathToModify + " already exists.");
            }

            try {
                nioFilesWrapper.move(currentFilePath,filePathToModify);
            } catch (IOException e) {
                try {
                    nioFilesWrapper.copy(backupFilePath,currentFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                    LOGGER.info("Restored file from {}", backupFilePath);
                } catch (IOException ex) {
                    throw new ServerException("Couldn't restore backup file " + backupFilePath, ex);
                }

                try {
                    nioFilesWrapper.delete(backupFilePath);
                }
                catch (IOException exy) {
                    throw new ServerException("Couldn't delete file: " + currentFilePath.toString(),exy);
                }

                throw new ServerException("Couldn't rename file " + fileName + " to " + file.getName() ,e);
            }
        }

        if(file.getContent()!=null){
            try {
                nioFilesWrapper.copy(new ByteArrayInputStream(file.getContent()),filePathToModify, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                try {
                    nioFilesWrapper.copy(backupFilePath,currentFilePath, StandardCopyOption.COPY_ATTRIBUTES);
                } catch (IOException exy) {
                    throw new ServerException("Couldn't restore backup file " + backupFilePath, exy);
                }

                try {
                    nioFilesWrapper.delete(backupFilePath);
                }
                catch (IOException ex) {
                    throw new ServerException("Couldn't delete file: " + currentFilePath.toString(),ex);
                }

                if(file.getName()!=null) {
                    try {
                        nioFilesWrapper.delete(filePathToModify);
                    }
                    catch (IOException ex) {
                        throw new ServerException("Couldn't delete file: " + filePathToModify.toString(),e);
                    }
                }
                throw new ServerException("Couldn't update content of file " + fileName, e);
            }
        }

        try {
            nioFilesWrapper.delete(backupFilePath);
        } catch (IOException e) {
            LOGGER.error("Failed to delete backupfile: {}", backupFilePath);
        }
    }

    @Override
    public long count() {
        return FCCache.getFolderFileCount();
    }

    @Override
    public List<String> getFilename(String regex) {

        Pattern pattern = Pattern.compile(regex);

        List<String> fileNames = new ArrayList<>();

        for(CharSequence fileName: FNCache.getFileNames()){
            if(pattern.matcher(fileName).matches()){
                fileNames.add(fileName.toString());
            }
        }

        return fileNames;
    }
}
