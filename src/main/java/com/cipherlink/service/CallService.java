package com.cipherlink.service;

import com.cipherlink.dto.CallRecordResponse;
import com.cipherlink.dto.InitiateCallRequest;
import com.cipherlink.dto.EndCallRequest;
import com.cipherlink.model.CallRecord;
import com.cipherlink.model.UserProfile;
import com.cipherlink.repository.CallRecordRepository;
import com.cipherlink.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallRecordRepository callRepo;
    private final UserProfileRepository profileRepo;

    @Transactional
    public CallRecord initiateCall(UUID callerId, InitiateCallRequest request) {
        CallRecord call = CallRecord.builder()
                .callerId(callerId)
                .receiverId(request.getReceiverId())
                .callType(request.getCallType())
                .roomId(request.getRoomId() != null ? request.getRoomId() : UUID.randomUUID().toString())
                .status("RINGING")
                .build();
        return callRepo.save(call);
    }

    @Transactional
    public CallRecord answerCall(UUID callId) {
        CallRecord call = callRepo.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));
        call.setStatus("COMPLETED");
        call.setAnsweredAt(LocalDateTime.now());
        return callRepo.save(call);
    }

    @Transactional
    public CallRecord endCall(UUID userId, UUID callId, String status, Integer durationSeconds) {
        CallRecord call = callRepo.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));

        call.setStatus(status != null ? status : "COMPLETED");
        call.setEndedAt(LocalDateTime.now());
        if (durationSeconds != null) call.setDurationSeconds(durationSeconds);
        return callRepo.save(call);
    }

    public List<CallRecordResponse> getCallHistory(UUID userId) {
        return callRepo.findCallHistory(userId, PageRequest.of(0, 50))
                .stream()
                .map(c -> buildResponse(c, userId))
                .collect(Collectors.toList());
    }

    public long getMissedCallCount(UUID userId) {
        return callRepo.countMissedCalls(userId);
    }

    private CallRecordResponse buildResponse(CallRecord c, UUID currentUserId) {
        boolean isIncoming = c.getReceiverId().equals(currentUserId);
        UUID otherUserId = isIncoming ? c.getCallerId() : c.getReceiverId();

        UserProfile profile = profileRepo.findByUserId(otherUserId).orElse(null);

        return CallRecordResponse.builder()
                .callId(c.getId())
                .callerId(c.getCallerId())
                .receiverId(c.getReceiverId())
                .callType(c.getCallType())
                .status(c.getStatus())
                .roomId(c.getRoomId())
                .startedAt(c.getStartedAt())
                .answeredAt(c.getAnsweredAt())
                .endedAt(c.getEndedAt())
                .durationSeconds(c.getDurationSeconds())
                .otherUserName(profile != null ? profile.getDisplayName() : "Unknown")
                .otherUserAvatarUrl(profile != null ? profile.getProfilePictureUrl() : null)
                .isIncoming(isIncoming)
                .build();
    }
}
