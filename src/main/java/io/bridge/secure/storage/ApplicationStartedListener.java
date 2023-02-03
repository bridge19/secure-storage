package io.bridge.secure.storage;

import io.bridge.secure.storage.indextable.initiate.Initiator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;

@Slf4j
public class ApplicationStartedListener implements ApplicationListener<ApplicationStartedEvent> {
  @Autowired
  @Qualifier(value="indexTableValueInitiator")
  private Initiator initiator;
  @Override
  public void onApplicationEvent(ApplicationStartedEvent event) {
    log.info("start checking index tables creation...");
    long start = System.currentTimeMillis();
    initiator.process();
    log.info("finish checking index tables creation, cost: {}",(System.currentTimeMillis()-start));
  }


}
