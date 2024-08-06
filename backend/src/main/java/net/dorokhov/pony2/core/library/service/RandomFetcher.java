package net.dorokhov.pony2.core.library.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class RandomFetcher {
    
    public interface Repository<T> {
        
        long fetchCount();
        
        List<T> fetchContent(Pageable pageable);
    }

    public <T> List<T> fetch(int count, Repository<T> repository) {

        ArrayList<T> result = new ArrayList<>();
        Random random = new Random();

        long totalCount = repository.fetchCount();
        while (result.size() < count) {
            int pageIndex = random.nextInt((int) Math.min(totalCount, Integer.MAX_VALUE));
            List<T> page = repository.fetchContent(PageRequest.of(pageIndex, 1));
            if (!page.isEmpty()) {
                result.add(page.getFirst());
            } else {
                totalCount = repository.fetchCount();
                if (totalCount == 0) {
                    return new ArrayList<>();
                }
            }
        }
        return result;
    } 
}
