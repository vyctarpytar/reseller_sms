package com.spa.smart_gate_springboot.account_setup.smpp;

import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountRepository;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerRepo;
import com.spa.smart_gate_springboot.dto.Layers;
import com.spa.smart_gate_springboot.user.Role;
import com.spa.smart_gate_springboot.user.User;
import com.spa.smart_gate_springboot.utils.StandardJsonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Locks who may provision an SMPP connection: a TOP user drilled into a reseller can, a reseller can
 * for its own accounts only, and the row's reseller always comes from the account — never the caller.
 */
class SmppConnectionServiceTest {
    private final UUID resellerId = UUID.randomUUID();
    private final UUID accId = UUID.randomUUID();

    private SmppConnectionRepo repo;
    private AccountService accountService;
    private AccountRepository accountRepository;
    private ResellerRepo resellerRepo;
    private SmppConnectionService service;

    @BeforeEach
    void setUp() {
        repo = mock(SmppConnectionRepo.class);
        accountService = mock(AccountService.class);
        accountRepository = mock(AccountRepository.class);
        resellerRepo = mock(ResellerRepo.class);
        service = new SmppConnectionService(repo, accountService, accountRepository, resellerRepo);

        Account account = Account.builder().accId(accId).accResellerId(resellerId).accName("World Interconnect").build();
        when(accountService.findByAccId(accId)).thenReturn(account);
        when(repo.saveAndFlush(any(SmppConnection.class))).thenAnswer(i -> i.getArgument(0));
    }

    private static User user(Layers layer, Role role, UUID usrResellerId) {
        return User.builder().usrId(UUID.randomUUID()).layer(layer).role(role).usrResellerId(usrResellerId).build();
    }

    private SmppConnectionDto newConnection() {
        return SmppConnectionDto.builder().accId(accId).systemId("worldinterco").password("Xk7mPq2w").build();
    }

    @Test
    void topUserCanAddAConnectionForAResellersAccount() {
        // A TOP user's own usrResellerId is not the reseller they drilled into.
        User top = user(Layers.TOP, Role.SUPER_ADMIN, null);

        StandardJsonResponse resp = service.save(newConnection(), top, resellerId.toString());

        assertTrue(resp.isSuccess(), String.valueOf(resp.getMessages()));
        ArgumentCaptor<SmppConnection> saved = ArgumentCaptor.forClass(SmppConnection.class);
        verify(repo).saveAndFlush(saved.capture());
        assertEquals(resellerId, saved.getValue().getResellerId());
        assertEquals(accId, saved.getValue().getAccId());
        assertEquals("SERVER", saved.getValue().getConnectionType());
        assertEquals("TRANSCEIVER", saved.getValue().getBindType());
        assertTrue(saved.getValue().getIsActive());
    }

    @Test
    void topUserListsTheDrilledInResellersConnections() {
        when(repo.findByResellerIdOrderByCreatedAtDesc(resellerId)).thenReturn(List.of());

        StandardJsonResponse resp = service.list(user(Layers.TOP, Role.SUPER_ADMIN, null), resellerId.toString(), null);

        assertTrue(resp.isSuccess());
        verify(repo).findByResellerIdOrderByCreatedAtDesc(resellerId);
    }

    @Test
    void resellerCannotAddAConnectionForAnotherResellersAccount() {
        User otherReseller = user(Layers.RESELLER, Role.ADMIN, UUID.randomUUID());

        assertThrows(AccessDeniedException.class, () -> service.save(newConnection(), otherReseller, null));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void accountUserCannotManageConnections() {
        assertThrows(AccessDeniedException.class,
                () -> service.save(newConnection(), user(Layers.ACCOUNT, Role.ADMIN, resellerId), null));
    }

    @Test
    void takenUsernameIsRefusedWithoutSaving() {
        when(repo.existsBySystemIdIgnoreCase("worldinterco")).thenReturn(true);

        StandardJsonResponse resp = service.save(newConnection(), user(Layers.TOP, Role.SUPER_ADMIN, null), null);

        assertFalse(resp.isSuccess());
        verify(repo, never()).saveAndFlush(any());
    }

    private SmppConnectionDto newUpstream() {
        return SmppConnectionDto.builder().connectionType("CLIENT").systemId("synqafrica").password("s3cr3t!")
                .host("102.220.72.50").port(7777).build();
    }

    @Test
    void topUserAddsAnUpstreamForTheResellerTheyAreIn() {
        when(resellerRepo.existsById(resellerId)).thenReturn(true);

        StandardJsonResponse resp = service.save(newUpstream(), user(Layers.TOP, Role.SUPER_ADMIN, null), resellerId.toString());

        assertTrue(resp.isSuccess(), String.valueOf(resp.getMessages()));
        ArgumentCaptor<SmppConnection> saved = ArgumentCaptor.forClass(SmppConnection.class);
        verify(repo).saveAndFlush(saved.capture());
        // The owning reseller is the one drilled into — no id is baked in anywhere.
        assertEquals(resellerId, saved.getValue().getResellerId());
        assertEquals("CLIENT", saved.getValue().getConnectionType());
        assertEquals("102.220.72.50", saved.getValue().getHost());
        assertEquals(7777, saved.getValue().getPort());
        // An upstream does not make an account an SMPP client.
        verify(accountRepository, never()).updateSmppEnabled(any(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void resellerCannotAddAnUpstream() {
        assertThrows(AccessDeniedException.class,
                () -> service.save(newUpstream(), user(Layers.RESELLER, Role.ADMIN, resellerId), null));
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void resellerNeverSeesUpstreamCredentials() {
        SmppConnection upstream = SmppConnection.builder().id(UUID.randomUUID()).resellerId(resellerId)
                .connectionType("CLIENT").systemId("synqafrica").password("s3cr3t!").build();
        SmppConnection downstream = SmppConnection.builder().id(UUID.randomUUID()).resellerId(resellerId).accId(accId)
                .connectionType("SERVER").systemId("worldinterco").password("Xk7mPq2w").build();
        when(repo.findByResellerIdOrderByCreatedAtDesc(resellerId)).thenReturn(List.of(upstream, downstream));

        assertEquals(1, service.list(user(Layers.RESELLER, Role.ADMIN, resellerId), null, null).getTotal());
        assertEquals(2, service.list(user(Layers.TOP, Role.SUPER_ADMIN, null), resellerId.toString(), null).getTotal());
    }
}
