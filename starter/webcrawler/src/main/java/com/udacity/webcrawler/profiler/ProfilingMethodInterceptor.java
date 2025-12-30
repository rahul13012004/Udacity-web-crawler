package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Object delegate;
  private final Clock clock;
  private final ProfilingState state;

  ProfilingMethodInterceptor(Object delegate, Clock clock, ProfilingState state) {
    this.delegate = Objects.requireNonNull(delegate);
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

    // Handle Object methods normally (equals, hashCode, toString)
    if (method.getDeclaringClass() == Object.class) {
      return method.invoke(delegate, args);
    }

    boolean profiled = method.isAnnotationPresent(Profiled.class);
    Instant start = null;

    if (profiled) {
      start = clock.instant();
    }

    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException e) {
      // Rethrow the ORIGINAL exception
      throw e.getCause();
    } finally {
      if (profiled) {
        Duration duration = Duration.between(start, clock.instant());
        state.record(
                delegate.getClass(),   // concrete implementation class
                method,
                duration);
      }
    }
  }


  private void record(Method method, Instant start) {
    Duration duration = Duration.between(start, clock.instant());
    state.record(
            delegate.getClass(),
            method,
            duration);

  }
}
