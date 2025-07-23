package io.argus.collector.repository;

import io.argus.common.entity.Span;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpanRepository extends JpaRepository<Span, String> { }