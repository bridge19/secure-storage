package io.bridge.secure.storage.indextable.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class IndexTableDeletedValue {

  private Long id;
  private String tableName;
  private Long idValue;
  private LocalDateTime createTime;
}
