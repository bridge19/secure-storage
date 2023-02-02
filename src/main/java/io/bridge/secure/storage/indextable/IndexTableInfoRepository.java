package io.bridge.secure.storage.indextable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IndexTableInfoRepository {
  private static final String INDEX_PREFIX = "idx_";
  private static Map<String, IndexTableInfo> indexTableInfoMap = new HashMap<>();

  public static void storeIndexTableInfo(IndexTableInfo indexTableInfo){
    indexTableInfoMap.putIfAbsent(indexTableInfo.getTableName(),indexTableInfo);
  }

  public static Set<String> allIndexTableName(){
    return indexTableInfoMap.keySet();
  }

  public static String getIndexTableName(String refTableName, String columnName){
    return INDEX_PREFIX +refTableName+"_"+columnName;
  }

  public static IndexTableInfo getIndexTableInfo(String idxTableName){
    return indexTableInfoMap.get(idxTableName);
  }
}
