package com.sprint.mission.discodeit.service.basic;

import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentStorage;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class BasicUserService implements UserService {

  private final UserRepository userRepository;
  private final UserStatusRepository userStatusRepository;
  private final UserMapper userMapper;
  private final BinaryContentRepository binaryContentRepository;
  private final BinaryContentStorage binaryContentStorage;

  @Transactional
  @Override
  public UserDto create(UserCreateRequest userCreateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    //생성 요청 로깅
    log.info("유저 생성 서비스 시작: email={}", userCreateRequest.email());
    String username = userCreateRequest.username();
    String email = userCreateRequest.email();

    if (userRepository.existsByEmail(email)) {
      //email 유효성 검사 실패 로깅
      log.warn("유저 생성 실패: 이메일 {}이(가) 이미 존재합니다.", email);
      throw new IllegalArgumentException("User with email " + email + " already exists");
    }
    if (userRepository.existsByUsername(username)) {
      //username 유효성 검사 실패 로깅
      log.warn("유저 생성 실패: 유저네임 {}이(가) 이미 존재합니다.", username);
      throw new IllegalArgumentException("User with username " + username + " already exists");
    }

    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(profileRequest -> {
          //프로필 파일 처리 시작 로깅
          log.debug("프로필 파일 처리 시작: fileName={}", profileRequest.fileName());
          String fileName = profileRequest.fileName();
          String contentType = profileRequest.contentType();
          byte[] bytes = profileRequest.bytes();
          BinaryContent binaryContent = new BinaryContent(fileName, (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          //프로필 파일처리 완료 로깅
          log.debug("프로필 파일 처리 완료. binaryContentId={}", binaryContent.getId());
          return binaryContent;
        })
        .orElse(null);
    String password = userCreateRequest.password();

    User user = new User(username, email, password, nullableProfile);
    Instant now = Instant.now();
    UserStatus userStatus = new UserStatus(user, now);

    userRepository.save(user);
    //유저생성 성공 로깅
    log.info("유저 생성 성공: userId={}", user.getId());
    return userMapper.toDto(user);
  }

  @Override
  public UserDto find(UUID userId) {
    //조회 요청 로깅
    log.info("유저 조회 서비스 요청: userId={}", userId);
    return userRepository.findById(userId)
        .map(userMapper::toDto)
        .orElseThrow(() -> {
          //조회 실패 로깅
          log.warn("유저 조회 실패: id {}에 해당하는 유저를 찾을 수 없습니다.", userId);
          return new NoSuchElementException("User with id " + userId + " not found");
        });
  }

  @Override
  public List<UserDto> findAll() {
    //전체 조회 요청 로깅
    log.debug("모든 유저 목록 조회 서비스 요청.");
    List<UserDto> users = userRepository.findAllWithProfileAndStatus()
        .stream()
        .map(userMapper::toDto)
        .toList();

    //전체 조회완료 로깅
    log.info("모든 유저 목록 조회 완료. 총 {}명", users.size());
    return users;
  }

  @Transactional
  @Override
  public UserDto update(UUID userId, UserUpdateRequest userUpdateRequest,
      Optional<BinaryContentCreateRequest> optionalProfileCreateRequest) {
    //업데이트 요청 로깅
    log.info("유저 업데이트 서비스 시작: userId={}", userId);
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          //유저 조회 실패 로깅
          log.warn("유저 업데이트 실패: id {}에 해당하는 유저를 찾을 수 없습니다.", userId);
          return new NoSuchElementException("User with id " + userId + " not found");
        });
    String newUsername = userUpdateRequest.newUsername();
    String newEmail = userUpdateRequest.newEmail();
    if (userRepository.existsByEmail(newEmail)) {
      //email 유효성 검사 실패 로깅
      log.warn("유저 업데이트 실패: 이메일 {}이(가) 이미 존재합니다.", newEmail);
      throw new IllegalArgumentException("User with email " + newEmail + " already exists");
    }
    if (userRepository.existsByUsername(newUsername)) {
      //username 유효성 검사 실패 로깅
      log.warn("유저 업데이트 실패: 유저네임 {}이(가) 이미 존재합니다.", newUsername);
      throw new IllegalArgumentException("User with username " + newUsername + " already exists");
    }

    BinaryContent nullableProfile = optionalProfileCreateRequest
        .map(profileRequest -> {
          //프로필 파일 업데이트 처리 시작 로깅
          log.debug("프로필 파일 업데이트 처리 시작: fileName={}", profileRequest.fileName());
          String fileName = profileRequest.fileName();
          String contentType = profileRequest.contentType();
          byte[] bytes = profileRequest.bytes();
          BinaryContent binaryContent = new BinaryContent(fileName, (long) bytes.length,
              contentType);
          binaryContentRepository.save(binaryContent);
          binaryContentStorage.put(binaryContent.getId(), bytes);
          //프로필 파일 업데이트 처리 완료 로깅
          log.debug("프로필 파일 업데이트 처리 완료. binaryContentId={}", binaryContent.getId());
          return binaryContent;
        })
        .orElse(null);

    String newPassword = userUpdateRequest.newPassword();
    user.update(newUsername, newEmail, newPassword, nullableProfile);

    //유저 업데이트 성공 로깅
    log.info("유저 업데이트 성공: userId={}", userId);
    return userMapper.toDto(user);
  }

  @Transactional
  @Override
  public void delete(UUID userId) {
    //삭제 요청 수신 로깅
    log.info("유저 삭제 서비스 시작: userId={}", userId);
    if (userRepository.existsById(userId)) {
      //삭제대상 조회 실패 로깅
      log.warn("유저 삭제 실패: id {}에 해당하는 유저를 찾을 수 없습니다.", userId);
      throw new NoSuchElementException("User with id " + userId + " not found");
    }
    userRepository.deleteById(userId);
    //삭제 성공 로깅
    log.info("유저 삭제 성공: userId={}", userId);
  }
}
