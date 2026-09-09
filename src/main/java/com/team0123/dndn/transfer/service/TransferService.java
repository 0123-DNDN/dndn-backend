package com.team0123.dndn.transfer.service;

import com.team0123.dndn.account.entity.Account;
import com.team0123.dndn.account.repository.AccountRepository;
import com.team0123.dndn.family.repository.GuardianRelationshipRepository;
import com.team0123.dndn.fds.dto.FdsAnalyzeRequest;
import com.team0123.dndn.fds.dto.FdsAnalyzeResponse;
import com.team0123.dndn.fds.dto.FdsRiskScoreResponse;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.BehaviorRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.ConditionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.DeviceRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.RecipientRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.TransactionRiskInput;
import com.team0123.dndn.fds.dto.FdsRiskScoreRequest.VelocityRiskInput;
import com.team0123.dndn.fds.entity.FdsRiskAnalysis;
import com.team0123.dndn.fds.repository.FdsRiskAnalysisRepository;
import com.team0123.dndn.fds.service.FdsAnalyzeService;
import com.team0123.dndn.fds.type.RecommendedAction;
import com.team0123.dndn.recipient.entity.RecipientAlias;
import com.team0123.dndn.recipient.repository.RecipientAliasRepository;
import com.team0123.dndn.transfer.dto.FdsCheckResponse;
import com.team0123.dndn.transfer.dto.FdsCheckRequest;
import com.team0123.dndn.transfer.dto.FdsResultResponse;
import com.team0123.dndn.transfer.dto.TransferCreateRequest;
import com.team0123.dndn.transfer.dto.TransferResponse;
import com.team0123.dndn.transfer.entity.Transfer;
import com.team0123.dndn.transfer.entity.TransferStatus;
import com.team0123.dndn.transfer.repository.TransferRepository;
import com.team0123.dndn.notification.entity.NotificationType;
import com.team0123.dndn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final FdsAnalyzeService fdsAnalyzeService;
    private final FdsRiskAnalysisRepository fdsRiskAnalysisRepository;
    private final RecipientAliasRepository recipientAliasRepository;
    private final GuardianRelationshipRepository guardianRelationshipRepository;
    private final NotificationService notificationService;

    /**
     * 송금 요청을 생성합니다.
     *
     * 실제 잔액은 차감하지 않습니다.
     * 송금 정보만 transactions 테이블에 저장하고
     * 상태는 CREATED로 시작합니다.
     */
    public TransferResponse createTransfer(
            Long userId,
            TransferCreateRequest request
    ) {

        Account senderAccount = accountRepository
                .findByAccountIdAndUserId(
                        request.getSenderAccountId(),
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 계좌를 찾을 수 없습니다."
                        )
                );

        if (senderAccount.getStatus()
                != com.team0123.dndn.account.entity.AccountStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "사용할 수 없는 송금 계좌입니다."
            );
        }

        if (request.getAmount() > senderAccount.getBalance()) {

            throw new IllegalArgumentException(
                    "잔액이 부족합니다."
            );
        }

        /*
         * 수취인 정보를 결정합니다.
         *
         * 1. 별칭 송금
         *    recipientAlias가 있으면 등록된 별칭을 조회합니다.
         *
         * 2. 계좌번호 직접 송금
         *    bankCode + accountNumber로 계좌를 조회합니다.
         */
        boolean hasAlias =
                request.getRecipientAlias() != null
                        && !request.getRecipientAlias().isBlank();

        boolean hasAccountNumber =
                request.getReceiverBankCode() != null
                        && !request.getReceiverBankCode().isBlank()
                        && request.getReceiverAccountNumber() != null
                        && !request.getReceiverAccountNumber().isBlank();

        if (hasAlias == hasAccountNumber) {
            throw new IllegalArgumentException(
                    "별칭 또는 계좌번호 중 하나만 입력해야 합니다."
            );
        }

        Long receiverAccountId;
        String receiverBankCode;
        String receiverAccountNumber;
        String receiverName;

        if (hasAlias) {

            /*
             * 등록된 별칭으로 수취인 조회
             */
            RecipientAlias recipientAlias =
                    recipientAliasRepository
                            .findByUserIdAndAliasNameContainingIgnoreCase(
                                    userId,
                                    request.getRecipientAlias()
                            )
                            .stream()
                            .findFirst()
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "등록된 수취인 별칭을 찾을 수 없습니다."
                                    )
                            );

            receiverBankCode =
                    recipientAlias.getBankCode();

            receiverAccountNumber =
                    recipientAlias.getAccountNumber();

            receiverName =
                    recipientAlias.getRecipientName();

            /*
             * 별칭에 등록된 계좌가 실제로 존재하는 경우
             * 계좌 ID도 함께 저장합니다.
             *
             * 외부 계좌(Mock)라면 receiverAccountId는 null입니다.
             */
            receiverAccountId =
                    accountRepository
                            .findByBankCodeAndAccountNumber(
                                    receiverBankCode,
                                    receiverAccountNumber
                            )
                            .map(Account::getAccountId)
                            .orElse(null);

        } else {

            /*
             * 계좌번호로 직접 수취인 계좌 조회
             */
            Account receiverAccount =
                    accountRepository
                            .findByBankCodeAndAccountNumber(
                                    request.getReceiverBankCode(),
                                    request.getReceiverAccountNumber()
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "수취인 계좌를 찾을 수 없습니다."
                                    )
                            );

            if (receiverAccount.getStatus()
                    != com.team0123.dndn.account.entity.AccountStatus.ACTIVE) {

                throw new IllegalArgumentException(
                        "사용할 수 없는 수취인 계좌입니다."
                );
            }

            receiverAccountId =
                    receiverAccount.getAccountId();

            receiverBankCode =
                    receiverAccount.getBankCode();

            receiverAccountNumber =
                    receiverAccount.getAccountNumber();

            receiverName =
                    receiverAccount.getOwnerName();
        }

        Long balanceBefore =
                senderAccount.getBalance();

        Transfer transfer = Transfer.builder()
                .senderAccountId(senderAccount.getAccountId())
                .receiverAccountId(receiverAccountId)
                .receiverBankCode(receiverBankCode)
                .receiverAccountNumber(receiverAccountNumber)
                .receiverName(receiverName)
                .amount(request.getAmount())
                .senderBalanceBefore(balanceBefore)
                .purpose(request.getPurpose())
                .status(TransferStatus.CREATED)
                .build();

        Transfer savedTransfer =
                transferRepository.save(transfer);

        return TransferResponse.from(
                savedTransfer,
                senderAccount.getAccountNumber()
        );
    }

    /**
     * 송금 정보를 조회합니다.
     */
    @Transactional(readOnly = true)
    public TransferResponse getTransfer(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer = transferRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 송금입니다."
                        )
                );

        validateSenderOwnership(
                userId,
                transfer.getSenderAccountId()
        );

        Account senderAccount = accountRepository.findById(transfer.getSenderAccountId())
                .orElseThrow(() -> new IllegalArgumentException("송금 계좌를 찾을 수 없습니다."));

        return TransferResponse.from(
                transfer,
                senderAccount.getAccountNumber()
        );
    }

    /**
     * 현재 사용자의 전체 거래내역을 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TransferResponse> getTransactions(Long userId) {

        List<Account> senderAccounts =
                accountRepository.findAllByUserId(userId);

        return senderAccounts.stream()
                .flatMap(account ->
                        transferRepository
                                .findAllBySenderAccountIdOrderByCreatedAtDesc(
                                        account.getAccountId()
                                )
                                .stream()
                                .map(transfer ->
                                        TransferResponse.from(
                                                transfer,
                                                account.getAccountNumber()
                                        )
                                )
                )
                .toList();
    }

    /**
     * 수취인 확인
     *
     * CREATED
     *     ↓
     * RECIPIENT_CONFIRMED
     */
    public TransferResponse confirmRecipient(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        validateStatus(
                transfer,
                TransferStatus.CREATED
        );

        transfer.changeStatus(
                TransferStatus.RECIPIENT_CONFIRMED
        );

        return toResponse(transfer);
    }

    /**
     * 금액 확인
     *
     * RECIPIENT_CONFIRMED
     *     ↓
     * AMOUNT_CONFIRMED
     */
    public TransferResponse confirmAmount(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        validateStatus(
                transfer,
                TransferStatus.RECIPIENT_CONFIRMED
        );

        transfer.changeStatus(
                TransferStatus.AMOUNT_CONFIRMED
        );

        return toResponse(transfer);
    }

    /**
     * FDS 분석
     *
     * AMOUNT_CONFIRMED
     *     ↓
     * FDS_CHECKING
     *     ↓
     * FDS 분석
     *     ↓
     * PROCEED   → NORMAL
     * RECONFIRM → NORMAL
     * WARN      → NORMAL
     * HOLD      → HIGH_RISK → WAITING_GUARDIAN
     */
    public FdsCheckResponse startFdsCheck(
            Long userId,
            Long transactionId,
            FdsCheckRequest fdsCheckRequest
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        validateStatus(
                transfer,
                TransferStatus.AMOUNT_CONFIRMED
        );

        // FDS 검사 중 상태로 변경합니다.
        transfer.changeStatus(
                TransferStatus.FDS_CHECKING
        );

        // FDS 분석 요청 데이터를 생성합니다.
        FdsAnalyzeRequest request =
                buildFdsAnalyzeRequest(transfer, fdsCheckRequest);

        // B의 FDS 분석 서비스를 호출합니다.
        FdsAnalyzeResponse response =
                fdsAnalyzeService.analyze(
                        request,
                        fdsCheckRequest.detectedSignals()
                );

        // FDS 분석 결과를 DB에 저장합니다.
        saveFdsAnalysis(
                transfer,
                response
        );

        // B가 판단한 RecommendedAction을 기준으로
        // A가 실제 송금 상태를 결정합니다.
        applyFdsResult(
                transfer,
                response
        );

        return FdsCheckResponse.from(
                transfer.getTransactionId(),
                transfer.getStatus(),
                FdsResultResponse.from(response)
        );
    }

    /**
     * FDS 분석 요청 DTO를 생성합니다.
     */
    private FdsAnalyzeRequest buildFdsAnalyzeRequest(
            Transfer transfer,
            FdsCheckRequest fdsCheckRequest
    ) {

        // 송금 계좌의 현재 잔액을 조회합니다.
        Account senderAccount = accountRepository
                .findById(transfer.getSenderAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 계좌를 찾을 수 없습니다."
                        )
                );

        /*
         * 현재 송금 금액이 송금 생성 당시 잔액에서
         * 차지하는 비율입니다.
         *
         * 예:
         * 송금액 300,000원
         * 잔액 1,000,000원
         * → 0.3
         */
        double balanceRatio = calculateBalanceRatio(
                transfer.getAmount(),
                transfer.getSenderBalanceBefore()
        );

        /*
         * 과거 완료된 송금의 평균 금액을 조회합니다.
         */
        Double averageAmount =
                transferRepository
                        .findAverageAmountBySenderAccountIdAndStatus(
                                transfer.getSenderAccountId(),
                                TransferStatus.COMPLETED
                        );

        /*
         * 과거 송금 이력이 없다면
         * 현재 송금 금액을 기준으로 1.0을 사용합니다.
         *
         * 따라서 첫 송금이라고 해서
         * 비정상적으로 높은 amountRatio가 만들어지지 않습니다.
         */
        double amountRatioToAverage =
                calculateAmountRatio(
                        transfer.getAmount(),
                        averageAmount
                );

        /*
         * 최근 송금 내역을 조회합니다.
         */
        LocalDateTime now = LocalDateTime.now();

        List<Transfer> recent10Minutes =
                transferRepository
                        .findAllBySenderAccountIdAndCreatedAtAfter(
                                transfer.getSenderAccountId(),
                                now.minusMinutes(10)
                        );

        List<Transfer> recent30Minutes =
                transferRepository
                        .findAllBySenderAccountIdAndCreatedAtAfter(
                                transfer.getSenderAccountId(),
                                now.minusMinutes(30)
                        );

        /*
         * 최근 10분 / 30분 송금 횟수입니다.
         */
        int transfersIn10Minutes =
                recent10Minutes.size();

        int transfersIn30Minutes =
                recent30Minutes.size();

        /*
         * 현재 프로젝트에서는 CANCELLED를
         * 실패 송금으로 간주합니다.
         */
        int failedTransfersIn10Minutes =
                (int) recent10Minutes.stream()
                        .filter(t ->
                                t.getStatus()
                                        == TransferStatus.CANCELLED
                        )
                        .count();

        /*
         * 최근 30분 내 서로 다른 수취인 수입니다.
         */
        int distinctRecipientsIn30Minutes =
                (int) recent30Minutes.stream()
                        .map(Transfer::getReceiverAccountNumber)
                        .distinct()
                        .count();

        /*
         * 최근 1시간 누적 송금 금액이 급증했는지 여부입니다.
         *
         * 현재는 단순 기준으로
         * 1시간 누적 금액이 현재 잔액의 2배 이상이면
         * 급증으로 판단합니다.
         *
         * 추후 실제 FDS 기준에 맞춰 조정할 수 있습니다.
         */
        List<Transfer> recent1Hour =
                transferRepository
                        .findAllBySenderAccountIdAndCreatedAtAfter(
                                transfer.getSenderAccountId(),
                                now.minusHours(1)
                        );

        long cumulativeAmount =
                recent1Hour.stream()
                        .mapToLong(Transfer::getAmount)
                        .sum();

        boolean rapidCumulativeAmountIncrease =
                cumulativeAmount
                        >= transfer.getSenderBalanceBefore() * 2L;

        boolean newRecipient =
                !recipientAliasRepository.existsByUserIdAndBankCodeAndAccountNumber(
                        senderAccount.getUserId(),
                        transfer.getReceiverBankCode(),
                        transfer.getReceiverAccountNumber()
                );

        /*
         * FDS 요청을 생성합니다.
         *
         * 현재 A에서 직접 확인할 수 없는
         * 단말 / 행동 / 음성·채팅 상태 정보는
         * 기본값으로 전달합니다.
         */
        return new FdsAnalyzeRequest(
                transfer.getPurpose(),
                fdsCheckRequest.followUpAnswers(),

                new TransactionRiskInput(
                        amountRatioToAverage,
                        transfer.getAmount(),
                        balanceRatio,
                        false
                ),

                new RecipientRiskInput(
                        newRecipient,
                        false,
                        false,
                        false
                ),

                new VelocityRiskInput(
                        transfersIn10Minutes,
                        transfersIn30Minutes,
                        failedTransfersIn10Minutes,
                        distinctRecipientsIn30Minutes,
                        rapidCumulativeAmountIncrease
                ),

                new DeviceRiskInput(
                        false,
                        false,
                        false,
                        false
                ),

                new BehaviorRiskInput(
                        0,
                        0,
                        0,
                        0,
                        0,
                        0
                ),

                new ConditionRiskInput(
                        null,
                        0.0,
                        0,
                        0,
                        0,
                        0.0,
                        0.0,
                        false,
                        false,
                        0.0,
                        0,
                        0,
                        0
                )
        );
    }

    /**
     * 현재 송금 금액 / 송금 당시 잔액
     *
     * 결과는 0.0 ~ 1.0 이상의 decimal 값입니다.
     */
    private double calculateBalanceRatio(
            Long amount,
            Long balance
    ) {

        if (balance == null || balance <= 0) {
            return 0.0;
        }

        return (double) amount / balance;
    }

    /**
     * 현재 송금 금액 / 평소 평균 송금 금액
     */
    private double calculateAmountRatio(
            Long amount,
            Double averageAmount
    ) {

        if (averageAmount == null || averageAmount <= 0) {
            return 1.0;
        }

        return amount / averageAmount;
    }

    /**
     * FDS 분석 결과를 DB에 저장합니다.
     */
    private void saveFdsAnalysis(
            Transfer transfer,
            FdsAnalyzeResponse response
    ) {

        FdsRiskScoreResponse score =
                response.scoreBreakdown();

        FdsRiskAnalysis analysis =
                FdsRiskAnalysis.builder()
                        .transactionId(transfer.getTransactionId())
                        .transactionScore(score.transactionScore())
                        .recipientScore(score.recipientScore())
                        .velocityScore(score.velocityScore())
                        .deviceScore(score.deviceScore())
                        .behaviorScore(score.behaviorScore())
                        .contextScore(score.contextScore())
                        .conditionScore(score.conditionScore())
                        .totalScore(score.totalScore())
                        .riskLevel(response.riskLevel())
                        .hardRuleTriggered(
                                response.hardRuleTriggered()
                        )
                        .combinationRuleTriggered(
                                response.combinationRuleTriggered()
                        )
                        .build();

        fdsRiskAnalysisRepository.save(analysis);
    }

    /**
     * FDS 결과를 실제 송금 상태에 반영합니다.
     *
     * B는 FDS 판단만 합니다.
     * 실제 TransferStatus 변경은 A가 담당합니다.
     */
    private void applyFdsResult(
            Transfer transfer,
            FdsAnalyzeResponse response
    ) {

        RecommendedAction action =
                response.recommendedAction();

        switch (action) {

            case PROCEED:
                transfer.changeStatus(
                        TransferStatus.NORMAL
                );
                break;

            case RECONFIRM:
                /*
                 * 한번 더 확인이 필요한 상태입니다.
                 *
                 * 별도의 TransferStatus를 추가하지 않고
                 * NORMAL 상태에서 FDS 응답의
                 * recommendedAction을 프론트에서 확인하도록 합니다.
                 */
                transfer.changeStatus(
                        TransferStatus.NORMAL
                );
                break;

            case WARN:
                /*
                 * 경고 이유를 사용자에게 보여주고
                 * 다시 확인하도록 합니다.
                 *
                 * 역시 별도의 TransferStatus를 만들지 않습니다.
                 */
                transfer.changeStatus(
                        TransferStatus.NORMAL
                );
                break;

            case HOLD:
                /*
                 * 고위험 송금입니다.
                 *
                 * 가족 승인 전까지 송금을 진행할 수 없습니다.
                 */
                transfer.changeStatus(
                        TransferStatus.HIGH_RISK
                );

                transfer.changeStatus(
                        TransferStatus.WAITING_GUARDIAN
                );

                Account senderAccount = accountRepository
                        .findById(transfer.getSenderAccountId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "송금 계좌를 찾을 수 없습니다."
                                )
                        );

                Long seniorUserId = senderAccount.getUserId();

                Long guardianUserId =
                        guardianRelationshipRepository
                                .findBySeniorUserIdAndStatus(
                                        seniorUserId,
                                        com.team0123.dndn.family.entity.GuardianRelationshipStatus.ACTIVE
                                )
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "연결된 보호자가 없습니다."
                                        )
                                )
                                .getGuardianUserId();

                notificationService.createNotification(
                        guardianUserId,
                        NotificationType.HIGH_RISK_TRANSFER,
                        "송금 확인이 필요합니다.",
                        "고위험 송금이 감지되었습니다. 가족 승인이 필요합니다."
                );

                break;

            default:
                throw new IllegalArgumentException(
                        "알 수 없는 FDS 처리 결과입니다."
                );
        }
    }

    /**
     * 송금 최종 확인
     */
    public TransferResponse finalConfirm(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        if (transfer.getStatus() != TransferStatus.NORMAL
                && transfer.getStatus() != TransferStatus.GUARDIAN_APPROVED) {

            throw new IllegalArgumentException(
                    "현재 상태에서는 해당 작업을 수행할 수 없습니다."
            );
        }

        transfer.changeStatus(
                TransferStatus.FINAL_CONFIRMED
        );

        return toResponse(transfer);
    }

    /**
     * 송금 실행
     */
    @Transactional
    public TransferResponse completeTransfer(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        validateStatus(
                transfer,
                TransferStatus.FINAL_CONFIRMED
        );

        Account senderAccount =
                accountRepository
                        .findById(transfer.getSenderAccountId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "송금 계좌를 찾을 수 없습니다."
                                )
                        );

        Long balanceBefore =
                senderAccount.getBalance();

        if (balanceBefore < transfer.getAmount()) {
            throw new IllegalArgumentException(
                    "계좌 잔액이 부족합니다."
            );
        }

        senderAccount.withdraw(
                transfer.getAmount()
        );

        Long balanceAfter =
                senderAccount.getBalance();

        transfer.complete(balanceAfter);

        return toResponse(transfer);
    }

    /**
     * 가족 승인
     *
     * HOLD 송금에 대해 가족 승인을 처리합니다.
     */
    @Transactional
    public TransferResponse guardianApprove(
            Long guardianUserId,
            Long transactionId
    ) {

        Transfer transfer = transferRepository.findById(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 정보를 찾을 수 없습니다."
                        )
                );

        validateStatus(
                transfer,
                TransferStatus.WAITING_GUARDIAN
        );

        validateGuardian(
                guardianUserId,
                transfer
        );

        transfer.changeStatus(
                TransferStatus.GUARDIAN_APPROVED
        );

        Account senderAccount = accountRepository
                .findById(transfer.getSenderAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 계좌를 찾을 수 없습니다."
                        )
                );

        Long seniorUserId = senderAccount.getUserId();

        notificationService.createNotification(
                seniorUserId,
                NotificationType.GUARDIAN_APPROVED,
                "가족 승인이 완료되었습니다.",
                "보호자가 송금을 승인했습니다. 송금을 계속 진행할 수 있습니다."
        );

        return toResponse(transfer);
    }

    /**
     * 가족 거절
     *
     * HOLD 송금을 가족이 거절하면
     * CANCELLED 상태로 변경합니다.
     */
    @Transactional
    public TransferResponse guardianReject(
            Long guardianUserId,
            Long transactionId
    ) {

        Transfer transfer = transferRepository.findById(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 정보를 찾을 수 없습니다."
                        )
                );

        validateStatus(
                transfer,
                TransferStatus.WAITING_GUARDIAN
        );

        validateGuardian(
                guardianUserId,
                transfer
        );

        transfer.cancel();

        Account senderAccount = accountRepository
                .findById(transfer.getSenderAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 계좌를 찾을 수 없습니다."
                        )
                );

        Long seniorUserId = senderAccount.getUserId();

        notificationService.createNotification(
                seniorUserId,
                NotificationType.GUARDIAN_REJECTED,
                "송금이 취소되었습니다.",
                "가족이 송금을 승인하지 않아 송금이 취소되었습니다."
        );

        return toResponse(transfer);
    }

    /**
     * 송금 취소
     */
    public TransferResponse cancelTransfer(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer =
                getOwnedTransfer(userId, transactionId);

        if (transfer.getStatus()
                == TransferStatus.COMPLETED) {

            throw new IllegalArgumentException(
                    "완료된 송금은 취소할 수 없습니다."
            );
        }

        if (transfer.getStatus()
                == TransferStatus.CANCELLED) {

            throw new IllegalArgumentException(
                    "이미 취소된 송금입니다."
            );
        }

        transfer.cancel();

        return toResponse(transfer);
    }

    /**
     * 현재 로그인한 사용자가 해당 송금의
     * 송금 계좌 소유자인지 확인합니다.
     */
    private void validateSenderOwnership(
            Long userId,
            Long senderAccountId
    ) {

        accountRepository
                .findByAccountIdAndUserId(
                        senderAccountId,
                        userId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "조회할 수 없는 송금입니다."
                        )
                );
    }

    /**
     * 현재 로그인한 사용자가
     * 해당 송금의 시니어와 연결된 보호자인지 확인합니다.
     */
    private void validateGuardian(
            Long guardianUserId,
            Transfer transfer
    ) {

        Account senderAccount = accountRepository
                .findById(transfer.getSenderAccountId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "송금 계좌를 찾을 수 없습니다."
                        )
                );

        Long seniorUserId = senderAccount.getUserId();

        guardianRelationshipRepository
                .findBySeniorUserIdAndGuardianUserId(
                        seniorUserId,
                        guardianUserId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "해당 송금에 대한 보호자 권한이 없습니다."
                        )
                );
    }

    /**
     * 송금 조회 + 소유권 검증
     */
    private Transfer getOwnedTransfer(
            Long userId,
            Long transactionId
    ) {

        Transfer transfer = transferRepository
                .findByTransactionId(transactionId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 송금입니다."
                        )
                );

        validateSenderOwnership(
                userId,
                transfer.getSenderAccountId()
        );

        return transfer;
    }

    /**
     * 현재 상태가 예상한 상태인지 검증합니다.
     */
    private void validateStatus(
            Transfer transfer,
            TransferStatus expectedStatus
    ) {

        if (transfer.getStatus() != expectedStatus) {

            throw new IllegalArgumentException(
                    "현재 상태에서는 해당 작업을 수행할 수 없습니다."
            );
        }
    }

    private TransferResponse toResponse(Transfer transfer) {
        Account senderAccount = accountRepository.findById(
                transfer.getSenderAccountId()
        ).orElseThrow(() ->
                new IllegalArgumentException("송금 계좌를 찾을 수 없습니다.")
        );

        return TransferResponse.from(
                transfer,
                senderAccount.getAccountNumber()
        );
    }
}
