package com.file.fileserver.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class FileDownloadService
{
    // 서버에 파일을 놓는 디렉토리 (프로젝트 루트 기준)
    private static final String FILE_DIR    = "./server-files/";
    // 한 번에 전송할 chunk 크기 = 500KB
    // 1024 * 500 = 512,000 bytes
    private static final int    CHUNK_SIZE  = 500 * 1024;   // 500 KB

    // ─── 파일명 유효성 검사 ──────────────────────────────────────
    // 경로 순회 공격(Path Traversal)을 방지
    //   "../../../etc/passwd" 같은 파일명이 오면 막음
    public boolean isValidFilename(String filename)
    {
        return filename != null
                && !filename.isBlank()
                && !filename.contains("..")
                && !filename.contains("/")
                && !filename.contains("\\");
    }
    // ─── 파일 존재 확인 ───────────────────────────────────────
    public boolean fileExists(String filename)
    {
        Path path = resolveFilePath(filename);
        // 파일이 존재하고, 디렉토리가 아닌 일반 파일인지 확인
        return Files.exists(path) && Files.isRegularFile(path);
    }
    // ─── 파일 크기 조회 ───────────────────────────────────────
    public long getFileSize(String filename) throws IOException
    {
        return Files.size(resolveFilePath(filename));
    }
    // ─── 다운로드 완료 여부 확인 ──────────────────────────────────
    // 클라이언트가 요청한 seq의 offset이 파일 크기 이상이면 → 더 전송할 데이터 없음
    // 예: 파일 1000KB, chunk 500KB
    //   seq=0 → offset=0    (0 < 1000KB) → 아직 남음
    //   seq=1 → offset=500  (500 < 1000KB) → 아직 남음
    //   seq=2 → offset=1000 (1000 >= 1000KB) → 완료! → seq=-1 응답
    public boolean isDownloadComplete(int seq, long fileSize)
    {
        long offset = (long) seq * CHUNK_SIZE;
        return offset >= fileSize;
    }
    // ─── chunk 읽기 + Base64 인코딩 ───────────────────────────────
    // seq 번호를 받아서 해당 위치의 파일 조각을 읽고 Base64로 변환
    public String readChunkAsBase64(String filename, int seq) throws IOException
    {
        Path filePath   = resolveFilePath(filename);
        long fileSize   = Files.size(filePath);
        long offset     = (long) seq * CHUNK_SIZE;
        int  bytesToRead = (int) Math.min(CHUNK_SIZE, fileSize - offset);        // 마지막 chunk는 CHUNK_SIZE보다 작을 수 있으므로 Math.min으로 조정
        byte[] chunk    = new byte[bytesToRead];
        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r"))        // RandomAccessFile → 원하는 위치로 바로 이동(seek)하여 읽기 가능
        {
            raf.seek(offset);
            raf.readFully(chunk);
        }
        // Base64 인코딩: 바이너리 → 텍스트로 변환 (JSON으로 전송 가능하도록)
        // Base64는 바이트를 약 1.33배(4/3) 크게 만든다
        return Base64.getEncoder().encodeToString(chunk);
    }
    // ─── 누적 전송 바이트 계산 ─────────────────────────────────
    public long calculateSentBytes(int seq, long fileSize)
    {
        long offset          = (long) seq * CHUNK_SIZE;
        int  bytesInThisChunk = (int) Math.min(CHUNK_SIZE, fileSize - offset); //둘중 더 작은 값으로 마지막 값을 계산
        return offset + bytesInThisChunk;
    }
    // ─── SHA-256 체크섬 계산 : 내용물 전체를 대조해 64글자의 문자열을 만들어내서 검사
    public String computeSHA256(String filename) throws Exception
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream is = Files.newInputStream(resolveFilePath(filename)))
        {
            byte[] buffer = new byte[8192]; // 8KB씩 읽기
            int len;
            while ((len = is.read(buffer)) != -1)
            {
                md.update(buffer, 0, len); // 해시에 추가
            }
        }
        // 해시 바이트 → 16진문자열로 변환
        return HexFormat.of().formatHex(md.digest());
    }
    // ─── 내부 헬퍼 ────────────────────────────────────────────
    private Path resolveFilePath(String filename)
    {
        return Paths.get(FILE_DIR, filename);
    }
}
