package fuud.stat;

import com.codahale.metrics.MetricRegistry;
import org.springframework.stereotype.Component;

@Component
public class Statistics {
    private final MetricRegistry metrics = new MetricRegistry();
}
