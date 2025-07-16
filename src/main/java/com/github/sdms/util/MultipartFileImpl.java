package com.github.sdms.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MultipartFileImpl implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public MultipartFileImpl(String name, String originalFilename, String contentType, InputStream inputStream) throws IOException {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int nRead;
        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        buffer.flush();
        this.content = buffer.toByteArray();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content;
    }

    @Override
    public InputStream getInputStream() {
        return new java.io.ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
        java.nio.file.Files.write(dest.toPath(), content);
    }
}
