package egovframework.com.sec.ram.service.impl;

import egovframework.com.sec.ram.entity.AuthorRoleRelated;
import egovframework.com.sec.ram.repository.EgovAuthorRoleRepository;
import egovframework.com.sec.ram.service.AuthorRoleRelatedVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * roleInfoInsert가 항목마다 delete·insert를 따로 호출하던 것을 서비스 메서드 하나로 옮겼다.
 * 기존에는 컨트롤러 루프가 매 항목마다 별도 트랜잭션을 여는 delete·insert를 직접 호출해,
 * 루프 중간에 예외가 나면 이미 delete된 롤 배정이 insert 없이 영구 유실될 수 있었다.
 */
class EgovAuthorRoleServiceImplUpdateListTest {

    private final EgovAuthorRoleRepository repository = mock(EgovAuthorRoleRepository.class);
    private final EgovAuthorRoleServiceImpl service = new EgovAuthorRoleServiceImpl(repository, null);

    @Test
    void updateRoleInfoListReplacesEachRoleAssignment() {
        when(repository.existsById(any())).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String[] authorCodes = {"AUTH01", "AUTH02"};
        String[] roleCodes = {"ROLE01", "ROLE02"};
        String[] regYns = {"Y", "Y"};

        boolean result = service.updateRoleInfoList(authorCodes, roleCodes, regYns, new AuthorRoleRelatedVO());

        assertThat(result).isTrue();

        ArgumentCaptor<AuthorRoleRelated> deleted = ArgumentCaptor.forClass(AuthorRoleRelated.class);
        verify(repository, times(2)).delete(deleted.capture());
        assertThat(deleted.getAllValues())
                .extracting(e -> e.getAuthorRoleRelatedId().getAuthorCode())
                .containsExactly("AUTH01", "AUTH02");

        ArgumentCaptor<AuthorRoleRelated> saved = ArgumentCaptor.forClass(AuthorRoleRelated.class);
        verify(repository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues())
                .extracting(e -> e.getAuthorRoleRelatedId().getRoleCode())
                .containsExactly("ROLE01", "ROLE02");
    }

    @Test
    void updateRoleInfoListOnlyDeletesWhenRegYnIsNotY() {
        when(repository.existsById(any())).thenReturn(false);

        String[] authorCodes = {"AUTH01"};
        String[] roleCodes = {"ROLE01"};
        String[] regYns = {"N"};

        service.updateRoleInfoList(authorCodes, roleCodes, regYns, new AuthorRoleRelatedVO());

        verify(repository, times(1)).delete(any());
        verify(repository, never()).save(any());
    }
}
