package com.slim.kreadevis_backend.repository;

import com.slim.kreadevis_backend.entity.Quote;
import com.slim.kreadevis_backend.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

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
    void findByDateRange_shouldReturnActiveQuotesWithinRange() {
        User user = persistUser("eve", "eve@test.com");
        persistQuote(user, LocalDate.of(2026, 1, 10), 1);
        persistQuote(user, LocalDate.of(2026, 1, 20), 2);
        persistQuote(user, LocalDate.of(2026, 2, 5), 3);

        List<Quote> result = quoteRepository.findByDateRange(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByDateRange_shouldReturnAll_whenBothDatesNull() {
        User user = persistUser("frank", "frank@test.com");
        persistQuote(user, LocalDate.of(2026, 1, 1), 1);
        persistQuote(user, LocalDate.of(2026, 6, 1), 2);

        List<Quote> result = quoteRepository.findByDateRange(null, null);

        assertThat(result).hasSize(2);
    }

    private User persistUser(String login, String email) {
        return em.persist(User.builder().login(login).email(email).password("hashed").build());
    }

    private Quote persistQuote(User user, LocalDate date, int sequence) {
        return em.persist(Quote.builder().createdBy(user).date(date).dailySequence(sequence).build());
    }
}
