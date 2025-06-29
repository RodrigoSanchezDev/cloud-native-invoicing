package com.sanchezdev.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

@Service
@RequiredArgsConstructor
public class FileStorageService {
    @Value("${efs.base.dir}")
    private String efsBaseDir;
    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;
    private final S3Client s3Client;

    public void saveFile(String client, String date, MultipartFile file) {
        // 1. Save on EFS
        String path = efsBaseDir + "/" + client + "/" + date;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, file.getOriginalFilename());
        try {
            file.transferTo(dest);
        } catch (Exception e) {
            throw new RuntimeException("Error guardando archivo en EFS", e);
        }
        // 2. Upload to S3
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(client + "/" + date + "/" + file.getOriginalFilename())
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putReq, RequestBody.fromFile(dest));
    }

    public byte[] downloadFile(String key) {
        // Download always from S3
        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        ResponseBytes<GetObjectResponse> resp = s3Client.getObjectAsBytes(getReq);
        return resp.asByteArray();
    }

    public void createAndUploadPdf(String client, String date, Map<String, Object> invoiceData) {
        // Generate PDF
        String path = efsBaseDir + "/" + client + "/" + date;
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();
        File pdfFile = new File(dir, "invoice.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.setLeading(14.5f);
            contentStream.newLineAtOffset(25, 725);
            for (Map.Entry<String, Object> entry : invoiceData.entrySet()) {
                contentStream.showText(entry.getKey() + ": " + entry.getValue());
                contentStream.newLine();
            }
            contentStream.endText();
            contentStream.close();
            document.save(pdfFile);
        } catch (IOException e) {
            throw new RuntimeException("Error generating PDF", e);
        }

        // Upload to S3
        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(client + "/" + date + "/invoice.pdf")
                .contentType("application/pdf")
                .build();
        s3Client.putObject(putReq, RequestBody.fromFile(pdfFile));
    }
}
