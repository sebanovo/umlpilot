package com.umlpilot.umlpilot_api.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "multiplicity")
public class MultiplicityJpaEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) @Column(name = "multiplicity_id") private String id;
    @Column(name = "relationship_id", nullable = false) private String relationshipId;
    @Column(name = "multiplicity_end", nullable = false) private String end;
    private Integer min;
    private Integer max;
    @Column(name = "is_ordered") private Boolean isOrdered;
    @Column(name = "is_unique") private Boolean isUnique;

    public MultiplicityJpaEntity() {}
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getRelationshipId() { return relationshipId; }
    public void setRelationshipId(String relationshipId) { this.relationshipId = relationshipId; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    public Integer getMin() { return min; }
    public void setMin(Integer min) { this.min = min; }
    public Integer getMax() { return max; }
    public void setMax(Integer max) { this.max = max; }
    public Boolean getIsOrdered() { return isOrdered; }
    public void setIsOrdered(Boolean isOrdered) { this.isOrdered = isOrdered; }
    public Boolean getIsUnique() { return isUnique; }
    public void setIsUnique(Boolean isUnique) { this.isUnique = isUnique; }
}
