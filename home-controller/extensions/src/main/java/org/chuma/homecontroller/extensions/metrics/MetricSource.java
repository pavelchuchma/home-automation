package org.chuma.homecontroller.extensions.metrics;

/**
 * Something sampled once per reporter tick. Write nothing when the state is unknown, so a device outage
 * shows up as a gap in Grafana instead of a run of stale values.
 */
public interface MetricSource {
    void sample(LineProtocolWriter writer, long nowMs);
}
