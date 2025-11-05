package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.MessageDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.response.PageResponse;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.mapper.MessageMapper;
import com.sprint.mission.discodeit.mapper.PageResponseMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.MessageService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicMessageService implements MessageService {

  private final MessageRepository messageRepository;
  //
  private final ChannelRepository channelRepository;
  private final UserRepository userRepository;
  private final MessageMapper messageMapper;
  private final BinaryContentStorage binaryContentStorage;
  private final BinaryContentRepository binaryContentRepository;
  private final PageResponseMapper pageResponseMapper;

  @Transactional
  @Override
  public MessageDto create(MessageCreateRequest messageCreateRequest,
      List<BinaryContentCreateRequest> binaryContentCreateRequests) {

    //메세지,파일첨부 요청 수신 로깅
    log.info("메시지 생성 서비스 시작: channelId={}, authorId={}, attachmentsCount={}",
        messageCreateRequest.channelId(), messageCreateRequest.authorId(),
        binaryContentCreateRequests.size());

    UUID channelId = messageCreateRequest.channelId();
    UUID authorId = messageCreateRequest.authorId();

    Channel channel = channelRepository.findById(channelId)
        .orElseThrow(
            () -> {
              //채널 조회 실패 로깅
              log.warn("메시지 생성 실패: Channel with id {} does not exist", channelId);
              return new NoSuchElementException("Channel with id " + channelId + " does not exist");
            });
    User author = userRepository.findById(authorId)
        .orElseThrow(
            () -> {
              //유저 조회 실패 로깅
              log.warn("메시지 생성 실패: Author with id {} does not exist", authorId);
              return new NoSuchElementException("Author with id " + authorId + " does not exist");
            }
        );

    //첨부 파일 처리 시작 로깅
    log.info("첨부 파일 {}개 처리 시작.", binaryContentCreateRequests.size());
    List<BinaryContent> attachments = binaryContentCreateRequests.stream()
        .map(attachmentRequest -> {
          String fileName = attachmentRequest.fileName();
          String contentType = attachmentRequest.contentType();
          byte[] bytes = attachmentRequest.bytes();

          //파일 메타데이터 저장, 스토리지 업로드 로깅
          log.debug("첨부 파일 처리: fileName={}, size={} bytes", fileName, bytes.length);

          BinaryContent binaryContent = new BinaryContent(fileName, (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          return binaryContent;
        })
        .toList();
    //첨부 파일 처리완료 로깅
    log.info("첨부 파일 처리 완료. Message에 연결.");

    String content = messageCreateRequest.content();
    Message message = new Message(
        content,
        channel,
        author,
        attachments
    );
    messageRepository.save(message);
    //메시지 생성 성공 로깅
    log.info("메시지 생성 성공: messageId={}, channelId={}", message.getId(), channelId);
    return messageMapper.toDto(message);
  }

  @Transactional(readOnly = true)
  @Override
  public MessageDto find(UUID messageId) {
    //단건 조회 요청 로깅
    log.debug("메시지 단건 조회 서비스 요청: messageId={}", messageId);
    return messageRepository.findById(messageId)
        .map(messageMapper::toDto)
        .orElseThrow(
            () -> {
              //조회 실패 로깅
              log.warn("메시지 조회 실패: Message with id {} not found", messageId);
              return new NoSuchElementException("Message with id " + messageId + " not found");
            });
  }

  @Transactional(readOnly = true)
  @Override
  public PageResponse<MessageDto> findAllByChannelId(UUID channelId, Instant createAt,
      Pageable pageable) {
    //메세지 목록 페이지 조회 요청 로깅
    log.info("메시지 목록 페이지 조회 서비스 요청: channelId={}, cursor={}", channelId, createAt);

    Slice<MessageDto> slice = messageRepository.findAllByChannelIdWithAuthor(channelId,
            Optional.ofNullable(createAt).orElse(Instant.now()),
            pageable)
        .map(messageMapper::toDto);

    Instant nextCursor = null;
    if (!slice.getContent().isEmpty()) {
      nextCursor = slice.getContent().get(slice.getContent().size() - 1)
          .createdAt();
    }

    //메세지 목록 조회 완료 로깅
    log.info("메시지 목록 조회 완료: channelId={}, count={}", channelId, slice.getNumberOfElements());
    return pageResponseMapper.fromSlice(slice, nextCursor);
  }

  @Transactional
  @Override
  public MessageDto update(UUID messageId, MessageUpdateRequest request) {
    //메서드 업데이트 요청 로깅
    log.info("메시지 업데이트 서비스 시작: messageId={}", messageId);

    String newContent = request.newContent();
    Message message = messageRepository.findById(messageId)
        .orElseThrow(
            () -> {
              //메세지 조회 실패 로깅
              log.warn("메시지 업데이트 실패: Message with id {} not found", messageId);
              return new NoSuchElementException("Message with id " + messageId + " not found");
            });
    //메세지 업데이트 내용 로깅
    log.debug("메시지 내용 업데이트: oldContent='{}', newContent='{}'",
        message.getContent(), newContent);

    message.update(newContent);

    //메세지 업데이트 성공 로깅
    log.info("메시지 업데이트 성공: messageId={}", messageId);
    return messageMapper.toDto(message);
  }

  @Transactional
  @Override
  public void delete(UUID messageId) {
    //메서드 삭제 요청 로깅
    log.info("메시지 삭제 서비스 시작: messageId={}", messageId);

    if (!messageRepository.existsById(messageId)) {
      //삭제 대상 조회 실패 로깅
      log.warn("메시지 삭제 실패: Message with id {} not found", messageId);
      throw new NoSuchElementException("Message with id " + messageId + " not found");
    }
    messageRepository.deleteById(messageId);
    //삭제 성공 로깅
    log.info("메시지 삭제 성공: messageId={}", messageId);
  }
}