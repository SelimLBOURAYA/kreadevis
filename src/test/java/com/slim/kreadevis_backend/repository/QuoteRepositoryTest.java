package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Address;
import com.slim.kreadevis_backend.entity.Client;
import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class QuoteRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired QuoteRepository quoteRepository;

    @Test
    void findMaxDailySequenceByUserAndDate_shouldReturn0_whenNoQuotes() {
        User user = persistUser("alice", "alice@test.com");

        int result = quoteRepository.findMaxDailySequenceByUserAndDate(user.getId(), LocalDate.now());

        assertThat(result).isZero();
    }

    @Test
    void findMaxDailySequenceByUserAndDate_shouldReturnMax_forGivenUserAndDate() {
        User user = persistUser("bob", "bob@test.com");
        LocalDate today = LocalDate.now();
        persistQuote(user, today, 1);
        persistQuote(user, today, 3);
        persistQuote(user, today, 2);

        int result = quoteRepository.findMaxDailySequenceByUserAndDate(user.getId(), today);

        assertThat(result).isEqualTo(3);
    }

    @Test
    void findMaxDailySequenceByUserAndDate_shouldIgnoreOtherUsers() {
        User user1 = persistUser("carol", "carol@test.com");
        User user2 = persistUser("dave", "dave@test.com");
        persistQuote(user1, LocalDate.now(), 5);

        int result = quoteRepository.findMaxDailySequenceByUserAndDate(user2.getId(), LocalDate.now());

        assertThat(result).isZero();
    }

    @Test
    void findByFilters_shouldReturnActiveQuotesWithinRange() {
        User user = persistUser("eve", "eve@test.com");
        persistQuote(user, LocalDate.of(2026, 1, 10), 1);
        persistQuote(user, LocalDate.of(2026, 1, 20), 2);
        persistQuote(user, LocalDate.of(2026, 2, 5), 3);

        Page<Quote> result = quoteRepository.findByFilters(null,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findByFilters_shouldReturnAll_whenNoFilters() {
        User user = persistUser("frank", "frank@test.com");
        persistQuote(user, LocalDate.of(2026, 1, 1), 1);
        persistQuote(user, LocalDate.of(2026, 6, 1), 2);

        Page<Quote> result = quoteRepository.findByFilters(null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void findByFilters_shouldRespectPageSize() {
        User user = persistUser("grace", "grace@test.com");
        persistQuote(user, LocalDate.of(2026, 1, 1), 1);
        persistQuote(user, LocalDate.of(2026, 1, 2), 2);
        persistQuote(user, LocalDate.of(2026, 1, 3), 3);

        Page<Quote> result = quoteRepository.findByFilters(null, null, null, PageRequest.of(0, 2));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    private User persistUser(String login, String email) {
        return em.persist(User.builder().login(login).email(email).password("hashed").build());
    }

    private Quote persistQuote(User user, LocalDate date, int sequence) {
        Client client = persistClient(user);
        return em.persist(Quote.builder().client(client).createdBy(user).date(date).dailySequence(sequence).build());
    }

    private Client persistClient(User owner) {
        Address address = em.persist(Address.builder().street("1 rue Test").city("Paris").build());
        return em.persist(Client.builder().lastName("Doe").address(address).createdBy(owner).build());
    }

    // --- findByFiltersForOwner ---

    @Test
    void findByFiltersForOwner_shouldOnlyReturnQuotesOfGivenOwner() {
        User owner = persistUser("heidi", "heidi@test.com");
        User other = persistUser("ivan", "ivan@test.com");
        persistQuote(owner, LocalDate.of(2026, 1, 10), 1);
        persistQuote(other, LocalDate.of(2026, 1, 11), 1);

        Page<Quote> result = quoteRepository.findByFiltersForOwner(owner.getId(), null, null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCreatedBy().getId()).isEqualTo(owner.getId());
    }
}
