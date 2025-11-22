package dinkplugin.notifiers.data;

import dinkplugin.message.Field;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@EqualsAndHashCode(callSuper = false)
public class PetNotificationData extends NotificationData {

    /**
     * The name of the pet.
     * <p>
     * This field is null when the name cannot be read from untradeable drop, collection log, or clan chat notification.
     */
    @Nullable
    String petName;

    /**
     * The milestone (e.g., skill XP, boss KC) at which this pet was acquiried.
     * <p>
     * This field relies upon clan chat notifications.
     */
    @Nullable
    String milestone;

    /**
     * Whether this pet is (currently) already owned.
     */
    boolean duplicate;

    /**
     * Whether the pet is already owned or was previously owned.
     * <p>
     * This field relies upon collection log chat notifications.
     */
    @Nullable
    Boolean previouslyOwned;

    /**
     * The approximate drop rate of the pet.
     * <p>
     * This value is least accurate for skilling pets and raids.
     */
    @Nullable
    Double rarity;

    /**
     * The approximate number of actions performed that would roll a drop table containing the pet.
     * <p>
     * This value is least accurate for skilling pets and pets dropped by multiple NPCs.
     */
    @Nullable
    Integer estimatedActions;

    @Nullable
    transient Double luck;

    @Override
    public List<Field> getFields() {
        if (petName == null || petName.isEmpty())
            return super.getFields();

        List<Field> fields = new ArrayList<>(5);
        fields.add(new Field("Name", Field.formatBlock("", petName)));
        String status = getStatus();
        if (status != null)
            fields.add(new Field("Status", Field.formatBlock("", status)));
        if (milestone != null)
            fields.add(new Field("Milestone", Field.formatBlock("", milestone)));
        if (rarity != null)
            fields.add(new Field("Rarity", Field.formatProbability(rarity)));
        if (luck != null)
            fields.add(Field.ofLuck(luck));
        return fields;
    }

    @Override
    public Map<String, Object> sanitized() {
        var m = new HashMap<String, Object>();
        if (petName != null) m.put("petName", petName);
        if (milestone != null) m.put("milestone", milestone);
        m.put("duplicate", duplicate);
        if (previouslyOwned != null) m.put("previouslyOwned", previouslyOwned);
        if (rarity != null) m.put("rarity", rarity);
        if (estimatedActions != null) m.put("estimatedActions", estimatedActions);
        return m;
    }

    private String getStatus() {
        if (duplicate) return "Already owned";
        if (previouslyOwned == null) return null;
        return previouslyOwned ? "Previously owned" : "New!";
    }
}
