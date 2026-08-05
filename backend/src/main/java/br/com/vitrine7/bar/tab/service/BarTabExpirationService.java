package br.com.vitrine7.bar.tab.service;

import br.com.vitrine7.bar.tab.repository.BarTabRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BarTabExpirationService {

    private final BarTabRepository tabRepository;

    @Transactional
    public int reopenExpiredTabs() {
        return tabRepository.reopenReleasedOrExpiredTabs();
    }
}
