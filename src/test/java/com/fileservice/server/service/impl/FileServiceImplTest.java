package com.fileservice.server.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import com.fileservice.server.model.File;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.fileservice.server.exception.ClientException;
import com.fileservice.server.exception.ClientExceptionMessage;
import com.fileservice.server.exception.ServerException;

@RunWith(SpringJUnit4ClassRunner.class)
public class FileServiceImplTest {

    public static final String FILE_CONTENT = "content";
    @InjectMocks
    private FileServiceImpl fileService;

    @Mock
    private NioFilesWrapper nioFilesWrapper;

    private Path basePath = Paths.get("rootFolderPathValue").toAbsolutePath().normalize();
    private Path countAddFile = basePath.resolve("countAddFileNameValue");
    private Path countDeleteFile = basePath.resolve("countDeleteFileNameValue");

    private String toDeleteFileName = "toDeletefileName";
    private Path deletePath = basePath.resolve(toDeleteFileName);


    @Before
    public void setup() {
        ReflectionTestUtils.setField(fileService, "path", "rootFolderPathValue");
        ReflectionTestUtils.setField(fileService, "name", "backupFolderNameValue");
        fileService.init();

    }

    @Test
    public void deleteFile() throws IOException {

        when(nioFilesWrapper.notExists(deletePath)).thenReturn(false);

        fileService.delete(toDeleteFileName);

        verify(nioFilesWrapper).delete(deletePath);

    }

    @Test
    public void deleteMissingDeleteFile() throws IOException {

        when(nioFilesWrapper.notExists(deletePath)).thenReturn(true);

        try {
            fileService.delete(toDeleteFileName);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.MISSING_FILE, e.getClientExceptionMessage());
        }
    }

    @Test
    public void deleteFileFailsAndRollbackSucceeds() throws IOException {
        when(nioFilesWrapper.notExists(deletePath)).thenReturn(false);
        doThrow(new IOException()).when(nioFilesWrapper).delete(deletePath);

        try {
            fileService.delete(toDeleteFileName);
            fail();
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertEquals("Couldn't delete file: " + deletePath.toString(), e.getMessage());
        }
    }

    @Test
    public void createFileMissingContent() {
        File file = new File();
        file.setContent(null);

        try {
            fileService.create(file);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.MISSING_CONTENT, e.getClientExceptionMessage());
        }
    }

    @Test
    public void createFileEmptyByteArray() {
        File file = new File();
        file.setContent(new byte[0]);

        try {
            fileService.create(file);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.MISSING_CONTENT, e.getClientExceptionMessage());
        }
    }

    @Test
    public void createFileInvalidNameTooShort() {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("");

        try {
            fileService.create(file);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_FILENAME, e.getClientExceptionMessage());
        }
    }

    @Test
    public void createFileInvalidNameTooLong() {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName(new String(new char[65]).replace('\0', 'a'));

        try {
            fileService.create(file);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_FILENAME, e.getClientExceptionMessage());
        }
    }

    @Test
    public void createFileInvalidNameInvalidCharacters() {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("!!");

        try {
            fileService.create(file);
            fail();
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_FILENAME, e.getClientExceptionMessage());
        }
    }

    @Test
    public void createNewFile() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("file.jpg");

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(false);

        fileService.create(file);


        verifyBackupFileNotCreated(basePath.resolve(file.getName()));
        verifyFileCreated(basePath.resolve(file.getName()));
        verifyBackupFileNotDeleted();

    }

    @Test
    public void createNewFileWithMaximumLengthAndExtension() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName(new String(new char[64]).replace('\0', 'a') + ".jpg");

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(false);

        fileService.create(file);

        verifyBackupFileNotCreated(basePath.resolve(file.getName()));
        verifyFileCreated(basePath.resolve(file.getName()));
        verifyBackupFileNotDeleted();
    }


    @Test
    public void createNewFileForExistingFile() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("file");

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(true);

        fileService.create(file);

        verifyBackupIsDone(file.getName());
        verifyFileCreated(basePath.resolve(file.getName()));
        verifyBackupFileDeleted();

    }

    @Test
    public void createFileFailureExistingFile() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("file");

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(true);
        doThrow(new IOException()).when(nioFilesWrapper).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(file.getName())),
                eq(StandardCopyOption.REPLACE_EXISTING));

        try {
            fileService.create(file);
            fail();
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertEquals("Couldn't create file", e.getMessage());
        }

        verifyBackupIsDone(file);
        verifyFileCreated(basePath.resolve(file.getName()));
        verifyBackupRestored(file.getName());
        verifyBackupFileDeleted();
    }

    @Test
    public void createFileFailureExistingFileFailedRollback() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("file");

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(true);
        doThrow(new IOException()).when(nioFilesWrapper).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(file.getName())),
                eq(StandardCopyOption.REPLACE_EXISTING));
        doThrow(new IOException()).when(nioFilesWrapper).copy(any(Path.class), eq(basePath.resolve(file.getName())), eq(StandardCopyOption.COPY_ATTRIBUTES));

        try {
            fileService.create(file);
            fail();
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertTrue(e.getMessage().contains("Couldn't restore backup file"));
        }

        verifyFileCreated(basePath.resolve(file.getName()));
        verifyBackupRestored(file.getName());
        verifyBackupFileNotDeleted();
    }

    @Test
    public void updateFileName() throws IOException {
        File file = new File();
        file.setName("newFileName");
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(false);
        when(nioFilesWrapper.notExists(basePath.resolve("newFileName"))).thenReturn(false);
        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);

        fileService.update("fileName", file);

        verifyBackupIsDone("fileName");
        verifyFileRenamed("fileName", file);
        verifyContentNotOverwritten("fileName", file);
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileContent() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);

        fileService.update("fileName", file);

        verifyBackupIsDone("fileName");
        verifyFileNotRenamed("fileName");
        verifyContentOverwritten("fileName", file);
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileNameAndContent() throws IOException {
        File file = new File();
        file.setName("newFileName");
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(false);
        when(nioFilesWrapper.notExists(basePath.resolve("newFileName"))).thenReturn(false);
        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);

        fileService.update("fileName", file);

        verifyBackupIsDone("fileName");
        verifyFileRenamed("fileName", file);
        verifyContentOverwritten("fileName", file);
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileMissingFile() throws IOException {
        File file = new File();
        file.setName("newFileName");
        file.setLastModified(new Date().getTime());

        when(nioFilesWrapper.notExists(basePath.resolve("fileName"))).thenReturn(true);

        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.MISSING_FILE, e.getClientExceptionMessage());
        }

        verifyFileNotRenamed("fileName");
        verifyContentNotOverwritten("fileName", file);
    }

    @Test
    public void updateFileNameFailure() throws IOException {
        File file = new File();
        file.setName("newFileName");
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(false);
        when(nioFilesWrapper.notExists(basePath.resolve("newFileName"))).thenReturn(false);
        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);
        doThrow(new IOException()).when(nioFilesWrapper).move(basePath.resolve("fileName"), basePath.resolve("newFileName"));


        try {
            fileService.update("fileName", file);
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertTrue(e.getMessage().contains("Couldn't rename file"));
        }

        verifyBackupIsDone("fileName");
        verifyFileRenamed("fileName", file);
        verifyContentNotOverwritten("fileName", file);
        verifyBackupRestored("fileName");
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileNameConflict() throws IOException {
        File file = new File();
        file.setName("newFileName");
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.exists(basePath.resolve(file.getName()))).thenReturn(true);
        when(nioFilesWrapper.notExists(basePath.resolve("newFileName"))).thenReturn(false);
        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);
        doThrow(new IOException()).when(nioFilesWrapper).move(basePath.resolve("fileName"), basePath.resolve("newFileName"));


        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.FILENAME_CONFLICT, e.getClientExceptionMessage());
        }

        verifyBackupIsDone("fileName");
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileContentOnlyFailure() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);
        doThrow(new IOException()).when(nioFilesWrapper).copy(
                any(ByteArrayInputStream.class),
                eq(basePath.resolve("fileName")),
                eq(StandardCopyOption.REPLACE_EXISTING)
        );


        try {
            fileService.update("fileName", file);
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertTrue(e.getMessage().contains("Couldn't update content of file"));
        }

        verifyBackupIsDone("fileName");
        verifyFileNotRenamed("fileName");
        verifyContentOverwritten("fileName", file);
        verifyBackupRestored("fileName");
        verifyBackupFileDeleted();
    }

    @Test
    public void updateFileContentAndNameContentFailure() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("newFileName");
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified());

        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);
        doThrow(new IOException()).when(nioFilesWrapper).copy(
                any(ByteArrayInputStream.class),
                eq(basePath.resolve(file.getName())),
                eq(StandardCopyOption.REPLACE_EXISTING)
        );


        try {
            fileService.update("fileName", file);
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
            assertTrue(e.getMessage().contains("Couldn't update content of file"));
        }

        verifyBackupIsDone("fileName");
        verifyFileRenamed("fileName", file);
        verifyContentOverwritten("fileName", file);
        verifyBackupRestored("fileName");
        verifyRenamedFileDeleted(file.getName());
    }

    @Test
    public void updateFileConcurrencyIssue() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        FileTime fileTime = FileTime.fromMillis(file.getLastModified()+1000);

        when(nioFilesWrapper.getLastModifiedTime(basePath.resolve("fileName"))).thenReturn(fileTime);

        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.CONCURRENCY_CONFLICT, e.getClientExceptionMessage());
        }

        verifyFileNotRenamed("fileName");
        verifyContentNotOverwritten("fileName", file);
    }

    @Test
    public void updateFileInvalidName() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setName("!!");
        file.setLastModified(new Date().getTime());

        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_FILENAME, e.getClientExceptionMessage());
        }

        verifyFileNotRenamed("fileName");
        verifyContentNotOverwritten("fileName", file);
    }

    @Test
    public void updateFileExistingFilenameNotGiven() {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        try {
            fileService.update("", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_FILENAME, e.getClientExceptionMessage());
        }
    }

    @Test
    public void updateFileChangeNotGiven() {
        File file = new File();

        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_REQUEST, e.getClientExceptionMessage());
        }
    }

    @Test
    public void updateFileMissingLastModified() {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());

        try {
            fileService.update("fileName", file);
        } catch (ClientException e) {
            assertEquals(ClientExceptionMessage.INVALID_REQUEST, e.getClientExceptionMessage());
        }
    }

    @Test
    public void updateFileUnableToGetLastModified() throws IOException {
        File file = new File();
        file.setContent(FILE_CONTENT.getBytes());
        file.setLastModified(new Date().getTime());

        doThrow(new IOException()).when(nioFilesWrapper).getLastModifiedTime(basePath.resolve("fileName"));

        try {
            fileService.update("fileName", file);
        } catch (ServerException e) {
            assertEquals(ClientExceptionMessage.GENERAL_ERROR, e.getClientExceptionMessage());
        }
    }





    private void verifyBackupIsDone(String fileName) throws IOException {
        verify(nioFilesWrapper).copy(eq(basePath.resolve(fileName)), any(Path.class),
                eq(StandardCopyOption.COPY_ATTRIBUTES));
    }

    private void verifyBackupRestored(String fileName) throws IOException {
        verify(nioFilesWrapper).copy(any(Path.class), eq(basePath.resolve(fileName)),
                eq(StandardCopyOption.COPY_ATTRIBUTES));
    }

    private void verifyContentOverwritten(String fileName, File file) throws IOException {
        if (file.getName() == null) {
            verify(nioFilesWrapper).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(fileName)), eq(StandardCopyOption.REPLACE_EXISTING));
        } else {
            verify(nioFilesWrapper).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(file.getName())), eq(StandardCopyOption.REPLACE_EXISTING));
        }
    }

    private void verifyContentNotOverwritten(String fileName, File file) throws IOException {
        if (file.getName() == null) {
            verify(nioFilesWrapper, never()).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(fileName)), eq(StandardCopyOption.REPLACE_EXISTING));
        } else {
            verify(nioFilesWrapper, never()).copy(any(ByteArrayInputStream.class), eq(basePath.resolve(file.getName())), eq(StandardCopyOption.REPLACE_EXISTING));
        }
    }

    private void verifyFileRenamed(String fileName, File file) throws IOException {
        verify(nioFilesWrapper).move(basePath.resolve(fileName), basePath.resolve(file.getName()));
    }

    private void verifyFileNotRenamed(String fileName) throws IOException {
        verify(nioFilesWrapper, never()).move(eq(basePath.resolve(fileName)), any(Path.class));
    }

    private void verifyBackupFileDeleted() throws IOException {
        verify(nioFilesWrapper).delete(any(Path.class));
    }

    private void verifyBackupIsDone(File file) throws IOException {
        verifyBackupIsDone(file.getName());
    }

    private void verifyRenamedFileDeleted(String name) throws IOException {
        verify(nioFilesWrapper).delete(basePath.resolve(name));
    }

    private void verifyFileCreated(Path file) throws IOException {
        verify(nioFilesWrapper).copy(any(ByteArrayInputStream.class), eq(file),
                eq(StandardCopyOption.REPLACE_EXISTING));
    }
    private void verifyBackupFileNotDeleted() throws IOException {
        verify(nioFilesWrapper, never()).delete(any(Path.class));
    }

    private void verifyBackupFileNotCreated(Path file) throws IOException {
        verify(nioFilesWrapper, never()).copy(eq(file), any(Path.class),
                eq(StandardCopyOption.COPY_ATTRIBUTES));
    }
}
