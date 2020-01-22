package ws.slink.model;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Setter
@Getter
@Accessors(fluent = true)
public class ProcessingResult {

    public enum ResultType {
        RT_PUB_SUCCESS, // publication success
        RT_PUB_FAILURE, // publication failure
        RT_DEL_SUCCESS, // removal success
        RT_DEL_FAILURE, // removal failure
        RT_SKIPPED      // document skipped due to being "hidden"
    }

    private Map<ResultType, AtomicInteger> results = new ConcurrentHashMap<>();

    public ProcessingResult() {
    }

    public ProcessingResult(ResultType type) {
        this.add(type);
    }

    private ProcessingResult add(ResultType key) {
        if (!results.containsKey(key))
            results.put(key, new AtomicInteger(0));
        results.get(key).addAndGet(1);
        return this;
    }

    public AtomicInteger get(ResultType key) {
        return results.getOrDefault(key, new AtomicInteger(0));
    }

    public ProcessingResult merge(ResultType other) {
        this.add(other);
        return this;
    }

    public ProcessingResult merge(ProcessingResult other) {
        Set keyset = new HashSet(this.results.keySet());
        keyset.addAll(other.results.keySet());
        keyset.stream().forEach(
            key -> {
                AtomicInteger ai = this.results.getOrDefault(key, new AtomicInteger(0));
                ai.addAndGet(other.results.getOrDefault(key, new AtomicInteger()).get());
                this.results.put((ResultType)key, ai);
            }
        );
        return this;
    }

}
