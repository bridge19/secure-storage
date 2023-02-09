package io.bridge.secure.storage.plugin;

import com.baomidou.mybatisplus.core.toolkit.PluginUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import io.bridge.secure.storage.plugin.processor.IStatementProcessor;
import io.bridge.secure.storage.plugin.processor.StatementProcessor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;

public class MybatisCryptoInnerInterceptor implements InnerInterceptor,IStatementChecker {

  private IStatementProcessor statementProcessor = StatementProcessor.getInstance();
  @Override
  public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    String statementId = ms.getId();
    if(statementId.endsWith("_QUERY_ID") || !needCrypto(statementId)){
      return;
    }
    long start = System.currentTimeMillis();
    statementProcessor.process(executor,ms, parameter, boundSql);
    logger.info("parse sql time cost: " + (System.currentTimeMillis() - start));
  }


}
