package net.dorokhov.pony2.core.library.service.discovery;

import jakarta.annotation.Nullable;
import net.dorokhov.pony2.api.library.domain.DiscoveryTask;
import net.dorokhov.pony2.common.JsonConverter;
import net.dorokhov.pony2.core.library.repository.DiscoveryTaskRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;
import static org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;

@Component
public class DiscoveryTaskExchangeRecorder {

    private final DiscoveryTaskRepository taskRepository;
    private final TransactionTemplate transactionTemplate;

    public DiscoveryTaskExchangeRecorder(DiscoveryTaskRepository taskRepository, PlatformTransactionManager transactionManager) {
        this.taskRepository = taskRepository;
        transactionTemplate = new TransactionTemplate(transactionManager, new DefaultTransactionDefinition(PROPAGATION_REQUIRES_NEW));
    }

    public void startExchange(DiscoveryTask task, Object request) {
        List<Object> requests = task.getRawRequests() != null ? new ArrayList<>(task.getRawRequests()) : new ArrayList<>();
        List<Object> responses = task.getRawResponses() != null ? new ArrayList<>(task.getRawResponses()) : new ArrayList<>();
        requests.add(request);
        responses.add(null);
        save(task, requests, responses);
    }

    private void save(DiscoveryTask task, List<Object> requests, List<Object> responses) {
        transactionTemplate.executeWithoutResult(status -> taskRepository.save(task
                .setRawRequests(requests)
                .setRawResponses(responses)));
    }

    /**
     * Updates the same task in memory; the next history or status save persists the effective request.
     */
    public void replaceLastRequest(DiscoveryTask task, Object request) {
        List<Object> requests = requireNonNull(task.getRawRequests());
        requests.set(requests.size() - 1, request);
    }

    public void completeExchange(DiscoveryTask task, @Nullable String response) {
        List<Object> requests = requireNonNull(task.getRawRequests());
        List<Object> responses = requireNonNull(task.getRawResponses());
        // Task finalization must retain this response if the history transaction fails.
        responses.set(responses.size() - 1, JsonConverter.fromJsonOrString(response));
        save(task, requests, responses);
    }

    public void completeExchange(DiscoveryTask task, String requestUrl, String response) {
        List<Object> requests = task.getRawRequests() != null ? new ArrayList<>(task.getRawRequests()) : new ArrayList<>();
        List<Object> responses = task.getRawResponses() != null ? new ArrayList<>(task.getRawResponses()) : new ArrayList<>();
        checkState(!requests.isEmpty() && requestUrl.equals(requests.getLast()), "MusicBrainz exchanges must be sequential.");
        responses.set(responses.size() - 1, JsonConverter.fromJsonOrString(response));
        save(task, requests, responses);
    }
}
