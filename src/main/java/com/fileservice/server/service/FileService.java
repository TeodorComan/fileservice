package com.fileservice.server.service;

import java.util.List;
import java.util.Optional;

import com.fileservice.server.model.File;
import com.fileservice.server.service.impl.Option;

public interface FileService {

    File get(String name, Option... option);

    void deleteFile(String name);

    void createFile(File file);

    void update (String fileName, File file);

    long count();

    List<String> getFilename(String regex);
}
