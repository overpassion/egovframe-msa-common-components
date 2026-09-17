package egovframework.com.sec.rgm.service.impl;

import egovframework.com.sec.rgm.entity.Emplyrscrtyestbs;
import egovframework.com.sec.rgm.repository.EgovAuthorGroupRepository;
import egovframework.com.sec.rgm.service.AuthorGroupVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * authorGroupInsert·authorGroupDelete가 항목마다 서비스를 따로 호출하던 것을
 * 서비스 메서드 하나로 옮겼다. 컨트롤러 루프가 매 항목마다 별도 트랜잭션을 여는
 * insert·update·delete를 직접 호출해, 루프 중간에 예외가 나면 그 뒤 항목은
 * 반영되지 않고 앞 항목만 커밋된 채 남을 수 있었다.
 */
class EgovAuthorGroupServiceImplBatchTest {

    private final EgovAuthorGroupRepository repository = mock(EgovAuthorGroupRepository.class);
    private final EgovAuthorGroupServiceImpl service = new EgovAuthorGroupServiceImpl(repository, null, null);

    @Test
    void insertOrUpdateListInsertsNewAndUpdatesExisting() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Emplyrscrtyestbs existing = new Emplyrscrtyestbs();
        existing.setScrtyDtrmnTrgetId("U02");
        existing.setMberTyCode("USR");
        existing.setAuthorCode("OLDCODE");
        when(repository.findById("U02")).thenReturn(Optional.of(existing));

        String[] userIds = {"U01", "U02"};
        String[] mberTyCodes = {"USR", "USR"};
        String[] authorCodes = {"A01", "A02"};
        String[] regYns = {"N", "Y"};

        service.insertOrUpdateList(userIds, mberTyCodes, authorCodes, regYns, new AuthorGroupVO());

        ArgumentCaptor<Emplyrscrtyestbs> saved = ArgumentCaptor.forClass(Emplyrscrtyestbs.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(Emplyrscrtyestbs::getScrtyDtrmnTrgetId)
                .containsExactly("U01", "U02");
        assertThat(saved.getAllValues().get(1).getAuthorCode()).isEqualTo("A02");
    }

    @Test
    void deleteListDeletesEachUserWithAuthenticatedCaller() {
        String[] userIds = {"U01", "U02", "U03"};
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("uniqId", "admin1");

        service.deleteList(userIds, new AuthorGroupVO(), userInfo);

        verify(repository, times(1)).deleteById("U01");
        verify(repository, times(1)).deleteById("U02");
        verify(repository, times(1)).deleteById("U03");
    }
}
