package com.file.fileserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class FileChunkResponse
{
    private String type; // "response" 또는 "error"
    private String filename; // 대상 파일명
    private int    seq; // 현재 chunk 번호 (종료시 -1)
    private String data; // 청크일 때=Base64, 종료일 때=체크섬, 에러일 때=메시지
    private long   sentBytes; // 지금까지 전송된 누적 바이트수 (진행율 표시용)

    // 청크 응답 생성 (data에 Base64 인코딩된 파일 조각)
    public static FileChunkResponse ofChunk(String filename, int seq, String base64Data, long sentBytes)
    {
        FileChunkResponse res = new FileChunkResponse();
        res.type      = "response";
        res.filename  = filename;
        res.seq       = seq;
        res.data      = base64Data;
        res.sentBytes = sentBytes;
        return res;
    }
    // 종료 응답 생성 (data에 체크섬을 넣음 → 별도 API 없이 무결성 검증 가능)
    public static FileChunkResponse ofEnd(String filename, long totalFileSize, String checksum)
    {
        FileChunkResponse res = new FileChunkResponse();
        res.type      = "response";
        res.filename  = filename;
        res.seq       = -1;
        res.data      = checksum;       // ← checksum을 data에 재사용
        res.sentBytes = totalFileSize;
        return res;
    }
    // 에러 응답 생성
    public static FileChunkResponse ofError(String message)
    {
        FileChunkResponse res = new FileChunkResponse();
        res.type      = "error";
        res.filename  = "";
        res.seq       = -1;
        res.data      = message;
        res.sentBytes = 0;
        return res;
    }
}