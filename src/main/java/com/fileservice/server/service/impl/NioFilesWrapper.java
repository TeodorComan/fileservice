package com.fileservice.server.service.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;

import org.springframework.stereotype.Service;

@Service
public class NioFilesWrapper {

    public boolean notExists(Path path){
        return Files.notExists(path);
    }

    public boolean isDirectory(Path path){
        return Files.isDirectory(path);
    }

    public byte[] readAllBytes(Path path) throws IOException {
        return Files.readAllBytes(path);
    }

    public void delete(Path path) throws IOException {
        Files.delete(path);
    }

    public void copy(ByteArrayInputStream byteArrayInputStream, Path filePath, CopyOption... copyOption) throws IOException {
        Files.copy(byteArrayInputStream,filePath, copyOption);
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public void move(Path source, Path target) throws IOException {
        Files.move(source,target);
    }

    public FileTime getLastModifiedTime(Path path) throws IOException {
        return Files.getLastModifiedTime(path);
    }

    public void createFile(Path path) throws IOException {
        Files.createFile(path);
    }

    public void write(Path filePath, byte[] bytes, StandardOpenOption option) throws IOException {
        Files.write(filePath,bytes,option);
    }

    public DirectoryStream<Path> newDirectoryStream(Path path) throws IOException {
        return Files.newDirectoryStream(path);
    }

    public void copy(Path source, Path target, StandardCopyOption copyOption) throws IOException {
        Files.copy(source,target,copyOption);
    }
}
