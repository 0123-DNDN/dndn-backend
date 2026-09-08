package com.team0123.dndn.recipient.service;

import com.team0123.dndn.recipient.dto.RecipientCreateRequest;
import com.team0123.dndn.recipient.dto.RecipientResponse;
import com.team0123.dndn.recipient.entity.RecipientAlias;
import com.team0123.dndn.recipient.repository.RecipientAliasRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecipientService {

    private final RecipientAliasRepository recipientAliasRepository;

    /**
     * 현재 사용자의 수취인 목록 조회
     */
    public List<RecipientResponse> getRecipients(Long userId) {

        return recipientAliasRepository.findAllByUserId(userId)
                .stream()
                .map(RecipientResponse::from)
                .toList();
    }

    /**
     * 현재 사용자의 수취인 검색
     * 별칭 또는 실제 예금주 이름으로 검색
     */
    public List<RecipientResponse> searchRecipients(
            Long userId,
            String keyword
    ) {

        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("검색어를 입력해주세요.");
        }

        List<RecipientAlias> aliasResults =
                recipientAliasRepository
                        .findByUserIdAndAliasNameContainingIgnoreCase(
                                userId,
                                keyword
                        );

        List<RecipientAlias> nameResults =
                recipientAliasRepository
                        .findByUserIdAndRecipientNameContainingIgnoreCase(
                                userId,
                                keyword
                        );

        return java.util.stream.Stream
                .concat(aliasResults.stream(), nameResults.stream())
                .distinct()
                .map(RecipientResponse::from)
                .toList();
    }

    /**
     * 새로운 수취인 등록
     */
    @Transactional
    public RecipientResponse createRecipient(
            Long userId,
            RecipientCreateRequest request
    ) {

        // 동일 사용자가 동일 계좌를 이미 등록했는지 확인
        boolean exists =
                recipientAliasRepository
                        .existsByUserIdAndBankCodeAndAccountNumber(
                                userId,
                                request.getBankCode(),
                                request.getAccountNumber()
                        );

        if (exists) {
            throw new IllegalArgumentException(
                    "이미 등록된 수취인 계좌입니다."
            );
        }

        // 예금주 인증 Mock
        verifyAccountHolder(request);

        RecipientAlias recipientAlias = RecipientAlias.builder()
                .userId(userId)
                .aliasName(request.getAliasName())
                .bankCode(request.getBankCode())
                .accountNumber(request.getAccountNumber())
                .recipientName(request.getRecipientName())
                .build();

        RecipientAlias savedRecipient =
                recipientAliasRepository.save(recipientAlias);

        return RecipientResponse.from(savedRecipient);
    }

    /**
     * 계좌 예금주 인증 Mock
     *
     * 실제 금융기관 API 연동 전까지 사용하는 임시 검증 로직
     */
    private void verifyAccountHolder(RecipientCreateRequest request) {

        if (request.getAccountNumber() == null
                || request.getAccountNumber().isBlank()) {

            throw new IllegalArgumentException(
                    "계좌번호가 올바르지 않습니다."
            );
        }

        if (request.getRecipientName() == null
                || request.getRecipientName().isBlank()) {

            throw new IllegalArgumentException(
                    "예금주 정보가 올바르지 않습니다."
            );
        }

        if (request.getBankCode() == null
                || request.getBankCode().isBlank()) {

            throw new IllegalArgumentException(
                    "은행 코드가 올바르지 않습니다."
            );
        }
    }
}