package com.sprint.mission.discodeit.controller;

import com.sprint.mission.discodeit.controller.api.UserApi;
import com.sprint.mission.discodeit.dto.data.UserDto;
import com.sprint.mission.discodeit.dto.data.UserStatusDto;
import com.sprint.mission.discodeit.dto.request.BinaryContentCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.UserUpdateRequest;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.service.UserStatusService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController implements UserApi {

  private final UserService userService;
  private final UserStatusService userStatusService;

  @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
  @Override
  public ResponseEntity<UserDto> create(
      @Valid
      @RequestPart("userCreateRequest") UserCreateRequest userCreateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    //요청 수신 로깅
    log.info("유저 생성 요청 수신: email={}, username={}",
        userCreateRequest.email(), userCreateRequest.username());
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    //파일 첨부 여부 로깅
    profileRequest.ifPresentOrElse(
        request -> log.debug("프로필 파일 첨부됨: fileName={}, contentType={}",
            request.fileName(), request.contentType()),
        () -> log.debug("프로필 파일 첨부 안 됨."));
    UserDto createdUser = userService.create(userCreateRequest, profileRequest);
    //생성 성공 로깅
    log.info("유저 생성 성공: userId={}", createdUser.id());
    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(createdUser);
  }

  @PatchMapping(
      path = "{userId}",
      consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}
  )
  @Override
  public ResponseEntity<UserDto> update(
      @Valid
      @PathVariable("userId") UUID userId,
      @RequestPart("userUpdateRequest") UserUpdateRequest userUpdateRequest,
      @RequestPart(value = "profile", required = false) MultipartFile profile
  ) {
    //요청 수신 로깅
    log.info("유저 정보 업데이트 요청수신: userId={}", userId);
    Optional<BinaryContentCreateRequest> profileRequest = Optional.ofNullable(profile)
        .flatMap(this::resolveProfileRequest);
    UserDto updatedUser = userService.update(userId, userUpdateRequest, profileRequest);
    //수정 성공 로깅
    log.info("유저 정보 업데이트 성공: userId={}", updatedUser.id());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(updatedUser);
  }

  @DeleteMapping(path = "{userId}")
  @Override
  public ResponseEntity<Void> delete(@PathVariable("userId") UUID userId) {
    //요청 수신 로깅
    log.info("유저 삭제 요청 수신: userId={}", userId);
    userService.delete(userId);
    //삭제 성공 로깅
    log.info("유저 삭제 성공: userId={}", userId);
    return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
  }

  @GetMapping
  @Override
  public ResponseEntity<List<UserDto>> findAll() {
    //요청 수신 로깅
    log.debug("모든 유저 목록 조회 요청 수신.");
    List<UserDto> users = userService.findAll();
    //조회 성공 로깅
    log.info("모든 유저 목록 조회 완료. 총 {}명", users.size());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(users);
  }

  @PatchMapping(path = "{userId}/userStatus")
  @Override
  public ResponseEntity<UserStatusDto> updateUserStatusByUserId(@PathVariable("userId") UUID userId,
      @RequestBody UserStatusUpdateRequest request) {
    //요청 수신 로깅
    log.info("유저 상태 업데이트 요청 수신: userId={}, newLastActiveAt={}",
        userId, request.newLastActiveAt());
    UserStatusDto updatedUserStatus = userStatusService.updateByUserId(userId, request);
    //유저상태 업데이트 성공 로깅
    log.info("유저 상태 업데이트 성공: userId={}, newLastActiveAt={}",
        userId, updatedUserStatus.lastActiveAt());
    return ResponseEntity
        .status(HttpStatus.OK)
        .body(updatedUserStatus);
  }

  private Optional<BinaryContentCreateRequest> resolveProfileRequest(MultipartFile profileFile) {
    if (profileFile.isEmpty()) {
      //파일 없을때 로깅
      log.debug("프로필 파일 변환: 파일이 첨부되지 않았습니다.");
      return Optional.empty();
    } else {
      try {
        //파일 처리 시작 로깅
        log.debug("프로필 파일 변환 시작: fileName={}, contentType={}",
            profileFile.getOriginalFilename(), profileFile.getContentType());
        BinaryContentCreateRequest binaryContentCreateRequest = new BinaryContentCreateRequest(
            profileFile.getOriginalFilename(),
            profileFile.getContentType(),
            profileFile.getBytes()
        );
        //파일처리 완료 로깅
        log.debug("프로필 파일 변환 완료. size={} bytes", profileFile.getSize());
        return Optional.of(binaryContentCreateRequest);
      } catch (IOException e) {
        //IOException발생시 ERROR레벨로 상세 로깅
        log.error("프로필 파일 변환 중 IOException 발생: fileName={}",
            profileFile.getOriginalFilename(), e);
        throw new RuntimeException(e);
      }
    }
  }
}
