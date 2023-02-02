package io.bridge.secure.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;

@Slf4j
public class SpringResourceScanner {

  public static Resource[] scanPackage(String basePackage){
    PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
    String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
            basePackage.replaceAll("\\.","/") + '/' + "**/*.class";
    Resource[] resources = null;
    try {
      resources = pathMatchingResourcePatternResolver.getResources(packageSearchPath);
    } catch (IOException e) {
      log.warn("load resource error. base package: " +basePackage);
    }
    return resources;
  }
}
