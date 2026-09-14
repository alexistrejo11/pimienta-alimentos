package io.github.alexistrejo11.pimienta.module.inventory.infrastructure.adapter.output.persistence.entity;
import jakarta.persistence.*; import java.time.LocalDateTime;
@Entity @Table(name="inventory_count_responses")
public class InventoryCountResponseJpaEntity {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id; @Column(name="session_id",nullable=false) Long sessionId; @Column(name="item_id",nullable=false) Long itemId;
 int expectedQuantity; Integer countedQuantity,variance; LocalDateTime countedAt;
 public Long getId(){return id;} public void setId(Long v){id=v;} public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;} public Long getItemId(){return itemId;} public void setItemId(Long v){itemId=v;}
 public int getExpectedQuantity(){return expectedQuantity;} public void setExpectedQuantity(int v){expectedQuantity=v;} public Integer getCountedQuantity(){return countedQuantity;} public void setCountedQuantity(Integer v){countedQuantity=v;} public Integer getVariance(){return variance;} public void setVariance(Integer v){variance=v;} public LocalDateTime getCountedAt(){return countedAt;} public void setCountedAt(LocalDateTime v){countedAt=v;}
}
