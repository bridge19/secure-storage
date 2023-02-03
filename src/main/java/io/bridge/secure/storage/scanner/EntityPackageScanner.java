package io.bridge.secure.storage.scanner;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.bridge.secure.storage.annotation.entity.EncryptionClass;
import io.bridge.secure.storage.annotation.entity.EncryptionField;
import io.bridge.secure.storage.annotation.entity.Id;
import io.bridge.secure.storage.annotation.entity.LogicDelete;
import io.bridge.secure.storage.enums.Algorithm;
import io.bridge.secure.storage.indextable.IndexTableInfo;
import io.bridge.secure.storage.indextable.IndexTableInfoRepository;
import io.bridge.secure.storage.util.SpringResourceScanner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class EntityPackageScanner implements IEntityPackageScanner {

  @Value("${secure.storage.entity-packages}")
  private String entityPackages;
  @Override
  public String[] packages() {
    return this.entityPackages.split(";");
  }

  public void scanPackages() {
    String[] entityPackages = packages();
    SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();
    ClassLoader classLoader = SimpleMetadataReaderFactory.class.getClassLoader();
    for(String basePackage: entityPackages){

      log.debug("scan entity package: " + basePackage);
      Resource[] resources = SpringResourceScanner.scanPackage(basePackage);
      if(resources == null || resources.length==0){
        continue;
      }
      for(Resource resource : resources) {
        try {
          MetadataReader metadataReader = factory.getMetadataReader(resource);
          AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
          boolean hasAnnotation =annotationMetadata.hasAnnotation(EncryptionClass.class.getName());
          if(hasAnnotation){
            Class<?> clazz = classLoader.loadClass(annotationMetadata.getClassName());
            prepareEncryptColumns(clazz);
          }
        } catch (ClassNotFoundException e){
          log.warn("load resource error. class not found.",e);
        } catch (IOException e) {
          log.warn("load resource error. class not found."+ resource.getFilename());
        }
      }
    }
  }

  private void prepareEncryptColumns(Class<?> clazz){
    EncryptionClass annotation = clazz.getAnnotation(EncryptionClass.class);
    CryptoTableInfo cryptoTableInfo = new CryptoTableInfo();
    cryptoTableInfo.setJavaClass(clazz);
    String tableName = annotation.value();
    cryptoTableInfo.setTableName(tableName);
    Field[] fields = clazz.getDeclaredFields();
    List<String> columns = new ArrayList<>();
    Map<String, CryptoColumnInfo> cryptoColumnInfoMap = new HashMap<>();
    cryptoTableInfo.setAllEncryptColumns(columns);
    cryptoTableInfo.setCryptoColumnInfoMap(cryptoColumnInfoMap);
    for(Field field : fields){
      Annotation[] annotations = field.getAnnotations();
      for(Annotation anno : annotations) {
        if(anno instanceof Id) {
          Id idAnno = (Id)anno;
          cryptoTableInfo.setIdColumnName(idAnno.value());
          cryptoTableInfo.setIdFieldName(field.getName());
        }else if(anno instanceof EncryptionField) {
          EncryptionField encryptionField = (EncryptionField)anno;
          String columnName = encryptionField.columnName();
          Class<?> tokenizer = encryptionField.getTokenizer();
          Class<?> cryptor = encryptionField.getCryptor();
          columns.add(columnName);
          cryptoColumnInfoMap.put(columnName, new CryptoColumnInfo(encryptionField.fuzzySearch(), field.getName(), columnName, tokenizer, cryptor));
        }else if(anno instanceof LogicDelete){
          LogicDelete logicDelete =(LogicDelete)anno;
          cryptoTableInfo.setLogicDelete(true);
          cryptoTableInfo.setDeleteColumnName(logicDelete.value());
          cryptoTableInfo.setDeleteFieldName(field.getName());
        }
      }
    }
    if(columns.size()==0){
      return;
    }
    CryptoTableInfoRepository.storeCryptoTableInfo(cryptoTableInfo);

    //模糊查询的字段需要建立索引表
    cryptoColumnInfoMap.values().stream().filter(item->item.isFuzzy()).forEach(item ->{
      IndexTableInfo indexTableInfo = new IndexTableInfo();
      String refTableName = cryptoTableInfo.getTableName();
      String columnName = item.getColumnName();
      String idxTableName = IndexTableInfoRepository.getIndexTableName(refTableName,columnName);
      indexTableInfo.setRefTableName(refTableName);
      indexTableInfo.setRefTableIdColumnName(cryptoTableInfo.getIdColumnName());
      indexTableInfo.setColumnName(columnName);
      indexTableInfo.setTableName(idxTableName);
      IndexTableInfoRepository.storeIndexTableInfo(indexTableInfo);
    });
  }
}
