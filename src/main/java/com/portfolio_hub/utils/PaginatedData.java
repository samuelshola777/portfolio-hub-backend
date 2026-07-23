package com.portfolio_hub.utils;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginatedData<T>(
  List<T> items,
  int currentPage,
  int pageSize,
  long totalItems,
  int totalPages,
  boolean hasNext,
  boolean hasPrevious
) {
  public static <S, T> PaginatedData<T> from(
    Page<S> page,
    java.util.function.Function<S, T> mapper
  ) {
    return new PaginatedData<>(
      page.getContent().stream().map(mapper).toList(),
      page.getNumber() + 1,
      page.getSize(),
      page.getTotalElements(),
      page.getTotalPages(),
      page.hasNext(),
      page.hasPrevious()
    );
  }
}
