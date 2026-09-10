package com.umlpilot.umlpilot_api.domain.model;

public class Multiplicity {
    private final MultiplicityId id;
    private final RelationshipId relationshipId;
    private String end;
    private Integer min;
    private Integer max;
    private Boolean isOrdered;
    private Boolean isUnique;

    private Multiplicity(MultiplicityId id, RelationshipId relationshipId, String end, Integer min, Integer max, Boolean isOrdered, Boolean isUnique) {
        this.id = id; this.relationshipId = relationshipId; this.end = end;
        this.min = min; this.max = max; this.isOrdered = isOrdered; this.isUnique = isUnique;
    }

    public static Multiplicity create(String relationshipId, String end, Integer min, Integer max) {
        return new Multiplicity(MultiplicityId.generate(), RelationshipId.from(relationshipId), end, min, max, false, false);
    }

    public static Multiplicity reconstitute(MultiplicityId id, RelationshipId relationshipId, String end, Integer min, Integer max, Boolean isOrdered, Boolean isUnique) {
        return new Multiplicity(id, relationshipId, end, min, max, isOrdered, isUnique);
    }

    public void updateMin(Integer min) { this.min = min; }
    public void updateMax(Integer max) { this.max = max; }

    public MultiplicityId getId() { return id; }
    public RelationshipId getRelationshipId() { return relationshipId; }
    public String getEnd() { return end; }
    public Integer getMin() { return min; }
    public Integer getMax() { return max; }
    public Boolean getIsOrdered() { return isOrdered; }
    public Boolean getIsUnique() { return isUnique; }
}
