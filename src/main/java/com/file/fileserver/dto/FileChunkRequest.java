package com.file.fileserver.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileChunkRequest
{
    private String type; // 항상 "request"
    private String filename; // 다운로드할 파일명 (예: "test_50MB.bin")
    private int seq; // 받고 싶은 chunk 번호 (0부터 시작, 매번 +1)
    private String data; // 다운로드 요청이므로 항상 빈 값 (null)
}
