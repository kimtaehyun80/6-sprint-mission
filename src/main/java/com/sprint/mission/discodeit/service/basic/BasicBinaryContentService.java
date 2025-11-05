package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.BinaryContentDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.mapper.BinaryContentMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.service.BinaryContentService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicBinaryContentService implements BinaryContentService {

  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentMapper binaryContentMapper;
  private final BinaryContentStorage binaryContentStorage;

  @Transactional
  @Override
  public BinaryContentDto create(BinaryContentCreateRequest request) {
    //요청 수신 로깅
    log.info("생성 서비스 시작: fileName={}, contentType={}",
        request.fileName(), request.contentType());
    String fileName = request.fileName();
    byte[] bytes = request.bytes();
    String contentType = request.contentType();
    BinaryContent binaryContent = new BinaryContent(
        fileName,
        (long) bytes.length,
        contentType
    );
    binaryContentRepository.save(binaryContent);
    binaryContentStorage.put(binaryContent.getId(), bytes);
    //생성,스토리지 저장 성공 로깅
    log.info("생성 성공 및 스토리지 저장 완료: binaryContentId={}", binaryContent.getId());
    return binaryContentMapper.toDto(binaryContent);
  }

  @Override
  public BinaryContentDto find(UUID binaryContentId) {
    //단건 조회 요칭 수신 로깅
    log.debug("단건 조회 서비스 요청: binaryContentId={}", binaryContentId);
    return binaryContentRepository.findById(binaryContentId)
        .map(binaryContentMapper::toDto)
        .orElseThrow(() -> {
          //조회 실패 로깅
          log.warn("바이너리 콘텐츠 조회 실패: id {}를 찾을 수 없습니다.", binaryContentId);
          return new NoSuchElementException(
              "BinaryContent with id " + binaryContentId + " not found");
        });
  }

  @Override
  public List<BinaryContentDto> findAllByIdIn(List<UUID> binaryContentIds) {
    //다수조회 요청 수신 로깅
    log.debug("다수 조회 서비스 요청: count={}", binaryContentIds.size());
    List<BinaryContentDto> contents = binaryContentRepository.findAllById(binaryContentIds).stream()
        .map(binaryContentMapper::toDto)
        .toList();
    //다수조회 완료 로싱
    log.debug("다수 조회 완료: foundCount={}", contents.size());
    return contents;
  }

  @Transactional
  @Override
  public void delete(UUID binaryContentId) {
    //삭제 요청 수신 로깅
    log.info("삭제 서비스 시작: binaryContentId={}", binaryContentId);
    if (!binaryContentRepository.existsById(binaryContentId)) {
      //삭제 대상 조회 실패 로깅
      log.warn("바이너리 콘텐츠 삭제 실패: id {}를 찾을 수 없습니다.", binaryContentId);
      throw new NoSuchElementException("BinaryContent with id " + binaryContentId + " not found");
    }
    binaryContentStorage.delete(binaryContentId);  //스토리지의 실제 파일 삭제
    binaryContentRepository.deleteById(binaryContentId); //DB의 메타데이터 삭제
    //삭제 성공 로깅
    log.info("바이너리 콘텐츠 삭제 성공: binaryContentId={}", binaryContentId);
  }
}