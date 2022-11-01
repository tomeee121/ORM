package teamblue.classes;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import teamblue.annotations.Column;
import teamblue.annotations.Entity;
import teamblue.annotations.Id;

@Entity
@Getter
@EqualsAndHashCode
public class StringId {
    @Id
    String name;
    @Column("last_names")
    String lastName;

    public StringId(){}

    public StringId(String name, String lastName) {
        this.name = name;
        this.lastName = lastName;
    }
}
