package com.team0123.dndn.transfer.service;

import com.team0123.dndn.transfer.entity.*;
import com.team0123.dndn.transfer.repository.TransferRepository;
import com.team0123.dndn.account.entity.Account;
import com.team0123.dndn.account.repository.AccountRepository;
import com.team0123.dndn.family.repository.GuardianRelationshipRepository;
import com.team0123.dndn.fds.repository.FdsRiskAnalysisRepository;
import com.team0123.dndn.fds.service.FdsAnalyzeService;
import com.team0123.dndn.recipient.repository.RecipientAliasRepository;
import com.team0123.dndn.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferDelayTest {
    private final TransferRepository transfers = mock(TransferRepository.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final GuardianRelationshipRepository relations = mock(GuardianRelationshipRepository.class);
    private final TransferService service = new TransferService(transfers, accounts,
            mock(FdsAnalyzeService.class), mock(FdsRiskAnalysisRepository.class),
            mock(RecipientAliasRepository.class), relations,
            mock(NotificationService.class));

    private Transfer owned(TransferStatus status, Instant availableAt) {
        Transfer transfer = Transfer.builder().transactionId(1L).senderAccountId(2L)
                .status(status).availableAt(availableAt).amount(100L).build();
        when(transfers.findForUpdate(1L)).thenReturn(Optional.of(transfer));
        Account account = mock(Account.class);
        when(accounts.findByAccountIdAndUserId(2L, 3L)).thenReturn(Optional.of(account));
        when(accounts.findById(2L)).thenReturn(Optional.of(account));
        return transfer;
    }

    @Test void delayStartsOnlyAfterConfirmationAndDoesNotAutoSend() {
        Transfer transfer = Transfer.builder().status(TransferStatus.DELAY_CONFIRM).build();
        assertFalse(transfer.isFinalConfirmationAvailable());
        Instant before = Instant.now();
        transfer.startDelay();
        assertEquals(TransferStatus.WAITING_GUARDIAN, transfer.getStatus());
        assertFalse(transfer.getAvailableAt().isBefore(before.plusSeconds(18000)));
        assertFalse(transfer.isFinalConfirmationAvailable());
    }
    @Test void earlyConfirmationIsRejected() {
        Transfer transfer = owned(TransferStatus.WAITING_GUARDIAN, Instant.now().plusSeconds(3600));
        assertThrows(IllegalArgumentException.class, () -> service.finalConfirm(3L, 1L));
        assertEquals(TransferStatus.WAITING_GUARDIAN, transfer.getStatus());
    }
    @Test void elapsedDelayStillRequiresExplicitFinalConfirmation() {
        Transfer transfer = owned(TransferStatus.WAITING_GUARDIAN, Instant.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> service.completeTransfer(3L, 1L));
        assertTrue(transfer.isFinalConfirmationAvailable());
        service.finalConfirm(3L, 1L);
        assertEquals(TransferStatus.FINAL_CONFIRMED, transfer.getStatus());
    }
    @Test void criticalWithoutApprovalCannotExpire() {
        owned(TransferStatus.WAITING_GUARDIAN, null);
        assertThrows(IllegalArgumentException.class, () -> service.finalConfirm(3L, 1L));
    }
    @Test void guardianApprovalUnlocksEarlyConfirmation() {
        Transfer transfer = owned(TransferStatus.GUARDIAN_APPROVED, Instant.now().plusSeconds(3600));
        service.finalConfirm(3L, 1L);
        assertEquals(TransferStatus.FINAL_CONFIRMED, transfer.getStatus());
    }
    @Test void blockedTransactionCannotProceedEvenIfApproved() {
        Transfer transfer = owned(TransferStatus.GUARDIAN_APPROVED, null);
        transfer.requireReview(true);
        assertThrows(IllegalArgumentException.class, () -> service.finalConfirm(3L, 1L));
    }
    @Test void cancellationCannotBeReleasedByTime() {
        owned(TransferStatus.CANCELLED, Instant.now().minusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> service.finalConfirm(3L, 1L));
    }
    @Test void delayWithoutConnectedGuardianStillPersistsDeadline() {
        Transfer transfer = owned(TransferStatus.DELAY_CONFIRM, null);
        var response = service.confirmDelay(3L, 1L);
        assertEquals(TransferStatus.WAITING_GUARDIAN, response.getStatus());
        assertEquals(transfer.getAvailableAt(), response.getAvailableAt());
        assertFalse(response.isFinalConfirmationAvailable());
        assertThrows(IllegalArgumentException.class, () -> service.confirmDelay(3L, 1L));
    }
    @Test void unrelatedGuardianCannotApprove() {
        owned(TransferStatus.WAITING_GUARDIAN, null);
        assertThrows(IllegalArgumentException.class, () -> service.guardianApprove(99L, 1L));
    }
    @Test void wrongOwnerCannotConfirm() {
        owned(TransferStatus.NORMAL, null);
        assertThrows(IllegalArgumentException.class, () -> service.finalConfirm(99L, 1L));
    }
    @Test void guardianPendingComesFromActiveFamilyTransactionsWithoutNotifications() {
        var relationship = com.team0123.dndn.family.entity.GuardianRelationship.builder()
                .seniorUserId(3L).guardianUserId(9L).build();
        when(relations.findByGuardianUserIdAndStatus(9L,
                com.team0123.dndn.family.entity.GuardianRelationshipStatus.ACTIVE))
                .thenReturn(Optional.of(relationship));
        Account account = mock(Account.class);
        when(account.getAccountId()).thenReturn(2L);
        when(accounts.findAllByUserId(3L)).thenReturn(java.util.List.of(account));
        Transfer waiting = Transfer.builder().transactionId(1L).status(TransferStatus.WAITING_GUARDIAN)
                .createdAt(java.time.LocalDateTime.now()).build();
        Transfer cancelled = Transfer.builder().transactionId(2L).status(TransferStatus.CANCELLED).build();
        when(transfers.findAllBySenderAccountIdOrderByCreatedAtDesc(2L))
                .thenReturn(java.util.List.of(waiting, cancelled));
        var pending = service.getGuardianPendingTransfers(9L);
        assertEquals(1, pending.size());
        assertEquals(1L, pending.get(0).getTransactionId());
    }
    @Test void unlinkedGuardianHasNoPendingTransactions() {
        assertTrue(service.getGuardianPendingTransfers(99L).isEmpty());
        verifyNoInteractions(accounts, transfers);
    }
}
