package io.bridge.secure.storage.plugin.processor.parser;

import org.apache.ibatis.session.Configuration;

public interface IStatementParser extends StatementInfoAccessor{
  void parse();
  void enhanceWhere(Configuration configuration);
}
