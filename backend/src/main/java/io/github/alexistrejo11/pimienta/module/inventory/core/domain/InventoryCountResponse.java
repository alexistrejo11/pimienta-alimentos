package io.github.alexistrejo11.pimienta.module.inventory.core.domain;
import java.time.LocalDateTime;
public class InventoryCountResponse {
  private Long id,itemId; private int expectedQuantity; private Integer countedQuantity,variance; private LocalDateTime countedAt;
  public InventoryCountResponse() {} public InventoryCountResponse(long itemId,int expected) { this.itemId=itemId; this.expectedQuantity=expected; }
  public Long getId(){return id;} public void setId(Long v){id=v;} public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
  public int getExpectedQuantity(){return expectedQuantity;} public void setExpectedQuantity(int v){expectedQuantity=v;} public Integer getCountedQuantity(){return countedQuantity;} public void setCountedQuantity(Integer v){countedQuantity=v;}
  public Integer getVariance(){return variance;} public void setVariance(Integer v){variance=v;} public LocalDateTime getCountedAt(){return countedAt;} public void setCountedAt(LocalDateTime v){countedAt=v;}
}
