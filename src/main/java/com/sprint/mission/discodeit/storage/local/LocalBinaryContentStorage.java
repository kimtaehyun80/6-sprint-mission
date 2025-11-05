package com.sprint.mission.discodeit.storage.local;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@ConditionalOnProperty(name = "discodeit.storage.type", havingValue = "local")
@Component
public class LocalBinaryContentStorage implements BinaryContentStorage {

  private final Path root;

  public LocalBinaryContentStorage(
      @Value("${discodeit.storage.local.root-path}") Path root
  ) {
    this.root = root;
    //초기 설정 확인 로깅
    log.info("Local File Storage 초기화: root path = {}", root.toAbsolutePath());
  }

  @PostConstruct
  public void init() {
    if (!Files.exists(root)) {
      try {
        Files.createDirectories(root);
        //디렉토리 생성 성공 로깅
        log.info("로컬 스토리지 디렉토리 생성 완료: {}", root.toAbsolutePath());
      } catch (IOException e) {
        //디렉토리 생성 실패 로깅
        log.error("로컬 스토리지 디렉토리 생성 실패: {}", root.toAbsolutePath(), e);
        throw new RuntimeException("Could not initialize storage folder", e);
      }
    }
  }

  public UUID put(UUID binaryContentId, byte[] bytes) {
    Path filePath = resolvePath(binaryContentId);
    //파일 저장 시도 로싱
    log.debug("파일 저장 시도: binaryContentId={}, filePath={}", binaryContentId, filePath);
    if (Files.exists(filePath)) {
      //파일 중복 로깅
      log.warn("파일 저장 실패: key {}에 해당하는 파일이 이미 존재합니다.", binaryContentId);
      throw new IllegalArgumentException("File with key " + binaryContentId + " already exists");
    }
    try (OutputStream outputStream = Files.newOutputStream(filePath)) {
      outputStream.write(bytes);
      //파일 저장 성공 로깅
      log.info("파일 저장 성공: binaryContentId={}", binaryContentId);
    } catch (IOException e) {
      //파일 I/O 에러 로깅
      log.error("파일 저장 중 IOException 발생: binaryContentId={}", binaryContentId, e);
      throw new RuntimeException("Failed to store file", e);
    }
    return binaryContentId;
  }

  public InputStream get(UUID binaryContentId) {
    Path filePath = resolvePath(binaryContentId);
    //파일 스트림 조회 시도 로깅
    log.debug("파일 스트림 조회 시도: binaryContentId={}, filePath={}", binaryContentId, filePath);

    if (Files.notExists(filePath)) {
      //조회 실패 로깅
      log.warn("파일 조회 실패: key {}에 해당하는 파일이 존재하지 않습니다.", binaryContentId);
      throw new NoSuchElementException("File with key " + binaryContentId + " does not exist");
    }
    try {
      return Files.newInputStream(filePath);
    } catch (IOException e) {
      //파일 I/O 에러 로깅
      log.error("파일 스트림 조회 중 IOException 발생: binaryContentId={}", binaryContentId, e);
      throw new RuntimeException("Failed to retrieve file stream", e);
    }
  }

  //스토리지 실제 파일 삭제 메서드 추가
  public void delete(UUID binaryContentId) {
    Path filePath = resolvePath(binaryContentId);
    //파일 삭제 시도 로깅
    log.info("파일 삭제 시도: binaryContentId={}, filePath={}", binaryContentId, filePath);
    try {
      if (Files.deleteIfExists(filePath)) {
        //삭제 성공 로깅
        log.info("파일 삭제 성공: binaryContentId={}", binaryContentId);
      } else {
        //파일 존재 하지 않을경우 로깅
        log.warn("파일 삭제 요청 처리: key {}에 해당하는 파일이 이미 존재하지 않습니다.", binaryContentId);
      }
    } catch (IOException e) {
      //파일 삭제 I/O 에러 로깅
      log.error("파일 삭제 중 IOException 발생: binaryContentId={}", binaryContentId, e);
      throw new RuntimeException("Failed to delete file", e);
    }
  }

  private Path resolvePath(UUID key) {
    return root.resolve(key.toString());
  }

  @Override
  public ResponseEntity<Resource> download(BinaryContentDto metaData) {
    //다운로드 응답 생성 로깅
    log.debug("다운로드 응답 생성 시작: binaryContentId={}", metaData.id());
    InputStream inputStream = get(metaData.id());
    Resource resource = new InputStreamResource(inputStream);

    return ResponseEntity
        .status(HttpStatus.OK)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + metaData.fileName() + "\"")
        .header(HttpHeaders.CONTENT_TYPE, metaData.contentType())
        .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(metaData.size()))
        .body(resource);
  }
}