package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.BinaryContentApi;
import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/binaryContents")
public class BinaryContentController implements BinaryContentApi {

  private final BinaryContentService binaryContentService;
  private final BinaryContentStorage binaryContentStorage;

  @GetMapping(path = "{binaryContentId}")
  public ResponseEntity<BinaryContentDto> find(
      @PathVariable("binaryContentId") UUID binaryContentId) {
    //요청 수신 로깅
    log.info("단건 조회 요청 수신: binaryContentId={}", binaryContentId);
    BinaryContentDto binaryContent = binaryContentService.find(binaryContentId);
    //조회 성공 로깅
    log.info("조회 성공: binaryContentId={}", binaryContentId);
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(binaryContent);
  }

  @GetMapping
  public ResponseEntity<List<BinaryContentDto>> findAllByIdIn(
      @RequestParam("binaryContentIds") List<UUID> binaryContentIds) {
    //요청 수신 로깅
    log.info("다수 조회 요청 수신: count={}", binaryContentIds.size());
    List<BinaryContentDto> binaryContents = binaryContentService.findAllByIdIn(binaryContentIds);
    //조회 성공로깅
    log.info("다수 조회 완료: foundCount={}", binaryContents.size());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(binaryContents);
  }

  @GetMapping(path = "{binaryContentId}/download")
  public ResponseEntity<?> download(
      @PathVariable("binaryContentId") UUID binaryContentId) {
    //파일 다운로드 요청 수신 로깅
    log.info("다운로드 요청 수신: binaryContentId={}", binaryContentId);
    BinaryContentDto binaryContentDto = binaryContentService.find(binaryContentId);
    //다운로드 시작 로깅
    log.info("다운로드 요청: binaryContentId={}, fileName={}",
        binaryContentId, binaryContentDto.fileName());
    ResponseEntity<?> response = binaryContentStorage.download(binaryContentDto);
    //다운로드 응답 반환 로깅
    log.info("다운로드 응답 반환: binaryContentId={}, status={}",
        binaryContentId, response.getStatusCode());

    return response;
  }
}