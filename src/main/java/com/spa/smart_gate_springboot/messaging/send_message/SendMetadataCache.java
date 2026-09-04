package com.spa.smart_gate_springboot.messaging.send_message;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.spa.smart_gate_springboot.account_setup.account.Account;
import com.spa.smart_gate_springboot.account_setup.account.AccountService;
import com.spa.smart_gate_springboot.account_setup.reseller.Reseller;
import com.spa.smart_gate_springboot.account_setup.reseller.ResellerService;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetup;
import com.spa.smart_gate_springboot.account_setup.shortsetup.MsgShortcodeSetupService;
import com.spa.smart_gate_springboot.user.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Short-TTL cache for the reference data the SMS send path reads once PER MESSAGE.
 *
 * <p>A bulk send is thousands of messages that share one account, one reseller, one sender ID and one
 * creator, yet the hot path re-read all of them for every single SMS: the account twice (once directly,
 * once again inside the per-SMS cost lookup), the reseller, the creator's user row for an email, and the
 * sender-ID setup inside the carrier client. That is five SELECTs of the same handful of rows, ten
 * thousand times, in front of the carrier round trip that actually costs the money.
 *
 * <h3>What is deliberately NOT cached</h3>
 * The unit <b>balance</b>. Nothing here participates in billing: the debit is the guarded
 * {@code UPDATE ... WHERE acc_msg_bal >= :dedAmt} in {@code AccountRepository.updateAccountMsgBal},
 * which always reads and writes the live row. Only descriptive fields (name, price, ids, sender type)
 * are held, so a stale entry can never let a send through unpaid or double-debit.
 *
 * <h3>Staleness window</h3>
 * Entries expire {@code sms.metadata-cache.ttl-seconds} (default 60s) after write, so an SMS price
 * change, an account rename or a sender-type change takes up to a minute to affect sends already in
 * flight. Lookups that FAIL are not cached — a sender ID mapped while a campaign is running starts
 * working on its next message rather than after the TTL.
 *
 * <h3>Why immutable projections, not entities</h3>
 * These caches hand the same instance to every consumer thread. Caching the JPA entities themselves
 * would publish a shared mutable {@code Account} to 32 threads and to unrelated callers that legitimately
 * mutate-and-save it (e.g. {@code AccountService.deleteAccount}), so each cache holds a small record
 * carrying only the fields the send path reads.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SendMetadataCache {

    private final AccountService accountService;
    private final ResellerService resellerService;
    private final UserService userService;
    private final MsgShortcodeSetupService msgShortcodeSetupService;

    @Value("${sms.metadata-cache.ttl-seconds:60}")
    private long ttlSeconds;

    @Value("${sms.metadata-cache.max-size:10000}")
    private long maxSize;

    private Cache<UUID, AccountMeta> accounts;
    private Cache<UUID, ResellerMeta> resellers;
    private Cache<UUID, Optional<String>> creatorEmails;
    private Cache<String, SenderMeta> senders;

    /** The account fields the send path reads. {@code smsPrice} may be null — callers apply the default. */
    public record AccountMeta(UUID accId, String accName, BigDecimal smsPrice, UUID resellerId) {
    }

    public record ResellerMeta(UUID rsId, String companyName) {
    }

    /** Resolved sender ID for a send: the code as registered, and TRANSACTION vs promotional. */
    public record SenderMeta(String code, String senderType) {
    }

    @PostConstruct
    void init() {
        this.accounts = build();
        this.resellers = build();
        this.creatorEmails = build();
        this.senders = build();
        log.info("[SMS] send-metadata cache enabled — ttl={}s maxSize={}", ttlSeconds, maxSize);
    }

    private <K, V> Cache<K, V> build() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(ttlSeconds))
                .maximumSize(maxSize)
                .build();
    }

    public AccountMeta account(UUID accId) {
        return accounts.get(accId, id -> {
            Account acc = accountService.findByAccId(id);
            return new AccountMeta(acc.getAccId(), acc.getAccName(), acc.getAccSmsPrice(), acc.getAccResellerId());
        });
    }

    public ResellerMeta reseller(UUID rsId) {
        return resellers.get(rsId, id -> {
            Reseller reseller = resellerService.findById(id);
            return new ResellerMeta(reseller.getRsId(), reseller.getRsCompanyName());
        });
    }

    /**
     * Email of the user who created the send, or empty if they can't be resolved. Absence is cached
     * (as an empty Optional) so a deleted or unresolvable creator doesn't re-query once per message —
     * this is a display field, and the original code already tolerated it being missing.
     */
    public Optional<String> creatorEmail(UUID userId) {
        return creatorEmails.get(userId, id -> {
            try {
                return Optional.ofNullable(userService.findById(id).getEmail());
            } catch (Exception e) {
                log.error("Error while creating email address : {}", e.getLocalizedMessage());
                return Optional.empty();
            }
        });
    }

    /**
     * Sender-ID setup for (code, account). Throws exactly as the underlying lookup does when the sender
     * is not mapped — and because the loader threw, nothing is cached, so mapping it takes effect at once.
     */
    public SenderMeta sender(String shCode, UUID accId) {
        return senders.get(shCode + "|" + accId, key -> {
            MsgShortcodeSetup setup = msgShortcodeSetupService.findByShCodeAndShAccId(shCode, accId);
            return new SenderMeta(setup.getShCode(), setup.getShSenderType());
        });
    }
}
