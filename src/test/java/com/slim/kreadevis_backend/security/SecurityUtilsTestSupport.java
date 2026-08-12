package com.slim.kreadevis_backend.security;

import java.util.function.Function;
import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * Test support for services that resolve owned entities through {@link SecurityUtils}.
 * <p>
 * {@code resolveOwned} is a real branch on a mocked collaborator, so a plain
 * {@code @Mock} returns {@code null} and the lookup never runs. This wires the mock
 * to execute the same branch as production, driven by the test's own
 * {@code isAdmin()} / {@code getCurrentUser()} stubs — so per-test stubbing stays
 * unchanged and admin-vs-owner cases are still exercised end to end.
 */
public final class SecurityUtilsTestSupport {

    private SecurityUtilsTestSupport() {
    }

    @SuppressWarnings("unchecked")
    public static void wireResolveOwned(SecurityUtils securityUtilsMock) {
        lenient().when(securityUtilsMock.resolveOwned(any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> {
                    Supplier<Object> adminLookup = invocation.getArgument(0);
                    Function<Long, Object> scopedLookup = invocation.getArgument(1);
                    return securityUtilsMock.isAdmin()
                            ? adminLookup.get()
                            : scopedLookup.apply(securityUtilsMock.getCurrentUser().getId());
                });
    }
}
