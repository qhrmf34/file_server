package com.file.fileserver.controller;

import com.file.fileserver.dto.FileChunkRequest;
import com.file.fileserver.dto.FileChunkResponse;
import com.file.fileserver.service.FileDownloadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileDownloadController
{
    private final FileDownloadService service;

    // ─── POST /api/download ───────────────────────────────────

    @PostMapping("/download")
    public ResponseEntity<FileChunkResponse> downloadChunk(@RequestBody FileChunkRequest request)
    {
        //type 검증
        if (!"request".equals(request.getType()))
        {
            return ResponseEntity.badRequest()
                    .body(FileChunkResponse.ofError("Invalid request type: " + request.getType()));
        }
        String filename = request.getFilename();
        //파일명 검증
        if (!service.isValidFilename(filename))
        {
            return ResponseEntity.badRequest()
                    .body(FileChunkResponse.ofError("Invalid filename"));
        }
        //파일 존재 확인
        if (!service.fileExists(filename))
        {
            return ResponseEntity.notFound().build();
        }
        try
        {
            long fileSize = service.getFileSize(filename);
            int  seq      = request.getSeq();
            if (service.isDownloadComplete(seq, fileSize))            //종료 여부 확인
            {
                String checksum = service.computeSHA256(filename);   // ← 체크섬 계산
                return ResponseEntity.ok(FileChunkResponse.ofEnd(filename, fileSize, checksum));
                //seq=-1응답
            }
            String base64Data = service.readChunkAsBase64(filename, seq);            // chunk 읽기 + 응답
            long   sentBytes  = service.calculateSentBytes(seq, fileSize);

            System.out.printf("[SERVER] seq=%-4d | sent=%,12d / %,12d%n", seq, sentBytes, fileSize);

            return ResponseEntity.ok(FileChunkResponse.ofChunk(filename, seq, base64Data, sentBytes));
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(FileChunkResponse.ofError("Server error: " + e.getMessage()));
        }
    }
}