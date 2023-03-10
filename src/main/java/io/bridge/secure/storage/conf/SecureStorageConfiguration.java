package io.bridge.secure.storage.conf;

import io.bridge.secure.storage.ApplicationStartedListener;
import io.bridge.secure.storage.indextable.delete.IdxTableValueDeleter;
import io.bridge.secure.storage.indextable.initiate.IndexTableInitiator;
import io.bridge.secure.storage.plugin.MybatisCryptoInnerInterceptor;
import io.bridge.secure.storage.plugin.MybatisSelectResultInterceptor;
import io.bridge.secure.storage.scanner.EntityPackageScanner;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@Configuration
@ComponentScan(basePackages = "io.bridge.secure.storage")
@MapperScan("io.bridge.secure.storage.indextable.mapper")
public class SecureStorageConfiguration {

  @Bean(name = "encryptTableColumn", initMethod = "scanPackages")
  public EntityPackageScanner getEntityScanner(){
    return new EntityPackageScanner();
  }
  @Bean(name = "indexTableValueInitiator")
  public IndexTableInitiator getIndexTableInitiator(){
    return new IndexTableInitiator();
  }
  @Bean(name = "cryptoInnerInterceptor")
  public MybatisCryptoInnerInterceptor resultHandlerInterceptor(){
    return new MybatisCryptoInnerInterceptor();
  }
  @Bean
  public ApplicationStartedListener getListener()
  {
    return new ApplicationStartedListener();
  }
  @Bean
  public MybatisSelectResultInterceptor mybatisSelectResultInterceptor(){
    return new MybatisSelectResultInterceptor();
  }
  @Bean
  public IdxTableValueDeleter idxTableValueDeleter(){
    return new IdxTableValueDeleter();
  }
}
