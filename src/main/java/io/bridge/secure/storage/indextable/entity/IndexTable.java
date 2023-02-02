package io.bridge.secure.storage.indextable.entity;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.bridge.secure.storage.annotation.entity.Id;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class IndexTable extends Model {
  /**
   * 主键
   */
  @Id("id")
  private Long id;

  private String columnValue;

  private Long refTableIdColumnValue;

  private Integer deleted;

  private LocalDateTime createTime;

}
